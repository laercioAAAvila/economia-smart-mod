# Arquitetura e modulos

## Camadas

### `common`

Codigo compartilhado entre cliente e servidor:

- Registro de itens
- Registro de blocos
- Menus e tipos de tela
- DTOs de pacotes
- Enums de dominio
- Utilitarios puros de dinheiro, item e validacao

Nao deve conter regra financeira autoritativa que dependa do cliente.

### `server`

Camada autoritativa:

- Comandos
- Sessoes bancarias
- Servicos de conta
- Servicos de cartao
- Servicos de caixa eletronico
- Servicos de lojas
- Servicos da Bancada do Banco
- Rotinas diarias
- Persistencia SQL
- Auditoria
- Recuperacao de operacoes pendentes

Toda alteracao de saldo, credito, estoque comercial e reserva de ouro deve passar por esta camada.

### `client`

Camada de interface:

- Telas
- Widgets
- Renderizacao de blocos e itens
- Envio de intencoes para o servidor
- Mensagens de erro e confirmacao

O cliente nunca calcula resultado financeiro definitivo.

## Modulos de dominio

### Dinheiro

Responsavel por:

- Denominacoes fisicas
- Calculo de total em notas
- Montagem de saque usando maiores notas
- Validacao de item de dinheiro

Servicos sugeridos:

- `MoneyDenomination`
- `MoneyItemRegistry`
- `MoneyStackCalculator`
- `CashChangeService`

### Contas

Responsavel por:

- Criacao de conta
- Login e logout
- Hash de senha
- Consulta de saldo
- Bloqueio e encerramento logico
- Limite de credito global
- Saldo disponivel considerando divida

Servicos sugeridos:

- `AccountService`
- `PasswordService`
- `BankSessionService`
- `AccountRepository`

### Cartoes e credito

Responsavel por:

- Emissao de cartoes
- Validacao de cartao por UUID
- Lista de cartoes da conta
- Bloqueio e desativacao logica
- Numero sequencial de criacao por conta
- Limite individual
- Limite diario de debito
- Principal em aberto
- Juros acumulados
- Fatura
- Reemissao de fatura perdida
- Pagamento de fatura mais antiga e pagamento total
- Pagamentos parciais
- Preservacao de divida em cartao desativado

Servicos sugeridos:

- `CardService`
- `CreditService`
- `InvoiceService`
- `InterestAccrualService`

### Caixa Eletronico

Responsavel por:

- Abertura de telas
- Login por credenciais
- Login por cartao
- Deposito
- Saque
- Compra de cartoes
- Alteracao de senha
- Consulta de saldo, limite e fatura
- Lista e acoes de cartoes criados
- Pedido de credito por faixa de saldo

Servicos sugeridos:

- `AtmBlock`
- `AtmBlockEntity`
- `AtmMenu`
- `AtmService`

### Lojas

Responsavel por:

- Loja de Venda
- Loja de Compra
- Estoque persistente
- Caixa fisico persistente
- Vinculacao bancaria
- Pagamentos por dinheiro, debito e credito
- Selecao explicita entre debito e credito para cartao combinado
- Feedback simples quando uma troca nao puder ser concluida
- Comparacao de itens

Servicos sugeridos:

- `SellShopService`
- `BuyShopService`
- `ShopOfferService`
- `CommercialInventoryService`
- `ItemComparisonService`

### Bancada do Banco

Responsavel por:

- Ofertas administrativas
- Estoque do banco
- Tesouraria
- Ouro monetario
- Emissao e resgate de moeda
- Precos dinamicos
- Recuperacao diaria de niveis

Servicos sugeridos:

- `BankCounterService`
- `GoldReserveService`
- `CurrencyIssuanceService`
- `DynamicPricingService`
- `TreasuryService`

### Auditoria e recuperacao

Responsavel por:

- Transacoes financeiras
- Ledger
- Logs administrativos
- Chaves de idempotencia
- Operacoes pendentes entre SQL e inventario Minecraft
- Retomada apos queda

Servicos sugeridos:

- `TransactionService`
- `LedgerService`
- `AuditService`
- `OperationRecoveryService`
- `DailyJobService`

## Dependencias internas

Fluxo recomendado:

```text
Tela/Comando/Pacote
  -> Service
    -> TransactionRunner
      -> Repository
      -> LedgerService
      -> AuditService
```

Repositorios nao devem conter regra de negocio complexa. Eles devem executar SQL preparado e retornar modelos de dominio.

## Configuracao

Configuracoes do servidor:

- Banco SQL
- Pool de conexoes
- Timezone economico
- Juros
- Ouro
- Precos dinamicos
- Precos de cartoes
- Limites diarios
- Permissoes administrativas

As configuracoes que afetam dinheiro devem ser recarregadas pelo servidor e auditadas quando alteradas por comando.
