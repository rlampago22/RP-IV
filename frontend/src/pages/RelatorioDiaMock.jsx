/**
 * PLACEHOLDER do "Relatório do dia" — conteúdo FICTÍCIO.
 *
 * O relatório real é um livro de turno: motivos de alerta, testes realizados, passagem de turno e
 * avisos deixados pelo operador que saiu. Isso é conteúdo **autoral**, escrito por pessoas, e o
 * sistema ainda não tem onde guardá-lo — o `historico-operacional.csv` só guarda fato derivado do
 * EventBus, e misturar texto humano ali diluiria a integridade daquele registro (RNF03).
 *
 * Este componente existe só para a equipe navegar pela tela enquanto a funcionalidade é
 * especificada. Todo dado aqui é inventado e a interface diz isso ao usuário.
 */

const ENTRADAS_FICTICIAS = [
  {
    hora: '06:12',
    tipo: 'Passagem de turno',
    autor: 'Carlos Mendes → Ana Operadora',
    texto:
      'Turno assumido. Circuito primário estável. Pendência deixada: válvula V-204 com ruído '
      + 'intermitente, manutenção avisada por telefone, sem ordem de serviço aberta.',
  },
  {
    hora: '09:48',
    tipo: 'Motivo de alerta',
    autor: 'Ana Operadora',
    texto:
      'Alarme de pressão às 09:45 decorrente de teste programado da bomba secundária. '
      + 'Não houve anomalia de processo. Alarme reconhecido e resolvido em 3 min.',
  },
  {
    hora: '11:30',
    tipo: 'Teste realizado',
    autor: 'Ana Operadora',
    texto:
      'Teste de resposta do sensor de radiação R-CONT-01 conforme procedimento semanal. '
      + 'Leitura retornou ao setpoint em 40 s. Resultado conforme.',
  },
  {
    hora: '14:05',
    tipo: 'Aviso',
    autor: 'Ana Operadora',
    texto:
      'Temperatura do núcleo operando no limite superior da faixa ideal durante a tarde. '
      + 'Recomendo acompanhar na próxima saída.',
  },
  {
    hora: '18:20',
    tipo: 'Passagem de turno',
    autor: 'Ana Operadora → Bruno Rocha',
    texto:
      'Turno repassado. Nenhum alarme ativo. V-204 segue pendente de manutenção.',
  },
]

const COR_POR_TIPO = {
  'Passagem de turno': 'scada-pill',
  'Motivo de alerta': 'scada-pill warn',
  'Teste realizado': 'scada-pill ok',
  Aviso: 'scada-pill purple',
}

export default function RelatorioDiaMock({ saida, onFechar, anexarGraficos, onAnexarGraficos }) {
  const geradoEm = new Date().toLocaleString('pt-BR', { hour12: false })

  return (
    <div className="scada-card scada-relatorio" style={{ marginTop: 14 }}>
      <div className="scada-cabecalho-impressao">
        <strong>Central de Supervisão · Reator-01</strong>
        <span>Relatório de saída de operação · gerado em {geradoEm}</span>
      </div>

      <div className="scada-placeholder">
        <strong>Conteúdo fictício — funcionalidade não implementada.</strong> O relatório do dia
        será um livro de turno com registros escritos pelos operadores (motivos de alerta, testes,
        passagem de turno, avisos). Nada abaixo vem do sistema: é exemplo para desenhar a tela.
      </div>

      <h2 style={{ marginTop: 14 }}>
        Relatório do dia · {saida.dia} · {saida.operador || 'operador não identificado'}
      </h2>

      <div className="scada-table-wrap" style={{ maxHeight: 'none' }}>
        <table className="scada-table">
          <thead>
            <tr>
              <th>Hora</th>
              <th>Tipo</th>
              <th>Autor</th>
              <th>Registro</th>
            </tr>
          </thead>
          <tbody>
            {ENTRADAS_FICTICIAS.map((entrada, indice) => (
              <tr key={indice}>
                <td className="ev">{entrada.hora}</td>
                <td>
                  <span className={COR_POR_TIPO[entrada.tipo] ?? 'scada-pill'}>{entrada.tipo}</span>
                </td>
                <td>{entrada.autor}</td>
                <td>{entrada.texto}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="scada-actions scada-nao-imprimir">
        <button type="button" className="scada-btn scada-btn-green" onClick={() => window.print()}>
          Imprimir / Salvar PDF
        </button>
        <button type="button" className="scada-btn" onClick={onFechar}>
          Fechar relatório
        </button>
        <label className="scada-opcao">
          <input
            type="checkbox"
            checked={anexarGraficos}
            onChange={(e) => onAnexarGraficos(e.target.checked)}
          />
          <span>Anexar gráficos ao PDF</span>
        </label>
      </div>

      <p className="scada-event-note">
        Quando implementado, cada entrada será append-only e atribuída a quem escreveu — um
        registro que pode ser editado depois não serve como livro de turno.
      </p>

      <p className="scada-rodape-impressao">
        Central de Supervisão — Usina Nuclear · AL0343 Resolução de Problemas IV · documento de
        exemplo, sem valor operacional
      </p>
    </div>
  )
}
