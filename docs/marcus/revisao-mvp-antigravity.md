# Revisão do MVP gerado no Antigravity

## Fontes comparadas

- PDF legado: `docs/legado-aps/APS-RPIV-ATUALIZADO-Diagrama-Implantacao.pdf` (69 páginas);
- cópia local: `C:\Users\Querol\Desktop\MVP-Usina-Nuclear`;
- versão Git: `mvp/` e `frontend/`.

A cópia local e `mvp/` possuem o mesmo código-fonte Java. As diferenças encontradas eram artefatos de execução (`out/`) e logs em `dados/`; por isso esses arquivos não foram importados para o Git.

## O que está alinhado

| Tema | Evidência |
|------|-----------|
| Telemetria | quatro sensores simulados e `ReatorFacade.receberLeitura` |
| EDA | `EventBus`, `EventoDominio` e subscribers desacoplados |
| Alarmes | Strategy (`AvaliadorLimiar`), Factory e `AlarmeFacade` |
| Destinatários | Operador de Reator e Supervisão Central explícitos |
| Auditoria | `AuditoriaSubscriber` + arquivo append-only encadeado por SHA-256 |
| Testabilidade | 11 cenários executáveis sem dependências externas |
| UI | React Opção A com T01–T05; Swing mantido somente como legado de fluxo |

## Lacunas e correções realizadas

| Lacuna encontrada | Tratamento |
|-------------------|------------|
| Documentos ativos exibiam Mermaid | Substituídos por PNGs exportados do Astah e fonte `.asta` editável |
| SEQ-UC01 inventava `ReatorRepository` | Diagrama corrigido para o histórico em memória realmente existente |
| SEQ-UC02 não refletia o código atual | Refeito com `EventBus`, `AlarmeFacade`, Strategy, Factory, atores e auditoria |
| T04 e T05 eram mocks isolados | Criado estado React compartilhado entre T01–T05 |
| Documentação dizia que o EventBus era assíncrono | Corrigido: ele é síncrono e in-process nesta versão |
| Documentação afirmava persistência completa | Corrigido: somente auditoria persiste; medições e alarmes ainda ficam em memória |
| Cadeia SHA-256 era descrita como imutável | Corrigido para “adulteração detectável”; o arquivo físico ainda pode ser editado |
| Cobertura era anunciada como RNF01–RNF10 completos | Corrigida para RF01–RF06 e RNFs verificáveis localmente |

## Avaliação arquitetural

O MVP é coerente como demonstração acadêmica EDA in-process: produtor, barramento e consumidores estão separados; Facade, Observer/Pub-Sub, Strategy e Factory aparecem no código. Ele ainda não implementa a implantação distribuída do PDF, bancos redundantes, API HTTP ou mensageria durável. Esses itens devem ser apresentados como evolução, não como funcionalidades prontas.

## Próximo incremento técnico

Criar uma API Java de estado/cenários entre `mvp/` e `frontend/`. O React já compartilha estado internamente, então a troca futura consiste em substituir o reducer demonstrativo por chamadas HTTP e dados derivados dos eventos reais.

