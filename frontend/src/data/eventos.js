/**
 * Tradução dos eventos de domínio (EventBus) em algo que um humano lê rápido.
 * O backend manda `resumo` como string técnica "chave=valor" (ver
 * mvp/src/.../infraestruturaeventos/*.java) — aqui só interpretamos essa
 * string para montar uma frase e um rótulo, sem tocar no domínio/mvp.
 */

const NOME_SENSOR = {
  TEMPERATURA: 'Temperatura',
  PRESSAO: 'Pressão',
  RADIACAO: 'Radiação',
  FLUXO_RESFRIAMENTO: 'Fluxo de resfriamento',
}

function nomeSensor(tipo) {
  return NOME_SENSOR[tipo] ?? tipo ?? 'sensor'
}

const PADROES = [
  {
    tipo: 'ALARME_EMITIDO',
    regex: /alarme=(\S+) sensor=(\d+) severidade=(\S+) destinatarios=(.*?) mensagem="([^"]*)"/,
    montar: ([, , , , destinatarios, mensagem]) => ({
      titulo: 'Alarme emitido',
      descricao: `${mensagem} — notificado para ${destinatarios}.`,
    }),
  },
  {
    tipo: 'ALARME_RECONHECIDO',
    regex: /alarme=(\S+) operador="([^"]*)" justificativa="([^"]*)"/,
    montar: ([, , operador, justificativa]) => ({
      titulo: 'Alarme reconhecido',
      descricao: `${operador} reconheceu o alarme: "${justificativa}".`,
    }),
  },
  {
    tipo: 'ALARME_RESOLVIDO',
    regex: /alarme=(\S+) responsavel="([^"]*)" solucao="([^"]*)"/,
    montar: ([, , responsavel, solucao]) => ({
      titulo: 'Alarme resolvido',
      descricao: `${responsavel} normalizou a ocorrência: "${solucao}".`,
    }),
  },
  {
    tipo: 'ACESSO_REGISTRADO',
    regex: /acesso registro=(\S+) credencial="([^"]*)" area="([^"]*)" resultado=(\S+)/,
    montar: ([, , credencial, area, resultado]) => ({
      titulo: resultado === 'SUCESSO' ? 'Acesso autorizado' : 'Acesso negado',
      descricao: `Crachá ${credencial} em ${area}.`,
    }),
  },
  {
    tipo: 'FALHA_SENSOR_DETECTADA',
    regex: /sensor=(\S+) tipo=(\S+) motivo="([^"]*)" destinatarios=(.*)/,
    montar: ([, sensorId, tipo, motivo, destinatarios]) => ({
      titulo: 'Falha técnica no sensor',
      descricao: `${nomeSensor(tipo)} (sensor ${sensorId}): ${motivo}. Equipe avisada: ${destinatarios}.`,
    }),
  },
  {
    tipo: 'OBSERVACAO_REGISTRADA',
    regex: /sensor=(\d+) tipo=(\S+) valor=([\d.,-]+) (\S+) obs="([^"]*)"/,
    montar: ([, sensorId, tipo, valor, unidade, obs]) => ({
      titulo: 'Observação preventiva',
      descricao: `${nomeSensor(tipo)} (sensor ${sensorId}) em ${valor} ${unidade} — ${obs}.`,
    }),
  },
  {
    tipo: 'MEDICAO_REGISTRADA',
    regex: /sensor=(\d+) tipo=(\S+) valor=([\d.,-]+) (\S+) faixa=\[([\d.,-]+), ([\d.,-]+)\]/,
    montar: ([, sensorId, tipo, valor, unidade, min, max]) => {
      const dentro = Number(valor) >= Number(min) && Number(valor) <= Number(max)
      return {
        titulo: 'Medição de rotina',
        descricao: `${nomeSensor(tipo)} (sensor ${sensorId}): ${valor} ${unidade}, ${dentro ? 'dentro' : 'fora'} da faixa segura.`,
      }
    },
  },
]

const META_TIPO = {
  ALARME_EMITIDO: { cor: 'var(--crit)', importante: true },
  ALARME_RECONHECIDO: { cor: 'var(--warn)', importante: true },
  ALARME_RESOLVIDO: { cor: 'var(--ok)', importante: true },
  ACESSO_REGISTRADO: { cor: 'var(--ok)', importante: true },
  FALHA_SENSOR_DETECTADA: { cor: 'var(--purple)', importante: true },
  OBSERVACAO_REGISTRADA: { cor: 'var(--sky)', importante: false },
  MEDICAO_REGISTRADA: { cor: 'var(--dim)', importante: false },
}

/**
 * @returns {{titulo:string, descricao:string, cor:string, importante:boolean}}
 */
export function interpretarEvento(tipo, resumo) {
  const padrao = PADROES.find((p) => p.tipo === tipo)
  const meta = META_TIPO[tipo] ?? { cor: 'var(--dim)', importante: true }
  if (!padrao) {
    return { titulo: tipo, descricao: resumo, ...meta }
  }
  const match = resumo?.match(padrao.regex)
  if (!match) {
    return { titulo: padrao.tipo.replaceAll('_', ' '), descricao: resumo, ...meta }
  }
  // ACESSO_REGISTRADO precisa do resultado pra escolher a cor (sucesso/falha)
  if (tipo === 'ACESSO_REGISTRADO') {
    const sucesso = match[4] === 'SUCESSO'
    return { ...padrao.montar(match), cor: sucesso ? 'var(--ok)' : 'var(--crit)', importante: true }
  }
  return { ...padrao.montar(match), ...meta }
}
