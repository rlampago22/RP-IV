# Análise — índice e método

Espaço para registrar **divergências doc × código** e lacunas.

## Método

1. Pegar requisito / UC em [`../DOCUMENTACAO_DE_ENGENHARIA.md`](../DOCUMENTACAO_DE_ENGENHARIA.md).
2. Localizar evidência em `mvp/` ou `frontend/` ([`../implementacao/00-INDICE.md`](../implementacao/00-INDICE.md)).
3. Se faltar: abrir/atualizar issue e anotar aqui em 1 linha.

## Lacunas conhecidas (16/09/2026)

| Item | Situação | Dono sugerido |
|------|----------|---------------|
| API HTTP estado → React | Ainda stub/mock no front | Bruno (#44) |
| Shell React = Opção A tokens | Front parcialmente no tema claro antigo; alinhar ao protótipo A | Grupo (#40) |
| Figma T01–T05 chrome Opção A | Reconstruir frames escuros | Grupo (#40) |
| Duplicidade `src/` × `mvp/` | Dois lugares Java; canônico = `mvp/` | Bruno (migração `backend/`) |
| Opção B (painel claro) | **Descartada** | — |

## Divergências intencionais PDF (APS) × MVP implementado (18/10/2026)

O modelo conceitual/domínio do PDF (`APS - Sistema de Controle de Usina Nuclear`, cap. 6, Fig. 2/3) é a
fonte oficial dos diagramas acadêmicos — não é redesenhado no repositório (ver `docs/plano-entregas.md`).
O recorte MVP (`docs/marco1/diagramas/*.puml`) implementa só o subconjunto Must e, em dois pontos,
diverge do nome/granularidade do PDF **de propósito**. Registrado aqui para o slide de classes não
parecer um erro de implementação:

| Classe/conceito no PDF | Classe real no MVP | Por quê diverge |
|---|---|---|
| `Acesso` (cap. 6, referenciada em `Acesso.id_funcionario`, seção 11.2) | `RegistroAcesso` (`segurancaacesso`) | Já documentado como correção intencional em [`../marco1/02-priorizacao-moscow.md`](../marco1/02-priorizacao-moscow.md) (RF08: "corrige classe APS") — o nome `RegistroAcesso` deixa explícito que a classe é um registro imutável de tentativa de acesso, não a entidade "Acesso" em si. Mantido: renomear agora reverteria uma decisão já validada em RF07/RF08/RNF08 (PR #72/#73), sem ganho e com risco de regressão a 1 dia da apresentação final. |
| `AuditoriaLogs` em caixa-preta (seção 11.1: "consomem eventos para... trilha de auditoria", sem classes detalhadas) | `RegistroAuditoria`/`EntradaAuditoria` (`auditorialogs`) | Não é uma lacuna: o PDF modela `AuditoriaLogs` como módulo caixa-preta de propósito (não é alvo de nenhum dos 10 UCs detalhados). O MVP precisou detalhar essas classes porque RF06/RNF05 (auditoria dos eventos do núcleo) é Must e exige implementação real, mesmo sem o PDF descer ao nível de classe para esse módulo. |

Pacote `historicooperacional` (RF16/RF17, PR #71 de Bruno) e a aba T06 não existem no PDF — são uma
extensão proposta além do Must (Could, pendente de aprovação do grupo em
[`../marco1/02-priorizacao-moscow.md`](../marco1/02-priorizacao-moscow.md)), não uma divergência de um
requisito já modelado.

## Feedback APS

Checklist: [`../marco1/checklist-feedback-aps.md`](../marco1/checklist-feedback-aps.md)
