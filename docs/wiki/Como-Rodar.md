# Como Rodar

## Pré-requisitos

- JDK para o MVP Java
- Node.js 20+ e npm (frontend)

## Domínio Java (MVP)

```bat
cd mvp
0-ABRIR-SISTEMA-GRAFICO.vbs
2-TESTAR-MVP.bat
```

- README: [`mvp/README.md`](https://github.com/rlampago22/RP-IV/blob/desenvolvimento/mvp/README.md)
- Roteiro 5–10 min: [`mvp/ROTEIRO-APRESENTACAO.md`](https://github.com/rlampago22/RP-IV/blob/desenvolvimento/mvp/ROTEIRO-APRESENTACAO.md)

## Frontend React

```bash
cd frontend
cp .env.example .env   # se ainda não existir
npm install
npm run dev
```

Abra a URL do Vite (em geral `http://localhost:5173`).

## Protótipo HTML (sem build)

Abra no navegador:

`docs/ui/propostas/opcao-a.html`

## Branch de trabalho

Sempre sincronize `desenvolvimento` antes de começar:

```bash
git checkout desenvolvimento
git pull origin desenvolvimento
git checkout <sua-branch>
git merge desenvolvimento
```
