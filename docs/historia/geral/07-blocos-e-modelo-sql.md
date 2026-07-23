# Parte 7 — Orientação dos blocos e modelo SQL

Esta parte integra a história funcional completa do Mod de Economia para Minecraft Java 1.21.1 com NeoForge 21.1.234+. As regras gerais e referências entre módulos continuam válidas.

---

## 25. Orientação dos blocos

Os seguintes blocos terão orientação horizontal:

- Caixa Eletrônico
- Loja de Venda
- Loja de Compra
- Bancada do Banco

Propriedade:

```text
horizontal_facing
```

Valores válidos:

- `north`
- `south`
- `east`
- `west`

Nenhum desses blocos poderá possuir orientação vertical.

---

## 26. SQL como fonte oficial

O banco SQL será a fonte oficial para:

- Contas
- Saldos
- Cartões
- Crédito
- Faturas
- Transações
- Lojas
- Propriedade
- Configurações
- Estoques comerciais
- Bancadas
- Tesouraria
- Auditoria

O cliente nunca será fonte oficial de nenhum dado financeiro.

O bloco no mundo poderá guardar apenas:

- Identificador UUID do bloco comercial
- Dados visuais mínimos
- Cache temporário

Os dados financeiros reais deverão ser carregados do SQL.

---

## 27. Tabelas SQL

### 27.1 `economy_accounts`

Representa contas bancárias pessoais e contas do sistema.

```sql
id UUID PRIMARY KEY
player_uuid UUID NULL
username VARCHAR NULL
username_normalized VARCHAR NULL
password_hash VARCHAR NULL
password_salt VARCHAR NULL
password_algorithm VARCHAR NULL
account_type VARCHAR NOT NULL
status VARCHAR NOT NULL
balance BIGINT NOT NULL
configured_credit_limit BIGINT NOT NULL
credit_principal_outstanding BIGINT NOT NULL
credit_interest_outstanding BIGINT NOT NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
last_login_at TIMESTAMP NULL
version BIGINT NOT NULL
```

Tipos de conta:

- `PLAYER`
- `SYSTEM_TREASURY`
- `SYSTEM_CASH`
- `SYSTEM_CURRENCY_ISSUANCE`

Restrições:

```text
balance >= 0
configured_credit_limit >= 0
credit_principal_outstanding >= 0
credit_interest_outstanding >= 0
```

Para conta de jogador, o limite efetivo será o menor valor entre `configured_credit_limit` e o limite elegível pela faixa do saldo. A dívida total poderá ultrapassar o limite somente por causa de juros já lançados.

Índices únicos:

- `player_uuid`, somente para contas de jogador
- `username_normalized`, somente para contas de jogador

### 27.2 `economy_cards`

```sql
id UUID PRIMARY KEY
account_id UUID NOT NULL
card_type VARCHAR NOT NULL
custom_name VARCHAR NULL
card_creation_number INTEGER NOT NULL
status VARCHAR NOT NULL
individual_credit_limit BIGINT NOT NULL
debit_daily_limit BIGINT NOT NULL
debit_daily_spent BIGINT NOT NULL
debit_daily_spent_on DATE NULL
credit_principal_outstanding BIGINT NOT NULL
credit_interest_outstanding BIGINT NOT NULL
interest_rounding_remainder BIGINT NOT NULL
security_version INTEGER NOT NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
disabled_at TIMESTAMP NULL
```

Tipos:

- `DEBIT`
- `CREDIT`
- `DEBIT_CREDIT`

Estados:

- `ACTIVE`
- `DISABLED`
- `BLOCKED`
- `EXPIRED`

Chave estrangeira:

```text
account_id -> economy_accounts.id
```

A exclusão física da conta deverá ser bloqueada.

### 27.3 `economy_transactions`

Representa uma operação financeira completa.

```sql
id UUID PRIMARY KEY
idempotency_key VARCHAR NOT NULL
transaction_type VARCHAR NOT NULL
status VARCHAR NOT NULL
amount BIGINT NOT NULL
initiator_player_uuid UUID NULL
source_account_id UUID NULL
destination_account_id UUID NULL
card_id UUID NULL
commercial_block_id UUID NULL
dimension VARCHAR NULL
block_x INTEGER NULL
block_y INTEGER NULL
block_z INTEGER NULL
failure_reason VARCHAR NULL
created_at TIMESTAMP NOT NULL
completed_at TIMESTAMP NULL
```

Índice único:

```text
idempotency_key
```

Estados:

- `PENDING`
- `PROCESSING`
- `COMPLETED`
- `FAILED`
- `REVERSED`

Tipos adicionais necessários:

- `DAILY_INTEREST`
- `GOLD_MINT`
- `GOLD_REDEMPTION`
- `DYNAMIC_PRICE_PURCHASE`
- `DYNAMIC_PRICE_SALE`

### 27.4 `economy_ledger_entries`

Cada transação financeira deverá gerar lançamentos contábeis.

```sql
id UUID PRIMARY KEY
transaction_id UUID NOT NULL
account_id UUID NOT NULL
entry_type VARCHAR NOT NULL
amount BIGINT NOT NULL
balance_before BIGINT NOT NULL
balance_after BIGINT NOT NULL
created_at TIMESTAMP NOT NULL
```

Tipos:

- `DEBIT`
- `CREDIT`
- `CREDIT_PRINCIPAL_INCREASE`
- `CREDIT_INTEREST_INCREASE`
- `CREDIT_DEBT_PAYMENT`
- `CURRENCY_ISSUANCE`
- `CURRENCY_REDEMPTION`
- `ADJUSTMENT`

O registro não poderá ser alterado depois de concluído. Correções deverão ser feitas por transações de estorno.

### 27.5 `economy_card_entries`

Armazena os lançamentos das faturas.

```sql
id UUID PRIMARY KEY
card_id UUID NOT NULL
transaction_id UUID NOT NULL
entry_type VARCHAR NOT NULL
amount BIGINT NOT NULL
remaining_amount BIGINT NOT NULL
description VARCHAR
merchant_name VARCHAR NULL
interest_eligible_date DATE NULL
business_date DATE NOT NULL
created_at TIMESTAMP NOT NULL
paid_at TIMESTAMP NULL
```

Tipos:

- `PURCHASE`
- `DAILY_INTEREST`
- `PAYMENT`
- `REVERSAL`
- `ADJUSTMENT`

### 27.6 `economy_interest_accruals`

Garante que os juros de um cartão sejam aplicados uma única vez por dia.

```sql
id UUID PRIMARY KEY
card_id UUID NOT NULL
account_id UUID NOT NULL
accrual_date DATE NOT NULL
interest_mode VARCHAR NOT NULL
rate_bps INTEGER NOT NULL
calculation_base BIGINT NOT NULL
remainder_before BIGINT NOT NULL
interest_amount BIGINT NOT NULL
remainder_after BIGINT NOT NULL
transaction_id UUID NULL
created_at TIMESTAMP NOT NULL
```

Restrição única:

```text
card_id + accrual_date
```

### 27.7 `economy_commercial_blocks`

Representa caixas, lojas e bancadas colocados no mundo.

```sql
id UUID PRIMARY KEY
block_type VARCHAR NOT NULL
owner_player_uuid UUID NULL
linked_account_id UUID NULL
funding_card_id UUID NULL
placed_by_player_uuid UUID NOT NULL
dimension VARCHAR NOT NULL
block_x INTEGER NOT NULL
block_y INTEGER NOT NULL
block_z INTEGER NOT NULL
status VARCHAR NOT NULL
custom_name VARCHAR NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
removed_at TIMESTAMP NULL
```

Tipos:

- `ATM`
- `SELL_SHOP`
- `BUY_SHOP`
- `BANK_COUNTER`

Deverá existir índice único para blocos ativos em dimensão e coordenadas.

### 27.8 `economy_shop_offers`

```sql
id UUID PRIMARY KEY
commercial_block_id UUID NOT NULL
slot_index INTEGER NOT NULL
item_id VARCHAR NOT NULL
item_components TEXT NULL
item_data_version INTEGER
quantity_per_operation INTEGER NOT NULL
base_buy_price BIGINT NULL
base_sell_price BIGINT NULL
minimum_buy_price BIGINT NULL
maximum_sell_price BIGINT NULL
target_quantity BIGINT NULL
purchased_quantity BIGINT NOT NULL
comparison_mode VARCHAR NOT NULL
pricing_mode VARCHAR NOT NULL
demand_level INTEGER NOT NULL
supply_level INTEGER NOT NULL
quantity_per_price_level BIGINT NULL
demand_increase_bps INTEGER NULL
supply_decrease_bps INTEGER NULL
recovery_levels_per_idle_day INTEGER NOT NULL
maximum_demand_level INTEGER NOT NULL
maximum_supply_level INTEGER NOT NULL
last_player_purchase_date DATE NULL
last_player_sale_date DATE NULL
is_buy_enabled BOOLEAN NOT NULL
is_sell_enabled BOOLEAN NOT NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
version BIGINT NOT NULL
```

Modos de preço:

- `FIXED`
- `DYNAMIC`
- `MONETARY_GOLD`

Comparação:

- `FULL_COMPONENTS`
- `ITEM_ID_ONLY`

### 27.9 `economy_offer_daily_stats`

Armazena o volume diário que influencia os preços.

```sql
id UUID PRIMARY KEY
offer_id UUID NOT NULL
business_date DATE NOT NULL
quantity_bought_from_bank BIGINT NOT NULL
quantity_sold_to_bank BIGINT NOT NULL
money_received_by_bank BIGINT NOT NULL
money_paid_by_bank BIGINT NOT NULL
highest_demand_level INTEGER NOT NULL
highest_supply_level INTEGER NOT NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
```

Restrição única:

```text
offer_id + business_date
```

### 27.10 `economy_inventory_slots`

Armazena os inventários comerciais.

```sql
id UUID PRIMARY KEY
commercial_block_id UUID NOT NULL
inventory_type VARCHAR NOT NULL
slot_index INTEGER NOT NULL
item_id VARCHAR NULL
item_count INTEGER NOT NULL
item_components TEXT NULL
item_data_version INTEGER NULL
updated_at TIMESTAMP NOT NULL
version BIGINT NOT NULL
```

Tipos de inventário:

- `PRODUCT_STOCK`
- `CASH_RESERVE`
- `PURCHASED_ITEMS`
- `BANK_STOCK`
- `GOLD_RESERVE`

Restrição única:

```text
commercial_block_id + inventory_type + slot_index
```

### 27.11 `economy_gold_exchange_entries`

Registra a emissão e o resgate de moeda por ouro.

```sql
id UUID PRIMARY KEY
transaction_id UUID NOT NULL
player_uuid UUID NOT NULL
operation_type VARCHAR NOT NULL
gold_item_id VARCHAR NOT NULL
gold_item_count BIGINT NOT NULL
gold_nugget_units BIGINT NOT NULL
unit_value BIGINT NOT NULL
money_amount BIGINT NOT NULL
commercial_block_id UUID NOT NULL
created_at TIMESTAMP NOT NULL
```

Tipos:

- `MINT`
- `REDEMPTION`
- `ADMIN_ADJUSTMENT`

### 27.12 `economy_gold_reserve_summary`

Mantém o resumo oficial da reserva.

```sql
id UUID PRIMARY KEY
reserve_code VARCHAR NOT NULL UNIQUE
gold_nugget_units BIGINT NOT NULL
currency_issued BIGINT NOT NULL
currency_redeemed BIGINT NOT NULL
updated_at TIMESTAMP NOT NULL
version BIGINT NOT NULL
```

Restrições:

```text
gold_nugget_units >= 0
currency_issued >= 0
currency_redeemed >= 0
```

### 27.13 `economy_audit_logs`

```sql
id UUID PRIMARY KEY
actor_player_uuid UUID NULL
actor_type VARCHAR NOT NULL
action VARCHAR NOT NULL
target_type VARCHAR NULL
target_id UUID NULL
old_value TEXT NULL
new_value TEXT NULL
dimension VARCHAR NULL
block_x INTEGER NULL
block_y INTEGER NULL
block_z INTEGER NULL
created_at TIMESTAMP NOT NULL
```

Deverá registrar também:

- Mudança da taxa de juros
- Aplicação diária de juros
- Alterações de preço-base
- Recuperação diária de preço
- Emissão e resgate por ouro
- Ajustes da Reserva de Ouro

### 27.14 `economy_operations`

Controla operações que envolvem simultaneamente SQL e inventários do Minecraft.

```sql
id UUID PRIMARY KEY
idempotency_key VARCHAR NOT NULL
operation_type VARCHAR NOT NULL
player_uuid UUID NULL
commercial_block_id UUID NULL
state VARCHAR NOT NULL
payload TEXT NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
completed_at TIMESTAMP NULL
```

Estados:

- `CREATED`
- `ITEMS_RESERVED`
- `SQL_COMMITTED`
- `ITEMS_DELIVERED`
- `COMPLETED`
- `ROLLBACK_REQUIRED`
- `ROLLED_BACK`

### 27.15 `economy_daily_job_runs`

Controla rotinas econômicas diárias.

```sql
id UUID PRIMARY KEY
job_type VARCHAR NOT NULL
business_date DATE NOT NULL
status VARCHAR NOT NULL
started_at TIMESTAMP NOT NULL
completed_at TIMESTAMP NULL
failure_reason VARCHAR NULL
```

Tipos iniciais:

- `CREDIT_INTEREST`
- `DYNAMIC_PRICE_RECOVERY`
- `DAILY_GOLD_LIMIT_RESET`

Restrição única:

```text
job_type + business_date
```

---
