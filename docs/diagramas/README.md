# Diagramas — política do repositório

## Essenciais (manter versionados e atualizados)

### UML do Marcus — fonte Astah ativa

| Arquivo | Conteúdo |
|---------|----------|
| [`../marcus/diagramas/Marcus-UML-MVP.asta`](../marcus/diagramas/Marcus-UML-MVP.asta) | Casos de uso Must + SEQ-UC01 + SEQ-UC02 |
| [`../marcus/diagramas/Marcus-UML-MVP/`](../marcus/diagramas/Marcus-UML-MVP/) | PNGs exportados pelo Astah para documentação/apresentação |

Os blocos Mermaid foram removidos dos documentos ativos. Os `.puml` abaixo são fontes UML técnicas auxiliares, e os PNGs gerados ficam versionados para visualização direta no GitHub e na apresentação.

Em [`../marco1/diagramas/`](../marco1/diagramas/):

| Arquivo | Por quê |
|---------|---------|
| `pacotes.puml` / `pacotes-mvp-usina.png` | Arquitetura EDA (módulos + bus) |
| `componentes-logicos.puml` / `componentes-logicos-mvp.png` | Componentes lógicos |
| `componentes-fisicos.puml` / `componentes-fisicos-mvp.png` | Artefatos da build/demo |
| `casos-de-uso-mvp.puml` / `casos-de-uso-mvp.png` | Escopo funcional MVP |
| `classes-projeto-mvp.puml` / `classes-projeto-mvp.png` | Classes do núcleo |
| `seq-uc01-medicao.puml` / `seq-uc01-medicao.png` | Fluxo medição |
| `seq-uc02-alarme.puml` / `seq-uc02-alarme.png` | Fluxo alarme |
| `seq-observer-v2-push.puml` / `seq-observer-v2-push.png` | Push de estado via SSE (Observer V2) |
| `seq-uc04-acesso.puml` / `seq-uc04-acesso.png` | UC04 — acesso físico (RF08) |
| `er-nucleo-mvp.puml` | Modelo ER do núcleo (ligado a `08-mapeamento-relacional-mvp.md`) |

## Legado (removido da árvore de trabalho, 16/11)

Astah APS completo (`astah/`) e o PDF APS completo (`legado-aps/`) eram só
referência histórica, já descritos como não-essenciais aqui mesmo. Continuam
no histórico do git (`git log --all -- docs/diagramas/astah docs/legado-aps`)
se precisar consultar de novo.

## Regra

Só sobe diagrama novo se for **essencial** para Marco 1/2 ou para a entrega vertical da semana.
