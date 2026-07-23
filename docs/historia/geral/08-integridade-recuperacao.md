# Parte 8 — Integridade, concorrência e recuperação

Esta parte integra a história funcional completa do Mod de Economia para Minecraft Java 1.21.1 com NeoForge 21.1.234+. As regras gerais e referências entre módulos continuam válidas.

---

## 28. Integridade SQL

### 28.1 Transações

Toda movimentação deverá utilizar transação SQL.

Exemplo de fluxo:

1. Iniciar transação SQL.
2. Bloquear conta com `SELECT ... FOR UPDATE`.
3. Bloquear cartão, loja e tesouraria envolvidos.
4. Validar valores.
5. Atualizar saldos.
6. Criar transação.
7. Criar lançamentos.
8. Confirmar a transação SQL.
9. Finalizar a entrega no Minecraft.

### 28.2 Ordem de bloqueio

Para evitar deadlock, as linhas deverão ser bloqueadas sempre na mesma ordem:

1. Contas por UUID ordenado
2. Cartões por UUID
3. Bloco comercial
4. Inventários
5. Ofertas

### 28.3 Valores, dívida e níveis válidos

O SQL deverá possuir restrições para impedir:

- Saldo negativo
- Limite negativo
- Principal negativo
- Juros acumulados negativos
- Resto de cálculo de juros negativo
- Quantidade negativa
- Preço negativo
- Reserva de ouro negativa
- Nível de demanda ou oferta negativo
- Stack acima do permitido
- Quantidade comprada acima da desejada

Juros poderão elevar a dívida total acima do limite configurado, mas nenhuma compra nova poderá aumentar ainda mais o principal enquanto a dívida estiver fora dos limites.

### 28.4 Exclusões

Não utilizar `ON DELETE CASCADE` em dados financeiros históricos.

Contas, cartões, lojas e transações deverão ser desativados ou marcados como removidos.

O histórico deverá permanecer.

### 28.5 Rotinas diárias idempotentes

Juros, recuperação de preços e limites diários de ouro deverão usar `business_date` e restrições únicas no SQL.

Cada rotina deverá:

1. Tentar criar ou bloquear o registro do dia.
2. Sair sem alterações se o dia já estiver concluído.
3. Executar tudo dentro de transação SQL.
4. Marcar falha sem considerar o dia concluído.
5. Poder ser retomada depois de reinício.

### 28.6 Concorrência nos preços

Ao confirmar compra ou venda na Bancada do Banco, a oferta deverá ser bloqueada com `SELECT ... FOR UPDATE` ou validada por versão otimista.

O preço deverá ser recalculado dentro da mesma transação que altera:

- Estoque
- Volume diário
- Nível de demanda ou oferta
- Tesouraria ou emissão monetária
- Transação do jogador

---

## 29. Proteção contra duplicação

Cada clique de compra, venda, saque ou depósito deverá possuir uma chave de idempotência criada pelo servidor ou validada pelo servidor.

Se o mesmo pacote for enviado duas vezes:

- A segunda operação deverá retornar o resultado da primeira
- Nenhum valor adicional será retirado
- Nenhum item adicional será entregue

A proteção deverá cobrir:

- Clique duplo
- Lag
- Reconexão
- Pacote duplicado
- Reinício durante a operação
- Duas pessoas usando a mesma loja
- Duas cópias do mesmo cartão

---

## 30. Operações entre SQL e inventário

Como o inventário do jogador pertence ao Minecraft e os saldos pertencem ao SQL, a operação deverá utilizar reserva e recuperação.

Exemplo de compra:

1. Criar operação com estado `CREATED`.
2. Reservar o estoque.
3. Marcar como `ITEMS_RESERVED`.
4. Executar movimentação financeira no SQL.
5. Marcar como `SQL_COMMITTED`.
6. Entregar o item.
7. Marcar como `ITEMS_DELIVERED`.
8. Concluir a operação.

Caso o servidor caia após o débito e antes da entrega:

- A operação deverá ser identificada na inicialização
- O item deverá ser entregue ao jogador quando ele entrar
- Ou a transação deverá ser estornada

O jogador nunca poderá perder dinheiro por queda do servidor.

---

## 31. Comportamento com SQL indisponível

Caso a conexão SQL esteja indisponível:

- Novos logins bancários serão bloqueados
- Depósitos serão bloqueados
- Saques serão bloqueados
- Compras por cartão serão bloqueadas
- Lojas vinculadas ao banco serão bloqueadas
- A Bancada do Banco será bloqueada
- Operações em dinheiro físico nas lojas também deverão ser bloqueadas quando utilizarem inventários armazenados no SQL

O mod não deverá utilizar um saldo temporário em memória como substituto.

Mensagem:

```text
O sistema bancário está temporariamente indisponível.
Nenhuma alteração foi realizada.
```

---

## 32. Conexão SQL

O mod deverá possuir configuração para:

```properties
database.type
database.host
database.port
database.name
database.username
database.password
database.ssl
database.pool.minimum
database.pool.maximum
database.connectionTimeout
database.queryTimeout

economy.timeZone

credit.interest.enabled
credit.interest.dailyRateBps
credit.interest.mode
credit.interest.graceDays
credit.interest.applicationHour

bank.gold.enabled
bank.gold.nuggetValue
bank.gold.dailyLimitPerPlayer
bank.gold.dailyGlobalLimit
bank.gold.allowAccountCredit
bank.gold.allowPhysicalNotes
bank.gold.allowCreditPurchase

bank.dynamicPricing.enabled
bank.dynamicPricing.recoveryHour
bank.dynamicPricing.defaultQuantityPerLevel
bank.dynamicPricing.defaultDemandIncreaseBps
bank.dynamicPricing.defaultSupplyDecreaseBps
bank.dynamicPricing.defaultRecoveryLevelsPerIdleDay
```

O servidor deverá utilizar:

- Pool de conexões
- Prepared statements
- Migrações versionadas
- Reconexão controlada
- Timeout
- Logs sem expor senha

Não concatenar valores recebidos dos jogadores diretamente em SQL.

---

## 33. Migrações

O mod deverá controlar a versão do banco.

Exemplos:

```text
V001__create_accounts.sql
V002__create_cards.sql
V003__create_transactions.sql
V004__create_commercial_blocks.sql
V005__create_inventory_slots.sql
V006__split_credit_principal_and_interest.sql
V007__create_interest_accruals.sql
V008__create_dynamic_price_stats.sql
V009__create_gold_reserve.sql
V010__create_daily_job_runs.sql
```

O servidor deverá aplicar migrações antes de liberar o sistema bancário.

Caso uma migração falhe:

- O sistema econômico permanecerá desativado
- O servidor deverá informar claramente o erro
- Nenhuma migração parcial deverá ser considerada concluída

---
