# História — Compra de chunk pelo Bloco de Claim e mapa texturizado

## Objetivo

Permitir que o líder do clã ou o proprietário privado compre a expansão de um terreno a partir do respectivo Bloco de Claim, conhecendo o preço antes de entrar no modo de seleção.

## Fluxo

1. O terreno precisa estar ativo.
2. O Bloco de Claim mostra a quantidade de chunks, o limite atual e o preço estimado do próximo chunk.
3. O responsável pressiona `Comprar chunk`.
4. O menu fecha e o mapa abre centralizado na âncora daquele terreno.
5. O mapa mostra o chunk apontado e seu preço exato.
6. O jogador pode cancelar ou clicar em um chunk.
7. O servidor valida proprietário, dimensão, limite, disponibilidade, distância e adjacência lateral ao mesmo terreno.
8. Quando aprovado, o chunk é adicionado, seu preço entra no valor e na dívida do terreno e um boleto é entregue ao comprador.

## Regras

- Clã: somente o líder compra.
- Propriedade privada: somente o proprietário compra.
- O chunk precisa tocar por um lado um chunk do território da âncora usada para abrir o mapa.
- Diagonal não conta como adjacência.
- O limite de chunks do grupo continua sendo obrigatório.
- Um chunk ocupado ou próximo demais de outro grupo é recusado.
- Adições gratuitas pelo clique comum do mapa não são permitidas; esse fluxo permanece disponível somente para remover chunks próprios que não contenham âncora.
- Cada compra cria um boleto `LAND` independente. Pagar um boleto reduz somente o valor correspondente da dívida total.
- O servidor sempre recalcula o preço usando o centro do chunk selecionado.

## Visualização do mapa

- Chunks carregados no cliente mostram amostras das texturas reais dos blocos encontrados na superfície.
- O detalhamento aumenta com o zoom.
- Chunks fora da distância carregada permanecem como grade, sem forçar carregamento nem transmitir dados adicionais do mundo.
- Claims, jogador e localizações continuam desenhados sobre o terreno.

## Critérios de aceite

1. Os dois tipos de Bloco de Claim exibem `Comprar chunk` e o preço antes do clique.
2. O botão fica indisponível quando o limite foi atingido.
3. O mapa possui cancelamento e seleção visual do chunk.
4. O preço muda no mapa conforme o chunk apontado.
5. Seleção inválida não cria claim, dívida ou boleto.
6. Seleção válida cria o claim e entrega exatamente um boleto.
7. Texturas são mostradas apenas para chunks já carregados.

