# Fluxos tecnicos das features

## Criar conta

Entrada:

- Comando `/economia conta criar <usuario>`
- Tela de senha e confirmacao

Fluxo:

```text
1. Validar usuario normalizado.
2. Validar que jogador nao possui conta.
3. Validar senha recebida por tela, nunca por chat.
4. Gerar salt e hash.
5. Criar conta ACTIVE.
6. Criar auditoria basica.
```

## Login

Fluxo:

```text
1. Jogador executa `/economia login <usuario>`.
2. Servidor abre tela de senha.
3. Servidor valida hash.
4. Sessao e guardada somente em memoria.
5. Sessao expira por logout, desconexao, reinicio ou timeout.
```

## Deposito

Fluxo:

```text
1. Jogador insere notas em slots permitidos.
2. Cliente envia confirmacao.
3. Servidor revalida itens.
4. Servidor calcula total.
5. Servidor remove notas.
6. SQL credita saldo.
7. Ledger registra credito.
```

Se qualquer validacao falhar, nenhuma nota e consumida.

## Saque

Fluxo:

```text
1. Jogador informa valor inteiro.
2. Servidor valida valor positivo.
3. Servidor valida saldo disponivel.
4. Servidor calcula notas pelas maiores denominacoes.
5. Servidor valida espaco no inventario.
6. SQL debita saldo.
7. Servidor entrega notas.
```

Saldo disponivel:

```text
max(0, balance - credit_principal_outstanding - credit_interest_outstanding)
```

## Compra de cartao

Fluxo:

```text
1. Exigir sessao por usuario e senha.
2. Validar limite de quantidade ativa por tipo.
3. Validar saldo disponivel para a taxa de emissao.
4. Validar espaco para cartao.
5. Criar registro SQL do cartao.
6. Debitar taxa de emissao.
7. Entregar item de cartao.
8. Registrar o nome e numero sequencial de criacao.
```

## Compra no debito

Fluxo:

```text
1. Validar cartao no SQL.
2. Confirmar funcao debito e status ACTIVE.
3. Confirmar conta ativa.
4. Bloquear conta do comprador e vendedor.
5. Validar saldo disponivel.
6. Debitar comprador.
7. Creditar vendedor.
8. Registrar transacao e ledger.
9. Entregar item.
```

## Compra no credito

Fluxo:

```text
1. Validar cartao no SQL.
2. Confirmar funcao credito e status ACTIVE.
3. Bloquear conta e cartao.
4. Validar limite individual.
5. Validar limite global.
6. Validar saldo garantidor.
7. Aumentar principal do cartao e da conta.
8. Creditar vendedor.
9. Criar lancamento de fatura.
10. Registrar transacao e ledger.
11. Entregar item.
```

## Pagamento de fatura

Fluxo:

```text
1. Jogador emite/reemite a fatura mais antiga disponivel ou escolhe pagar tudo.
2. Servidor valida a fatura da conta conectada e o saldo bancario.
3. Buscar lancamentos em aberto em ordem cronologica.
4. Quitar juros mais antigos.
5. Quitar principal mais antigo.
6. Reduzir divida do cartao e da conta.
7. Reduzir saldo bancario.
8. Ajustar limite configurado se ficar acima do limite elegivel pelo saldo.
```

## Aplicacao diaria de juros

Fluxo:

```text
1. DailyJob calcula datas pendentes.
2. Para cada cartao elegivel, bloquear conta e cartao.
3. Verificar economy_interest_accruals.
4. Calcular juros inteiro e resto.
5. Criar DAILY_INTEREST.
6. Atualizar juros acumulados.
7. Criar fatura, ledger e auditoria.
```

## Loja de Venda

Pagamento fisico:

```text
1. Revalidar loja ativa, preco e quantidade.
2. Revalidar item de referencia.
3. Validar estoque interno suficiente.
4. Validar dinheiro fisico inserido pelo cliente.
5. Mover pagamento para o caixa interno da loja.
6. Remover produto do estoque.
7. Entregar produto ao cliente.
8. Persistir estoque e caixa.
```

Pagamento por cartao:

```text
1. Revalidar loja ativa, preco e quantidade.
2. Validar cartao inserido pelo cliente.
3. Localizar conta ativa do dono da loja.
4. Usar a selecao Debito/Credito enviada pela tela.
5. Executar debito ou credito para a conta do dono.
6. Remover produto do estoque.
7. Entregar produto ao cliente.
8. Persistir estoque.
```

## Loja de Compra

Financiamento fisico:

```text
1. Revalidar loja ativa, preco e quantidade.
2. Validar item entregue pelo jogador contra o item de referencia.
3. Validar espaco no armazem de itens comprados.
4. Validar dinheiro suficiente na reserva fisica.
5. Entregar notas ao jogador.
6. Remover itens do slot do jogador.
7. Mover itens para o armazem de comprados.
8. Persistir estoque e caixa.
```

Financiamento por saldo:

```text
1. Validar conta vinculada.
2. Debitar saldo disponivel do proprietario.
3. Gerar notas para vendedor.
4. Guardar item na loja.
```

Financiamento por credito:

```text
1. Validar cartao de credito do proprietario.
2. Aumentar principal.
3. Gerar notas para vendedor.
4. Guardar item na loja.
```

## Bancada do Banco

Venda de item comum ao banco:

```text
1. Validar oferta.
2. Bloquear oferta, estoque e tesouraria.
3. Calcular preco atual.
4. Validar saldo da tesouraria.
5. Receber item.
6. Pagar jogador.
7. Atualizar estoque e estatistica diaria.
8. Registrar transacao.
```

Compra de item comum do banco:

```text
1. Validar estoque.
2. Bloquear oferta, estoque e tesouraria.
3. Recalcular preco progressivo.
4. Receber pagamento.
5. Creditar tesouraria.
6. Entregar item.
7. Atualizar demanda diaria.
```

Venda de ouro ao banco:

```text
1. Validar sessao bancaria ativa.
2. Validar todos os slots internos de ouro monetario.
3. Converter pepitas, lingotes e blocos para unidades de pepita.
4. Calcular valor dinamico por pepita no servidor.
5. Aplicar desconto por ouro vendido ao banco nas ultimas 24 horas.
6. Aplicar recuperacao/aumento por dias sem venda de ouro ao banco.
7. Limitar o preco entre 50% e 150% do valor-base.
8. Atualizar reserva de ouro.
9. Emitir moeda na conta do cartao inserido.
10. Limpar os slots convertidos.
11. Registrar GOLD_MINT com o valor por pepita usado.
```

Interface da Bancada do Banco:

```text
1. Mostrar valor atual pago por pepita.
2. Mostrar valor derivado para barra.
3. Mostrar valor derivado para bloco.
4. Mostrar o total estimado do ouro colocado no inventario de troca.
5. Desativar a troca se nao houver cartao valido inserido.
```

Compra de ouro do banco:

```text
1. Validar estoque e reserva.
2. Receber moeda.
3. Recolher moeda de circulacao.
4. Reduzir reserva.
5. Entregar ouro.
6. Registrar GOLD_REDEMPTION.
```

## Administracao

Todo comando administrativo deve:

- Validar permissao.
- Executar via servico de dominio.
- Gerar auditoria.
- Nunca editar ledger antigo.
- Usar estorno para correcao financeira.
