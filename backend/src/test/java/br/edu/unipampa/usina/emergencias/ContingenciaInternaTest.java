package br.edu.unipampa.usina.emergencias;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issue #99 — ponte interna que o núcleo ({@code mvp/}) consulta para saber se há protocolo de
 * contingência ativo e travar as operações de rotina.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:contingencia-interna;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "app.bootstrap-password=senha-de-teste",
})
@AutoConfigureMockMvc
@Transactional
class ContingenciaInternaTest {
    private static final String OPERADOR = "operador@usina.local";

    @Autowired MockMvc mvc;
    @Autowired EmergenciaRepository emergencias;

    @Test
    void nucleoLocalVeProtocoloAtivoSemSessao() throws Exception {
        emergencias.save(new Emergencia(Emergencia.Tipo.OUTRA, "Sem protocolo", OPERADOR));
        Emergencia ativa = new Emergencia(Emergencia.Tipo.SUPERAQUECIMENTO, "Núcleo aquecendo", OPERADOR);
        ativa.ativarProtocolo(ProtocoloEmergencia.DESLIGAMENTO_EMERGENCIAL, OPERADOR);
        Long id = emergencias.save(ativa).getId();

        mvc.perform(get("/api/interno/contingencia"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1)) // a emergência sem protocolo não entra
            .andExpect(jsonPath("$[0].emergenciaId").value(id))
            .andExpect(jsonPath("$[0].status").value("PROTOCOLO_ATIVO"))
            .andExpect(jsonPath("$[0].protocolo").value("DESLIGAMENTO_EMERGENCIAL"))
            .andExpect(jsonPath("$[0].ativadoPor").value(OPERADOR));
    }

    @Test
    void encerramentoApareceComQuemEncerrou() throws Exception {
        Emergencia emergencia = new Emergencia(Emergencia.Tipo.VAZAMENTO_RADIOATIVO, "Vazamento", OPERADOR);
        emergencia.ativarProtocolo(ProtocoloEmergencia.CONTENCAO_VAZAMENTO, OPERADOR);
        emergencia.encerrar("supervisao@usina.local");
        emergencias.save(emergencia);

        mvc.perform(get("/api/interno/contingencia"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("ENCERRADA"))
            .andExpect(jsonPath("$[0].encerradoPor").value("supervisao@usina.local"));
    }

    @Test
    void navegadorOuOutraMaquinaNaoAcessam() throws Exception {
        mvc.perform(get("/api/interno/contingencia").header("Origin", "http://localhost:5173"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/interno/contingencia").with(requisicao -> {
            requisicao.setRemoteAddr("10.0.0.5");
            return requisicao;
        })).andExpect(status().isForbidden());
    }
}
