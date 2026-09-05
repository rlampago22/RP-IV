# 08 — Mapeamento Relacional (núcleo MVP corrigido)

**Sistema:** Controle de Usina Nuclear — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol

Correções do feedback APS (nota 4) aplicadas ao **recorte Must**. Tipos usam `VARCHAR` / `INTEGER` / `DECIMAL` / `TIMESTAMP` (não existe `String` em SQL). Enums viram **tabelas de domínio**. Comparação de chaves conforme multiplicidades (sem N:N injustificado).

Fonte: [diagramas/er-nucleo-mvp.puml](diagramas/er-nucleo-mvp.puml)

---

## 1. Tabelas de domínio (enums)

| Tabela | PK | Colunas | Substitui |
|--------|----|---------|-----------|
| `status_operacional` | `id_status_operacional` INT | `codigo VARCHAR(40) UNIQUE`, `descricao VARCHAR(120)` | StatusOperacionalEnum |
| `status_sensor` | `id_status_sensor` INT | `codigo VARCHAR(40)`, `descricao VARCHAR(120)` | StatusSensorEnum |
| `tipo_sensor` | `id_tipo_sensor` INT | `codigo VARCHAR(40)`, `descricao VARCHAR(120)` | TipoSensorEnum |
| `tipo_alarme` | `id_tipo_alarme` INT | `codigo VARCHAR(40)`, `descricao VARCHAR(120)` | TipoAlarmeEnum |
| `status_alarme` | `id_status_alarme` INT | `codigo VARCHAR(40)`, `descricao VARCHAR(120)` | StatusAlarmeEnum |

---

## 2. Tabelas de entidades do núcleo

### `reator`

| Coluna | Tipo | Restrição |
|--------|------|-----------|
| id_reator | INT | PK |
| codigo | VARCHAR(40) | UNIQUE NOT NULL |
| id_status_operacional | INT | FK → status_operacional NOT NULL |

### `turbina` (relacionamento corrigido)

| Coluna | Tipo | Restrição |
|--------|------|-----------|
| id_turbina | INT | PK |
| codigo | VARCHAR(40) | UNIQUE NOT NULL |
| id_reator | INT | FK → reator (**FK em Turbina aponta para Reator**; Reator não “pertence” a Turbina) |
| id_status_operacional | INT | FK → status_operacional |

### `sensor`

| Coluna | Tipo | Restrição |
|--------|------|-----------|
| id_sensor | INT | PK |
| codigo | VARCHAR(40) | UNIQUE NOT NULL |
| id_reator | INT | FK → reator |
| id_tipo_sensor | INT | FK → tipo_sensor |
| id_status_sensor | INT | FK → status_sensor |

### `medicao_reator`

| Coluna | Tipo | Restrição |
|--------|------|-----------|
| id_medicao | INT | PK |
| id_sensor | INT | FK → sensor **NOT NULL** (faltava no feedback) |
| id_reator | INT | FK → reator NOT NULL |
| valor | DECIMAL(18,6) | NOT NULL |
| unidade | VARCHAR(20) | NOT NULL |
| registrado_em | TIMESTAMP | NOT NULL |

### `alarme`

| Coluna | Tipo | Restrição |
|--------|------|-----------|
| id_alarme | INT | PK |
| id_medicao | INT | FK → medicao_reator |
| id_sensor | INT | FK → sensor (**relação Sensor–Alarme**) |
| id_tipo_alarme | INT | FK → tipo_alarme |
| id_status_alarme | INT | FK → status_alarme |
| mensagem | VARCHAR(255) | |
| emitido_em | TIMESTAMP | NOT NULL |

### `registro_auditoria`

| Coluna | Tipo | Restrição |
|--------|------|-----------|
| id_registro | INT | PK |
| tipo_evento | VARCHAR(80) | NOT NULL |
| payload | VARCHAR(2000) | |
| registrado_em | TIMESTAMP | NOT NULL |

### `limiar`

| Coluna | Tipo | Restrição |
|--------|------|-----------|
| id_limiar | INT | PK |
| id_tipo_sensor | INT | FK → tipo_sensor |
| parametro | VARCHAR(40) | NOT NULL |
| valor_min | DECIMAL(18,6) | |
| valor_max | DECIMAL(18,6) | |

---

## 3. Should (acesso) — recorte mínimo

### `registro_acesso`

| Coluna | Tipo | Restrição |
|--------|------|-----------|
| id_registro_acesso | INT | PK |
| credencial | VARCHAR(80) | NOT NULL |
| area | VARCHAR(80) | NOT NULL |
| resultado | VARCHAR(20) | NOT NULL (ou FK para tabela dominio_resultado_acesso) |
| registrado_em | TIMESTAMP | NOT NULL |

**Não** incluir `id_cargo` nesta tabela (feedback APS).

---

## 4. O que NÃO entra no ER do Marco 1 (backlog documentado)

| Problema APS | Decisão |
|--------------|---------|
| ContingenciaEnum / Emergencia enums → tabelas | Could — quando RF09/RF10 entrarem |
| Emergencia_Alarme N:N / FKs excessivas | Removido; usar 1:N `alarme.id_emergencia` se necessário no futuro |
| Agregação no modelo relacional | Não modelar agregação UML no ER |
| Capacitacao/Treinamento invertidos, Alocacao, FuncionarioEnum | Won't MVP |
| Material/Movimentacao FKs | Backlog RF15 |
| Componente–Evacuacao / Substituicao–Manutencao | Backlog manutenção |

---

## 5. Comparação de chaves (exemplos)

| Relação | Cardinalidade | Onde fica a FK |
|---------|---------------|----------------|
| Reator 1 — * Turbina | 1:N | `turbina.id_reator` |
| Reator 1 — * Sensor | 1:N | `sensor.id_reator` |
| Sensor 1 — * Medicao | 1:N | `medicao_reator.id_sensor` |
| Sensor 1 — * Alarme | 1:N | `alarme.id_sensor` |
| Medicao 1 — * Alarme | 1:N | `alarme.id_medicao` |

Nenhuma tabela associativa N:N no núcleo.
