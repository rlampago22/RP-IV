# Como contribuir — RP-IV

## Branches

1. Atualize `desenvolvimento`.
2. Trabalhe na sua branch pessoal (`alvaro`, `bernardo`, `bruno`, `jose`, `marcus`) **ou** em `fix/...` para ajustes estruturais.
3. Abra PR → `desenvolvimento`.
4. **Nunca** commit direto na `main`.

```bash
git checkout desenvolvimento
git pull
git checkout -b bernardo   # ou sua branch
```

## Commits

Conventional Commits (hook em `.githooks/`):

- `feat:`, `fix:`, `docs:`, `chore:`, `test:`, `refactor:`

Ativar hooks:

```bash
git config core.hooksPath .githooks
```

## Entrega vertical semanal

Cada PR deve incluir, quando for issue de sprint:

- [ ] Código da trilha (Java / doc)
- [ ] Pedaço React T0X no chrome **Opção A**
- [ ] Relatório `docs/semanas/AAAA-MM-DD/<nome>.md`

Templates: `docs/semanas/_template-entrega-vertical.md`

## UI

Referência visual obrigatória: [`docs/ui/propostas/opcao-a.html`](docs/ui/propostas/opcao-a.html)  
Spec: [`docs/ui/direcao-opcao-a.md`](docs/ui/direcao-opcao-a.md)

## Referências externas (somente leitura)

- SafePlace (Iuri): estrutura monorepo / README rico  
- Sentinela: `docs/DOCUMENTACAO_*`, `PLANEJAMENTO_*`, `implementacao/`, `analise/`

Não altere repositórios de outros grupos.
