# Legado APS → fonte atualizada (RP IV)

## Arquivos

| Arquivo | Descrição |
|---------|-----------|
| [`APS-RPIV-ATUALIZADO-Diagrama-Implantacao.pdf`](APS-RPIV-ATUALIZADO-Diagrama-Implantacao.pdf) | Documento **ATUALIZADO** (69 págs.) usado como base do RP IV |
| [`../diagramas/astah/`](../diagramas/astah/) | Projeto Astah (casos de uso / modelo) |

Documento antigo de referência (fora do Git, pasta Desktop): `Aps8entrega (1).pdf` (77 págs., equipe APS anterior).

---

## O que mudou no PDF atualizado (vs APS antiga)

### Mudanças claras (texto)

1. **Título:** de *Análise e Projeto de Software — Oitava Entrega* para **Resolução de Problemas IV**.
2. **Equipe:** Gustavo / Kelvin / Luan / Juliano → **Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol** (Álvaro permanece).
3. **Volume:** 77 → **69** páginas (exportação mais leve; sumário interno ainda cita páginas até ~77 — inconsistência do próprio PDF).

### O que **não** mudou de forma relevante no texto extraído

- Escopo dentro/fora por domínio (reatores, vazamentos, acesso, combustível, ambiental, manutenção, cibernética, emergências, pessoal, relatórios).
- Lista **RF-1…RF-18** e **RNF-01…RNF-10** com MoSCoW.
- Estrutura de capítulos: UCs, classes, atividades, arquitetura EDA, sequências, componentes, ER, implantação.
- Seção **10.3 Alterações na arquitetura** (controllers/fachadas por módulo).
- Relato de uso de IA nos diagramas.

Conclusão: o PDF atualizado é essencialmente o **mesmo projeto APS**, rebatizado para RP IV e com a **equipe nova**. O recorte de MVP do RP IV continua necessário (não entregar o APS inteiro).

---

## Astah

- Fonte: `Downloads/Diagrama de Caso de Usos - Usina Nuclear.asta.zip` (o `.asta` solto no Downloads estava como placeholder de 1 byte / nuvem).
- No repo: `docs/diagramas/astah/Diagrama-Casos-Uso-Usina-Nuclear.asta` (+ `EntityStore`).
- Conteúdo identificado no modelo: atores/UCs amplos da APS (`Monitorar Reatores e Turbinas`, acesso, evacuação, radiação ambiental, etc.), entidades `Reator`, `Sensor`, `MedicaoReator`, `Turbina`, `Alarme`, `Emergencia`, controllers/visões MVC.

**Ação:** Marcus (UCs) deve **recortar** o Astah/PlantUML para o MVP (UC01–UC03 + UC04 Should), alinhado a `docs/marco1/diagramas/casos-de-uso-mvp.puml` — não versionar o modelo APS completo como “entrega final”.

---

## O que falta fazer no repositório (agora)

### Imediato (atraso / Semana 2 — entrega **15/09/2026**)

| Quem | Issue | Ação |
|------|-------|------|
| Álvaro | #16 | Critérios de aceite Must |
| Bruno | #17 | EventBus + `MedicaoRegistrada` |
| Bernardo | #18 | `receberLeitura` + medição em memória |
| José | #19 | Strategy limiar + criar Alarme |
| Marcus | #15/#20 + PR #36 | Fechar UCs Semana 1 e AuditoriaSubscriber |

### Repo / merge

- [ ] Mergear PRs abertos da Semana 1 (#36 Marcus, #37 Bernardo) em `desenvolvimento`
- [ ] Tratar `docs/marco1/` como rascunho alimentado por este PDF + recorte MVP (já existente)
- [ ] Não colocar `EntityStore` solto na raiz do projeto

### Até Marco 1 (05/10)

Seguir [`../plano-marco1.md`](../plano-marco1.md): Semanas 2→4 + apresentação. Escopo Must apenas.
