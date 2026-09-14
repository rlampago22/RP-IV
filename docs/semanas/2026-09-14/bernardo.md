# Relatório semanal — Bernardo Dorneles

- **Semana:** 2 (entrega 14/09/2026)
- **Issue:** #18
- **Trilha:** ControleReator, medições
- **Data:** 2026-09-14

## O que entreguei

- `ReatorFacade.receberLeitura` registra medição em memória
- Uso de `MedicaoReator.registrarMedicao(...)` na entidade
- `Sensor` com construtor simples para a demo
- Classe `DemoMedicoes` (main) mostrando 3 leituras no console
- Histórico consultável via `consultarHistorico` (lista em memória)

## Evidências

- Issue: [https://github.com/rlampago22/RP-IV/issues/18](https://github.com/rlampago22/RP-IV/issues/18)
- Arquivos:
  - `src/main/java/.../controlereator/ReatorFacade.java`
  - `src/main/java/.../controlereator/Sensor.java`
  - `src/main/java/.../controlereator/DemoMedicoes.java`
  - `docs/semanas/2026-09-14/bernardo.md`

## Como rodar a demo

No Explorer ou no terminal (mais fácil no Windows):

```bat
scripts\run-demo-medicoes.bat
```

Ou manualmente (PowerShell — listar os `.java`, sem `*`):

```powershell
cd c:\Users\bNd\Desktop\RP4\RP-IV
mkdir out -Force
javac -encoding UTF-8 -d out (Get-ChildItem src\main\java\br\edu\unipampa\usina\controlereator\*.java).FullName
java -cp out br.edu.unipampa.usina.controlereator.DemoMedicoes
```

## Bloqueios / dúvidas

- Nenhum. Integração com EventBus (Bruno) e limiar/alarme (José) fica nas próximas entregas.

## Próxima semana (previsto)

- Issue #23 — histórico + alinhar SEQ-UC01 ao código

