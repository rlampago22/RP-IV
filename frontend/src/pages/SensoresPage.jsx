export default function SensoresPage() {
  return (
    <section>
      <p className="scada-kicker">T02 · Telemetria RF-1</p>
      <h1 className="scada-heading">Sensores</h1>
      <p className="scada-lead">Quatro parâmetros · MedicaoRegistrada</p>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <div className="scada-table-wrap" style={{ maxHeight: 'none' }}>
          <table className="scada-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Tipo</th>
                <th>Valor</th>
                <th>Unidade</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>T-CORE-01</td>
                <td>TEMPERATURA</td>
                <td>312.00</td>
                <td>°C</td>
                <td><span className="scada-pill ok">OK</span></td>
              </tr>
              <tr>
                <td>P-PRIM-01</td>
                <td>PRESSAO</td>
                <td>155.00</td>
                <td>bar</td>
                <td><span className="scada-pill ok">OK</span></td>
              </tr>
              <tr>
                <td>R-CONT-01</td>
                <td>RADIACAO</td>
                <td>0.12</td>
                <td>mSv/h</td>
                <td><span className="scada-pill warn">Atenção</span></td>
              </tr>
              <tr>
                <td>F-COOL-01</td>
                <td>FLUXO</td>
                <td>980</td>
                <td>m³/h</td>
                <td><span className="scada-pill ok">OK</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  )
}
