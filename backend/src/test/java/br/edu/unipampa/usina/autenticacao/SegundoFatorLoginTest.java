package br.edu.unipampa.usina.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF18 / UC05 (spec do Álvaro) — 2FA por celular no login — e issue #81: toda etapa do 2FA entra na
 * trilha de auditoria encadeada, sem o código.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:segundo-fator;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "app.bootstrap-password=senha-de-teste",
    "app.segundo-fator.login=true",
})
@AutoConfigureMockMvc
@Transactional
class SegundoFatorLoginTest {
    private static final String OPERADOR = "operador@usina.local";

    @Autowired MockMvc mvc;
    @Autowired CelularSimulado celular;
    @Autowired AuditoriaAutenticacao auditoria;

    @Test
    void senhaCertaNaoAbreSessaoSemOSegundoFator() throws Exception {
        MockHttpSession sessao = loginComSenha();
        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isForbidden()); // sem sessao autenticada
    }

    @Test
    void codigoCertoDoCelularAbreASessao() throws Exception {
        MockHttpSession sessao = loginComSenha();
        mvc.perform(codigo(sessao, ultimaSolicitacao().getCodigo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(OPERADOR));
        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isOk());
    }

    @Test
    void codigoErradoNaoAbreSessaoEBloqueiaDepoisDeCincoErros() throws Exception {
        MockHttpSession sessao = loginComSenha();
        String certo = ultimaSolicitacao().getCodigo();
        String errado = certo.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < SegundoFatorService.MAX_TENTATIVAS; i++) {
            mvc.perform(codigo(sessao, errado)).andExpect(status().isUnauthorized());
        }
        // Mesmo o código certo não vale mais: a tentativa foi bloqueada.
        mvc.perform(codigo(sessao, certo)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isForbidden()); // sem sessao autenticada
    }

    @Test
    void aprovarNoCelularAbreASessaoUmaUnicaVez() throws Exception {
        MockHttpSession sessao = loginComSenha();
        String id = ultimaSolicitacao().getId();
        mvc.perform(post("/api/auth/segundo-fator/aprovacao").session(sessao)).andExpect(status().isUnauthorized()); // ainda pendente
        mvc.perform(post("/api/celular-simulado/{id}/aprovar", id)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/segundo-fator/aprovacao").session(sessao)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isOk());
        // Uso único: a mesma aprovação não serve para outra sessão.
        mvc.perform(post("/api/celular-simulado/{id}/aprovar", id)).andExpect(status().isConflict());
    }

    @Test
    void recusarNoCelularNegaOAcesso() throws Exception {
        MockHttpSession sessao = loginComSenha();
        mvc.perform(post("/api/celular-simulado/{id}/recusar", ultimaSolicitacao().getId())).andExpect(status().isOk());
        mvc.perform(post("/api/auth/segundo-fator/aprovacao").session(sessao)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isForbidden()); // sem sessao autenticada
    }

    @Test
    void cadaEtapaDo2faVaiParaATrilhaEncadeadaSemOCodigo() throws Exception {
        MockHttpSession sessao = loginComSenha();
        String codigo = ultimaSolicitacao().getCodigo();
        mvc.perform(codigo(sessao, codigo.equals("000000") ? "111111" : "000000")).andExpect(status().isUnauthorized());
        mvc.perform(codigo(sessao, codigo)).andExpect(status().isOk());

        List<String> resultados = auditoria.todos().stream().map(RegistroAutenticacao::getResultado).toList();
        assertThat(resultados).containsSubsequence(
            "SENHA_OK_AGUARDANDO_2FA", "2FA_SOLICITACAO_ENVIADA", "2FA_CODIGO_INVALIDO", "2FA_SUCESSO_CODIGO");
        assertThat(auditoria.todos()).allSatisfy(r -> {
            assertThat(r.getResultado()).doesNotContain(codigo);
            assertThat(r.getHash()).hasSize(64);
        });
        assertThat(auditoria.primeiroRegistroAdulterado()).isNull();

        mvc.perform(get("/api/admin/auditoria/autenticacao").with(user("admin@usina.local").roles("ADMINISTRADOR_SISTEMA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.integra").value(true));
    }

    private MockHttpSession loginComSenha() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + OPERADOR + "\",\"senha\":\"senha-de-teste\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.segundoFator").value(true))
            .andExpect(jsonPath("$.codigo").doesNotExist())
            .andReturn().getRequest().getSession(false);
    }

    private SegundoFatorService.Desafio ultimaSolicitacao() {
        return celular.recebidas().get(0);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder codigo(MockHttpSession sessao, String codigo) {
        return post("/api/auth/segundo-fator/codigo").session(sessao)
            .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"" + codigo + "\"}");
    }
}
