# Mod de Economia — Documentação dividida

Esta pasta contém a mesma história funcional do arquivo completo, separada em partes menores para facilitar a implementação sem perder dependências entre os módulos.

## Ordem recomendada de leitura

1. `01-visao-geral-itens-dinheiro.md`
2. `02-contas-autenticacao-saldo.md`
3. `03-credito-cartoes-juros-faturas.md`
4. `04-caixa-eletronico.md`
5. `05-lojas.md`
6. `06-bancada-ouro-precos-dinamicos.md`
7. `07-blocos-e-modelo-sql.md`
8. `08-integridade-recuperacao.md`
9. `09-permissoes-administracao.md`
10. `10-criterios-e-desenvolvimento.md`

## Regras novas desta revisão

- O crédito da conta não é concedido automaticamente: o jogador precisa pedir crédito no Caixa Eletrônico.
- O pedido de crédito usa faixas por saldo bancário: 40%, 60%, 80% e 95%.
- Cartões exibem tipo, conta, limite diário de débito e limite de crédito quando aplicável.
- Cartões podem ser listados, bloqueados e desativados no Caixa Eletrônico sem apagar dívidas.
- Faturas podem ser reemitidas, pagas da mais antiga para a mais nova ou quitadas em lote.
- Lojas de venda permitem selecionar débito ou crédito ao pagar com cartão combinado.
- Lojas mostram mensagens simples quando uma compra ou venda não pode ser concluída.
- A Bancada do Banco não possui craft.
- Ouro pode ser vendido ao banco para emitir dinheiro lastreado.
- Pepita, lingote e bloco usam conversão matemática exata.
- Dívidas de cartão recebem juros diários configuráveis.
- Juros são processados de forma idempotente no SQL.
- Ofertas da Bancada do Banco podem usar preços dinâmicos.
- Grande demanda diária aumenta o preço de venda.
- Excesso de oferta pode reduzir o preço pago pelo banco.
- Dias sem movimentação fazem o preço voltar gradualmente ao valor-base.
