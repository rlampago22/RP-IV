# RP-IV — Sistema de Controle de Usina Nuclear

Repositório **AL0343 — Resolução de Problemas IV** (UNIPAMPA Alegrete).

> Trabalho em [`desenvolvimento`](https://github.com/rlampago22/RP-IV/tree/desenvolvimento). A `main` fica limpa até o final.

## Arquitetura (única)

**Arquitetura Orientada a Eventos (EDA)** — ver [`docs/marco1/04-arquitetura-eda.md`](docs/marco1/04-arquitetura-eda.md).

## Stack

| Camada | Tecnologia |
|--------|------------|
| Backend | Java + EventBus (pasta `mvp/` hoje → migrar para `backend/`) |
| Frontend | React + Vite (`frontend/`) |
| UI idealizada | [`docs/ui/idealizacao-telas.md`](docs/ui/idealizacao-telas.md) |

A UI **Swing** em `mvp/` é **legado de fluxo**, não o visual final.

## Equipe e entrega vertical

Cada semana: **back + front** na trilha + relatório [`docs/semanas/_template-entrega-vertical.md`](docs/semanas/_template-entrega-vertical.md).  
Ao mergear a sprint: [`docs/semanas/_template-resumo-merge.md`](docs/semanas/_template-resumo-merge.md).

| Integrante | Branch | Trilha |
|------------|--------|--------|
| Álvaro Domingues | `alvaro` | Requisitos, aceite, contratos |
| Bruno Rocha | `bruno` | EventBus, pacotes/componentes, API eventos |
| Bernardo Dorneles | `bernardo` | Medições / ControleReator + UI sensores |
| José Guilherme Monteiro | `jose` | Alarmes / GoF + UI alarmes |
| Marcus Querol | `marcus` | Auditoria / UCs + UI auditoria/demo |

## Plano

**Operacional:** [`docs/plano-entregas.md`](docs/plano-entregas.md) · Resumo: [`docs/plano-marco1.md`](docs/plano-marco1.md) · Status: [`docs/semanas/STATUS.md`](docs/semanas/STATUS.md) · Figma: [`docs/ui/FIGMA.md`](docs/ui/FIGMA.md)

| Entrega | Data |
|---------|------|
| Idealização Figma/React | agora → 21/09/2026 |
| Semana 3 (vertical) | 21/09/2026 |
| Semana 4 (ensaio) | 28/09/2026 |
| Marco 1 | 05/10/2026 |

## Como rodar

```bat
REM Domínio Java (legado MVP)
mvp\1-EXECUTAR-MVP.bat

REM Front React (idealização)
cd frontend
npm install
npm run dev
```

## Estrutura

```text
frontend/          # React — supervisão web
mvp/               # Java EDA executável (legado Swing)
docs/ui/           # Idealização de telas + exports
docs/marco1/       # Artefatos Marco 1 (diagramas essenciais)
docs/semanas/      # Entregas individuais + RESUMO-MERGE
docs/legado-aps/   # PDF APS histórico
```
