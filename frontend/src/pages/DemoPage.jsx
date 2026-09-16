export default function DemoPage() {
  return (
    <section>
      <p className="scada-kicker">T05 · Roteiro Marcus</p>
      <h1 className="scada-heading">Demo / Cenários</h1>
      <p className="scada-lead">
        Dispara os fluxos da apresentação: Normal, Observação, Falha Sensor, Anomalia Crítica.
      </p>

      <div className="scada-scenarios">
        <button type="button" className="scada-scenario">
          <strong>Normal / Tempo Real</strong>
          <span>Medições na faixa · núcleo ESTÁVEL</span>
        </button>
        <button type="button" className="scada-scenario">
          <strong>Simular Observação (Alt. 1)</strong>
          <span>OBSERVACAO_REGISTRADA · sem alarme alto</span>
        </button>
        <button type="button" className="scada-scenario">
          <strong>Falha Sensor (Exceção)</strong>
          <span>FALHA_SENSOR_DETECTADA · manutenção</span>
        </button>
        <button type="button" className="scada-scenario">
          <strong>Simular Anomalia (Crítico)</strong>
          <span>Temp 372 · fluxo baixo · ALARME_EMITIDO</span>
        </button>
      </div>

      <div className="scada-note">
        Integração: estes botões devem publicar no EventBus via API (S4 · issue #52).
        Protótipo interativo: <code>docs/ui/propostas/opcao-a.html</code>.
      </div>
    </section>
  )
}
