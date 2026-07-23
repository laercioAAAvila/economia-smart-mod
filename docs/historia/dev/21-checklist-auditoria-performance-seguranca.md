# Checklist - Auditoria de performance e seguranca

Codigo: `CHK-DEV-21`
Historia base: `HIST-DEV-02`, `HIST-DEV-03`, `HIST-DEV-05`
Historias gerais relacionadas: `HIST-GERAL-03`, `HIST-GERAL-04`, `HIST-GERAL-08`
Escopo: seguranca de acoes de cartao, payloads e preservacao de dividas.

- [x] Exigir senha para bloquear cartao pela lista do ATM.
- [x] Exigir senha para excluir/desativar cartao pela lista do ATM.
- [x] Preservar divida e fatura ao desativar cartao de credito.
- [x] Evitar leituras repetidas de campos na listagem de cartoes.
- [x] Ajustar marcador de exclusao para nao tratar bloqueio como exclusao.
- [x] Reforcar validacao defensiva do payload de lista de cartoes.
- [x] Manter UI alinhada sem novos campos grandes.
