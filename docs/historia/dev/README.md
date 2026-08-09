# Documentacao tecnica do Mod de Economia

Esta pasta traduz a historia funcional de `docs/historia/geral` em decisoes tecnicas para implementar o mod em Minecraft Java 1.21.1 com NeoForge 21.1.234+ e Java 21.

## Codigos de referencia

- Historias gerais: `HIST-GERAL-01` ate `HIST-GERAL-10`, seguindo a numeracao dos arquivos em `docs/historia/geral`.
- Historias tecnicas: `HIST-DEV-01` ate `HIST-DEV-06`, seguindo a numeracao dos arquivos tecnicos base.
- Checklists: `CHK-DEV-07` em diante, seguindo a numeracao dos arquivos de checklist.

## Ordem de leitura

| Codigo | Arquivo | Referencia principal |
| --- | --- | --- |
| `HIST-DEV-01` | `01-arquitetura-modulos.md` | `HIST-GERAL-01` a `HIST-GERAL-10` |
| `HIST-DEV-02` | `02-persistencia-sql.md` | `HIST-GERAL-07`, `HIST-GERAL-08` |
| `HIST-DEV-03` | `03-transacoes-idempotencia.md` | `HIST-GERAL-08` |
| `HIST-DEV-04` | `04-itens-blocos-inventarios.md` | `HIST-GERAL-01`, `HIST-GERAL-04`, `HIST-GERAL-05`, `HIST-GERAL-06` |
| `HIST-DEV-05` | `05-fluxos-features.md` | `HIST-GERAL-02`, `HIST-GERAL-03`, `HIST-GERAL-04`, `HIST-GERAL-05`, `HIST-GERAL-06` |
| `HIST-DEV-06` | `06-fases-implementacao.md` | Planejamento tecnico geral |
| `CHK-DEV-07` | `07-checklist-implementacao.md` | `HIST-DEV-01` a `HIST-DEV-06` |
| `CHK-DEV-08` | `08-checklist-auditoria-finalizacao.md` | `HIST-GERAL-05`, `HIST-GERAL-08`, `HIST-GERAL-10` |
| `CHK-DEV-09` | `09-checklist-compatibilidade-itens-modded.md` | `HIST-GERAL-05`, `HIST-GERAL-10` |
| `CHK-DEV-10` | `10-checklist-texturas.md` | `HIST-GERAL-01`, `HIST-GERAL-04`, `HIST-GERAL-05`, `HIST-GERAL-06` |
| `CHK-DEV-11` | `11-checklist-integridade-performance.md` | `HIST-GERAL-08`, `HIST-GERAL-10` |
| `CHK-DEV-12` | `12-checklist-atm-seguranca-credito-ui.md` | `HIST-GERAL-03`, `HIST-GERAL-04` |
| `CHK-DEV-13` | `13-checklist-pedido-credito-atm.md` | `HIST-GERAL-03`, `HIST-GERAL-04` |
| `CHK-DEV-14` | `14-checklist-cartoes-senha-lojas-jei.md` | `HIST-GERAL-03`, `HIST-GERAL-04`, `HIST-GERAL-05` |
| `CHK-DEV-15` | `15-checklist-lojas-limite-texturas-acentos.md` | `HIST-GERAL-04`, `HIST-GERAL-05` |
| `CHK-DEV-16` | `16-checklist-loja-compra-inventario-venda.md` | `HIST-GERAL-05` |
| `CHK-DEV-17` | `17-checklist-lojas-cartoes-fatura-ui.md` | `HIST-GERAL-03`, `HIST-GERAL-04`, `HIST-GERAL-05`, `HIST-GERAL-06` |
| `CHK-DEV-18` | `18-checklist-auditoria-credito.md` | `HIST-GERAL-03` |
| `CHK-DEV-19` | `19-checklist-fatura-cartoes-atm.md` | `HIST-GERAL-03`, `HIST-GERAL-04` |
| `CHK-DEV-20` | `20-checklist-nome-cartao-fatura-exclusao.md` | `HIST-GERAL-03`, `HIST-GERAL-04` |
| `CHK-DEV-21` | `21-checklist-auditoria-performance-seguranca.md` | `HIST-GERAL-03`, `HIST-GERAL-04`, `HIST-GERAL-08` |
| `CHK-DEV-22` | `22-checklist-ui-atm-concorrencia-dinheiro.md` | `HIST-GERAL-02`, `HIST-GERAL-03`, `HIST-GERAL-04`, `HIST-GERAL-08` |
| `CHK-DEV-23` | `23-checklist-cartoes-tooltip-lojas-feedback.md` | `HIST-GERAL-03`, `HIST-GERAL-05` |
| `CHK-DEV-24` | `24-checklist-botoes-lojas-feedback.md` | `HIST-GERAL-05` |
| `CHK-DEV-25` | `25-checklist-correio.md` | Correio, pagamento e permissões comerciais |
| `HIST-DEV-26` | `26-especificacao-funcional-completa-mapa-clas-propriedades-privadas-claims.md` | Especificação funcional recebida |
| `HIST-DEV-27` | `27-arquitetura-mapa-clas-propriedades-privadas-claims.md` | Arquitetura de mapa, grupos, claims e permissões |
| `CHK-DEV-28` | `28-checklist-mapa-clas-propriedades-privadas-claims.md` | `HIST-DEV-26` e `HIST-DEV-27` |
| `HIST-DEV-29` | `29-atualizacao-claims-propriedades-privadas-ancoras-vendas.md` | Atualização de claims, boletos, âncoras e vendas |
| `CHK-DEV-30` | `30-checklist-atualizacao-claims-propriedades-privadas.md` | `HIST-DEV-29` |

## Principios tecnicos

- O servidor e a unica autoridade financeira.
- O cliente apenas envia intencoes e renderiza interfaces.
- SQL e a fonte oficial para contas, saldos, cartoes, lojas, bancada, estoque, auditoria e transacoes.
- Valores monetarios usam `long` no Java e `BIGINT` no SQL.
- Nao usar `float`, `double` ou centavos visiveis.
- Toda operacao financeira usa transacao SQL, chave de idempotencia e registro de auditoria quando aplicavel.
- Blocos no mundo guardam somente UUID tecnico e dados visuais minimos.
- Inventarios comerciais persistentes ficam no SQL.
- Nenhuma falha de servidor pode fazer jogador perder dinheiro ou item.
