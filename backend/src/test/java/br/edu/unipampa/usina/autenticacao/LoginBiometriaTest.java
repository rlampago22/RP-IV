package br.edu.unipampa.usina.autenticacao;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * RNF08 — login com senha; a digital é o segundo fator das ações críticas (4.2.10 do PDF) e só é
 * cadastrada com o código de ativação emitido pelo Administrador (UC02, "previamente cadastrado").
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:login-biometria;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "app.bootstrap-password=senha-de-teste",
    "app.segundo-fator.login=false",
})
@AutoConfigureMockMvc
@Transactional
class LoginBiometriaTest {
    private static final String SENHA = "senha-de-teste";
    private static final String CREDENCIAL_FALSA = "Y3JlZGVuY2lhbC10ZXN0ZQ"; // "credencial-teste" em base64url
    // Mesmo atributo que ConfirmacaoBiometrica grava ao conferir a digital.
    private static final String CONFIRMADA_ATE = "biometria.confirmada.ate";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired CredencialBiometricaRepository credenciais;
    @Autowired CodigoAtivacaoService codigos;

    // ---- Login ----

    @Test
    void loginComSenhaAbreASessaoSemPedirDigital() throws Exception {
        MockHttpSession sessao = sessaoDe(login("operador@usina.local")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("operador@usina.local")));

        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isOk());
    }

    @Test
    void senhaErradaEhRecusada() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"operador@usina.local\",\"senha\":\"errada\"}"))
            .andExpect(status().isUnauthorized());
    }

    // ---- Ações críticas ----

    @Test
    void acaoCriticaSemDigitalConfirmadaPedeConfirmacao() throws Exception {
        mvc.perform(comoAdmin(post("/api/admin/usuarios/{id}/codigo-ativacao", idDe("operador@usina.local"))))
            .andExpect(status().isPreconditionRequired())
            .andExpect(jsonPath("$.exigeBiometria").value(true));
    }

    @Test
    void acaoCriticaComDigitalConfirmadaHaPoucoPassa() throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute(CONFIRMADA_ATE, Instant.now().plus(ConfirmacaoBiometrica.JANELA));

        mvc.perform(comoAdmin(post("/api/admin/usuarios/{id}/codigo-ativacao", idDe("operador@usina.local"))).session(sessao))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("operador@usina.local"))
            .andExpect(jsonPath("$.codigo").value(org.hamcrest.Matchers.matchesPattern("[A-Z2-9]{4}-[A-Z2-9]{4}")));
    }

    @Test
    void confirmacaoVencidaPedeDigitalDeNovo() throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute(CONFIRMADA_ATE, Instant.now().minusSeconds(1));

        mvc.perform(comoAdmin(post("/api/admin/usuarios/{id}/biometria/revogar", idDe("operador@usina.local"))).session(sessao))
            .andExpect(status().isPreconditionRequired());
    }

    @Test
    void soOAdministradorGeraCodigo() throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute(CONFIRMADA_ATE, Instant.now().plus(ConfirmacaoBiometrica.JANELA));

        mvc.perform(post("/api/admin/usuarios/{id}/codigo-ativacao", idDe("operador@usina.local"))
                .with(user("supervisao@usina.local").roles("SUPERVISAO_CENTRAL")).session(sessao))
            .andExpect(status().isForbidden());
    }

    @Test
    void quemNaoTemDigitalCadastradaEhOrientadoACadastrar() throws Exception {
        mvc.perform(comoAdmin(post("/api/auth/biometria/confirmacao/opcoes")))
            .andExpect(status().isConflict());
    }

    @Test
    void quemTemDigitalRecebeODesafioExigindoVerificacao() throws Exception {
        cadastrarDigitalFalsa("admin@usina.local");

        mvc.perform(comoAdmin(post("/api/auth/biometria/confirmacao/opcoes")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.challenge").isNotEmpty())
            .andExpect(jsonPath("$.userVerification").value("required"))
            .andExpect(jsonPath("$.allowCredentials[0].id").value(CREDENCIAL_FALSA));
    }

    @Test
    void assinaturaForjadaNaoLiberaAcaoCritica() throws Exception {
        cadastrarDigitalFalsa("admin@usina.local");
        MockHttpSession sessao = new MockHttpSession();
        mvc.perform(comoAdmin(post("/api/auth/biometria/confirmacao/opcoes")).session(sessao)).andExpect(status().isOk());

        String respostaForjada = """
            {"id":"%s","rawId":"%s","type":"public-key",
             "response":{"clientDataJSON":"e30","authenticatorData":"AAAA","signature":"AAAA"},
             "clientExtensionResults":{}}""".formatted(CREDENCIAL_FALSA, CREDENCIAL_FALSA);
        mvc.perform(comoAdmin(post("/api/auth/biometria/confirmacao")).session(sessao)
                .contentType(MediaType.APPLICATION_JSON).content(respostaForjada))
            .andExpect(status().isUnauthorized());
        mvc.perform(comoAdmin(post("/api/admin/usuarios/{id}/codigo-ativacao", idDe("operador@usina.local"))).session(sessao))
            .andExpect(status().isPreconditionRequired());
    }

    // ---- Cadastro da digital com código de ativação ----

    @Test
    void cadastroSemCodigoValidoEhRecusado() throws Exception {
        mvc.perform(comoOperador(post("/api/auth/biometria/cadastro/opcoes"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"AAAA-AAAA\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void codigoCertoLiberaOCadastroDaDigital() throws Exception {
        String codigo = emitirCodigo("operador@usina.local");

        mvc.perform(comoOperador(post("/api/auth/biometria/cadastro/opcoes"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"" + codigo.toLowerCase() + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rp.id").value("localhost"))
            .andExpect(jsonPath("$.user.name").value("operador@usina.local"))
            .andExpect(jsonPath("$.authenticatorSelection.userVerification").value("required"));
    }

    @Test
    void codigoDeOutroFuncionarioNaoServe() throws Exception {
        String codigoDaSupervisao = emitirCodigo("supervisao@usina.local");

        mvc.perform(comoOperador(post("/api/auth/biometria/cadastro/opcoes"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"" + codigoDaSupervisao + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void codigoNovoInvalidaOAnterior() throws Exception {
        String antigo = emitirCodigo("operador@usina.local");
        emitirCodigo("operador@usina.local");

        mvc.perform(comoOperador(post("/api/auth/biometria/cadastro/opcoes"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"" + antigo + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void cincoCodigosErradosBloqueiamOCodigoVigente() throws Exception {
        String certo = emitirCodigo("operador@usina.local");
        MockHttpSession sessao = new MockHttpSession();

        for (int i = 0; i < 4; i++) {
            cadastroComCodigo(sessao, "AAAA-AAAA").andExpect(status().isBadRequest());
        }
        cadastroComCodigo(sessao, "AAAA-AAAA").andExpect(status().isTooManyRequests());
        // Depois do bloqueio, nem o código certo serve: só um novo, emitido pelo Administrador.
        cadastroComCodigo(sessao, certo).andExpect(status().isBadRequest());
    }

    @Test
    void removerDigitalTambemEhAcaoCritica() throws Exception {
        CredencialBiometrica digital = cadastrarDigitalFalsa("operador@usina.local");

        mvc.perform(comoOperador(post("/api/auth/biometria/{id}/remover", digital.getId())))
            .andExpect(status().isPreconditionRequired());
    }

    /** Só para desenvolvimento sem leitor de digital: com a política desligada, ações críticas não pedem a digital. */
    @Nested
    @SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:biometria-desligada;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "app.bootstrap-password=senha-de-teste",
        "app.segundo-fator.login=false",
        "app.biometria.acoes-criticas=false",
    })
    @AutoConfigureMockMvc
    class ComAcoesCriticasDesprotegidas {
        @Autowired MockMvc mvcDesprotegido;
        @Autowired UsuarioRepository usuariosDesprotegido;

        @Test
        void administradorGeraCodigoSemDigital() throws Exception {
            Long operador = usuariosDesprotegido.findByEmailIgnoreCase("operador@usina.local").orElseThrow().getId();
            mvcDesprotegido.perform(post("/api/admin/usuarios/{id}/codigo-ativacao", operador)
                    .with(user("admin@usina.local").roles("ADMINISTRADOR_SISTEMA")))
                .andExpect(status().isCreated());
        }
    }

    private ResultActions login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"));
    }

    private ResultActions cadastroComCodigo(MockHttpSession sessao, String codigo) throws Exception {
        return mvc.perform(comoOperador(post("/api/auth/biometria/cadastro/opcoes")).session(sessao)
            .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"" + codigo + "\"}"));
    }

    private static MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder requisicao) {
        return requisicao.with(user("admin@usina.local").roles("ADMINISTRADOR_SISTEMA"));
    }

    private static MockHttpServletRequestBuilder comoOperador(MockHttpServletRequestBuilder requisicao) {
        return requisicao.with(user("operador@usina.local").roles("OPERADOR_REATOR"));
    }

    private static MockHttpSession sessaoDe(ResultActions resultado) {
        return (MockHttpSession) resultado.andReturn().getRequest().getSession(false);
    }

    private Long idDe(String email) {
        return usuarios.findByEmailIgnoreCase(email).orElseThrow().getId();
    }

    private String emitirCodigo(String email) {
        Usuario alvo = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        Usuario admin = usuarios.findByEmailIgnoreCase("admin@usina.local").orElseThrow();
        return codigos.emitir(alvo, admin).codigo();
    }

    private CredencialBiometrica cadastrarDigitalFalsa(String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        // A chave é irrelevante: nenhum teste aqui envia uma assinatura válida.
        return credenciais.save(new CredencialBiometrica(usuario, CREDENCIAL_FALSA, "aGFuZGxl", "AA", 0, "Leitor de teste"));
    }
}
