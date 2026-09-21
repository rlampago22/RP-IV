# Diagramas — política do repositório

## Essenciais (manter versionados e atualizados)

Em [`../marco1/diagramas/`](../marco1/diagramas/):

| Arquivo | Por quê |
|---------|---------|
| `pacotes.puml` | Arquitetura EDA (módulos + bus) |
| `componentes-logicos.puml` | Componentes lógicos |
| `componentes-fisicos.puml` | Artefatos da build/demo |
| `casos-de-uso-mvp.puml` | Escopo funcional MVP |
| `classes-projeto-mvp.puml` | Classes do núcleo |
| `seq-uc01-medicao.puml` | Fluxo medição |
| `seq-uc02-alarme.puml` | Fluxo alarme |

## Não essenciais (legado / arquivo)

| Item | Destino |
|------|---------|
| Astah APS completo | [`astah/`](astah/) — referência histórica, **não** entrega ativa |
| `er-nucleo-mvp.puml` | Opcional; mover para `arquivo/` se não for usado na apresentação |
| PDF APS completo | [`../legado-aps/`](../legado-aps/) |

## Regra

Só sobe diagrama novo se for **essencial** para Marco 1/2 ou para a entrega vertical da semana.
