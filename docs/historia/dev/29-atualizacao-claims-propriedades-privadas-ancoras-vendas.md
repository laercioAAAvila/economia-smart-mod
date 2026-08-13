# Atualização da história — Claims, propriedades privadas, âncoras e vendas

> O pagamento inicial de `Dar Claim` foi atualizado por `HIST-DEV-31`. A expansão e
> as cobranças recorrentes seguem `HIST-DEV-35`: chunk novo é pago diretamente com o
> cartão autenticado; boletos do claim representam taxas, consolidação de taxas e venda.

## Propriedade privada

A Propriedade Privada é um lote particular do jogador e não possui líder. Qualquer
jogador pode colocar o bloco de claim em uma posição válida. O jogador que confirma
`Dar Claim` torna-se proprietário do território. Outros jogadores podem ser convidados
como membros daquela propriedade.

Colocar o bloco cria somente uma âncora pendente. No primeiro acesso, a interface mostra
tipo, posição, preço calculado, quantidade de territórios do proprietário e limite
configurado. `Dar Claim` só fica disponível quando o limite e as regras territoriais
forem atendidos. Ao confirmar, abre o pagamento por cartão ou dinheiro. `ESC`, `Sair`
ou pagamento recusado fecham o fluxo sem criar o território.

## Claim do clã

O bloco do clã também nasce pendente. Ao abrir, exibe posição, preço e territórios
atuais/limite. Somente a liderança autorizada confirma `Dar Claim`; a confirmação segue
as regras de claim e abre o pagamento. O território só é ativado após confirmação
financeira idempotente.

## Preço do terreno

O preço usa a distância euclidiana até `(0, 0)` da dimensão:

```text
faixas = floor(distância / intervalo)
preço = base + faixas × aumentoLinear + faixas² × aumentoProgressivo
```

Padrões:

| Dimensão | Base | Intervalo | Linear | Progressivo |
|---|---:|---:|---:|---:|
| Overworld | 5.000 | 1.000 | 500 | 20 |
| Nether | 10.000 | 500 | 800 | 30 |
| End | 15.000 | 1.000 | 1.000 | 35 |
| Outras | 10.000 | 1.000 | 750 | 25 |

Todos os parâmetros ficam no arquivo de configuração do mod.

## Âncora e carregamento de chunk

Depois do claim, o menu oferece `Âncora`. O preço é:

```text
preçoÂncora = anchorBasePrice + preçoTerreno × anchorLandPercentage / 100
```

Padrões: base `1.000`, percentual `50`, fatura inicial `25` dias do Minecraft e
máximo acumulado `1.000` dias. O jogador informa os dias, gera o boleto e o chunk só
é forçado após confirmação do pagamento. Novas faturas adicionam dias sem ultrapassar
o máximo. A venda não cancela dias já pagos.

## Boletos

Taxas de âncora e vendas geram boletos persistidos e vinculados ao território. A página
`Taxas a pagar` permite imprimir a taxa atual ou reunir as cobranças pendentes em um
único boleto. O Caixa Eletrônico aceita esses boletos e conclui seus efeitos somente
depois de pagamento confirmado e idempotente.

A cobrança inicial do claim é concluída no próprio Bloco de Claim conforme
`HIST-DEV-31`. A compra de expansão é debitada diretamente do cartão autenticado,
conforme `HIST-DEV-35`, e não emite boleto. Boletos `LAND` antigos permanecem
compatíveis apenas para preservar dados já existentes.

## Permissões da propriedade privada

O proprietário pode convidar jogadores por território e configurar individualmente:

- `USAR`: abrir e utilizar itens e blocos no território;
- `COLOCAR`: colocar blocos e itens no chão;
- `REMOVER`: quebrar ou remover blocos comuns.

Lojas de compra e venda são exceção pública à permissão de uso. Qualquer visitante
pode abrir a loja dentro ou fora do território, sem perder as proteções de propriedade
do bloco comercial nem o direito do dono de removê-lo.

O clã aplica as mesmas ações territoriais por máscara de permissão dos cargos e membros.

## Remoção administrativa

Um administrador com nível de permissão `2` pode quebrar o Bloco de Claim de clã ou de
propriedade privada. A ação desativa a âncora e remove o território e todos os chunks
vinculados, mesmo quando o administrador não é o controlador do grupo.

## Venda

O proprietário privado ou líder do clã informa comprador e preço. O preço sugerido é
o valor do terreno somado ao valor atual da âncora, quando houver. O comprador recebe
o boleto; o pagamento transfere o território e credita o vendedor.

Dívidas do terreno permanecem no território e passam ao comprador. Dias já pagos da
âncora continuam válidos.

## Crédito

Uma nova solicitação de crédito exige simultaneamente dívida de cartão igual a zero e
dívida de terreno igual a zero. Qualquer uma delas bloqueia a solicitação.

## Navegação

As telas de informações, âncora, boleto e venda oferecem `Voltar`; `ESC` encerra a
interface sem executar ações pendentes. Ao fechar o Bloco de Claim, a autenticação é
encerrada, o cartão é devolvido e a próxima abertura exige autenticação novamente.
