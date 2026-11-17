# 0.5.2 — Lojas, Correio e Banco Seguros / Safe Shops, Mail and Bank

## PT-BR

- Lojas de jogador não duplicam mais itens. Antes, dois jogadores com a mesma loja aberta podiam levar o mesmo estoque: a segunda compra entregava os itens, mas o estoque não diminuía. Agora a loja confere estoque e preço na hora de cada compra ou venda; se algo mudou desde que o menu foi aberto, a compra para e a loja aparece atualizada.
- Enquanto o dono está mexendo na loja, ninguém compra nem vende nela ("O dono está mexendo nesta loja agora").
- Quebrar uma loja não cria mais itens do nada. O item que mostra o que a loja vende ou compra é só um modelo, mas caía junto com o estoque quando a loja era quebrada; na loja de compra esse modelo podia ser qualquer item, com qualquer encantamento. Agora só o estoque cai.
- Encomendas não somem mais do correio. Antes, se chegasse uma encomenda enquanto o dono estava com o correio aberto, ela desaparecia quando ele fechava. Agora o correio guarda cada retirada na hora e mostra as encomendas que chegam com ele aberto.
- Pagar em dinheiro com o valor exato não trava mais o servidor. Um valor impossível de pagar com as cédulas do inventário (por exemplo um preço ímpar só com cédulas pares) podia segurar o servidor por muito tempo; agora a resposta é imediata.
- O login no caixa eletrônico tem limite de tentativas: 5 senhas erradas em 5 minutos bloqueiam aquele usuário, e 10 bloqueiam o jogador de tentar qualquer usuário. Conferir senha também tem limite de 4 vezes a cada 10 segundos por jogador, para que um cliente modificado não consiga travar o servidor nem adivinhar senhas. Quem entrou com usuário e senha sai da conta depois de 5 senhas erradas em operações como saque e transferência.
- O banco nunca vende ouro por menos do que está pagando por ele. Com o preço dinâmico, depois de dias sem venda o banco pagava até 50% a mais pelo ouro do que cobrava para vendê-lo, e comprar e revender gerava dinheiro do nada.
- Só o titular da conta pode resgatar ouro na bancada com o cartão. Antes, um cartão perdido ou roubado podia transformar todo o saldo da conta em ouro.
- Bloco de Claim de clã: o líder e o vice-líder podem quebrá-lo (antes só o líder). Membros comuns não podem; na propriedade privada, só o proprietário.
- Loja, correio e caixa eletrônico passam a ser protegidos só pelo claim onde estão, e não mais por uma proteção própria. Fora de claim, qualquer jogador pode quebrá-los e o que estiver guardado cai no chão. Dentro de claim, só o dono do bloco e a liderança (líder ou vice do clã, ou o dono da propriedade) podem quebrar. Quem é de fora continua podendo comprar e vender nas lojas de qualquer território.
- Loja, correio ou caixa eletrônico destruído por explosão fora de claim deixa cair o que guardava; antes o conteúdo se perdia. A bancada do banco não é destruída por explosões.
- Os chunks das âncoras de claim não ficam mais carregados para sempre depois de reiniciar o servidor, mesmo com a âncora vencida. Chunks presos por versões anteriores são liberados ao iniciar.
- Explosões perto de claims pesam menos no servidor.
- Ações no caixa eletrônico, na loja, na bancada ou no correio feitas depois de o bloco ser quebrado ou de o jogador se afastar são recusadas.
- Novos testes dentro do jogo conferem o pagamento em dinheiro, o correio recebendo encomenda enquanto o dono retira itens, dois clientes comprando o último item da mesma loja, a quebra de lojas e o correio fora de claim, com SQLite e com PostgreSQL.
- Não muda o banco de dados nem as configurações.

## EN-US

- Player shops no longer duplicate items. Before, two players with the same shop open could take the same stock: the second purchase handed over the items, but the stock did not go down. The shop now checks stock and price at the moment of each purchase or sale; if anything changed since the menu was opened, the purchase stops and the shop shows up to date.
- While the owner is working on the shop, nobody can buy or sell there ("The owner is managing this shop right now").
- Breaking a shop no longer creates items from nothing. The item that shows what the shop sells or buys is only a model, but it dropped together with the stock when the shop was broken; in a buying shop that model could be any item, with any enchantment. Now only the stock drops.
- Shipments no longer vanish from the mailbox. Before, a shipment that arrived while the owner had the mailbox open disappeared when they closed it. The mailbox now saves every item taken at once and shows shipments that arrive while it is open.
- Paying the exact amount in cash no longer freezes the server. An amount that the banknotes in the inventory could not pay (for example an odd price with only even banknotes) could hold the server for a long time; the answer is now immediate.
- ATM login has an attempt limit: 5 wrong passwords in 5 minutes lock that username, and 10 lock the player out of trying any username. Password checks are also limited to 4 every 10 seconds per player, so a modified client can neither freeze the server nor guess passwords. Someone logged in with username and password is logged out after 5 wrong passwords in operations such as withdrawals and transfers.
- The bank never sells gold for less than it is paying for it. With dynamic pricing, after days without sales the bank paid up to 50% more for gold than it charged to sell it, and buying and selling back made money from nothing.
- Only the account holder can redeem gold at the bank counter with the card. Before, a lost or stolen card could turn the whole account balance into gold.
- Clan Claim Block: the leader and the vice-leader can break it (before, only the leader). Ordinary members cannot; in a private property, only the owner.
- Shops, mailboxes and ATMs are now protected only by the claim they stand in, no longer by a protection of their own. Outside a claim, any player can break them and whatever they hold drops on the ground. Inside a claim, only the block's owner and the leadership (clan leader or vice-leader, or the property owner) can break them. Outsiders can still buy and sell at shops in any territory.
- A shop, mailbox or ATM destroyed by an explosion outside a claim drops what it held; before, the contents were lost. The bank counter is not destroyed by explosions.
- Claim anchor chunks no longer stay loaded forever after a server restart, even when the anchor has expired. Chunks held by earlier versions are released at startup.
- Explosions near claims are lighter on the server.
- ATM, shop, bank counter and mailbox actions sent after the block was broken or the player walked away are refused.
- New in-game tests check cash payment, the mailbox receiving a shipment while the owner takes items, two customers buying the last item of the same shop, breaking shops and the mailbox outside a claim, with SQLite and with PostgreSQL.
- Does not change the database or configuration.

# 0.5.1 - 2026-08-31

## Website / autenticacao
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

## Configuracao
- SQLite permanece o banco padrao e a Web API permanece `enabled = false` por padrao.
- Geracao do `economia-server.toml` recebeu comentarios mais claros separando SQLite, PostgreSQL e Web API.

# 0.5.0 - 2026-08-30

## Banco de dados
- Adicionado suporte a SQLite, selecionavel com `database.type = "sqlite"` ou `"sql"`.
- Mantido PostgreSQL com aliases `postgresql`, `postgres` e `pgsql`.
- SQLite usa arquivo por save, foreign keys, WAL, busy timeout e uma unica conexao escritora.
- Adicionada camada de compatibilidade para UUID, datas, `FOR UPDATE`, `LEAST/GREATEST` e SQL dependente de dialeto.
- Adicionado baseline SQLite equivalente ao schema atual; migrations historicas PostgreSQL continuam intactas.
- Executor de migrations deixou de dividir scripts ingenuamente por `;` e passa a respeitar strings, comentarios e blocos dollar-quoted.
- Reset de banco agora diferencia PostgreSQL/SQLite e retorna o estado do banco para indisponivel se falhar.

## Seguranca economica / anti-duplicacao
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

## Senhas
- Novas senhas da interface do jogo exigem 8 a 64 caracteres.
- Novos hashes PBKDF2-HMAC-SHA256 usam 600.000 iteracoes.
- Hashes legados continuam compativeis para login.

## Website / Web API
- Adicionada Web API autenticada, habilitada somente em PostgreSQL.
- API aceita apenas bind loopback e foi feita para ficar atras de reverse proxy HTTPS.
- Adicionados login/logout, resumo de conta, historico e transferencia.
- Transferencias web usam o mesmo `AccountFinancialService`, locks, ledger e idempotencia das transferencias do jogo.
- Conta de origem e derivada da sessao autenticada, nunca do corpo enviado pelo navegador.
- Adicionados rate limit de tentativas, confirmacao de senha, CORS por origem exata, limite de body e headers de seguranca.
- Adicionado frontend estatico em `web/` e exemplo de Caddy; token fica somente em memoria.

## Projeto
- Removido `org.gradle.java.home` especifico de uma maquina Windows para deixar o projeto portavel com Java 21.
- Versao atualizada para `0.5.0`.
- Adicionada documentacao `docs/DATABASE_WEB_SECURITY.md`.

## Validacao realizada neste pacote
- Baseline SQLite executado localmente: 30 tabelas, 51 indices, `PRAGMA integrity_check = ok` e `PRAGMA foreign_key_check` sem erros.
- Teste Java 21 das funcoes independentes de engine/dialeto/fingerprint/idempotencia/parser SQL concluido com sucesso.
- Build Gradle completo nao foi executado neste ambiente porque a distribuicao/dependencias do Gradle nao estavam disponiveis offline; compile com `./gradlew build` em ambiente com acesso aos repositorios antes de publicar o JAR.
