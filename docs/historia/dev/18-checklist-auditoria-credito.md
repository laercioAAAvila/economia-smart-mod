# Checklist - Auditoria de credito por saldo

Codigo: `CHK-DEV-18`
Historia base: `HIST-DEV-02`, `HIST-DEV-05`
Historias gerais relacionadas: `HIST-GERAL-03`
Escopo: auditoria das regras de credito solicitado por saldo.

- [x] Confirmar que o credito nao e concedido automaticamente ao entrar ou atualizar conta.
- [x] Manter concessao apenas no fluxo de pedir credito.
- [x] Aplicar faixas por saldo: 40%, 60%, 80% e 95%.
- [x] Bloquear pedido de credito quando existir divida aberta.
- [x] Impedir ajuste manual da conta acima do limite permitido pela faixa.
- [x] Fazer compras e limite de cartao respeitarem o menor valor entre limite salvo e faixa atual.
- [x] Rebaixar limite configurado quando o saldo cair abaixo da faixa permitida.
- [x] Ajustar tela do caixa para o limite da conta ficar somente como leitura.
- [x] Adicionar mensagem para limite acima do permitido.
