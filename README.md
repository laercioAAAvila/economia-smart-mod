# Economia Smart Mod

## English

Economia Smart Mod adds a server-oriented economy system for Minecraft NeoForge. It includes bank accounts, ATM screens, debit/credit cards, physical banknotes, player shops, transfers, and gold exchange through the bank counter.

### Requirements

- Minecraft with NeoForge
- Java 21
- PostgreSQL

PostgreSQL is required to run the mod. The mod stores accounts, cards, balances, shops, inventories, transactions, and gold exchange data in SQL.

### PostgreSQL with Docker

This repository includes a `docker-compose.yml` for local PostgreSQL.

Set a local password before starting it:

```powershell
$env:POSTGRES_PASSWORD="change-this-password"
docker compose up -d
```

Default Docker database values:

- Host: `localhost`
- Port: `55432`
- Database: `economia`
- User: `economia`
- Password: value from `POSTGRES_PASSWORD`

Configure the mod server config with the same PostgreSQL values.

### Main Features

- Bank account creation and login
- ATM for balance, deposits, withdrawals, cards, credit, and transfers
- Debit, credit, and debit/credit cards
- Physical banknotes
- Sell shops and buy shops between players
- Card or cash payments in shops
- Bank counter for exchanging gold
- Dynamic gold pricing based on demand
- SQL persistence for multiplayer servers

## Portugues

Economia Smart Mod adiciona um sistema de economia para Minecraft NeoForge focado em servidores. O mod inclui contas bancarias, caixa eletronico, cartoes, dinheiro em especie, lojas entre jogadores, transferencias e troca de ouro pela bancada do banco.

### Requisitos

- Minecraft com NeoForge
- Java 21
- PostgreSQL

PostgreSQL e obrigatorio para rodar o mod. O mod salva contas, cartoes, saldos, lojas, inventarios, transacoes e dados da troca de ouro no banco SQL.

### PostgreSQL com Docker

Este repositorio inclui um `docker-compose.yml` para subir PostgreSQL local.

Defina uma senha local antes de iniciar:

```powershell
$env:POSTGRES_PASSWORD="troque-esta-senha"
docker compose up -d
```

Valores padrao do banco no Docker:

- Host: `localhost`
- Porta: `55432`
- Banco: `economia`
- Usuario: `economia`
- Senha: valor definido em `POSTGRES_PASSWORD`

Configure o arquivo de server config do mod com os mesmos dados do PostgreSQL.

### Principais Recursos

- Criacao e login de conta bancaria
- Caixa eletronico para saldo, deposito, saque, cartoes, credito e transferencia
- Cartoes de debito, credito e debito/credito
- Dinheiro em especie
- Loja de venda e loja de compra entre jogadores
- Pagamento por cartao ou dinheiro nas lojas
- Bancada do banco para trocar ouro
- Preco dinamico do ouro por demanda
- Persistencia SQL para servidores multiplayer
