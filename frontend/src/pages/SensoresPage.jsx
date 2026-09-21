export default function SensoresPage({ sensors }) {
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
              {sensors.map((sensor) => (
                <tr key={sensor.id}>
                  <td>{sensor.id}</td>
                  <td>{sensor.tipo}</td>
                  <td>{sensor.valor === null ? 'SEM LEITURA' : sensor.valor.toFixed(sensor.valor < 10 ? 2 : 0)}</td>
                  <td>{sensor.unidade}</td>
                  <td><span className={`scada-pill ${sensor.status === 'OK' ? 'ok' : 'warn'}`}>{sensor.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  )
}
