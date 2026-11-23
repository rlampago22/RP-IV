package br.edu.unipampa.usina.reatores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unipampa.usina.autenticacao.AuditoriaAutenticacao;
import br.edu.unipampa.usina.autenticacao.ConfirmacaoBiometrica;
import br.edu.unipampa.usina.autenticacao.RegistroAutenticacao;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issue #96 — tela Reatores: só o Administrador, ações críticas com digital, nada é excluído (só sai
 * de operação), o R-01 não sai, e as faixas são validadas antes de chegar ao núcleo.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:cadastro-reatores;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "app.bootstrap-password=senha-de-teste",
})
@AutoConfigureMockMvc
@Transactional
class CadastroReatoresTest {
    private static final String CONFIRMADA_ATE = "biometria.confirmada.ate";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuditoriaAutenticacao auditoria;

    @Test
    void listaOsCincoReatoresDaCargaInicial() throws Exception {
        mvc.perform(comoAdmin(get("/api/reatores")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5))
            .andExpect(jsonPath("$[0].id").value("R-01"))
            .andExpect(jsonPath("$[0].sensores.length()").value(4))
            .andExpect(jsonPath("$[1].sensores[2].setpoint").value(2.325));
    }

    @Test
    void soOAdministradorAcessa() throws Exception {
        mvc.perform(get("/api/reatores").with(user("operador@usina.local").roles("OPERADOR_REATOR")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/reatores")).andExpect(status().isForbidden()); // sem sessão
    }

    @Test
    void incluirSemDigitalPedeConfirmacaoENaoGrava() throws Exception {
        mvc.perform(comoAdmin(post("/api/reatores"))).andExpect(status().isPreconditionRequired());
        assertThat(totalReatores()).isEqualTo(5);
    }

    @Test
    void incluirCriaOProximoReatorComOsQuatroSensores() throws Exception {
        mvc.perform(comoAdmin(post("/api/reatores")).session(comDigital())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sensores\":[{\"sensorId\":1,\"setpoint\":300}]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("R-06"))
            .andExpect(jsonPath("$.nome").value("Reator-06"))
            .andExpect(jsonPath("$.ordem").value(6))
            .andExpect(jsonPath("$.ativo").value(true))
            .andExpect(jsonPath("$.sensores.length()").value(4))
            .andExpect(jsonPath("$.sensores[0].setpoint").value(300.0))
            .andExpect(jsonPath("$.sensores[2].limiteMaximo").value(5.0));
    }

    @Test
    void ajusteComFaixaInvalidaEhRecusado() throws Exception {
        // setpoint fora da faixa de atenção
        mvc.perform(ajustar("R-02", "{\"sensores\":[{\"sensorId\":3,\"setpoint\":4.5}]}"))
            .andExpect(status().isBadRequest());
        // atenção máxima acima do limite máximo
        mvc.perform(ajustar("R-02", "{\"sensores\":[{\"sensorId\":3,\"atencaoMaxima\":6}]}"))
            .andExpect(status().isBadRequest());
        // sensor que o reator não tem
        mvc.perform(ajustar("R-02", "{\"sensores\":[{\"sensorId\":9,\"setpoint\":1}]}"))
            .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject(
            "SELECT setpoint FROM nucleo.sensor_reator WHERE reator_id = 'R-02' AND sensor_id = 3", Double.class))
            .isEqualTo(2.325);
    }

    @Test
    void ajusteValidoGravaSoOSensorInformado() throws Exception {
        mvc.perform(ajustar("R-02", "{\"sensores\":[{\"sensorId\":3,\"atencaoMaxima\":4.0,\"limiteMaximo\":4.8,\"setpoint\":2.0}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sensores[2].limiteMaximo").value(4.8))
            .andExpect(jsonPath("$.sensores[2].setpoint").value(2.0))
            .andExpect(jsonPath("$.sensores[0].setpoint").value(308.5));
    }

    @Test
    void retirarEReativarSemExcluir() throws Exception {
        MockHttpSession sessao = comDigital();
        mvc.perform(comoAdmin(post("/api/reatores/R-04/retirar")).session(sessao))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(comoAdmin(post("/api/reatores/R-04/retirar")).session(sessao)).andExpect(status().isConflict());
        assertThat(totalReatores()).isEqualTo(5);

        mvc.perform(comoAdmin(post("/api/reatores/R-04/reativar")).session(sessao))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void reatorPrincipalNaoSaiDeOperacao() throws Exception {
        mvc.perform(comoAdmin(post("/api/reatores/R-01/retirar")).session(comDigital()))
            .andExpect(status().isConflict());
        mvc.perform(comoAdmin(post("/api/reatores/R-99/retirar")).session(comDigital()))
            .andExpect(status().isNotFound());
    }

    @Test
    void cadaAlteracaoVaiParaATrilhaEncadeada() throws Exception {
        MockHttpSession sessao = comDigital();
        mvc.perform(comoAdmin(post("/api/reatores")).session(sessao)).andExpect(status().isCreated());
        mvc.perform(ajustar("R-06", "{\"sensores\":[{\"sensorId\":2,\"setpoint\":151}]}")).andExpect(status().isOk());
        mvc.perform(comoAdmin(post("/api/reatores/R-06/retirar")).session(sessao)).andExpect(status().isOk());

        List<String> resultados = auditoria.todos().stream().map(RegistroAutenticacao::getResultado).toList();
        assertThat(resultados).containsSubsequence("REATOR_INCLUIDO:R-06", "REATOR_AJUSTADO:R-06", "REATOR_RETIRADO:R-06");
        assertThat(auditoria.primeiroRegistroAdulterado()).isNull();
    }

    private int totalReatores() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM nucleo.reator", Integer.class);
    }

    private MockHttpServletRequestBuilder ajustar(String id, String corpo) {
        return comoAdmin(post("/api/reatores/{id}", id)).session(comDigital())
            .contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private static MockHttpSession comDigital() {
        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute(CONFIRMADA_ATE, Instant.now().plus(ConfirmacaoBiometrica.JANELA));
        return sessao;
    }

    private static MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder requisicao) {
        return requisicao.with(user("admin@usina.local").roles("ADMINISTRADOR_SISTEMA"));
    }
}
