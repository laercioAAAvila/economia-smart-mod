# Changelog

## 0.5.1 - 2026-08-31

### Website / autenticacao
- Frontend web redesenhado em paginas separadas, com `pages/`, `css/`, `js/` e `assets/`.
- Paginas: visao geral/saldo, transferencias, historico, cartoes, credito/faturas, cotacao do ouro e seguranca.
- Removidos do website saque, deposito e troca/recuperacao de senha.
- Login web passou a usar token de uso unico gerado dentro do ATM em `Seguranca -> Token web`; a senha bancaria nao e enviada ao site.
- Token de login: 16 caracteres/80 bits, validade de 120 segundos, single-use e substituicao do token anterior da mesma conta.
- Apos validar o token, a API emite Bearer token de sessao de 256 bits, armazenado pelo frontend somente em `sessionStorage`.
- Transferencias web passaram a ser autorizadas apenas pela sessao Bearer e continuam usando idempotencia/ledger/locks do servico financeiro do mod.
- Adicionados endpoints de sessao, cartoes, credito/faturas e cotacao do ouro.
- Historico web deixou de expor UUID interno de contas.
- Adicionado rate limit para transferencias e reforco contra XSS no frontend.
- Protocolo de rede do mod atualizado para versao `2` para incluir a acao segura de geracao do token web no ATM.

### Configuracao
- SQLite permanece o banco padrao e a Web API permanece `enabled = false` por padrao.
- Geracao do `economia-server.toml` recebeu comentarios mais claros separando SQLite, PostgreSQL e Web API.

## 0.5.0 - 2026-08-30

### Banco de dados
- Adicionado suporte a SQLite, selecionavel com `database.type = "sqlite"` ou `"sql"`.
- Mantido PostgreSQL com aliases `postgresql`, `postgres` e `pgsql`.
- SQLite usa arquivo por save, foreign keys, WAL, busy timeout e uma unica conexao escritora.
- Adicionada camada de compatibilidade para UUID, datas, `FOR UPDATE`, `LEAST/GREATEST` e SQL dependente de dialeto.
- Adicionado baseline SQLite equivalente ao schema atual; migrations historicas PostgreSQL continuam intactas.
- Executor de migrations deixou de dividir scripts ingenuamente por `;` e passa a respeitar strings, comentarios e blocos dollar-quoted.
- Reset de banco agora diferencia PostgreSQL/SQLite e retorna o estado do banco para indisponivel se falhar.

### Seguranca economica / anti-duplicacao
- Adicionados fingerprints de requisicao e deteccao de conflito de idempotencia.
- Adicionada origem da transacao (`MINECRAFT`, `WEB`, `ADMIN`, `SYSTEM`).
- Operacoes fisicas passaram a usar estados de reserva/commit/entrega e `RECONCILIATION_REQUIRED` em quedas ambiguas.
- Recovery deixou de fazer estorno financeiro automatico quando a entrega fisica pode ja ter ocorrido.
- Corrigido replay no resgate/cunhagem de ouro que podia repetir entrega de ouro ou dinheiro.
- Corrigido replay de compras/vendas em lojas que podia reaproveitar pagamento concluido.
- Corrigido pagamento generico em dinheiro que podia apagar novas cedulas ao receber uma chave antiga.
- Endurecidos deposito e saque contra quedas entre commit SQL e movimentacao das cedulas.
- Endurecidos pagamentos do correio e lojas contra quedas entre reserva, pagamento e entrega.
- Removidos tres servicos legados de loja sem uso que mantinham fluxos inseguros de replay.
- Emissao de cartao agora registra a taxa em transaction/ledger e protege a entrega fisica por operacao idempotente.
- Operacoes do ATM agora exigem que o jogador esteja com o menu do ATM aberto no servidor, bloqueando pacotes remotos de cliente modificado.
- Encerramento administrativo de conta deixou de apagar ledger/transacoes; conta vira `CLOSED`, cartoes sao desabilitados e o encerramento e recusado enquanto houver saldo ou divida.

### Senhas
- Novas senhas da interface do jogo exigem 8 a 64 caracteres.
- Novos hashes PBKDF2-HMAC-SHA256 usam 600.000 iteracoes.
- Hashes legados continuam compativeis para login.

### Website / Web API
- Adicionada Web API autenticada, habilitada somente em PostgreSQL.
- API aceita apenas bind loopback e foi feita para ficar atras de reverse proxy HTTPS.
- Adicionados login/logout, resumo de conta, historico e transferencia.
- Transferencias web usam o mesmo `AccountFinancialService`, locks, ledger e idempotencia das transferencias do jogo.
- Conta de origem e derivada da sessao autenticada, nunca do corpo enviado pelo navegador.
- Adicionados rate limit de tentativas, confirmacao de senha, CORS por origem exata, limite de body e headers de seguranca.
- Adicionado frontend estatico em `web/` e exemplo de Caddy; token fica somente em memoria.

### Projeto
- Removido `org.gradle.java.home` especifico de uma maquina Windows para deixar o projeto portavel com Java 21.
- Versao atualizada para `0.5.0`.
- Adicionada documentacao `docs/DATABASE_WEB_SECURITY.md`.

### Validacao realizada neste pacote
- Baseline SQLite executado localmente: 30 tabelas, 51 indices, `PRAGMA integrity_check = ok` e `PRAGMA foreign_key_check` sem erros.
- Teste Java 21 das funcoes independentes de engine/dialeto/fingerprint/idempotencia/parser SQL concluido com sucesso.
- Build Gradle completo nao foi executado neste ambiente porque a distribuicao/dependencias do Gradle nao estavam disponiveis offline; compile com `./gradlew build` em ambiente com acesso aos repositorios antes de publicar o JAR.
