# Checklist de Integridade e Performance

Codigo: `CHK-DEV-11`
Historia base: `HIST-DEV-02`, `HIST-DEV-03`
Historias gerais relacionadas: `HIST-GERAL-08`, `HIST-GERAL-10`
Escopo: recuperacao, idempotencia, indices e riscos de performance.

- [x] Recuperar operacoes financeiras em `ROLLBACK_REQUIRED`, nao apenas `SQL_COMMITTED`.
- [x] Tratar resultado incerto em batch de slots como falha conservadora.
- [x] Paginar processamento do job de juros para evitar lote unico grande.
- [x] Alinhar sequencia de numeros de conta com dados ja existentes.
- [x] Criar indice para busca de operacoes pendentes por estado/data.
- [x] Usar `requestId` de payload nas chaves de idempotencia de bancada, loja e transferencia do ATM.
- [x] Manter HikariCP, cache TTL de precos de ouro e indices de performance ja adicionados.
- [ ] Avaliar execucao async para jobs administrativos pesados.
- [ ] Avaliar retry visual/feedback quando salvamento de slot falhar por conflito de versao.
- [ ] Avaliar request id tambem para comandos diretos, caso comandos passem a ser chamados por UI externa.
