# Checklist - Nome de cartao e fatura ao excluir

Codigo: `CHK-DEV-20`
Historia base: `HIST-DEV-02`, `HIST-DEV-04`, `HIST-DEV-05`
Historias gerais relacionadas: `HIST-GERAL-03`, `HIST-GERAL-04`
Escopo: numero sequencial de cartao, nome do item e preservacao de divida ao excluir.

- [x] Criar numero sequencial de criacao por conta.
- [x] Garantir que cartao excluido nao reutilize numero antigo.
- [x] Nomear novo cartao como numero-dd-mm-aaaa.
- [x] Enviar nome do cartao para o item criado.
- [x] Manter numero da conta na descricao do cartao.
- [x] Mostrar nome do cartao na lista do ATM.
- [x] Preservar divida e entradas de fatura ao excluir cartao de credito.
- [x] Confirmar que faturas de cartoes excluidos continuam na ordem de pagamento.
- [x] Evitar divida orfa usando exclusao logica do cartao.
