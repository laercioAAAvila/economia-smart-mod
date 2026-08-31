# Economia Smart Mod

Mod de economia para Minecraft NeoForge 1.21.1, Java 21. Inclui contas bancarias, ATM, dinheiro fisico, cartoes, lojas, correio, claims, transferencias e troca de ouro.

## Banco de dados

Por padrao o mod usa **SQLite**, sem servidor externo:

```toml
[database]
type = "sqlite"
sqliteFile = "economia/economia.db"
```

O arquivo relativo fica dentro do save atual (`<world>/economia/economia.db`). Para website/API use PostgreSQL:

```toml
[database]
type = "postgresql" # aliases: postgres, pgsql
host = "127.0.0.1"
port = 5432
name = "economia"
username = "economia"
password = ""
ssl = false
```

Trocar `database.type` nao migra dados automaticamente.

## Website

A Web API fica **desativada por padrao** e so inicia em PostgreSQL. Quando quiser publicar o Internet Banking:

```toml
[webApi]
enabled = true
bind = "127.0.0.1"
port = 8765
allowedOrigin = "https://economia.example.com"
```

O login web **nao envia a senha da conta pela Internet**. O jogador entra no ATM do Minecraft, abre **Seguranca -> Token web**, recebe um codigo de uso unico valido por 120 segundos e informa esse codigo no site. A API troca o codigo por um Bearer token temporario.

Paginas web: visao geral/saldo, transferencias, historico, cartoes, credito/faturas, cotacao do ouro e seguranca da sessao. Nao existem saque, deposito ou troca de senha na web. Operacoes fisicas continuam no Minecraft.

O frontend estatico esta organizado em `web/pages`, `web/css`, `web/js` e `web/assets`. Publique-o por HTTPS e encaminhe `/api/*` para `127.0.0.1:8765`. Nunca exponha PostgreSQL ou a porta interna da API diretamente para a Internet.

## PostgreSQL com Docker

O `docker-compose.yml` e para desenvolvimento/local. Defina uma senha antes de iniciar:

```powershell
$env:POSTGRES_PASSWORD="troque-esta-senha"
docker compose up -d
```

## Seguranca economica

A camada financeira usa fingerprint de idempotencia, ledger e rastreamento das fases de operacoes SQL + inventario. Replays com parametros diferentes sao rejeitados; quedas ambiguas viram `RECONCILIATION_REQUIRED`, evitando estornos automaticos que poderiam duplicar ou apagar patrimonio.

Veja `CHANGELOG.md` e `docs/DATABASE_WEB_SECURITY.md`.

## Build

```bash
./gradlew build
```

Requer Java 21 e acesso aos repositorios Gradle/NeoForge quando as dependencias ainda nao estiverem em cache.
