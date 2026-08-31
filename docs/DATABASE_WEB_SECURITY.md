# Banco, Web API e seguranca economica

## 1. Banco

O padrao e SQLite:

```toml
[database]
type = "sqlite"
sqliteFile = "economia/economia.db"
```

Aliases aceitos: `sqlite`, `sqlite3`, `sql`, `postgresql`, `postgres`, `pgsql`. SQLite usa arquivo por save, foreign keys, WAL, busy timeout e uma conexao escritora. A Web API nao inicia em SQLite.

Para Web API use PostgreSQL e mantenha a porta do banco fechada para a Internet.

## 2. Web API

```toml
[webApi]
enabled = true
bind = "127.0.0.1"
port = 8765
allowedOrigin = "https://economia.example.com"
sessionTimeoutSeconds = 900
loginMaxAttempts = 5
loginWindowSeconds = 300
```

A API rejeita bind que nao seja loopback. Publique-a somente por reverse proxy HTTPS.

### Login por token gerado no jogo

1. O jogador entra normalmente no ATM do Minecraft.
2. Abre **Seguranca** e clica **Token web**.
3. O servidor gera um codigo aleatorio de 16 caracteres (80 bits), exibido em grupos de 4.
4. O codigo expira em 120 segundos, so pode ser usado uma vez e um novo codigo invalida o anterior da mesma conta/jogador.
5. O site envia o codigo para `/api/v1/auth/token`.
6. A API destrói o codigo e devolve um Bearer token aleatorio de 256 bits com expiracao configurada.
7. O frontend guarda apenas o Bearer token em `sessionStorage`; a senha da conta nunca e enviada ao website.

Rotas atuais:

| Metodo | Rota | Funcao |
|---|---|---|
| GET | `/api/v1/health` | Saude da API |
| POST | `/api/v1/auth/token` | Troca token de uso unico por sessao Bearer |
| GET | `/api/v1/auth/session` | Valida a sessao atual |
| POST | `/api/v1/auth/logout` | Encerra a sessao |
| GET | `/api/v1/account` | Saldo e resumo |
| POST | `/api/v1/transfers` | Transferencia entre contas |
| GET | `/api/v1/transactions` | Historico/ledger |
| GET | `/api/v1/cards` | Cartoes, status e limites |
| GET | `/api/v1/credit` | Limites, divida e fatura |
| GET | `/api/v1/gold` | Cotacao atual do ouro |

`POST /api/v1/transfers` exige Bearer token e `Idempotency-Key`. A conta de origem sempre vem da sessao autenticada; o navegador informa somente conta de destino e valor.

```json
{
  "destinationAccount": "000123",
  "amount": 500
}
```

A transferencia usa o mesmo `AccountFinancialService` do Minecraft, incluindo locks, ledger, origem `WEB` e fingerprint de idempotencia.

## 3. O que existe no website

- visao geral e saldos;
- transferencia bancaria;
- historico;
- cartoes (consulta);
- credito e faturas (consulta);
- cotacao dinamica do ouro;
- dados da conta e expiracao da sessao.

Nao existem na web: saque, deposito, emissao/entrega de ItemStack, troca/recuperacao de senha ou qualquer acao que dependa do inventario do jogador.

## 4. Protecoes web

- token de login curto, aleatorio, temporario e single-use;
- Bearer token de sessao com 256 bits;
- rate limit para tentativa de token e transferencia;
- CORS por origem HTTPS exata;
- API somente loopback;
- `Cache-Control: no-store`, `X-Frame-Options: DENY`, `nosniff` e outras politicas;
- frontend sem bibliotecas externas e com CSP restritiva no exemplo do Caddy;
- token nunca vai na URL;
- token de sessao usa `sessionStorage`, nao `localStorage`;
- valores de texto vindos do banco sao escapados antes de entrar em HTML;
- historico web nao expoe UUID interno das contas.

## 5. Operacoes fisicas e idempotencia

Operacoes que atravessam SQL e inventario continuam no jogo. Transacoes financeiras possuem `idempotency_key` + `request_fingerprint`; uma chave reutilizada com parametros diferentes retorna `IDEMPOTENCY_CONFLICT`.

Operacoes compostas usam estados como `CREATED -> ITEMS_RESERVED -> SQL_COMMITTED -> ITEMS_DELIVERED -> COMPLETED`. Quedas ambiguas vao para `RECONCILIATION_REQUIRED` em vez de estorno automatico arriscado.

## 6. Backup e troca de engine

Mudar `database.type` nao copia dados. Faca backup antes de trocar. Para SQLite, prefira parar o servidor antes de copiar o save; preserve o `.db` e, se presentes durante execucao, `-wal`/`-shm`. Para PostgreSQL use backup consistente (`pg_dump`/provedor) e teste restauracao.
