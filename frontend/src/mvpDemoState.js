const NORMAL_SENSORS = [
  { id: 'T-CORE-01', tipo: 'TEMPERATURA', valor: 312, unidade: '°C', status: 'OK', color: 'var(--crit)', percent: 78 },
  { id: 'P-PRIM-01', tipo: 'PRESSAO', valor: 145, unidade: 'bar', status: 'OK', color: 'var(--warn)', percent: 80 },
  { id: 'R-CONT-01', tipo: 'RADIACAO', valor: 0.12, unidade: 'mSv/h', status: 'OK', color: 'var(--purple)', percent: 12 },
  { id: 'F-COOL-01', tipo: 'FLUXO', valor: 980, unidade: 'm³/h', status: 'OK', color: 'var(--cyan)', percent: 82 },
]

function shortHash(sequence) {
  return ((sequence * 2654435761) >>> 0).toString(16).padStart(8, '0') + 'a4f2'
}

function event(sequence, tipo, detalhe) {
  return {
    sequence,
    timestamp: new Date().toISOString(),
    tipo,
    detalhe,
    hash: shortHash(sequence),
  }
}

export const initialMvpState = {
  coreStatus: 'ESTÁVEL',
  scenario: 'NORMAL',
  sensors: NORMAL_SENSORS,
  alarms: [],
  events: [
    event(4, 'MEDICAO_REGISTRADA', 'Fluxo 980 m³/h'),
    event(3, 'MEDICAO_REGISTRADA', 'Radiação 0,12 mSv/h'),
    event(2, 'MEDICAO_REGISTRADA', 'Pressão 145 bar'),
    event(1, 'MEDICAO_REGISTRADA', 'Temperatura 312 °C'),
  ],
  nextSequence: 5,
}

function appendEvents(state, definitions) {
  let sequence = state.nextSequence
  const added = definitions.map(({ tipo, detalhe }) => event(sequence++, tipo, detalhe))
  return {
    events: [...added.reverse(), ...state.events],
    nextSequence: sequence,
  }
}

export function mvpDemoReducer(state, action) {
  switch (action.type) {
    case 'RUN_NORMAL': {
      const audit = appendEvents(state, [
        { tipo: 'MEDICAO_REGISTRADA', detalhe: 'Ciclo normal: parâmetros dentro da faixa' },
      ])
      return { ...state, ...audit, coreStatus: 'ESTÁVEL', scenario: 'NORMAL', sensors: NORMAL_SENSORS, alarms: [] }
    }
    case 'RUN_OBSERVATION': {
      const sensors = NORMAL_SENSORS.map((sensor) => (
        sensor.id === 'T-CORE-01'
          ? { ...sensor, valor: 328, status: 'ATENÇÃO', percent: 88, color: 'var(--warn)' }
          : sensor
      ))
      const audit = appendEvents(state, [
        { tipo: 'MEDICAO_REGISTRADA', detalhe: 'Temperatura 328 °C' },
        { tipo: 'OBSERVACAO_REGISTRADA', detalhe: 'Faixa preventiva; nenhum alarme crítico' },
      ])
      return { ...state, ...audit, coreStatus: 'ATENÇÃO', scenario: 'OBSERVAÇÃO', sensors, alarms: [] }
    }
    case 'RUN_SENSOR_FAILURE': {
      const sensors = NORMAL_SENSORS.map((sensor) => (
        sensor.id === 'P-PRIM-01'
          ? { ...sensor, valor: null, status: 'FALHA', percent: 0, color: 'var(--crit)' }
          : sensor
      ))
      const audit = appendEvents(state, [
        { tipo: 'FALHA_SENSOR_DETECTADA', detalhe: 'Timeout P-PRIM-01; manutenção notificada' },
      ])
      return { ...state, ...audit, coreStatus: 'ATENÇÃO', scenario: 'FALHA SENSOR', sensors, alarms: [] }
    }
    case 'RUN_CRITICAL': {
      const alarmId = `AL-${String(state.nextSequence + 2).padStart(3, '0')}`
      const sensors = NORMAL_SENSORS.map((sensor) => {
        if (sensor.id === 'T-CORE-01') return { ...sensor, valor: 372, status: 'CRÍTICO', percent: 100, color: 'var(--crit)' }
        if (sensor.id === 'F-COOL-01') return { ...sensor, valor: 420, status: 'CRÍTICO', percent: 30, color: 'var(--crit)' }
        return sensor
      })
      const audit = appendEvents(state, [
        { tipo: 'MEDICAO_REGISTRADA', detalhe: 'Temperatura 372 °C' },
        { tipo: 'MEDICAO_REGISTRADA', detalhe: 'Fluxo 420 m³/h' },
        { tipo: 'ALARME_EMITIDO', detalhe: `${alarmId}: Operador e Supervisão Central notificados` },
      ])
      return {
        ...state,
        ...audit,
        coreStatus: 'CRÍTICO',
        scenario: 'ANOMALIA CRÍTICA',
        sensors,
        alarms: [{ id: alarmId, mensagem: 'Temperatura alta e fluxo de resfriamento baixo', status: 'ATIVO' }],
      }
    }
    case 'ACK_ALARM': {
      const current = state.alarms.find((alarm) => alarm.status === 'ATIVO')
      if (!current) return state
      const audit = appendEvents(state, [
        { tipo: 'ALARME_RECONHECIDO', detalhe: `${current.id}: reconhecido pelo Operador de Reator` },
      ])
      return {
        ...state,
        ...audit,
        coreStatus: 'EM TRATAMENTO',
        alarms: state.alarms.map((alarm) => alarm.id === current.id ? { ...alarm, status: 'RECONHECIDO' } : alarm),
      }
    }
    case 'RESOLVE_ALARM': {
      const current = state.alarms.find((alarm) => alarm.status !== 'RESOLVIDO')
      if (!current) return state
      const audit = appendEvents(state, [
        { tipo: 'ALARME_RESOLVIDO', detalhe: `${current.id}: parâmetros normalizados` },
      ])
      return {
        ...state,
        ...audit,
        coreStatus: 'ESTÁVEL',
        scenario: 'NORMALIZADO',
        sensors: NORMAL_SENSORS,
        alarms: state.alarms.map((alarm) => alarm.id === current.id ? { ...alarm, status: 'RESOLVIDO' } : alarm),
      }
    }
    default:
      return state
  }
}

export const scenarioActions = {
  normal: 'RUN_NORMAL',
  observation: 'RUN_OBSERVATION',
  failure: 'RUN_SENSOR_FAILURE',
  critical: 'RUN_CRITICAL',
}
