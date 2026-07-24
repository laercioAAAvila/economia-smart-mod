# Checklist - Botoes de lojas e feedback de troca

Codigo: `CHK-DEV-24`
Historia base: `HIST-DEV-04`, `HIST-DEV-05`
Historias gerais relacionadas: `HIST-GERAL-05`
Escopo: manter botoes Comprar/Vender clicaveis e retornar erro claro sem quebrar regras da loja.

- [x] Confirmar que o botao Comprar/Vender do cliente fica ativo mesmo quando a troca nao esta pronta.
- [x] Manter validacao definitiva no servidor antes de alterar itens, saldo, estoque ou limite.
- [x] Loja de Compra: separar erro de metodo de recebimento ausente.
- [x] Loja de Compra: separar erro de cartao de recebimento invalido.
- [x] Loja de Compra: separar erro de conta ativa do dono ausente.
- [x] Loja de Compra: separar erro de saldo insuficiente da loja.
- [x] Loja de Venda: manter diagnostico de pagamento por cartao, dinheiro, debito, credito e limite diario.
- [x] Adicionar mensagens simples para o jogador entender por que a troca nao ocorreu.
