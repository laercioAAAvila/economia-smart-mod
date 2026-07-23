# Checklist - Fatura e cartoes no ATM

Codigo: `CHK-DEV-19`
Historia base: `HIST-DEV-04`, `HIST-DEV-05`
Historias gerais relacionadas: `HIST-GERAL-03`, `HIST-GERAL-04`
Escopo: reemissao, pagamento de faturas e lista de cartoes no ATM.

- [x] Permitir reemitir fatura perdida na aba de credito.
- [x] Emitir sempre a fatura mais antiga em aberto.
- [x] Ao pagar uma fatura, gerar a proxima em aberto quando existir.
- [x] Adicionar botao para pagar todas as faturas em aberto.
- [x] Manter pagamento individual seguindo a ordem da mais antiga para a mais nova.
- [x] Adicionar lista rolavel de cartoes da conta no ATM.
- [x] Permitir bloquear cartao perdido pela lista.
- [x] Permitir excluir cartao pela lista usando desativacao logica.
- [x] Preservar divida de cartao de credito excluido.
- [x] Ajustar alinhamento da aba de credito e da aba de cartoes.
