# História — Compra de chunk pelo Bloco de Claim e mapa de superfície

## Objetivo

Permitir que o líder do clã ou o proprietário privado compre a expansão de um terreno
pelo respectivo Bloco de Claim, usando o cartão autenticado no início da interface e
conhecendo o preço antes da confirmação.

Esta é a regra vigente para expansão de território. A compra de chunk não gera boleto
nem dívida de terreno. Os boletos disponíveis no Bloco de Claim são destinados às
taxas do território e à venda.

## Autenticação

1. Toda abertura do Bloco de Claim começa na tela de autenticação.
2. O jogador insere um cartão válido pertencente a uma de suas contas ativas.
3. O cartão fica protegido pelo menu durante a sessão.
4. Fechar com `Sair`, `ESC` ou invalidar o menu encerra a autenticação e devolve o cartão.
5. Uma nova abertura exige nova autenticação.

## Fluxo de compra

1. O terreno precisa estar ativo e abaixo do limite de chunks.
2. A tela mostra a quantidade atual, o limite e o preço estimado do próximo chunk.
3. O responsável pressiona `Comprar chunk`.
4. O menu fecha e o mapa abre centralizado na âncora daquele terreno.
5. O mapa mostra coordenadas e preço exato do chunk apontado.
6. Ao clicar em um chunk, abre uma confirmação com o valor, `Confirmar`, `Cancelar` e o
   aviso de que a compra não pode ser desfeita pela interface.
7. Ao confirmar, o servidor revalida autorização, cartão, dimensão, limite,
   disponibilidade, distância e adjacência lateral.
8. O preço é debitado do cartão autenticado e creditado ao tesouro na mesma transação SQL
   que grava o claim.
9. Se a revalidação ou a gravação do território falhar, toda a cobrança é revertida.
10. Após aprovação, o chunk é adicionado ao território e o valor patrimonial do terreno
    é atualizado, sem criar boleto ou nova dívida.

O cartão e a intenção de compra ficam vinculados ao jogador e à âncora por uma sessão
temporária de até cinco minutos. A seleção é consumida na tentativa confirmada.

## Regras territoriais

- Clã: somente o líder pode comprar.
- Propriedade privada: somente o proprietário pode comprar.
- O chunk precisa tocar lateralmente um chunk do território associado à âncora usada.
- Contato apenas diagonal não é válido.
- O limite total de chunks do grupo é obrigatório.
- Chunk ocupado ou próximo demais de outro grupo é recusado.
- O servidor sempre recalcula o preço pelo centro do chunk selecionado.
- A expansão comum pelo mapa não é gratuita; a remoção de chunk próprio continua
  separada e não pode remover o chunk que contém a âncora.

## Mapa de superfície e desempenho

- Somente chunks já carregados no cliente são amostrados; o mapa não força carregamento.
- A cor usa o bloco superior do `WORLD_SURFACE`, incluindo cor de mapa e tonalidade do
  bioma quando disponível, para representar vegetação, água, areia, pedra e construções.
- A amostragem varia com o zoom e fica limitada à região próxima ao jogador.
- As cores calculadas são mantidas em cache limitado para evitar recomputação por frame.
- Novas amostras são calculadas com orçamento por frame; áreas ainda não amostradas aparecem
  progressivamente sem travar a renderização.
- Chunks não carregados permanecem na grade.
- Claims, jogador, localizações, seleção e confirmação são desenhados sobre o terreno.
- O mapa não aplica uma camada de desfoque sobre o painel nem sobre o terreno.
- A tela começa com dados vazios e só exibe claims após receber a resposta do mundo
  atual, impedindo marcações mantidas em memória ao trocar de servidor ou save.

## Isolamento por mundo e servidor

- O primeiro carregamento cria um UUID aleatório dentro dos dados salvos do mundo.
- O UUID é lido uma vez na inicialização; não há verificação contínua por tick ou frame.
- Um mundo recriado recebe outro UUID e não enxerga nem aplica proteção de claims do
  mundo apagado.
- Consultas de grupos, claims, âncoras, territórios, localizações e blocos comerciais usam
  `server_uuid` com índices próprios, permitindo vários servidores compartilharem o PostgreSQL.
- O relógio de tempo ativo usado nas taxas também é independente por mundo.
- Lojas, caixas, correios e bancadas de mundos diferentes podem ocupar as mesmas
  coordenadas sem colisão no banco.
- Registros antigos sem `server_uuid` ficam isolados e não bloqueiam nenhum mundo novo.

## Taxas e boletos do claim

- `Taxas a pagar` mostra a taxa do intervalo atual, o total pendente, a quantidade de
  cobranças e a duração configurada do intervalo em dias do Minecraft.
- `Imprimir taxa atual` cria ou reemite o boleto referente ao intervalo atual.
- `Imprimir todas` gera um boleto consolidado para as taxas pendentes do território.
- A taxa base usa `claimAnchor.basePrice` mais o percentual configurado sobre o valor
  acumulado do terreno.
- Para clãs, o resultado é multiplicado por `claimAnchor.clanTaxMultiplierPercentage`,
  cujo padrão é `200%` do valor da propriedade privada.
- O intervalo padrão é `25` dias do Minecraft e o total de dias acumulados respeita o
  máximo configurado.
- Boletos `LAND` antigos continuam compatíveis, mas novas compras de chunk não criam
  esse tipo de boleto.

## Critérios de aceite

1. Os dois tipos de Bloco de Claim exigem autenticação em cada abertura.
2. Ambos exibem `Comprar chunk` e o preço estimado antes de abrir o mapa.
3. O clique no mapa abre confirmação com preço e aviso de irreversibilidade.
4. Cancelar não cobra nem altera o território.
5. Confirmar usa débito no cartão autenticado e não gera boleto de compra.
6. Seleção inválida não cria claim, dívida ou boleto.
7. O terreno representa cores da superfície dos chunks carregados sem desfoque global.
