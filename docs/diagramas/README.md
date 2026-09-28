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

## Não essenciais (legado / arquivo)

| Item | Destino |
|------|---------|
| Astah APS completo | [`astah/`](astah/) — referência histórica, **não** entrega ativa |
| `er-nucleo-mvp.puml` | Opcional; mover para `arquivo/` se não for usado na apresentação |
| PDF APS completo | [`../legado-aps/`](../legado-aps/) |

## Regra

Só sobe diagrama novo se for **essencial** para Marco 1/2 ou para a entrega vertical da semana.
