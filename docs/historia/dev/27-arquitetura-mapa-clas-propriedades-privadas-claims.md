# História técnica — Mapa, clãs, propriedades privadas, claims e permissões

## Objetivo

Adicionar ao mod um sistema integrado de mapa, localizações, chat, clãs, propriedades privadas,
claims, permissões territoriais, blocos protegidos e contas coletivas sem alterar
as regras financeiras e comerciais existentes.

Toda decisão crítica é validada no servidor. O cliente envia intenções e exibe o
resultado. O SQL permanece como fonte oficial dos dados persistentes.

## Compatibilidade obrigatória

- Preservar Caixa Eletrônico, Loja de Compra, Loja de Venda, Correio e Bancada do Banco.
- Preservar propriedade, estoques, saldos, cartões, sessões e transações existentes.
- Nunca permitir que uma permissão territorial substitua a proteção específica de um bloco.
- Bloquear por segurança quando uma autorização depender do SQL indisponível.
- Criar propriedade somente durante a colocação; abrir um bloco nunca cria ou transfere dono.

## Configuração inicial

```text
clanMemberLimit = 20
privatePropertyMemberLimit = 5
claimMinChunks = 4
claimMaxChunks = 20
claimUpgradeBasePrice = 10000
claimUpgradeMinPercentage = 10
claimUpgradeMaxPercentage = 30
claimExternalDistance = 3
privatePropertyClaimDistance = 1
clanMaxTerritories = 3
privatePropertyMaxTerritories = 3
clanLeadershipInactivityDays = 20
clanLeadershipCandidateActiveDays = 3
```

Receitas dos blocos de claim, destinos de saldo ao encerrar um grupo, validade de
convites, limites de nomes/localizações e distância entre
territórios do mesmo clã ficam isolados para configuração posterior.

## Clãs

- Um jogador pertence a no máximo um clã.
- Cargos: `LEADER`, `VICE_LEADER` e `MEMBER`.
- Novo membro recebe apenas `USE`.
- Líder e vice podem convidar, remover membros e administrar permissões.
- Somente o líder desfaz o clã e administra claims.
- Líder e vice administram tesouraria, fundo de apoio, upgrades e nome.
- Inatividade usa tempo acumulado de servidor online, nunca tempo de calendário offline.
- Se líder e vice excederem o limite, o membro elegível mais recente assume; sem candidato, o clã é encerrado e seus claims são removidos.

## Propriedades Privadas

- Um jogador pode possuir até o limite configurado de propriedades e manter simultaneamente um clã.
- Não existe líder; cada território registra seu `owner_player_uuid`.
- Membros são convidados por território e recebem acesso apenas àquela propriedade.
- Propriedade Privada não expira por inatividade.
- Somente o proprietário confirma, expande, reduz, vende ou remove o território.
- O grupo interno com papel `OWNER` funciona apenas como portfólio técnico e conta coletiva do proprietário.

## Claims e âncoras

- Claims de clã e propriedade privada são separados, mas nunca se sobrepõem.
- Cada território conectado precisa conter ao menos um bloco de claim válido.
- Clã: líder ou vice coloca a âncora; somente o líder ativa, expande, reduz ou quebra.
- Propriedade Privada: qualquer jogador coloca a âncora pendente; quem confirma torna-se proprietário e passa a controlar o território.
- O chunk da âncora conta no limite e não pode ser removido diretamente pelo mapa.
- A expansão é iniciada pelo botão `Comprar chunk` da âncora, usa seleção e confirmação
  no mapa e debita o preço do cartão autenticado, sem gerar boleto.
- O mapa comum remove chunks permitidos, mas não adiciona chunks gratuitamente.
- Expansão aceita apenas vizinhos ortogonais.
- Remover chunk ou âncora recalcula componentes e elimina partes sem âncora.
- O limite pertence ao grupo inteiro, somando todos os territórios.
- Clã e propriedade privada possuem, por padrão, até 3 territórios independentes; cada limite é configurável de 1 até `Integer.MAX_VALUE`.
- A distância externa é aplicada entre grupos diferentes; `0` a desabilita.
- A colocação é negada dentro de claim, em chunk com âncora, perto demais de outro grupo ou sem cargo suficiente.

## Permissões territoriais

Permissões do clã:

- `USE`: usar blocos comuns/interativos autorizados.
- `DESTROY`: destruir blocos comuns.
- `PLACE`: colocar blocos comuns.

Visitantes não recebem `USE`. Podem receber apenas `USE_BUY_SHOP` e
`USE_SELL_SHOP`, independentemente.

Prioridade de autorização:

1. `SYSTEM_PROTECTED`: somente regra administrativa própria.
2. `GROUP_PROTECTED`: regra do grupo e do tipo do bloco.
3. `OWNER_PROTECTED`: dono ou liderança autorizada dentro do claim do próprio grupo.
4. `COMMON`: permissões territoriais `USE`, `DESTROY` e `PLACE`.

Dentro do claim do próprio clã, líder e vice substituem o dono de Caixa Eletrônico,
Loja de Compra, Loja de Venda e Correio. Na propriedade privada não existe líder: somente
o proprietário daquele terreno substitui o dono. Fora do território, vale exclusivamente a regra original.
A Bancada do Banco nunca recebe override territorial.

## Blocos de grupo

- Bloco de Claim do Clã e Bloco de Claim da Propriedade Privada: âncoras físicas.
- Baú do Clã: somente líder e vice usam ou quebram dentro do claim do clã.
- Baú da Propriedade Privada: somente proprietário e membros convidados usam; somente o proprietário quebra.
- Bloco de Gerenciamento do Clã e da Propriedade Privada: autenticação por cartão e navegação gráfica.
- Baús e blocos de gerenciamento não alteram a propriedade dos blocos comerciais.

Receitas definidas:

```text
Baú da Propriedade Privada              Baú do Clã
T | C | T                 T | F | T
T | P | T                 T | P | T
T | T | T                 T | T | T

Gerenciamento Propriedade Privada      Gerenciamento Clã
T | C | T                 T | F | T
P | R | P                 P | R | P
T | T | T                 T | T | T
```

`T` é tábua, `C` cobre, `F` ferro, `P` papel e `R` redstone.

## Mapa e localizações

- `M` abre mapa em tela cheia e configurável nos controles.
- O mapa permite arrastar, aplicar zoom e visualizar jogador, chunks, claims e localizações.
- `P` abre o modal de localização usando a coordenada sob o ponteiro, não a posição do jogador.
- Localização contém dono, nome, dimensão, X, Y e Z e pode ser editada antes de salvar.
- A lista permite excluir e compartilhar em Geral, Clã ou Propriedade Privada quando disponíveis.
- Compartilhamento gera mensagem clicável; o receptor abre o mapa e confirma antes de salvar.

## Chat

- Canais internos: `GENERAL`, `CLAN` e `PRIVATE_PROPERTY`; este último é exibido como Propriedade Privada.
- A troca de canal ocorre pela interface, sem comando obrigatório.
- Canais de grupo aparecem somente para seus membros.
- Localizações compartilhadas são componentes clicáveis e nunca são salvas automaticamente.

## Interfaces

- Todas as telas são responsivas a largura, altura e escala da GUI.
- Botões, campos, listas, modais, cabeçalhos, saldo, Voltar e Sair são reutilizáveis.
- Voltar preserva a sessão e retorna um nível da navegação.
- Sair ou `ESC` devolve cartão, encerra a sessão e fecha a interface.
- A autenticação dos blocos de gerenciamento usa cartão antes de liberar ações.
- O Bloco de Claim também exige cartão em toda abertura; a autenticação não sobrevive
  ao fechamento da interface.
- Depois da autenticação, o slot e o item do cartão ficam ocultos e inativos; o servidor mantém o item protegido até devolvê-lo no fechamento.
- Todo slot interativo possui desenho individual compatível com o inventário do Minecraft.
- Menus exibem somente ações compatíveis com o cargo, mas o servidor sempre revalida.

## Persistência

Persistir:

- relógio de tempo ativo do servidor e última atividade dos jogadores;
- clãs, cargos, membros, permissões, convites, limites, tesouraria, fundo e visitantes;
- propriedades privadas por proprietário, membros convidados por terreno, limites, conta e visitantes;
- claims, territórios, preço e dívida do terreno, blocos-âncora e validade do carregamento de chunk;
- boletos de taxas/âncora, consolidação de taxas e venda, incluindo comprador e estado
  do pagamento; boletos de terreno antigos permanecem apenas por compatibilidade;
- localizações pessoais;
- estado necessário para upgrades e auditoria.

As contas coletivas reutilizam `economy_accounts` e as transferências reutilizam o
ledger/transações existentes. IDs existentes não são alterados.

## Critérios de segurança

- Nenhuma ação sensível depende apenas de botão habilitado no cliente.
- Colocação, quebra, interação, convites, cargos, permissões, claims, upgrades,
  nomes e encerramento são revalidados no servidor.
- O item de bloco registrado não preserva dono nem ID comercial ao cair.
- A remoção de bloco comercial continua derrubando inventário e marcando SQL como `REMOVED`.
- Explosões, pistões e alterações não originadas por jogador não podem contornar claims ou âncoras.
- Administrador com nível de permissão `2` pode quebrar a âncora de claim; nesse caso o
  território e todos os chunks vinculados são removidos.
- Lojas de compra e venda são públicas para interação, inclusive dentro de claims, mas
  continuam protegidas e removíveis por seu proprietário ou pela administração autorizada.

## Confirmação, cobrança e transferência

- Colocar o bloco cria uma âncora pendente; o claim nasce somente após `Dar Claim`.
- O preço é calculado pela dimensão e distância ao centro com valores do config do mod.
- `Dar Claim` abre pagamento por cartão ou dinheiro; somente a aprovação idempotente cria o território ativo.
- Cartão exige escolha explícita entre crédito e débito; dinheiro usa somente os slots do menu e não dá troco.
- O pagamento do boleto da âncora estende `anchor_paid_until_millis`; somente períodos vigentes forçam o chunk.
- A compra de expansão usa débito no cartão autenticado, após confirmação explícita no
  mapa, e não cria boleto nem dívida nova.
- A taxa atual e as taxas pendentes podem ser impressas separadamente ou consolidadas
  em um boleto `BUNDLE`.
- A venda é concluída no pagamento idempotente do boleto e altera grupo/proprietário de todos os chunks do território.
- Dívida do terreno e validade paga da âncora pertencem ao território e não são apagadas na venda.

## Upgrade do limite de chunks

- Clã e Propriedade Privada começam em `claimMinChunks` e podem alcançar `claimMaxChunks`.
- Cada compra aumenta o limite total do grupo em um chunk.
- Percentual e preço seguem a progressão configurável de `HIST-DEV-31`.
- O servidor recalcula o próximo nível no pagamento e concede o upgrade uma única vez.
- No limite máximo, a interface informa o estado e desabilita a compra.
