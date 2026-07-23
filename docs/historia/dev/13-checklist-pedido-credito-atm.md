# Checklist ATM: Pedido de Credito

Codigo: `CHK-DEV-13`
Historia base: `HIST-DEV-05`
Historias gerais relacionadas: `HIST-GERAL-03`, `HIST-GERAL-04`
Escopo: pedido manual de credito por faixa de saldo no Caixa Eletronico.

- [x] Confirmar que limite de cartoes continua limitado pelo credito da conta.
- [x] Criar acao de rede separada para pedido de credito.
- [x] Calcular credito solicitado por faixa de saldo: 40%, 60%, 80% e 95%.
- [x] Confirmar que credito nao e concedido automaticamente fora do fluxo de pedir credito.
- [x] Bloquear pedido de credito quando houver divida de credito.
- [x] Nao reduzir limite existente quando o valor elegivel for menor.
- [x] Adicionar cooldown servidor-side antes de consultar o banco.
- [x] Adicionar cooldown visual no botao da aba Credito.
- [x] Alinhar o botao de pedido de credito com os campos da aba Credito.
- [x] Adicionar mensagens de sucesso, divida, indisponibilidade e cooldown.
