# RP-IV — Sistema de Controle de Usina Nuclear

[![Arquitetura](https://img.shields.io/badge/Architecture-EDA%20(Event%20Driven)-blue.svg)](docs/marco1/04-arquitetura-eda.md)
[![UI](https://img.shields.io/badge/UI-Opção%20A%20SCADA%20escuro-0B0F19.svg)](docs/ui/direcao-opcao-a.md)
[![Unipampa](https://img.shields.io/badge/Unipampa-Engenharia%20de%20Software-red.svg)](https://unipampa.edu.br/alegrete/)

Repositório da disciplina **AL0343 — Resolução de Problemas IV** (UNIPAMPA Alegrete).  
Continuidade do projeto de **Análise e Projeto de Software (APS)** — Central de Supervisão do Reator.

> Trabalho ativo em [`desenvolvimento`](https://github.com/rlampago22/RP-IV/tree/desenvolvimento). A `main` fica limpa até a consolidação final.

---

## Sobre o projeto

Sistema de **supervisão nuclear acadêmica** com:

- telemetria contínua dos **4 parâmetros RF-1** (temperatura, pressão, radiação, fluxo);
- detecção de limiar e ciclo de **alarmes** (emitir → reconhecer → resolver);
- **autenticação** na Central (RF07) e **registro de acesso** (RF08);
- **auditoria append-only** com cadeia SHA-256;
- UI web **Opção A (SCADA escuro)** — DNA do MVP Swing do Marcus, redesenhado em React.

Arquitetura de domínio: Orientada a Eventos (**EDA**) — EventBus Java in-process (`mvp/`). Auth web: Spring em `backend/`.

---

## Estrutura do repositório (monorepo)

```text
RP-IV/
├── frontend/                 # React + Vite — UI oficial (Opção A)
├── mvp/                      # Java EDA executável (demo + Swing legado de fluxo)
├── backend/                  # Auth Spring (RF07) + destino de migração gradual
├── src/                      # Esqueleto Java legado (não usar para demo — ver src/README)
├── docs/                     # Engenharia, plano, UI, semanas
│   ├── DOCUMENTACAO_DE_ENGENHARIA.md
│   ├── PLANEJAMENTO_DESENVOLVIMENTO.md
│   ├── marco1/               # Artefatos Marco 1 (rascunho → entrega 05/10)
│   ├── mvp/                  # Spec + checklist aceite Must
│   ├── ui/                   # Idealização / Opção A / protótipos
│   ├── implementacao/        # Rastreabilidade do que já roda
│   └── semanas/              # Entregas individuais + STATUS
├── scripts/                  # Helpers (demo medições, conventional commit)
├── AGENTS.md                 # Contexto obrigatório para IAs
└── README.md
```

**Regra de ouro:** demo Must expandido = `mvp/` (domínio + `/api/estado`) + `backend/` (auth) + `frontend/` (login + SCADA). Domínio permanece EDA; auth não substitui o EventBus.

---

## Como rodar

### Como rodar (Must expandido)

```bat
REM 1) Auth (porta 8081, H2)
cd backend
set APP_BOOTSTRAP_PASSWORD=troque-esta-senha
mvn spring-boot:run

REM 2) API estado EDA (porta 8080)
cd mvp
4-EXECUTAR-API-ESTADO.bat

REM 3) UI
cd frontend
npm install
npm run dev
```

Login demo: `operador@usina.local` / `troque-esta-senha`

### Domínio Java (MVP) — testes

```bat
cd mvp
REM Interface gráfica Swing (legado de fluxo)
0-ABRIR-SISTEMA-GRAFICO.vbs

REM Testes Must (núcleo + alarmes HTTP + RF08)
2-TESTAR-MVP.bat
```

Detalhes: [`mvp/README.md`](mvp/README.md) · Roteiro: [`mvp/ROTEIRO-APRESENTACAO.md`](mvp/ROTEIRO-APRESENTACAO.md) · Auth: [`backend/README.md`](backend/README.md)

---

## Documentação e Wiki

**Wiki (navegação do time):** [`docs/wiki/Home.md`](docs/wiki/Home.md) · aba GitHub: https://github.com/rlampago22/RP-IV/wiki

| Documento | Onde | Descrição |
|-----------|------|-----------|
| Wiki / Home | [`docs/wiki/`](docs/wiki/) | Visão geral, EDA, UI, como rodar, equipe |
| Índice de docs | [`docs/README.md`](docs/README.md) | Mapa de toda a pasta `docs/` |
| Engenharia (hub) | [`docs/DOCUMENTACAO_DE_ENGENHARIA.md`](docs/DOCUMENTACAO_DE_ENGENHARIA.md) | RF/RNF, MoSCoW, MVP, UML, arquitetura |
| Planejamento | [`docs/PLANEJAMENTO_DESENVOLVIMENTO.md`](docs/PLANEJAMENTO_DESENVOLVIMENTO.md) | Calendário até Marco 1 + issues |
| UI Opção A | [`docs/ui/direcao-opcao-a.md`](docs/ui/direcao-opcao-a.md) | Tokens, chrome, aceite visual |
| Implementação | [`docs/implementacao/00-INDICE.md`](docs/implementacao/00-INDICE.md) | O que já está no código |
| Status semanal | [`docs/semanas/STATUS.md`](docs/semanas/STATUS.md) | Issues abertas por sprint |
| Contribuição | [`CONTRIBUTING.md`](CONTRIBUTING.md) | Branches, PRs, entrega vertical |

---

## Stack

| Camada | Tecnologia | Pasta |
|--------|------------|-------|
| Domínio / EventBus | Java (sem libs externas no MVP) | `mvp/` |
| Auth (RF07) | Spring Boot + sessão | `backend/` |
| UI oficial | React + Vite | `frontend/` |
| Idealização | HTML protótipo + Figma | `docs/ui/` |
| API estado | JDK `apiestado` | `mvp/` |

---

## MVP (Must expandido) — fluxo da demo

```text
Login (RF07) → T05 Cenário Crítico → EventBus → T01 Overview (núcleo CRÍTICO + banner)
                                            → T03 Alarmes (ACK → Resolver com identidade)
                                            → T04 Auditoria (trilha SHA-256)
             → UC04 RegistroAcesso (RF08) quando demonstrar área restrita
```

Priorização MoSCoW: [`docs/marco1/02-priorizacao-moscow.md`](docs/marco1/02-priorizacao-moscow.md)

---

## Equipe

| Integrante | Branch | Trilha |
|------------|--------|--------|
| Álvaro Domingues | `alvaro` | Requisitos, aceite, UX |
| Bruno Rocha | `bruno` | EventBus, API estado, T01 |
| Bernardo Dorneles | `bernardo` | Medições, T02 |
| José Guilherme Monteiro | `jose` | Alarmes / GoF, T03 |
| Marcus Querol | `marcus` | Auditoria / UCs, T04–T05 |

### Docentes

Prof. Dr. Fabio Paulo Basso · Prof. Dr. Gilleanes Thorwald Araujo Guedes

---

## Calendário (Marco 1)

| Entrega | Data |
|---------|------|
| Idealização Opção A + shell React | agora → 21/09/2026 |
| S3 — 1ª fatia vertical | 21/09/2026 |
| S4 — ensaio integrado | 28/09/2026 |
| Marco 1 | 05/10/2026 |

Issues: https://github.com/rlampago22/RP-IV/issues
