# 01 — Arquitetura híbrida: EDA na supervisão e camadas/MVC no backend

**Origem:** feedback do professor de 04/11/2026.  
**Documento canônico detalhado:** [Arquitetura EDA](../marco1/04-arquitetura-eda.md).

## Decisão

O sistema não usa um único estilo para todas as responsabilidades. A supervisão operacional conserva EDA, enquanto a autenticação web usa um monólito Spring em camadas/MVC:

```text
Sensores / cenários → EventBus (mvp/) → alarmes e auditoria → API de estado → UI React
UI React → API HTTP → controllers → serviços → entidades/repositórios → banco (backend/)
```

Essa divisão deixa explícito que MVC/camadas organiza a borda web e a persistência de autenticação. Ela não substitui o EventBus, nem transforma o projeto em microserviços.

## Responsabilidades e limites

| Contexto | Implementação | Limite |
|----------|---------------|--------|
| Operação nuclear | `mvp/` com EventBus | Eventos de medição, alarme, auditoria e acesso físico. |
| Supervisão web | `frontend/` React | Consumo e apresentação do estado/eventos operacionais. |
| Autenticação da Central | `backend/` Spring Boot | HTTP, sessão, papéis, autenticação, 2FA futuro e registros de autenticação. |

O detalhamento de camadas, a relação com RF18 e as regras para a sessão estão em [`../marco1/04-arquitetura-eda.md`](../marco1/04-arquitetura-eda.md#6-backend-spring-camadasmvc).
