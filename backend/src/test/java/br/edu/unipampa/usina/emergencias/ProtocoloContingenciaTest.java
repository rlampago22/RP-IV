package br.edu.unipampa.usina.emergencias;

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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF09 (criar emergência) e RF10 (ativar protocolo de contingência), com a regra do 4.2.10 do PDF:
 * ativar o protocolo exige o segundo fator (digital), para evitar ativação acidental.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:contingencia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "app.bootstrap-password=senha-de-teste",
})
@AutoConfigureMockMvc
@Transactional
class ProtocoloContingenciaTest {
    // Mesmo atributo que ConfirmacaoBiometrica grava ao conferir a digital.
    private static final String CONFIRMADA_ATE = "biometria.confirmada.ate";

    @Autowired MockMvc mvc;
    @Autowired EmergenciaRepository emergencias;
    @Autowired AuditoriaAutenticacao auditoria;

    @Test
    void operadorRegistraEmergenciaSemPrecisarDaDigital() throws Exception {
        mvc.perform(comoOperador(criar("SUPERAQUECIMENTO", "Temperatura do núcleo subindo")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ABERTA"))
            .andExpect(jsonPath("$.criadaPor").value("operador@usina.local"));
    }

    @Test
    void emergenciaSemDescricaoOuComTipoInvalidoEhRecusada() throws Exception {
        mvc.perform(comoOperador(criar("SUPERAQUECIMENTO", "  "))).andExpect(status().isBadRequest());
        mvc.perform(comoOperador(criar("INVENTADO", "Algo"))).andExpect(status().isBadRequest());
    }

    @Test
    void ativarProtocoloSemDigitalPedeConfirmacaoENaoAtiva() throws Exception {
        Long id = novaEmergencia();

        mvc.perform(comoOperador(ativar(id, "DESLIGAMENTO_EMERGENCIAL")))
            .andExpect(status().isPreconditionRequired())
            .andExpect(jsonPath("$.exigeBiometria").value(true));

        assertThat(emergencias.findById(id).orElseThrow().getStatus()).isEqualTo(Emergencia.Status.ABERTA);
    }

    @Test
    void ativarProtocoloComDigitalConfirmadaAtivaEMostraOsPassos() throws Exception {
        Long id = novaEmergencia();

        mvc.perform(comoOperador(ativar(id, "DESLIGAMENTO_EMERGENCIAL")).session(comDigital()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PROTOCOLO_ATIVO"))
            .andExpect(jsonPath("$.protocolo.codigo").value("DESLIGAMENTO_EMERGENCIAL"))
            .andExpect(jsonPath("$.protocolo.passos.length()").value(3))
            .andExpect(jsonPath("$.protocoloAtivadoPor").value("operador@usina.local"));
    }

    @Test
    void naoAtivaDoisProtocolosNaMesmaEmergencia() throws Exception {
        Long id = novaEmergencia();
        MockHttpSession sessao = comDigital();
        mvc.perform(comoOperador(ativar(id, "CONTENCAO_VAZAMENTO")).session(sessao)).andExpect(status().isOk());

        mvc.perform(comoOperador(ativar(id, "EVACUACAO_AREA")).session(sessao)).andExpect(status().isConflict());
    }

    @Test
    void encerrarTambemPedeADigitalEFechaAEmergencia() throws Exception {
        Long id = novaEmergencia();

        mvc.perform(comoOperador(post("/api/emergencias/{id}/encerrar", id)))
            .andExpect(status().isPreconditionRequired());
        mvc.perform(comoOperador(post("/api/emergencias/{id}/encerrar", id)).session(comDigital()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENCERRADA"));
        mvc.perform(comoOperador(ativar(id, "EVACUACAO_AREA")).session(comDigital())).andExpect(status().isConflict());
    }

    @Test
    void guardaDeAcessoNaoMexeEmEmergencias() throws Exception {
        mvc.perform(criar("OUTRA", "Teste").with(user("guarda@usina.local").roles("GUARDA_ACESSO")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/emergencias")).andExpect(status().isForbidden()); // sem sessão
    }

    @Test
    void cadaPassoVaiParaATrilhaEncadeada() throws Exception {
        mvc.perform(comoOperador(criar("VAZAMENTO_RADIOATIVO", "Vazamento no circuito 2"))).andExpect(status().isCreated());
        Long id = emergencias.findAllByOrderByIdDesc().get(0).getId();
        MockHttpSession sessao = comDigital();
        mvc.perform(comoOperador(ativar(id, "EVACUACAO_AREA")).session(sessao)).andExpect(status().isOk());
        mvc.perform(comoOperador(post("/api/emergencias/{id}/encerrar", id)).session(sessao)).andExpect(status().isOk());

        List<String> resultados = auditoria.todos().stream().map(RegistroAutenticacao::getResultado).toList();
        assertThat(resultados).containsSubsequence(
            "EMERGENCIA_CRIADA", "PROTOCOLO_CONTINGENCIA_ATIVADO", "EMERGENCIA_ENCERRADA");
        assertThat(auditoria.primeiroRegistroAdulterado()).isNull();
    }

    private Long novaEmergencia() {
        return emergencias.save(new Emergencia(Emergencia.Tipo.SUPERAQUECIMENTO, "Teste", "operador@usina.local")).getId();
    }

    private static MockHttpSession comDigital() {
        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute(CONFIRMADA_ATE, Instant.now().plus(ConfirmacaoBiometrica.JANELA));
        return sessao;
    }

    private static MockHttpServletRequestBuilder criar(String tipo, String descricao) {
        return post("/api/emergencias").contentType(MediaType.APPLICATION_JSON)
            .content("{\"tipo\":\"%s\",\"descricao\":\"%s\"}".formatted(tipo, descricao));
    }

    private static MockHttpServletRequestBuilder ativar(Long id, String protocolo) {
        return post("/api/emergencias/{id}/protocolo", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"protocolo\":\"%s\"}".formatted(protocolo));
    }

    private static MockHttpServletRequestBuilder comoOperador(MockHttpServletRequestBuilder requisicao) {
        return requisicao.with(user("operador@usina.local").roles("OPERADOR_REATOR"));
    }
}
