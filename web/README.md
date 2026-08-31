# Website Economia Smart

Frontend estatico sem dependencias externas. Ele nao acessa PostgreSQL diretamente; todas as consultas e transferencias passam pela Web API do mod em `/api/v1`.

## Estrutura

```text
web/
├── index.html
├── pages/
│   ├── login.html
│   ├── dashboard.html
│   ├── transferencias.html
│   ├── historico.html
│   ├── cartoes.html
│   ├── credito.html
│   ├── ouro.html
│   └── seguranca.html
├── css/
├── js/
├── assets/
└── Caddyfile.example
```

Nao ha paginas de saque, deposito ou troca de senha.

## Login

O website nao recebe a senha bancaria. No jogo, entre no ATM e use **Seguranca -> Token web**. O codigo vale por 120 segundos e uma unica utilizacao. Depois da troca, o Bearer token da sessao fica em `sessionStorage` e expira conforme `webApi.sessionTimeoutSeconds`.

## Publicacao

1. Use PostgreSQL no mod.
2. Ative `[webApi].enabled = true`, mantendo `bind = "127.0.0.1"`.
3. Configure `allowedOrigin` com a origem HTTPS exata.
4. Copie **todo o conteudo de `web/`**, preservando `pages/`, `css/`, `js/` e `assets/`.
5. Encaminhe `/api/*` para `127.0.0.1:8765` e preserve o `X-Real-IP` como no exemplo para o rate limit por cliente.
6. Use o `Caddyfile.example` como referencia.

O site foi feito para ser servido na raiz do dominio, por exemplo `https://economia.example.com/`.
