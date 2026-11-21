/**
 * RF10 / #99 — texto formal de cada protocolo de contingência (alerta de tela cheia e painel da
 * Visão Geral) e o anúncio falado. Códigos = enum ProtocoloEmergencia do backend.
 *
 * As frases faladas ficam em contingencia-falas.json e viram .mp3 em public/audio/contingencia/
 * pelo script frontend/scripts/gerar-audio-contingencia.py (voz Antonio, grave, alto-falante).
 */
import FALAS from './contingencia-falas.json'

const AVISOS = {
  DESLIGAMENTO_EMERGENCIAL: {
    titulo: 'DESLIGAMENTO EMERGENCIAL DO REATOR',
    diretriz: 'SCRAM em curso: inserção total das barras de controle. Equipes de operação em posição de emergência.',
  },
  CONTENCAO_VAZAMENTO: {
    titulo: 'CONTENÇÃO DE VAZAMENTO',
    diretriz: 'Isolamento do circuito afetado e selagem da área de contenção em curso. Acesso à área restrito.',
  },
  EVACUACAO_AREA: {
    titulo: 'EVACUAÇÃO DA ÁREA',
    diretriz: 'Evacuação ordenada em curso. Todo o pessoal deve se dirigir aos pontos de encontro designados.',
  },
}

const PASTA_AUDIO = `${import.meta.env.BASE_URL}audio/contingencia/`

export function avisoDoProtocolo(codigo, nome) {
  const conhecido = AVISOS[codigo] && FALAS[codigo]
  const aviso = AVISOS[codigo] ?? {
    titulo: (nome ?? 'PROTOCOLO DE CONTINGÊNCIA').toUpperCase(),
    diretriz: 'Siga o roteiro do protocolo. Operações de rotina suspensas até o encerramento.',
  }
  return {
    ...aviso,
    fala: FALAS[conhecido ? codigo : 'GENERICO'],
    audio: `${PASTA_AUDIO}${conhecido ? codigo : 'GENERICO'}.mp3`,
  }
}
