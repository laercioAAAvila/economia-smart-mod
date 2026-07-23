# Persistencia SQL

## Banco como fonte oficial

O SQL e a fonte oficial para qualquer dado financeiro ou comercial persistente. O estado salvo no mundo deve guardar apenas o identificador do bloco comercial e dados visuais minimos.

## Migracoes

Usar migracoes versionadas e aplicadas na inicializacao do servidor antes de liberar o sistema economico.

Sequencia inicial sugerida:

```text
V001__create_accounts.sql
V002__create_cards.sql
V003__create_transactions_and_ledger.sql
V004__create_card_entries_and_interest.sql
V005__create_commercial_blocks.sql
V006__create_shop_offers.sql
V007__create_inventory_slots.sql
V008__create_gold_reserve.sql
V009__create_daily_stats_and_jobs.sql
V010__create_audit_logs.sql
V011__create_operations.sql
V012__allow_gold_exchange_without_block.sql
V013__add_account_numbers.sql
V014__add_performance_indexes.sql
V015__align_account_number_sequence.sql
V016__add_operation_recovery_index.sql
V017__add_debit_daily_limits.sql
V018__add_card_creation_numbers.sql
```

Se uma migracao falhar:

- Sistema bancario fica desativado.
- Nenhuma operacao financeira e liberada.
- O erro e registrado sem expor senha ou dados sensiveis.

## Tipos monetarios

Java:

```text
long
```

SQL:

```text
BIGINT
```

Unidade:

```text
1 = R$ 1
```

Nao usar centavos visiveis.

## Tabelas obrigatorias

### Contas

`economy_accounts`

Responsavel por contas de jogador e contas do sistema.

Campos obrigatorios:

- `id`
- `player_uuid`
- `username`
- `username_normalized`
- `password_hash`
- `password_salt`
- `password_algorithm`
- `account_type`
- `status`
- `balance`
- `configured_credit_limit`
- `credit_principal_outstanding`
- `credit_interest_outstanding`
- `created_at`
- `updated_at`
- `last_login_at`
- `version`

Contas do sistema iniciais:

- `SYSTEM_TREASURY`
- `SYSTEM_CASH`
- `SYSTEM_CURRENCY_ISSUANCE`

Restricoes:

- Saldo nunca negativo.
- Limite nunca negativo.
- Principal nunca negativo.
- Juros nunca negativos.
- Jogador possui no maximo uma conta.
- Nome normalizado e unico para contas de jogador.

### Cartoes

`economy_cards`

Campos obrigatorios:

- `id`
- `account_id`
- `card_type`
- `custom_name`
- `card_creation_number`
- `status`
- `individual_credit_limit`
- `debit_daily_limit`
- `debit_daily_spent`
- `debit_daily_spent_on`
- `credit_principal_outstanding`
- `credit_interest_outstanding`
- `interest_rounding_remainder`
- `security_version`
- `created_at`
- `updated_at`
- `disabled_at`

O item de cartao deve guardar apenas dados para localizar o registro SQL.

### Transacoes

`economy_transactions`

Toda operacao financeira gera uma transacao com `idempotency_key` unico.

Tipos minimos:

- `DEPOSIT`
- `WITHDRAW`
- `DEBIT_PURCHASE`
- `CREDIT_PURCHASE`
- `CARD_ISSUE`
- `INVOICE_PAYMENT`
- `DAILY_INTEREST`
- `GOLD_MINT`
- `GOLD_REDEMPTION`
- `DYNAMIC_PRICE_PURCHASE`
- `DYNAMIC_PRICE_SALE`
- `ADMIN_ADJUSTMENT`
- `REVERSAL`

### Ledger

`economy_ledger_entries`

Cada transacao concluida gera lancamentos contabeis imutaveis.

Correcoes devem ser feitas por estorno, nunca por edicao do lancamento antigo.

### Fatura

`economy_card_entries`

Guarda compras, juros, pagamentos, estornos e ajustes do cartao.

Pagamentos parciais reduzem `remaining_amount` em ordem cronologica, quitando juros antes de principal.

### Juros diarios

`economy_interest_accruals`

Chave unica:

```text
card_id + accrual_date
```

Garante idempotencia da cobranca diaria.

### Blocos comerciais

`economy_commercial_blocks`

Representa:

- Caixa Eletronico
- Loja de Venda
- Loja de Compra
- Bancada do Banco

Deve existir chave unica para bloco ativo por dimensao e coordenada.

### Ofertas

`economy_shop_offers`

Usada por lojas e Bancada do Banco.

Campos de preco dinamico devem suportar:

- Modo fixo
- Modo dinamico
- Ouro monetario
- Nivel de demanda
- Nivel de oferta
- Limites minimo e maximo
- Recuperacao diaria

### Inventarios persistentes

`economy_inventory_slots`

Tipos:

- `PRODUCT_STOCK`
- `CASH_RESERVE`
- `PURCHASED_ITEMS`
- `BANK_STOCK`
- `GOLD_RESERVE`

Cada slot deve ter `version` para concorrencia otimista quando necessario.

### Ouro

`economy_gold_exchange_entries`

Registra entrada e saida de ouro monetario.

`economy_gold_reserve_summary`

Resumo oficial da reserva:

- Pepitas totais
- Moeda emitida
- Moeda recolhida

### Rotinas diarias

`economy_daily_job_runs`

Chave unica:

```text
job_type + business_date
```

Usada por juros, recuperacao de precos e reset de limites diarios.

### Operacoes pendentes

`economy_operations`

Controla operacoes que atravessam SQL e inventario Minecraft.

Estados:

- `CREATED`
- `ITEMS_RESERVED`
- `SQL_COMMITTED`
- `ITEMS_DELIVERED`
- `COMPLETED`
- `ROLLBACK_REQUIRED`
- `ROLLED_BACK`

## Indices minimos

- `economy_accounts(player_uuid)`
- `economy_accounts(username_normalized)`
- `economy_cards(account_id)`
- `economy_transactions(idempotency_key)`
- `economy_ledger_entries(transaction_id)`
- `economy_card_entries(card_id, business_date)`
- `economy_interest_accruals(card_id, accrual_date)`
- `economy_commercial_blocks(dimension, block_x, block_y, block_z, status)`
- `economy_shop_offers(commercial_block_id, slot_index)`
- `economy_offer_daily_stats(offer_id, business_date)`
- `economy_daily_job_runs(job_type, business_date)`
