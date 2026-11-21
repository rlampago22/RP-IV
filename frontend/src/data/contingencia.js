/**
 * RF10 / #99 — texto formal de cada protocolo de contingência: o que aparece no alerta de tela
 * cheia e no painel da Visão Geral, e o que a voz anuncia. Códigos = enum ProtocoloEmergencia do
 * backend.
 */
const AVISOS = {
  DESLIGAMENTO_EMERGENCIAL: {
    titulo: 'DESLIGAMENTO EMERGENCIAL DO REATOR',
    diretriz: 'SCRAM em curso: inserção total das barras de controle. Equipes de operação em posição de emergência.',
    fala: 'Atenção. Atenção. Protocolo de contingência ativado. Desligamento emergencial do reator em curso. Equipes de operação, assumam posição de emergência.',
  },
  CONTENCAO_VAZAMENTO: {
    titulo: 'CONTENÇÃO DE VAZAMENTO',
    diretriz: 'Isolamento do circuito afetado e selagem da área de contenção em curso. Acesso à área restrito.',
    fala: 'Atenção. Atenção. Protocolo de contingência ativado. Contenção de vazamento em curso. Área de contenção isolada.',
  },
  EVACUACAO_AREA: {
    titulo: 'EVACUAÇÃO DA ÁREA',
    diretriz: 'Evacuação ordenada em curso. Todo o pessoal deve se dirigir aos pontos de encontro designados.',
    fala: 'Atenção. Atenção. Protocolo de contingência ativado. Evacuação da área em curso. Todo o pessoal, dirija-se aos pontos de encontro designados.',
  },
}

export function avisoDoProtocolo(codigo, nome) {
  return (
    AVISOS[codigo] ?? {
      titulo: (nome ?? 'PROTOCOLO DE CONTINGÊNCIA').toUpperCase(),
      diretriz: 'Siga o roteiro do protocolo. Operações de rotina suspensas até o encerramento.',
      fala: `Atenção. Atenção. Protocolo de contingência ativado. ${nome ?? ''}.`,
    }
  )
}
