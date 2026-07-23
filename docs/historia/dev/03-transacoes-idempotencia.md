# Transacoes, idempotencia e recuperacao

## Regra central

Toda movimentacao financeira deve ser executada em transacao SQL. Nenhum saldo, estoque persistente, fatura, reserva de ouro ou preco dinamico pode ser alterado fora desse fluxo.

## Ordem de bloqueio

Para evitar deadlocks, bloquear sempre nesta ordem:

1. Contas por UUID ordenado
2. Cartoes por UUID
3. Bloco comercial
4. Inventarios
5. Ofertas

Nunca inverter a ordem em fluxos especificos.

## Chave de idempotencia

Cada acao confirmada pelo jogador deve receber uma chave unica controlada pelo servidor.

Exemplos:

- Deposito no Caixa Eletronico
- Saque
- Compra com cartao
- Compra em dinheiro
- Venda para loja
- Venda de ouro
- Pagamento de fatura
- Aplicacao diaria de juros
- Recuperacao diaria de precos

Se a mesma chave chegar novamente, o sistema deve retornar o resultado ja produzido sem executar nova alteracao.

## Fluxo padrao de operacao financeira

```text
1. Receber intencao do cliente.
2. Validar sessao, permissao e contexto.
3. Criar ou localizar idempotency_key.
4. Abrir transacao SQL.
5. Bloquear linhas na ordem oficial.
6. Revalidar todos os dados dentro da transacao.
7. Atualizar saldos, dividas, estoque ou reserva.
8. Criar economy_transactions.
9. Criar economy_ledger_entries.
10. Criar registros especificos do dominio.
11. Confirmar transacao SQL.
12. Entregar/remover itens no Minecraft.
13. Marcar economy_operations como concluida quando houver inventario envolvido.
```

## SQL e inventario Minecraft

Como o inventario do jogador nao faz parte da transacao SQL, fluxos com itens devem usar `economy_operations`.

### Exemplo: saque

```text
1. Criar operacao CREATED.
2. Verificar espaco no inventario.
3. Abrir transacao SQL.
4. Bloquear conta.
5. Validar saldo disponivel.
6. Debitar saldo.
7. Registrar transacao e ledger.
8. Commit SQL.
9. Entregar notas.
10. Marcar COMPLETED.
```

Se cair depois do commit e antes da entrega, a recuperacao deve entregar as notas ou estornar.

### Exemplo: compra em loja

```text
1. Criar operacao CREATED.
2. Reservar estoque da loja.
3. Marcar ITEMS_RESERVED.
4. Processar pagamento no SQL.
5. Marcar SQL_COMMITTED.
6. Entregar item ao jogador.
7. Marcar ITEMS_DELIVERED.
8. Marcar COMPLETED.
```

## Recuperacao na inicializacao

Ao iniciar o servidor:

- Carregar operacoes nao concluidas.
- Identificar estado atual.
- Reexecutar etapa segura ou estornar.
- Nunca duplicar entrega.
- Nunca cobrar duas vezes.
- Registrar auditoria quando houver recuperacao manual ou automatica relevante.

## Rotinas diarias

Rotinas diarias usam `economy_daily_job_runs`.

Fluxo:

```text
1. Calcular business_date pelo timezone configurado.
2. Tentar criar ou bloquear job_type + business_date.
3. Se ja estiver COMPLETED, sair.
4. Se estiver FAILED, permitir nova tentativa.
5. Executar alteracoes dentro de transacao SQL.
6. Marcar COMPLETED apenas ao final.
```

Rotinas iniciais:

- `CREDIT_INTEREST`
- `DYNAMIC_PRICE_RECOVERY`
- `DAILY_GOLD_LIMIT_RESET`

## Juros diarios

Cada cartao e data possuem uma linha unica em `economy_interest_accruals`.

Calculo:

```text
base = principal
```

No modo `COMPOUND`:

```text
base = principal + juros acumulados
```

Unidade de calculo:

```text
valor_calculado = base * taxa_bps + resto_anterior
juros_inteiros = valor_calculado / 10000
resto_novo = valor_calculado % 10000
```

O juro inteiro aumenta os juros acumulados do cartao e da conta.

## Precos dinamicos

Confirmacao de compra ou venda na Bancada do Banco deve recalcular o preco dentro da transacao SQL.

Se o preco visto pelo cliente mudou:

- Nao cobrar silenciosamente.
- Retornar `PRICE_CHANGED`.
- Pedir nova confirmacao com o valor atualizado.

Compras grandes devem atravessar faixas progressivamente.

## Falha de SQL

Se o SQL estiver indisponivel:

- Bloquear login bancario.
- Bloquear depositos.
- Bloquear saques.
- Bloquear cartoes.
- Bloquear lojas com inventario persistente.
- Bloquear Bancada do Banco.
- Nao usar saldo temporario em memoria.

Mensagem padrao:

```text
O sistema bancario esta temporariamente indisponivel.
Nenhuma alteracao foi realizada.
```

