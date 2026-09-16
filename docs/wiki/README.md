# Wiki — fonte versionada

Estas páginas são a **wiki oficial do RP-IV**, versionadas no Git.

## No GitHub

Aba **Wiki** do repositório: https://github.com/rlampago22/RP-IV/wiki  

Se a aba ainda estiver vazia, qualquer membro com permissão de escrita cria a primeira página (`Create the first page`), salva, e em seguida roda:

```powershell
.\scripts\publish-wiki.ps1
```

Isso copia `docs/wiki/*.md` para o repositório `RP-IV.wiki.git`.

## Editar

1. Altere os `.md` em `docs/wiki/`
2. Abra PR → `desenvolvimento`
3. Rode `publish-wiki.ps1` (ou peça a quem tiver o wiki já inicializado)
