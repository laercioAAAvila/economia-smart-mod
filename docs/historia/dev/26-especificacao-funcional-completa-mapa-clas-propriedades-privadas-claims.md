# Especificação Funcional Completa

> Atualizações vigentes: Propriedade Privada não possui líder. O jogador que confirma o claim é o proprietário do lote, e os membros são convidados por propriedade. As regras de confirmação, cobrança, âncora e venda estão consolidadas na história `HIST-DEV-29`. O upgrade progressivo, o pagamento de `Dar Claim` e os ajustes visuais são definidos por `HIST-DEV-31`, que prevalece em caso de divergência.

## Mapa, Clãs, Propriedades Privadas, Claims, Permissões, Blocos Protegidos, Chat e Sistema Bancário

# 1. Objetivo geral

O mod deverá adicionar um sistema integrado contendo:

* Mapa em tela cheia.
* Localizações personalizadas.
* Compartilhamento de localizações.
* Chat Geral, de Clã e de Propriedade Privada.
* Sistema de Clãs.
* Sistema de Propriedades Privadas.
* Claims por chunk.
* Blocos de Claim.
* Proteção territorial.
* Permissões de membros.
* Permissões de visitantes.
* Blocos e baús especiais.
* Blocos de gerenciamento de Clã e Propriedade Privada.
* Autenticação utilizando cartão.
* Contas bancárias de Clã e Propriedade Privada.
* Fundo de Apoio do Clã.
* Upgrades.
* Controle de inatividade do Clã.

Todos os recursos destinados aos jogadores deverão funcionar por **interfaces gráficas**.

Não deverão existir comandos obrigatórios para utilizar esses sistemas.

---

# 2. Regra principal de desenvolvimento

Esta implementação será adicionada a um mod que já possui vários sistemas funcionando.

Portanto:

> É obrigatório tomar cuidado para não quebrar, substituir ou alterar funcionalidades já existentes no mod que não estejam diretamente relacionadas com esta implementação.

Antes de modificar classes, eventos, registros, banco de dados ou interfaces existentes, deverá ser analisado como o sistema atual funciona.

Sempre que possível:

* reaproveitar serviços existentes;
* reaproveitar sistema bancário existente;
* reaproveitar autenticação por cartão;
* reaproveitar componentes de interface;
* reaproveitar eventos e validações já existentes;
* adicionar funcionalidades sem remover comportamento anterior.

Não deverão ser feitas grandes refatorações desnecessárias apenas para implementar Clã, Propriedade Privada ou Claim.

---

# 3. Compatibilidade com sistemas existentes

O novo sistema deverá coexistir com:

* Caixa Eletrônico.
* Loja de Compra.
* Loja de Venda.
* Correio.
* Bancada do Banco.
* Sistema bancário.
* Cartões.
* Outros blocos existentes.
* Outros blocos que possam ser adicionados futuramente.

As regras existentes desses blocos deverão ser preservadas.

---

# 4. Interfaces responsivas

Todas as novas interfaces deverão ser responsivas.

Isso significa que a interface deverá continuar utilizável em diferentes resoluções e escalas de GUI do Minecraft.

Não posicionar toda a interface utilizando valores absolutos sem considerar:

```text
screenWidth
screenHeight
guiScale
```

Elementos deverão ser centralizados ou posicionados relativamente à tela/conteúdo.

---

# 5. Componentes reutilizáveis

Os elementos das interfaces deverão ser reutilizáveis.

Principalmente:

* botões;
* inputs;
* modais;
* listas;
* cabeçalhos;
* menus laterais;
* botão Voltar;
* botão Sair;
* botões de confirmação;
* componentes de saldo;
* linhas de membros.

Não criar um botão visualmente diferente em cada tela quando o comportamento for equivalente.

Exemplo conceitual:

```text
ModButton
ModTextField
ModModal
ModList
ModBackButton
ModConfirmModal
```

Isso deverá facilitar futuras alterações sem quebrar várias interfaces.

---

# 6. Navegação das interfaces

Depois que o jogador estiver dentro de um menu, as telas deverão possuir navegação em cascata.

Exemplo:

```text
Menu Principal
    ↓
Gerenciar membros
    ↓
Detalhes do membro
```

Ao clicar em Voltar:

```text
Detalhes
→ Gerenciar membros
→ Menu Principal
```

O botão:

```text
Voltar
```

não encerra autenticação.

O botão:

```text
Sair
```

encerra a sessão, devolve o cartão e fecha a interface.

---

# 7. UTF-8 e idioma

Todo o sistema deverá possuir suporte correto a UTF-8.

Textos em português deverão exibir corretamente:

* Clã.
* Líder.
* Vice-líder.
* Localização.
* Permissão.
* Destruir.
* Configuração.
* Não.
* Usuário.

Arquivos de tradução deverão ser salvos em UTF-8.

Preferencialmente utilizar:

```text
assets/<modid>/lang/pt_br.json
```

Evitar textos fixos diretamente no código.

---

# 8. Mapa

A tecla padrão para abrir o mapa será:

```text
M
```

Essa tecla poderá ser alterada pelo jogador através dos controles do Minecraft.

O mapa deverá abrir em tela cheia.

Deverá permitir:

* arrastar o mapa;
* zoom;
* visualizar jogador;
* visualizar chunks;
* visualizar claims;
* visualizar localizações;
* adicionar localizações;
* gerenciar localizações.

---

# 9. Criar localização

Com o mapa aberto, a tecla padrão:

```text
P
```

criará uma nova localização.

Também deverá ser configurável.

A posição utilizada será aquela onde o **ponteiro do mouse estiver localizado sobre o mapa** no momento em que P for pressionado.

Não necessariamente a posição atual do jogador.

---

# 10. Modal de nova localização

Ao pressionar P:

```text
NOVA LOCALIZAÇÃO

Nome:
[________________________]

Localização:
[X: ___ Y: ___ Z: ______]

[Cancelar] [Criar]
```

O campo Localização será preenchido automaticamente.

O jogador poderá alterá-lo antes de confirmar.

Também deverá ser armazenada a dimensão:

```text
minecraft:overworld
minecraft:the_nether
minecraft:the_end
```

---

# 11. Menu lateral do mapa

No lado esquerdo haverá um menu.

Uma das opções será:

```text
Localizações
```

Se o jogador não possuir nenhuma localização:

```text
Localizações = desabilitado
```

Se possuir:

```text
Localizações = habilitado
```

---

# 12. Gerenciamento de localizações

Ao entrar:

```text
LOCALIZAÇÕES

Minha Casa
X: 120 Z: 450
[Excluir] [Compartilhar]

Mina
X: 850 Z: -300
[Excluir] [Compartilhar]

[Voltar]
```

---

# 13. Compartilhamento

Ao clicar em Compartilhar:

```text
COMPARTILHAR

[Clã]
[Propriedade Privada]
[Geral]

[Cancelar]
```

Clã só estará disponível se o jogador possuir Clã.

Propriedade Privada só estará disponível se possuir Propriedade Privada.

---

# 14. Localização no chat

A localização compartilhada deverá aparecer como mensagem clicável.

Exemplo:

```text
[Localização] Mina de Ferro
X: 1450 Z: -720
```

Ao clicar:

1. Abrir mapa.
2. Centralizar na posição.
3. Abrir modal de localização.
4. Preencher os dados recebidos.
5. Jogador confirma se deseja salvar.

Nunca salvar automaticamente.

---

# 15. Chat

Existirão três canais:

```text
GERAL
CLÃ
PROPRIEDADE PRIVADA
```

A troca deverá ocorrer pela própria interface.

Não serão necessários comandos.

O canal Clã somente aparecerá para membros de Clã.

Propriedade Privada somente para proprietários ou membros convidados de ao menos um
território privado.

---

# 16. Clã

Cada jogador poderá fazer parte de apenas:

```text
1 Clã
```

mas poderá possuir simultaneamente:

```text
1 Clã
+
Propriedades Privadas até o limite configurado de territórios
```

---

# 17. Limite do Clã

Padrão:

```text
20 membros
```

Configuração:

```text
clanMemberLimit = 20
```

---

# 18. Cargos do Clã

Existirão:

```text
Líder
Vice-líder
Membro
```

---

# 19. Líder do Clã

Poderá:

* convidar.
* remover membros.
* gerenciar permissões.
* colocar Bloco de Claim.
* ativar Claims.
* adicionar chunks.
* remover chunks.
* quebrar Bloco de Claim.
* utilizar Baú do Clã.
* quebrar Baú do Clã.
* movimentar Tesouraria.
* gerenciar Fundo de Apoio.
* realizar upgrades.
* alterar nome.
* desfazer Clã.

---

# 20. Vice-líder do Clã

Poderá:

* convidar.
* remover membros.
* gerenciar permissões.
* colocar Bloco de Claim.
* utilizar Baú do Clã.
* quebrar Baú do Clã.
* movimentar Tesouraria.
* gerenciar Fundo de Apoio.
* realizar upgrades.
* alterar nome.

Entretanto:

> O Vice-líder NÃO poderá quebrar o Bloco de Claim.

Também não poderá desfazer o Clã.

---

# 21. Bloco de Claim — quem pode colocar

## Clã

Poderão colocar:

```text
Líder
Vice-líder
```

## Propriedade Privada

Somente:

```text
Proprietário da Propriedade Privada
```

---

# 22. Bloco de Claim — quem pode quebrar

Independentemente de quem colocou:

## Clã

Somente:

```text
Líder do Clã
```

poderá quebrar.

O Vice-líder poderá colocar, mas não poderá destruir.

## Propriedade Privada

Somente:

```text
Proprietário da Propriedade Privada
```

poderá quebrar.

A permissão:

```text
D
```

nunca autoriza destruir um Bloco de Claim.

---

# 23. Desfazer Clã

Somente:

```text
Líder
Administrador autorizado do servidor
```

poderão desfazer.

---

# 24. Inatividade da liderança

Valor padrão:

```text
20 dias
```

Configuração:

```text
clanLeadershipInactivityDays = 20
```

A regra será aplicada somente se:

```text
Líder inativo
E
Vice-líder inativo
```

pelo período necessário.

---

# 25. Tempo de inatividade do servidor

Os 20 dias são baseados em **tempo real de funcionamento do servidor**.

Não são dias do Minecraft.

Também não é simplesmente diferença entre datas do calendário.

Exemplo:

```text
Servidor ficou:

3 dias ONLINE
2 dias OFFLINE
```

Contabilizado:

```text
3 dias
```

Não:

```text
5 dias
```

---

# 26. Relógio de atividade

Deverá existir um contador persistente equivalente a:

```text
serverActiveTime
```

Ele acumulará somente o tempo real durante o qual o servidor estiver funcionando.

Servidor desligado:

```text
contador pausado
```

---

# 27. Última atividade

Cada membro armazenará o valor correspondente ao momento do último login.

Exemplo:

```text
lastServerActiveTime
```

Inatividade:

```text
serverActiveTimeAtual - lastServerActiveTime
```

---

# 28. Sucessão do Clã

Quando Líder e Vice-líder atingirem o limite:

```text
Procurar membro ativo recentemente
```

Padrão:

```text
3 dias ativos do servidor
```

Configuração:

```text
clanLeadershipCandidateActiveDays = 3
```

Escolher o membro que acessou mais recentemente.

Se nenhum membro for elegível:

```text
Clã é desfeito.
```

Claims também serão removidos.

---

# 29. Convites do Clã

Podem convidar:

```text
Líder
Vice-líder
```

Somente jogadores:

* online;
* que ainda não possuem Clã.

Interface:

```text
CONVIDAR

Nome:
[________________]

[Enviar convite]
[Voltar]
```

---

# 30. Propriedade Privada

Cada jogador poderá possuir:

```text
até o limite configurado de territórios privados
```

Limite padrão:

```text
5
```

Configuração:

```text
privatePropertyMemberLimit = 5
```

A Propriedade Privada terá, por território:

```text
Proprietário
Membros convidados
```

Não terá Líder nem Vice-líder.

---

# 31. Inatividade da Propriedade Privada

Propriedade Privada não é removida automaticamente por inatividade.

O proprietário poderá remover ou vender o território conforme as regras vigentes.

---

# 32. Claims

Existirão:

```text
Claim de Clã
Claim de Propriedade Privada
```

São sistemas separados.

Claims não podem se sobrepor.

As permissões do respectivo grupo serão aplicadas a todo território reclamado.

---

# 33. Blocos de Claim

Existirão:

```text
Bloco de Claim do Clã
Bloco de Claim da Propriedade Privada
```

O bloco é a **âncora física** do território.

Sem uma âncora válida não poderá existir um conjunto independente de Claims.

---

# 34. Colocação do Bloco de Claim

Não poderá ser colocado:

* em chunk que já tenha outro Bloco de Claim;
* dentro de Claim existente;
* dentro de território de Propriedade Privada;
* dentro de território de Clã;
* em local que viole distância mínima;
* por jogador sem cargo suficiente.

Toda validação deverá acontecer no servidor.

---

# 35. Interface do Bloco de Claim

Ao abrir:

```text
CLAIM DO CLÃ

[Claim]
[Sair]
```

ou:

```text
CLAIM DA PROPRIEDADE PRIVADA

[Claim]
[Sair]
```

---

# 36. Botão Claim

Somente estará habilitado para:

## Clã

```text
Líder
```

## Propriedade Privada

```text
Líder
```

Mesmo o Vice-líder do Clã podendo colocar o Bloco de Claim, a expansão territorial continua sendo controlada pelo Líder.

A validação real deverá acontecer no servidor.

---

# 37. Primeiro Claim

Ao clicar em:

```text
Claim
```

o chunk do bloco deverá ser reclamado imediatamente.

Depois o mapa será aberto automaticamente.

Exemplo:

```text
C  | C  | C
C  | BC | C
C  | C  | C
```

Onde:

```text
BC = Bloco de Claim + chunk reclamado
C  = livre
```

---

# 38. Chunk do Bloco de Claim

O chunk com o Bloco de Claim:

* conta no limite;
* não poderá ser removido pelo mapa.

Ao clicar tentando remover:

```text
Este chunk contém um Bloco de Claim.
Quebre o Bloco de Claim para remover esta âncora.
```

---

# 39. Expansão pelo mapa

Depois da ativação, a expansão parte do Bloco de Claim do território:

```text
Comprar chunk
→ abrir mapa de seleção
→ mostrar o preço do chunk apontado
→ clicar em chunk livre adjacente
→ adicionar Claim e emitir boleto

No mapa comum, clique em chunk próprio
→ remover Claim
```

Exceto o chunk da âncora.

A adição gratuita pelo mapa comum não é permitida.

---

# 40. Adjacência obrigatória

Um novo chunk somente poderá ser adicionado se estiver ligado diretamente por um lado a outro Claim do mesmo território.

Permitido:

```text
cima
baixo
esquerda
direita
```

Não permitido:

```text
diagonal
```

---

# 41. Exemplo válido

```text
C  | C  | C
C  | BC | CC
C  | CC | CC
```

Válido.

---

# 42. Exemplo inválido

```text
C  | C  | CC
C  | BC | C
C  | C  | CC
```

Inválido porque os chunks estão ligados apenas diagonalmente.

---

# 43. Vizinhos válidos

Para:

```text
chunkX
chunkZ
```

considerar somente:

```text
X + 1, Z
X - 1, Z
X, Z + 1
X, Z - 1
```

---

# 44. Limite da Propriedade Privada

Inicial:

```text
4
```

Máximo padrão:

```text
20
```

Configuração:

```text
claimMinChunks = 4
claimMaxChunks = 20
```

---

# 45. Limite do Clã

Inicial:

```text
4
```

Máximo:

```text
16
```

Configuração:

```text
claimMinChunks = 4
claimMaxChunks = 20
```

---

# 46. Contagem

Todos os chunks contam no limite, inclusive os chunks contendo Bloco de Claim.

Exemplo:

```text
Limite atual = 8

2 chunks com Bloco de Claim
+
4 chunks normais
=
6 / 8
```

---

# 47. Vários Blocos de Claim

Um mesmo grupo poderá possuir vários Blocos de Claim se tiver limite disponível.

O limite é do grupo inteiro, não de cada bloco.

Exemplo:

```text
Propriedade Privada possui limite 8.

Território A = 3
Território B = 2

Total:
5 / 8
```

---

# 48. Distância entre grupos diferentes

Padrão:

```text
3 chunks
```

Configuração:

```text
claimExternalDistance = 3
```

Aplica-se entre:

* Clã e Clã.
* Clã e Propriedade Privada.
* Propriedade Privada e Clã.
* Propriedade Privada e Propriedade Privada diferentes.

Pode ser:

```text
0
```

para desabilitar a distância.

---

# 49. Distância da Propriedade Privada

Para novos territórios separados da mesma Propriedade Privada:

```text
privatePropertyClaimDistance = 1
```

Configurável.

---

# 50. Território conectado

Todo conjunto reclamado deverá possuir pelo menos um Bloco de Claim conectado.

Conectividade é somente por lados.

---

# 51. Quebra do Bloco de Claim

Quando o Líder quebrar:

```text
Bloco destruído
↓
Chunk da âncora perde Claim
↓
Servidor recalcula território
↓
Procura outro Bloco de Claim conectado
```

Se não existir:

```text
todo aquele território perde seus Claims
```

---

# 52. Remover chunk intermediário

Ao remover um chunk pelo mapa, o servidor deverá recalcular os componentes.

Se alguma parte ficar isolada sem Bloco de Claim:

```text
Claims isolados são removidos.
```

Isso impede Claims flutuantes.

---

# 53. Proteção territorial

Jogador externo não poderá:

* destruir.
* colocar.
* usar blocos privados.
* abrir baús privados.
* modificar máquinas.
* alterar território.

Exceto recursos explicitamente permitidos a visitantes.

---

# 54. Permissões do Clã

Existirão:

```text
U = Usar
D = Destruir
C = Colocar
```

Novo membro começa apenas com:

```text
U
```

---

# 55. U — Usar

Permite utilizar blocos comuns/interativos autorizados.

Não ignora regras específicas de propriedade.

---

# 56. D — Destruir

Permite destruir **blocos comuns** dentro do território.

Não significa:

```text
pode destruir qualquer bloco
```

---

# 57. C — Colocar

Permite colocar blocos dentro do território, respeitando regras especiais de blocos protegidos.

---

# 58. Permissões da Propriedade Privada

Por padrão os membros possuem:

```text
U D C
```

Não haverá gerenciamento individual da Propriedade Privada nesta versão.

---

# 59. Regra fundamental das permissões

A proteção específica de um bloco sempre possui prioridade sobre a permissão territorial.

Portanto:

> U, D e C nunca podem anular uma proteção que já existe no bloco.

---

# 60. Blocos existentes com proprietário

Atualmente:

| Bloco            | Quem pode colocar      | Dono         | Destruição normal                      |
| ---------------- | ---------------------- | ------------ | -------------------------------------- |
| Caixa Eletrônico | Qualquer jogador       | Quem colocou | Dono ou regra administrativa existente |
| Loja de Venda    | Qualquer jogador       | Quem colocou | Dono ou regra administrativa existente |
| Loja de Compra   | Qualquer jogador       | Quem colocou | Dono ou regra administrativa existente |
| Correio          | Qualquer jogador       | Quem colocou | Dono ou regra administrativa existente |
| Bancada do Banco | Operador/admin nível 2 | Sem dono     | Regra especial existente               |

Essas regras devem continuar funcionando.

---

# 61. D e blocos protegidos

Exemplo:

Pedro tem:

```text
U D C
```

João colocou uma Loja de Venda.

Pedro não é o proprietário.

Resultado:

```text
D NÃO permite quebrar a loja.
```

---

# 62. Override do Clã

Dentro de Claim do próprio Clã:

```text
Proprietário
OU
Líder
OU
Vice-líder
```

podem destruir blocos protegidos por proprietário, como:

* Loja.
* Correio.
* Caixa Eletrônico.

---

# 63. Override da Propriedade Privada

Dentro de Claim da própria Propriedade Privada:

```text
Proprietário
OU
Proprietário da Propriedade Privada
```

podem destruir.

Membro comum não poderá mesmo tendo `D`.

---

# 64. Fora do território

Líder ou Vice-líder não ganha poder sobre propriedade dos membros fora do Claim.

Fora do Claim:

```text
regra original do bloco
```

permanece.

---

# 65. Bancada do Banco

A Bancada do Banco é um bloco sistêmico.

Ela não possui proprietário comum.

As regras de Clã e Propriedade Privada não poderão permitir sua destruição.

Continuará seguindo sua regra administrativa própria.

---

# 66. Categorias de proteção

Para facilitar crescimento futuro, os blocos deverão ser tratados por política.

Sugestão:

```text
COMMON
OWNER_PROTECTED
GROUP_PROTECTED
SYSTEM_PROTECTED
```

---

# 67. COMMON

Blocos comuns.

U/D/C podem atuar normalmente.

---

# 68. OWNER_PROTECTED

Exemplos:

```text
Loja de Compra
Loja de Venda
Correio
Caixa Eletrônico
```

Possuem proprietário.

`D` não substitui propriedade.

---

# 69. GROUP_PROTECTED

Exemplos:

```text
Bloco de Claim
Baú do Clã
Baú da Propriedade Privada
```

Possuem regras próprias do grupo.

---

# 70. SYSTEM_PROTECTED

Exemplo:

```text
Bancada do Banco
```

Somente suas regras administrativas internas.

---

# 71. Regra para novos blocos

Qualquer novo bloco protegido deverá definir:

* quem coloca;
* se possui dono;
* quem usa;
* quem destrói;
* se Líder pode substituir dono;
* se Vice-líder pode substituir dono;
* se o Proprietário da Propriedade Privada pode substituir dono;
* se visitante pode utilizar;
* se U se aplica;
* se D se aplica;
* se C se aplica.

Não espalhar verificações específicas por dezenas de classes.

---

# 72. Baú da Propriedade Privada

Somente:

```text
Proprietário da Propriedade Privada
```

poderá:

* abrir;
* utilizar;
* quebrar.

Mesmo se outro membro tiver colocado.

Proteção especial somente dentro de Claim da própria Propriedade Privada.

---

# 73. Receita do Baú da Propriedade Privada

```text
T = Tábua
BC = Barra de cobre
P = Papel
```

```text
T | BC | T
T | P  | T
T | T  | T
```

---

# 74. Baú do Clã

Somente:

```text
Líder
Vice-líder
```

podem:

* usar;
* abrir;
* quebrar.

Mesmo que outro membro tenha colocado.

Proteção especial somente dentro do Claim do próprio Clã.

---

# 75. Receita do Baú do Clã

```text
T = Tábua
BF = Barra de ferro
P = Papel
```

```text
T | BF | T
T | P  | T
T | T  | T
```

---

# 76. Permissão de visitante

Claims poderão permitir acesso público controlado.

Visitante significa:

```text
jogador que não pertence ao grupo proprietário do Claim
```

---

# 77. Visitantes e lojas

Deverão existir permissões específicas:

```text
Usar Loja de Compra
Usar Loja de Venda
```

Cada uma poderá ser ligada/desligada separadamente.

Exemplo:

```text
PERMISSÕES DE VISITANTE

Loja de Compra   [ON]
Loja de Venda    [ON]
```

---

# 78. Visitante não recebe U

Visitante não deverá ganhar a permissão genérica `U`.

Ele recebe apenas acesso ao recurso explicitamente autorizado.

---

# 79. Visitante não poderá

Mesmo com lojas liberadas:

* destruir.
* colocar.
* abrir baús privados.
* abrir Baú do Clã.
* abrir Baú da Propriedade Privada.
* usar Bloco de Claim.
* editar loja.
* remover estoque diretamente.
* alterar configuração da loja.
* gerenciar Clã.
* gerenciar Propriedade Privada.

---

# 80. Regras internas das lojas

Permissão de visitante apenas autoriza abrir/utilizar como cliente.

A Loja ainda deverá verificar suas regras normais:

```text
estoque
preço
saldo
transação
proprietário
configuração
```

---

# 81. Propriedade ausente no banco

Existe atualmente um comportamento perigoso:

> Loja ou Correio sem registro pode acabar vinculando o primeiro jogador que abrir.

Isso deverá ser corrigido.

Abrir um bloco nunca deverá estabelecer propriedade.

---

# 82. Registro correto

Propriedade deverá ser criada no momento da colocação.

```text
Jogador coloca
↓
Servidor cria registro
↓
Novo ID
↓
UUID do proprietário
```

Se depois o registro desaparecer:

```text
Bloquear interação
```

Não transformar quem abriu em proprietário.

---

# 83. Banco de dados indisponível

Se uma operação exigir consulta ao banco e ele estiver indisponível:

```text
bloquear por segurança
```

Nunca presumir autorização.

Aplica-se a:

* colocar blocos registrados;
* destruir;
* proprietário;
* Claims;
* membros;
* permissões;
* transações.

---

# 84. Destruição de bloco registrado

Manter comportamento atual:

```text
destruir
↓
derrubar inventário interno
↓
registro SQL = REMOVED
```

O item derrubado não preserva dono.

Quando recolocado:

```text
novo ID
novo proprietário
```

---

# 85. Bloco de gerenciamento da Propriedade Privada

Receita:

```text
T = Tábua
BC = Barra de cobre
P = Papel
R = Redstone
```

```text
T | BC | T
P | R  | P
T | T  | T
```

---

# 86. Bloco de gerenciamento do Clã

```text
T = Tábua
BF = Barra de ferro
P = Papel
R = Redstone
```

```text
T | BF | T
P | R  | P
T | T  | T
```

---

# 87. Autenticação

Ao abrir:

```text
CARTÃO

[ SLOT ]

[Entrar]
```

Os demais botões ficam desabilitados antes da autenticação.

---

# 88. Depois de entrar

No Bloco do Clã:

```text
[CLÃ]
[SAIR]
```

No da Propriedade Privada:

```text
[PROPRIEDADE PRIVADA]
[SAIR]
```

---

# 89. ESC

Ao pressionar ESC:

1. devolver cartão;
2. encerrar autenticação;
3. fechar interface.

Mesmo comportamento de Sair.

---

# 90. Jogador sem grupo

Exibir:

```text
[Criar]
[Entrar]
[Voltar]
```

---

# 91. Criar grupo

```text
Nome:
[________________]

[Criar]
[Voltar]
```

Servidor deverá validar:

* nome vazio;
* duplicado;
* tamanho;
* caracteres;
* participação atual.

---

# 92. Entrar em grupo

Mostrar somente convites recebidos.

```text
CONVITES

Guerreiros
[Aceitar] [Recusar]

[Voltar]
```

---

# 93. Menu do Líder/Vice do Clã

Exibir conforme cargo:

```text
Convidar membro
Gerenciar membros
Gerenciar permissões
Tesouraria do Clã
Fundo de Apoio do Clã
Upgrade do Clã
Alterar nome
Desfazer Clã
Sair
```

Desfazer somente para Líder.

---

# 94. Menu do Proprietário da Propriedade Privada

```text
Convidar membro
Gerenciar membros
Conta da Propriedade Privada
Upgrade da Propriedade Privada
Alterar nome
Desfazer Propriedade Privada
Sair
```

---

# 95. Menu do membro do Clã

```text
Upgrade do Clã
Sair do Clã
Ver membros
Fundo de Apoio do Clã
Sair
```

---

# 96. Menu do membro da Propriedade Privada

```text
Upgrade da Propriedade Privada
Sair da Propriedade Privada
Ver membros
Conta da Propriedade Privada
Sair
```

---

# 97. Gerenciar membros

Exemplo:

```text
Nome       Cargo          Último acesso

João       Membro         Hoje
Pedro      Membro         2 dias
Carlos     Membro         10 dias

[Remover]
```

---

# 98. Gerenciar permissões

Somente para Clã.

Exemplo:

```text
João       U       [U] [D] [C]
Pedro      U D     [U] [D] [C]
Carlos     U D C   [U] [D] [C]
```

Novo membro começa:

```text
U
```

---

# 99. Conta da Propriedade Privada

Todos os membros poderão:

* ver saldo;
* depositar da conta pessoal;
* retirar para conta pessoal.

```text
Pessoa → Propriedade Privada
Propriedade Privada → Pessoa
```

---

# 100. Tesouraria do Clã

Somente:

```text
Líder
Vice-líder
```

poderão visualizar e movimentar.

```text
Pessoa → Tesouraria
Tesouraria → Pessoa
```

---

# 101. Fundo de Apoio do Clã

Todos os membros poderão visualizar.

Todos poderão:

```text
Pessoa → Fundo
Fundo → Pessoa
```

Nome:

```text
Fundo de Apoio do Clã
```

---

# 102. Upgrade

Deverá mostrar:

```text
Limite atual
Próximo limite
Preço
```

Exemplo:

```text
CLAIMS

Atual:
8

Próximo:
9

Preço:
$ 15.000

[Comprar]
[Voltar]
```

O preço é progressivo e recalculado a partir de `claimUpgradeBasePrice`,
`claimUpgradeMinPercentage` e `claimUpgradeMaxPercentage`. A fórmula, o pagamento e o
estado de limite máximo seguem `HIST-DEV-31`.

---

# 103. Persistência do Clã

Armazenar:

```text
id
nome
líder
vice-líder
membros
permissões
último acesso
claims
blocos de claim
limite de claims
upgrades
tesouraria
fundo de apoio
convites
permissões de visitante
```

---

# 104. Persistência da Propriedade Privada

```text
id
nome
proprietário por território
membros convidados por território
claims
blocos de claim
limite
upgrades
conta
convites
permissões de visitante
```

---

# 105. Persistência das localizações

```text
id
playerUUID
nome
dimensão
x
y
z
```

---

# 106. Configuração inicial

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

clanLeadershipInactivityDays = 20

clanLeadershipCandidateActiveDays = 3
```

---

# 107. Segurança

Todas as decisões importantes deverão acontecer no servidor.

Nunca confiar apenas no estado visual da interface.

Validar no servidor:

* criação de Clã;
* Propriedade Privada;
* convite;
* remoção;
* cargo;
* permissões;
* Claim;
* quebra;
* colocação;
* uso;
* visitante;
* proprietário;
* transações;
* upgrades;
* alteração de nome;
* encerramento de grupo.

---

# 108. Regra de proteção das funcionalidades existentes

Ao implementar, não deverá ser feita uma nova lógica genérica que simplesmente substitua os eventos atuais de quebra ou interação.

Exemplo incorreto:

```text
Está no Claim + possui D
→ permitir quebra
```

Isso quebraria a segurança de vários blocos existentes.

O comportamento correto é:

```text
Jogador tenta quebrar
        ↓
Existe proteção própria do bloco?
        ↓
       SIM
        ↓
Aplicar proteção específica
        ↓
Verificar liderança/proprietário
```

Somente blocos sem regra específica chegam à autorização `D`.

---

# 109. Prioridade correta de destruição

A decisão deverá considerar:

```text
Bloco especial?
↓
Proteção específica

Bloco possui dono?
↓
Proprietário / liderança autorizada

Bloco comum?
↓
D territorial
```

---

# 110. Matriz principal de destruição

| Bloco                                 | Membro Clã com D | Vice-líder | Líder Clã | Membro Propriedade Privada | Proprietário Propriedade Privada |
| ------------------------------------- | ---------------: | ---------: | --------: | -----------: | ----------: |
| Bloco comum                           |              Sim |        Sim |       Sim |          Sim |         Sim |
| Loja de outro membro no próprio Claim |              Não |        Sim |       Sim |          Não |         Sim |
| Correio de outro membro               |              Não |        Sim |       Sim |          Não |         Sim |
| Caixa Eletrônico de outro membro      |              Não |        Sim |       Sim |          Não |         Sim |
| Baú do Clã                            |              Não |        Sim |       Sim |            — |           — |
| Baú da Propriedade Privada                          |                — |          — |         — |          Não |         Sim |
| Bloco de Claim do Clã                 |              Não |        Não |       Sim |            — |           — |
| Bloco de Claim da Propriedade Privada               |                — |          — |         — |          Não |         Sim |
| Bancada do Banco                      |              Não |        Não |       Não |          Não |         Não |

Se o jogador for proprietário de Loja, Correio ou Caixa Eletrônico, ele continua tendo a autorização da própria regra do bloco.

---

# 111. Matriz do Bloco de Claim

## Clã

| Ação           | Membro | Vice-líder | Líder |
| -------------- | -----: | ---------: | ----: |
| Colocar        |    Não |        Sim |   Sim |
| Ativar Claim   |    Não |        Não |   Sim |
| Expandir Claim |    Não |        Não |   Sim |
| Remover chunk  |    Não |        Não |   Sim |
| Quebrar bloco  |    Não |        Não |   Sim |

## Propriedade Privada

| Ação                  | Visitante | Membro convidado | Proprietário |
| --------------------- | --------: | ---------------: | ------------: |
| Colocar bloco pendente|       Sim |              Sim |           Sim |
| Confirmar Claim       |       Não |              Não |           Sim |
| Expandir              |       Não |              Não |           Sim |
| Remover               |       Não |              Não |           Sim |
| Quebrar âncora        |       Não |              Não |           Sim |

---

# 112. Fluxo geral do Claim

```text
Líder/Vice coloca Bloco de Claim do Clã
OU
Qualquer jogador coloca o Bloco da Propriedade Privada
        ↓
Servidor valida local
        ↓
Bloco colocado
        ↓
Líder do Clã ou futuro proprietário abre
        ↓
Clica Claim
        ↓
Chunk do bloco é reclamado
        ↓
Mapa abre
        ↓
Mostra limite
        ↓
Líder seleciona chunks adjacentes
        ↓
Servidor valida tudo
        ↓
Território cresce
```

No Clã, o Vice-líder pode colocar a âncora, porém o Líder é quem ativa e controla o território.

---

# 113. Regra central do sistema

A principal regra para implementação deverá ser:

> O Claim controla o território, mas nunca pode retirar a proteção específica dos blocos já existentes no mod.

E:

> As proteções específicas dos blocos sempre prevalecem sobre U, D e C.

E:

> Todo conjunto de Claim deve permanecer conectado por cima, baixo ou lados a pelo menos um Bloco de Claim válido.

---

# 114. Cuidados obrigatórios durante a implementação

O Codex deverá ser instruído a:

1. Analisar as classes existentes antes de modificar qualquer sistema.
2. Não substituir eventos globais sem entender o comportamento atual.
3. Não remover validações atuais de proprietário.
4. Não alterar comportamento bancário existente sem necessidade.
5. Não alterar receitas existentes.
6. Não alterar IDs existentes de banco de dados.
7. Não quebrar saves antigos.
8. Criar migrations quando necessário.
9. Evitar duplicar serviços existentes.
10. Preservar compatibilidade com blocos já colocados no mundo.
11. Implementar novas permissões de forma extensível.
12. Fazer validações críticas sempre server-side.
13. Reutilizar componentes de interface.
14. Fazer todas as interfaces responsivas.
15. Evitar coordenadas fixas de GUI que quebrem em outras resoluções.
16. Manter suporte UTF-8.
17. Não assumir que todo bloco poderá ser quebrado apenas porque o jogador possui D.

---

# 115. Auditoria final

Após juntar todas as regras, a estrutura está consistente nos seguintes pontos:

### Clã

* 1 por jogador.
* Líder/Vice/Membro.
* Limite configurável.
* Inatividade baseada somente em tempo real de servidor ativo.
* Sucessão automática.
* Claims.
* Permissões individuais.
* Banco.
* Fundo de Apoio.

### Propriedade Privada

* 1 por jogador.
* Líder/Membro.
* Limite configurável.
* Não expira por inatividade.
* Claims.
* U/D/C padrão.
* Conta coletiva.

### Claims

* Dependem de Bloco de Claim.
* Não podem sobrepor.
* Distância configurável.
* Expansão somente ortogonal.
* Limite configurável.
* Upgrade.
* Território sem âncora é removido.

### Bloco de Claim

Clã:

```text
Colocar:
Líder ou Vice-líder

Gerenciar Claim:
Líder

Quebrar:
Somente Líder
```

Propriedade Privada:

```text
Colocar:
Líder

Gerenciar:
Líder

Quebrar:
Líder
```

### Proteção

`D` não quebra proteções existentes.

Líderes possuem override apenas dentro do território de seu próprio grupo.

Blocos sistêmicos continuam com suas próprias regras.

### Visitantes

Podem utilizar Loja de Compra e Loja de Venda quando a opção for liberada.

Não recebem acesso genérico ao território.

### Interface

* Responsiva.
* Reutilizável.
* Sem comandos.
* Navegação por telas.
* Modais.
* UTF-8.
* Componentes compartilhados.

---

# 116. Pontos ainda não definidos

A auditoria completa ainda deixa alguns parâmetros de negócio que poderão ser decididos posteriormente:

1. Receita do Bloco de Claim do Clã.
2. Receita do Bloco de Claim da Propriedade Privada.
3. Destino exato do saldo quando um Clã é desfeito.
4. Destino exato do saldo quando uma Propriedade Privada é desfeita.
5. Se o Fundo de Apoio terá limite de retirada por membro.
6. Distância entre dois territórios separados do mesmo Clã.
7. Limites de tamanho dos nomes de Clã e Propriedade Privada.
8. Prazo de validade dos convites.
9. Quantidade máxima de localizações pessoais.
10. Se localizações serão salvas apenas no servidor ou poderão existir localmente no cliente.

Esses pontos não impedem a implementação da arquitetura principal, mas não deverão ser inventados silenciosamente pelo Codex.

Caso ainda não estejam definidos quando a implementação chegar nesses recursos, devem ficar parametrizados ou isolados para configuração posterior.
