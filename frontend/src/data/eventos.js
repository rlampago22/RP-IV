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

/**
 * Reator de um evento da timeline: medição, observação, falha e alarme emitido trazem `reator=`
 * no resumo; reconhecimento e resolução só trazem o id do alarme, então o reator vem do próprio
 * alarme (`reatorPorAlarme`: id → reatorId). Acesso não pertence a reator (`null`).
 */
export function reatorDoEvento(evento, reatorPorAlarme) {
  const direto = /(?:^|\s)reator=(R-\d+)/.exec(evento.resumo ?? '')
  if (direto) return direto[1]
  const alarme = /alarme=(\S+)/.exec(evento.resumo ?? '')
  return alarme ? reatorPorAlarme?.get(alarme[1]) ?? null : null
}

/** Eventos de medição e alarme começam com `reator=R-0X` desde que a usina tem 5 reatores. */
function prefixoReator(resumo) {
  const achado = /(?:^|\s)reator=(R-\d+)/.exec(resumo ?? '')
  return achado ? `Reator-${achado[1].slice(2)} · ` : ''
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
    tipo: 'PROTOCOLO_CONTINGENCIA_ATIVADO',
    regex: /emergencia=(\d+) protocolo=(\S+) nome="([^"]*)" ativadoPor="([^"]*)"/,
    montar: ([, emergencia, , nome, ativadoPor]) => ({
      titulo: 'Protocolo de contingência ativado',
      descricao: `${nome} (emergência #${emergencia}) por ${ativadoPor}. Operações de rotina travadas.`,
    }),
  },
  {
    tipo: 'PROTOCOLO_CONTINGENCIA_ENCERRADO',
    regex: /emergencia=(\d+) protocolo=(\S+) encerradoPor="([^"]*)"/,
    montar: ([, emergencia, , encerradoPor]) => ({
      titulo: 'Protocolo de contingência encerrado',
      descricao: `Emergência #${emergencia} encerrada por ${encerradoPor}. Operação normal liberada.`,
    }),
  },
  {
    tipo: 'ACESSO_REGISTRADO',
    regex: /acesso registro=(\S+) credencial="([^"]*)" area="([^"]*)" resultado=(\S+)/,
    montar: ([, , credencial, area, resultado]) => ({
      titulo: resultado === 'SUCESSO' ? 'Acesso autorizado'
        : resultado === 'BLOQUEADO_CONTINGENCIA' ? 'Acesso bloqueado (contingência)' : 'Acesso negado',
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
  PROTOCOLO_CONTINGENCIA_ATIVADO: { cor: 'var(--crit)', importante: true },
  PROTOCOLO_CONTINGENCIA_ENCERRADO: { cor: 'var(--ok)', importante: true },
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
  const montado = padrao.montar(match)
  // A mensagem do alarme já começa com o reator ("R-04 · ..."): não repetir.
  const prefixo = /^R-\d+ · /.test(montado.descricao) ? '' : prefixoReator(resumo)
  return { ...montado, descricao: prefixo + montado.descricao, ...meta }
}
