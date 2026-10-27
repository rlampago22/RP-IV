package br.edu.unipampa.usina.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** RNF08 — quem tem passkey cadastrada só entra depois de confirmar no celular. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:login-biometria;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "app.bootstrap-password=senha-de-teste",
})
@AutoConfigureMockMvc
@Transactional
class LoginBiometriaTest {
    private static final String SENHA = "senha-de-teste";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired CredencialBiometricaRepository credenciais;

    @Test
    void semBiometriaCadastradaOLoginContinuaSoComSenha() throws Exception {
        MockHttpSession sessao = (MockHttpSession) login("operador@usina.local")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("operador@usina.local"))
            .andExpect(jsonPath("$.biometriaCadastrada").value(false))
            .andReturn().getRequest().getSession(false);

        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isOk());
    }

    @Test
    void comBiometriaCadastradaASenhaSozinhaNaoAbreASessao() throws Exception {
        cadastrarPasskeyFalsa("supervisao@usina.local");

        MvcResult resultado = login("supervisao@usina.local")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etapa").value("BIOMETRIA"))
            .andExpect(jsonPath("$.opcoes.challenge").isNotEmpty())
            .andExpect(jsonPath("$.opcoes.userVerification").value("required"))
            .andExpect(jsonPath("$.opcoes.allowCredentials[0].id").value("Y3JlZGVuY2lhbC10ZXN0ZQ"))
            .andReturn();
        MockHttpSession sessao = (MockHttpSession) resultado.getRequest().getSession(false);

        mvc.perform(get("/api/auth/me").session(sessao))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void assinaturaInvalidaEhRecusadaEExigeNovoLogin() throws Exception {
        cadastrarPasskeyFalsa("admin@usina.local");
        MockHttpSession sessao = (MockHttpSession) login("admin@usina.local")
            .andReturn().getRequest().getSession(false);

        String respostaForjada = """
            {"id":"Y3JlZGVuY2lhbC10ZXN0ZQ","rawId":"Y3JlZGVuY2lhbC10ZXN0ZQ","type":"public-key",
             "response":{"clientDataJSON":"e30","authenticatorData":"AAAA","signature":"AAAA"},
             "clientExtensionResults":{}}""";
        mvc.perform(post("/api/auth/login/biometria").session(sessao)
                .contentType(MediaType.APPLICATION_JSON).content(respostaForjada))
            .andExpect(status().isUnauthorized());
        // O desafio foi consumido: repetir a tentativa não serve, tem que voltar para a senha.
        mvc.perform(post("/api/auth/login/biometria").session(sessao)
                .contentType(MediaType.APPLICATION_JSON).content(respostaForjada))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensagem").value("Entre com e-mail e senha antes de confirmar a biometria."));
        mvc.perform(get("/api/auth/me").session(sessao))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void cadastroDeBiometriaExigeEstarLogado() throws Exception {
        mvc.perform(post("/api/auth/biometria/cadastro/opcoes"))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void usuarioLogadoRecebeOpcoesDeCadastroExigindoBiometria() throws Exception {
        MockHttpSession sessao = (MockHttpSession) login("operador@usina.local")
            .andReturn().getRequest().getSession(false);

        mvc.perform(post("/api/auth/biometria/cadastro/opcoes").session(sessao))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rp.id").value("localhost"))
            .andExpect(jsonPath("$.user.name").value("operador@usina.local"))
            .andExpect(jsonPath("$.authenticatorSelection.userVerification").value("required"));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"));
    }

    private void cadastrarPasskeyFalsa(String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        // "credencial-teste" em base64url; a chave é irrelevante porque nenhuma assinatura válida é enviada.
        credenciais.save(new CredencialBiometrica(usuario, "Y3JlZGVuY2lhbC10ZXN0ZQ", "aGFuZGxl", "AA", 0, "Celular de teste"));
    }
}
