# História — Upgrade de limite de chunks, pagamento do claim e ajustes visuais

## Objetivo

Permitir que Clãs e Propriedades Privadas aumentem progressivamente o limite total de
chunks, concluir `Dar Claim` somente após pagamento aprovado e corrigir a apresentação
dos inventários, cartões e texturas dos novos conteúdos.

Esta história complementa `HIST-DEV-26`, `HIST-DEV-27` e `HIST-DEV-29`. Em caso de
divergência sobre limite, preço de upgrade ou pagamento inicial do terreno, esta é a
regra vigente.

## Configuração do limite e do preço

Todos os parâmetros pertencem à configuração do mod no servidor:

```text
claimMinChunks = 4
claimMaxChunks = 20
claimUpgradeBasePrice = 10000
claimUpgradeMinPercentage = 10
claimUpgradeMaxPercentage = 30
```

- `claimMinChunks`: limite inicial de chunks.
- `claimMaxChunks`: maior limite alcançável por upgrade.
- `claimUpgradeBasePrice`: base usada no primeiro cálculo.
- `claimUpgradeMinPercentage`: percentual do primeiro upgrade.
- `claimUpgradeMaxPercentage`: teto da progressão percentual.

Os valores são compartilhados pelo fluxo de upgrade de Clã e Propriedade Privada.
Nenhum valor pode ficar fixo no código. O servidor deve validar que o máximo não é
menor que o mínimo e que o percentual máximo não é menor que o mínimo.

## Progressão

```text
upgradeCount = claimMaxChunks - claimMinChunks
percentageRange = claimUpgradeMaxPercentage - claimUpgradeMinPercentage
percentageStep = percentageRange / (upgradeCount - 1)
percentage = claimUpgradeMinPercentage + upgradeIndex × percentageStep
newPrice = previousPrice × (1 + percentage / 100)
```

O primeiro índice é `0`. O primeiro preço usa `claimUpgradeBasePrice` como preço
anterior. Cada preço seguinte usa o resultado do nível anterior. Com mais de um nível,
o primeiro upgrade usa o percentual mínimo e o último usa o máximo.

O mod trabalha com valores monetários inteiros. A implementação deve calcular a
progressão com precisão determinística e arredondar cada preço para a unidade monetária
mais próxima, sem usar `float` ou `double`. Se houver somente um upgrade possível, ele
usa o percentual mínimo, evitando divisão por zero.

Exemplo padrão:

```text
4 → 5   = 10,00%
5 → 6   = 11,33%
6 → 7   = 12,67%
...
19 → 20 = 30,00%
```

Alterar qualquer parâmetro recalcula automaticamente toda a sequência. O preço não é
armazenado como uma tabela fixa.

## Tela de upgrade

A tela deve exibir:

```text
UPGRADE DE CLAIM

Limite atual: 4 chunks
Próximo limite: 5 chunks
Percentual: 10,00%
Preço: 11.000

[Comprar Upgrade]
[Voltar]
```

Ao atingir `claimMaxChunks`, deve mostrar `Limite atual: 20 / 20 chunks`, informar que
o limite máximo foi atingido e desabilitar a compra.

`Comprar Upgrade` abre o componente reutilizável de pagamento. O servidor recalcula e
revalida limite, percentual e preço no momento da confirmação. Somente depois de um
pagamento idempotente aprovado o limite aumenta em um chunk.

## Pagamento de `Dar Claim`

`Dar Claim` não ativa o território imediatamente. O fluxo vigente é:

```text
Abrir Bloco de Claim
→ visualizar preço
→ Dar Claim
→ escolher Cartão ou Dinheiro
→ pagamento aprovado
→ ativar território e seguir o fluxo normal do claim
```

A cobrança pode possuir registro financeiro interno persistente, mas o jogador não
deve precisar levar o boleto inicial ao Caixa Eletrônico para concluir o primeiro
claim. Falha, cancelamento ou fechamento da tela mantém a âncora pendente e não cria o
território ativo.

### Cartão

O modal contém um slot de cartão e exige escolha explícita entre `Crédito` e `Débito`
antes de habilitar `Pagar`. Devem ser reutilizadas as validações atuais de cartão,
conta, limite, crédito, débito, idempotência e auditoria.

### Dinheiro

Somente o dinheiro colocado nos slots do próprio menu participa do pagamento. O mod
não retira valores do inventário normal do jogador e não dá troco. Cancelar ou fechar
devolve os itens inseridos segundo as garantias atuais de inventário.

## Inventários e autenticação

- Todo slot utilizável deve possuir contorno, fundo e estado visual compatíveis com os
  slots do Minecraft; uma área cinza sem delimitação não é suficiente.
- Slots de cartão, dinheiro e itens precisam permanecer identificáveis em todas as
  escalas de GUI suportadas.
- No bloco de gerenciamento, o cartão aparece apenas na etapa de autenticação.
- Após autenticar, o slot e o item do cartão ficam ocultos e inativos na interface.
- O cartão permanece protegido no menu do servidor e é devolvido ao sair, usar `ESC`
  ou perder a validade do menu.
- As telas autenticadas exibem somente cabeçalho, dados do grupo, ações permitidas e
  inventário necessário ao fluxo atual.

## Texturas

Blocos e itens novos de Clã, Propriedade Privada e Claims devem possuir texturas finais
próprias, coerentes com Minecraft e com o mod. Não são aceitos textura ausente, textura
de depuração ou placeholder. Recursos existentes não podem ser substituídos nem
quebrados.

## Reutilização e responsividade

Os fluxos de claim e upgrade devem reutilizar componentes de pagamento, seleção de
crédito/débito, botões, slots e navegação. `Pagar`, `Cancelar`, `Voltar`, `Crédito`,
`Débito`, `Comprar Upgrade` e `Dar Claim` mantêm o mesmo comportamento visual.

Todas as telas devem permanecer utilizáveis nas resoluções e escalas previstas, sem
texto cortado sobre botões, controles fora da tela, sobreposição de inventário ou slots
sem indicação visual.

## Segurança e critérios de aceite

- O servidor é a autoridade do preço, limite atual, pagamento e concessão do upgrade.
- Repetir a mesma solicitação não cobra nem concede o upgrade duas vezes.
- Mudança concorrente de limite ou configuração invalida preço antigo e força
  atualização da tela.
- Pagamento recusado nunca ativa claim nem aumenta limite.
- Pagamento aprovado e falha posterior devem ser recuperáveis sem perda de dinheiro.
- O comportamento existente de dinheiro físico, cartão de crédito e débito é
  preservado.
- Ao autenticar nas telas mostradas como Clã ou Propriedade Privada, o cartão deixa de
  aparecer na etapa autenticada.
- Os slots exibidos nas interfaces possuem desenho individual claro.

