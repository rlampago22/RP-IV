# Resumo da semana 2 — merge em desenvolvimento

- **Período:** 08/09–15/09/2026
- **Data do resumo:** 2026-09-15
- **Escopo:** EDA only + idealização UI web + consolidação entregas

## Visão geral

O grupo consolidou o núcleo Must em Java (EventBus, medições, limiar/alarme, auditoria) via pasta `mvp/`, alinhou docs para **somente Arquitetura Orientada a Eventos**, e iniciou a direção de entrega vertical **backend Java + frontend React**, com idealização de telas SCADA (substituindo o visual Swing).

## Por integrante


| Aluno    | Issue   | Back                                       | Front        | Resultado                                                 |
| -------- | ------- | ------------------------------------------ | ------------ | --------------------------------------------------------- |
| Álvaro   | #11 #16 | —                                          | —            | Spec RF/MoSCoW/MVP + checklist aceite                     |
| Bruno    | #12 #17 | EventBus (em `mvp/`, via integração grupo) | —            | Pacotes/diagramas ainda pendentes de evidência individual |
| Bernardo | #13 #18 | `receberLeitura` + demo                    | —            | Entregue; próximo: API para T02                           |
| José     | #14 #19 | Alarmes/Strategy em `mvp/`                 | —            | Código no MVP; evidência S1 GoF ainda aberta              |
| Marcus   | #15 #20 | AuditoriaSubscriber                        | Swing legado | UC/SEQ + auditoria; UI Swing marcada como legado          |


## Eventos EDA tocados

`MedicaoRegistrada`, `AlarmeEmitido`, `AlarmeReconhecido`, `AlarmeResolvido`, falha/observação (MVP).

## Telas T0X

Idealização documentada em `docs/ui/idealizacao-telas.md` + export T01 em `docs/ui/exports/`.  
Scaffold React em `frontend/` (rotas T01–T05).

## Como rodar

```bat
REM legado Swing (referência de fluxo)
mvp\1-EXECUTAR-MVP.bat

REM front idealizado (após npm install)
cd frontend
npm install
npm run dev
```

## Débitos / próxima sprint

- Ativar **Figma MCP** e materializar frames T01–T05
- Bruno/José: fechar issues abertas com relatório vertical
- Expor API HTTP a partir do domínio Java para o React
- Migração gradual `mvp/` → `backend/`

