package br.edu.unipampa.usina.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

/** RNF08 — login em dois fatores obrigatório, com cadastro do celular autorizado pelo Administrador (UC02). */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:login-biometria;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "app.bootstrap-password=senha-de-teste",
})
@AutoConfigureMockMvc
@Transactional
class LoginBiometriaTest {
    private static final String SENHA = "senha-de-teste";
    private static final String CREDENCIAL_FALSA = "Y3JlZGVuY2lhbC10ZXN0ZQ"; // "credencial-teste" em base64url
    private static final ResultMatcher SEM_SESSAO =
        result -> assertThat(result.getResponse().getStatus()).isIn(401, 403);

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired CredencialBiometricaRepository credenciais;
    @Autowired CodigoAtivacaoService codigos;

    @Test
    void senhaCertaSemCelularCadastradoLevaAoPrimeiroAcessoSemAbrirSessao() throws Exception {
        MockHttpSession sessao = sessaoDe(login("operador@usina.local")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etapa").value("PRIMEIRO_ACESSO")));

        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(SEM_SESSAO);
    }

    @Test
    void comCelularCadastradoASenhaSozinhaNaoAbreASessao() throws Exception {
        cadastrarPasskeyFalsa("supervisao@usina.local");

        MockHttpSession sessao = sessaoDe(login("supervisao@usina.local")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etapa").value("BIOMETRIA"))
            .andExpect(jsonPath("$.opcoes.challenge").isNotEmpty())
            .andExpect(jsonPath("$.opcoes.userVerification").value("required"))
            .andExpect(jsonPath("$.opcoes.allowCredentials[0].id").value(CREDENCIAL_FALSA)));

        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(SEM_SESSAO);
    }

    @Test
    void assinaturaInvalidaEhRecusadaEExigeNovoLogin() throws Exception {
        cadastrarPasskeyFalsa("admin@usina.local");
        MockHttpSession sessao = sessaoDe(login("admin@usina.local"));

        String respostaForjada = """
            {"id":"%s","rawId":"%s","type":"public-key",
             "response":{"clientDataJSON":"e30","authenticatorData":"AAAA","signature":"AAAA"},
             "clientExtensionResults":{}}""".formatted(CREDENCIAL_FALSA, CREDENCIAL_FALSA);
        postJson("/api/auth/login/biometria", sessao, respostaForjada).andExpect(status().isUnauthorized());
        // O desafio foi consumido: repetir a tentativa não serve, tem que voltar para a senha.
        postJson("/api/auth/login/biometria", sessao, respostaForjada)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensagem").value("Entre com e-mail e senha antes de confirmar a biometria."));
        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(SEM_SESSAO);
    }

    @Test
    void codigoDeAtivacaoCertoLiberaOCadastroDoCelular() throws Exception {
        String codigo = emitirCodigo("operador@usina.local");
        MockHttpSession sessao = sessaoDe(login("operador@usina.local"));

        postJson("/api/auth/primeiro-acesso/opcoes", sessao, "{\"codigo\":\"" + codigo.toLowerCase() + "\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rp.id").value("localhost"))
            .andExpect(jsonPath("$.user.name").value("operador@usina.local"))
            .andExpect(jsonPath("$.authenticatorSelection.userVerification").value("required"));
        // Só o código não abre a sessão: falta cadastrar a biometria.
        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(SEM_SESSAO);
    }

    @Test
    void codigoDeOutroFuncionarioNaoServe() throws Exception {
        String codigoDaSupervisao = emitirCodigo("supervisao@usina.local");
        MockHttpSession sessao = sessaoDe(login("operador@usina.local"));

        postJson("/api/auth/primeiro-acesso/opcoes", sessao, "{\"codigo\":\"" + codigoDaSupervisao + "\"}")
            .andExpect(status().isBadRequest());
    }

    @Test
    void cincoCodigosErradosEncerramOPrimeiroAcesso() throws Exception {
        emitirCodigo("operador@usina.local");
        MockHttpSession sessao = sessaoDe(login("operador@usina.local"));

        for (int i = 0; i < 4; i++) {
            postJson("/api/auth/primeiro-acesso/opcoes", sessao, "{\"codigo\":\"AAAA-AAAA\"}")
                .andExpect(status().isBadRequest());
        }
        postJson("/api/auth/primeiro-acesso/opcoes", sessao, "{\"codigo\":\"AAAA-AAAA\"}")
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensagem").value("Muitas tentativas com código errado. Entre novamente com a senha."));
    }

    @Test
    void codigoNovoInvalidaOAnterior() throws Exception {
        String antigo = emitirCodigo("operador@usina.local");
        emitirCodigo("operador@usina.local");
        MockHttpSession sessao = sessaoDe(login("operador@usina.local"));

        postJson("/api/auth/primeiro-acesso/opcoes", sessao, "{\"codigo\":\"" + antigo + "\"}")
            .andExpect(status().isBadRequest());
    }

    @Test
    void soOAdministradorEmiteCodigoDeAtivacao() throws Exception {
        Long operador = usuarios.findByEmailIgnoreCase("operador@usina.local").orElseThrow().getId();

        mvc.perform(post("/api/admin/usuarios/{id}/codigo-ativacao", operador)
                .with(user("admin@usina.local").roles("ADMINISTRADOR_SISTEMA")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("operador@usina.local"))
            .andExpect(jsonPath("$.codigo").value(org.hamcrest.Matchers.matchesPattern("[A-Z2-9]{4}-[A-Z2-9]{4}")));
        mvc.perform(post("/api/admin/usuarios/{id}/codigo-ativacao", operador)
                .with(user("supervisao@usina.local").roles("SUPERVISAO_CENTRAL")))
            .andExpect(status().isForbidden());
    }

    @Test
    void administradorRevogaCelularPerdidoEOFuncionarioVoltaAoPrimeiroAcesso() throws Exception {
        cadastrarPasskeyFalsa("operador@usina.local");
        Long operador = usuarios.findByEmailIgnoreCase("operador@usina.local").orElseThrow().getId();

        mvc.perform(post("/api/admin/usuarios/{id}/biometria/revogar", operador)
                .with(user("admin@usina.local").roles("ADMINISTRADOR_SISTEMA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.removidos").value(1));
        login("operador@usina.local").andExpect(jsonPath("$.etapa").value("PRIMEIRO_ACESSO"));
    }

    @Test
    void funcionarioNaoRemoveOProprioUnicoCelular() throws Exception {
        CredencialBiometrica unica = cadastrarPasskeyFalsa("operador@usina.local");

        mvc.perform(post("/api/auth/biometria/{id}/remover", unica.getId())
                .with(user("operador@usina.local").roles("OPERADOR_REATOR")))
            .andExpect(status().isConflict());
    }

    @Test
    void cadastroDeOutroAparelhoExigeEstarLogado() throws Exception {
        mvc.perform(post("/api/auth/biometria/cadastro/opcoes")).andExpect(SEM_SESSAO);
        mvc.perform(post("/api/auth/primeiro-acesso/opcoes").contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"X\"}"))
            .andExpect(status().isUnauthorized());
    }

    /** Só para desenvolvimento sem celular: com a política desligada, a senha basta. */
    @Nested
    @SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:login-sem-obrigatoriedade;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "app.bootstrap-password=senha-de-teste",
        "app.webauthn.obrigatoria=false",
    })
    @AutoConfigureMockMvc
    class ComBiometriaOpcional {
        @Autowired MockMvc mvcOpcional;

        @Test
        void senhaSemCelularAbreASessao() throws Exception {
            MockHttpSession sessao = sessaoDe(mvcOpcional.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"operador@usina.local\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometriaCadastrada").value(false)));

            mvcOpcional.perform(get("/api/auth/me").session(sessao)).andExpect(status().isOk());
        }
    }

    private ResultActions login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"));
    }

    private ResultActions postJson(String url, MockHttpSession sessao, String corpo) throws Exception {
        return mvc.perform(post(url).session(sessao).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static MockHttpSession sessaoDe(ResultActions resultado) {
        return (MockHttpSession) resultado.andReturn().getRequest().getSession(false);
    }

    private String emitirCodigo(String email) {
        Usuario alvo = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        Usuario admin = usuarios.findByEmailIgnoreCase("admin@usina.local").orElseThrow();
        return codigos.emitir(alvo, admin).codigo();
    }

    private CredencialBiometrica cadastrarPasskeyFalsa(String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        // A chave é irrelevante: nenhum teste aqui envia uma assinatura válida.
        return credenciais.save(new CredencialBiometrica(usuario, CREDENCIAL_FALSA, "aGFuZGxl", "AA", 0, "Celular de teste"));
    }
}
