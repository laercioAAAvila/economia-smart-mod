# Atualização da história — Claims, propriedades privadas, âncoras e vendas

## Propriedade privada

A Propriedade Privada é um lote particular do jogador e não possui líder. Qualquer
jogador pode colocar o bloco de claim em uma posição válida. O jogador que confirma
`Dar Claim` torna-se proprietário do território. Outros jogadores podem ser convidados
como membros daquela propriedade.

Colocar o bloco cria somente uma âncora pendente. No primeiro acesso, a interface mostra
tipo, posição, preço calculado, quantidade de territórios do proprietário e limite
configurado. `Dar Claim` só fica disponível quando o limite e as regras territoriais
forem atendidos. `ESC` e `Sair` fecham a interface sem criar o território.

## Claim do clã

O bloco do clã também nasce pendente. Ao abrir, exibe posição, preço e territórios
atuais/limite. Somente a liderança autorizada confirma `Dar Claim`; a confirmação segue
as regras de claim, cria a dívida do terreno e gera o boleto correspondente.

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

Claims, âncoras e vendas geram boletos persistidos e vinculados ao território. O Caixa
Eletrônico aceita esses boletos e conclui seus efeitos somente depois de um pagamento
confirmado e idempotente.

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
interface sem executar ações pendentes.
