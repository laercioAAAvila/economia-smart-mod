# Parte 6 — Bancada do Banco, ouro e preços dinâmicos

Esta parte integra a história funcional completa do Mod de Economia para Minecraft Java 1.21.1 com NeoForge 21.1.234+. As regras gerais e referências entre módulos continuam válidas.

---

## 25. Ajuste implementado da troca de ouro

A Bancada do Banco possui um inventario interno para troca de ouro.

Esse inventario aceita somente ouro monetario configurado:

- Pepita de ouro
- Lingote de ouro
- Bloco de ouro

Ao clicar para trocar, o servidor percorre todos os slots internos da bancada, converte todo ouro valido em dinheiro e credita o valor na conta ativa da sessao bancaria do jogador.

O fluxo nao depende mais do ouro estar na mao do jogador.

Itens restantes no inventario interno devem ser devolvidos ao jogador quando a interface for fechada.

### 25.1 Preco dinamico implementado para ouro da bancada

O valor pago pelo banco ao receber ouro na Bancada do Banco usa o valor-base da pepita como referencia.

A regra aplicada e segura e limitada:

- Mais ouro vendido ao banco nas ultimas 24 horas reduz o valor pago por pepita.
- A primeira venda recente ja ativa o primeiro nivel de ajuste.
- Para ouro, a quantidade por nivel e interpretada como blocos equivalentes, evitando que poucos blocos empurrem o preco direto ao limite minimo.
- Dias completos sem venda de ouro ao banco recuperam/aumentam o valor pago por pepita.
- O valor nunca fica abaixo de 50% do valor-base.
- O valor nunca fica acima de 150% do valor-base.
- Pepita, barra e bloco sempre derivam matematicamente do mesmo valor da pepita.
- O servidor recalcula o preco na confirmacao da troca.
- A interface mostra uma estimativa baseada no preco sincronizado no momento.

O calculo usa os registros existentes de `economy_gold_exchange_entries`, filtrando operacoes `MINT`, sem criar nova tabela.

A interface da Bancada do Banco deve exibir:

- Valor pago por pepita.
- Valor pago por barra.
- Valor pago por bloco.
- Percentual atual aplicado ao valor-base.
- Total estimado que sera pago pelo ouro colocado no inventario de troca.

## 24. Bancada do Banco

### 24.1 Regra de obtenção

A Bancada do Banco não terá craft e não aparecerá em nenhuma receita.

Somente poderá ser obtida por operador ou administrador usando comando.

```text
/economia admin bloco dar bancada-banco
/economia admin bloco dar bancada-banco <jogador>
```

Somente jogadores com permissão administrativa poderão colocar ou remover a Bancada do Banco.

Caso um jogador comum tente colocá-la:

- A colocação será cancelada
- O item permanecerá no inventário

### 24.2 Propriedade

A Bancada do Banco não terá proprietário comum. Ela pertencerá ao sistema bancário do servidor.

Deverão ser registrados:

- Operador que colocou
- Data da colocação
- Dimensão
- Coordenadas

### 24.3 Interface administrativa

A bancada terá 16 ofertas configuráveis.

Cada oferta possuirá:

- Slot de item de referência
- Preço-base pelo qual o banco compra
- Preço-base pelo qual o banco vende
- Quantidade por operação
- Compra ativada ou desativada
- Venda ativada ou desativada
- Comparação completa ou somente por item
- Modo de preço
- Limites mínimo e máximo de preço
- Quantidade necessária para alterar um nível de preço
- Percentual de alteração por nível
- Quantidade de níveis recuperados por dia sem movimentação

Modos de preço:

- `FIXED`: preço não muda automaticamente
- `DYNAMIC`: preço muda conforme oferta e procura
- `MONETARY_GOLD`: câmbio oficial de ouro, sem oscilação automática por padrão

### 24.4 Estoque

A bancada terá inventário interno persistido no SQL.

Quando um jogador vender itens ao banco:

- Os itens entrarão no estoque da bancada

Quando um jogador comprar itens do banco:

- Os itens sairão do estoque

O banco não venderá itens sem estoque, incluindo ouro.

### 24.5 Tesouraria do sistema

A Bancada do Banco utilizará uma conta especial chamada Tesouraria do Sistema.

Essa conta será criada automaticamente e não pertencerá a um jogador.

A tesouraria será utilizada nas negociações comuns:

- Compra de itens que não sejam ouro monetário
- Recebimento de vendas feitas pelo banco
- Pagamentos e ajustes administrativos

Quando o jogador comprar um item comum do banco:

- O pagamento será creditado na tesouraria

Quando o jogador vender um item comum para o banco:

- O pagamento será retirado da tesouraria

Caso a tesouraria não tenha saldo suficiente, o banco não comprará itens comuns.

### 24.6 Ouro como origem oficial do dinheiro

O ouro poderá ser trocado por dinheiro na Bancada do Banco.

Itens monetários iniciais:

- `minecraft:gold_nugget`
- `minecraft:gold_ingot`
- `minecraft:gold_block`

O valor-base será definido a partir da pepita de ouro.

Exemplo configurável:

```text
1 pepita de ouro = R$ 1
1 lingote de ouro = 9 pepitas = R$ 9
1 bloco de ouro = 81 pepitas = R$ 81
```

O valor do lingote e do bloco deverá sempre ser derivado matematicamente da pepita. O administrador não poderá definir valores incompatíveis que permitam lucro apenas criando ou desfazendo blocos.

### 24.7 Emissão monetária lastreada em ouro

Quando um jogador vender ouro monetário ao banco:

1. O ouro será validado.
2. A quantidade será convertida para unidades de pepita.
3. O ouro entrará na Reserva de Ouro do sistema.
4. O sistema emitirá o valor correspondente em moeda.
5. O jogador receberá notas ou crédito em conta, conforme escolher.
6. A emissão será registrada no livro contábil e na auditoria.

Essa operação não depende do saldo prévio da tesouraria, porque representa a criação oficial de moeda lastreada no ouro recebido.

Tipos de transação:

- `GOLD_MINT`: ouro recebido e moeda emitida
- `GOLD_REDEMPTION`: moeda recolhida e ouro entregue

Quando um jogador comprar ouro da bancada:

- O dinheiro pago será retirado de circulação
- A quantidade correspondente sairá da Reserva de Ouro
- A bancada somente entregará ouro realmente existente no estoque

### 24.8 Reserva de Ouro

A Reserva de Ouro deverá registrar:

- Quantidade total em unidades de pepita
- Quantidade física por item
- Entradas de ouro
- Saídas de ouro
- Dinheiro emitido
- Dinheiro recolhido
- Operador responsável por ajustes

A reserva nunca poderá ficar negativa.

Ajustes manuais somente poderão ser feitos por administrador e deverão gerar auditoria.

### 24.9 Limites de troca de ouro

Para controlar fazendas muito grandes e proteger a economia, o servidor poderá configurar:

- Limite diário de troca por jogador
- Limite diário global
- Tempo mínimo entre operações
- Itens ou tags de ouro aceitos
- Permissão para receber em notas
- Permissão para receber diretamente na conta

Os limites poderão ser desativados pelo administrador.

### 24.10 Ouro e preço dinâmico

Por padrão, ofertas `MONETARY_GOLD` terão valor fixo e não serão afetadas pelo preço dinâmico.

Isso mantém o ouro como referência monetária estável e impede manipulação do câmbio oficial.

O administrador poderá habilitar oscilação para ouro, mas deverá receber um aviso de que isso pode permitir especulação e alterar toda a oferta de moeda do servidor.

### 24.11 Compra feita pelo jogador

O jogador poderá pagar utilizando:

- Dinheiro físico
- Cartão de débito
- Cartão de crédito
- Cartão de débito e crédito

No pagamento físico:

- As notas serão consumidas
- O valor será creditado na tesouraria, ou retirado de circulação quando for resgate de ouro

No débito:

- A conta do jogador será debitada
- A tesouraria será creditada, ou o valor será recolhido quando for resgate de ouro

No crédito:

- A dívida do jogador aumentará
- A tesouraria será creditada
- Compra de ouro no crédito poderá ser desativada por configuração para evitar criação de dívida destinada à aquisição da própria reserva monetária

### 24.12 Venda feita pelo jogador

O jogador poderá:

- Colocar itens na interface
- Utilizar itens da mão
- Escolher a quantidade
- Vender uma unidade
- Vender o stack disponível

Para item comum:

1. O item será validado.
2. O saldo da tesouraria será verificado.
3. O espaço para pagamento será verificado.
4. O item será removido.
5. O item será adicionado ao estoque.
6. A tesouraria será debitada.
7. O pagamento será entregue.
8. A operação será registrada.

Para ouro monetário, será utilizado o fluxo de emissão lastreada descrito anteriormente.

### 24.13 Preço dinâmico por oferta e procura

O preço dinâmico será aplicado às ofertas da Bancada do Banco marcadas como `DYNAMIC`.

Ele não alterará automaticamente os preços das lojas dos jogadores.

O sistema trabalhará com dois movimentos independentes:

- **Demanda:** jogadores comprando itens do banco
- **Oferta:** jogadores vendendo itens ao banco

#### Aumento por demanda

Quando jogadores comprarem repetidamente grandes quantidades do mesmo item no mesmo dia, o preço de venda do banco aumentará por níveis.

Exemplo:

```text
Preço-base de venda: R$ 100
Quantidade por nível: 64 itens
Aumento por nível: 5%

0 a 63 comprados no dia: R$ 100
64 a 127 comprados no dia: R$ 105
128 a 191 comprados no dia: R$ 110
```

#### Redução por excesso de oferta

Quando jogadores venderem grandes quantidades do mesmo item ao banco, o preço pago pelo banco poderá diminuir por níveis.

Exemplo:

```text
Preço-base de compra: R$ 80
Quantidade por nível: 64 itens
Redução por nível: 5%

0 a 63 vendidos ao banco: R$ 80
64 a 127 vendidos ao banco: R$ 76
128 a 191 vendidos ao banco: R$ 72
```

### 24.14 Fórmulas de preço

Preço pelo qual o banco vende:

```text
preço atual de venda =
arredondar para cima(
  preço-base de venda ×
  (10.000 + nível de demanda × aumento em pontos-base)
  ÷ 10.000
)
```

Preço pelo qual o banco compra:

```text
preço atual de compra =
arredondar para baixo(
  preço-base de compra ×
  (10.000 - nível de oferta × redução em pontos-base)
  ÷ 10.000
)
```

O preço nunca poderá:

- Ficar abaixo do mínimo configurado
- Ficar acima do máximo configurado
- Tornar o preço de compra do banco maior ou igual ao preço de venda do banco

Essa diferença impede que um jogador compre e revenda imediatamente com lucro garantido.

### 24.15 Compra grande atravessando níveis

Uma compra grande deverá usar preço progressivo por faixa.

Exemplo:

- Faltam 10 unidades para atingir o próximo nível
- O jogador compra 30 unidades
- As primeiras 10 usam o preço atual
- As 20 restantes usam o preço do nível seguinte

A interface deverá mostrar:

- Quantidade solicitada
- Total estimado
- Preço médio por unidade
- Faixas de preço utilizadas

No momento da confirmação, o servidor bloqueará a oferta no SQL e recalculará o total. Caso outro jogador tenha alterado o preço, a interface deverá solicitar nova confirmação em vez de cobrar um valor diferente silenciosamente.

### 24.16 Recuperação diária do preço

Cada oferta terá níveis persistentes de demanda e oferta.

No fechamento diário:

- Se ninguém comprou aquele item do banco no dia, o nível de demanda diminuirá
- Se ninguém vendeu aquele item ao banco no dia, o nível de oferta diminuirá
- A quantidade de níveis recuperados por dia será configurável
- Depois de dias suficientes sem movimentação, o preço voltará ao preço-base

Exemplo:

```text
Nível de demanda atual: 4
Recuperação: 1 nível por dia sem compra

1º dia sem compra: nível 3
2º dia sem compra: nível 2
3º dia sem compra: nível 1
4º dia sem compra: nível 0 e preço normal
```

Por padrão, qualquer compra realizada naquele dia impede a recuperação da demanda naquela virada. Esse comportamento poderá ser configurável.

### 24.17 Proteção contra manipulação de preços

O sistema deverá possuir opções para:

- Quantidade mínima antes de alterar preço
- Limite máximo de níveis
- Preço mínimo e máximo
- Limite diário por jogador
- Intervalo mínimo entre operações do mesmo jogador
- Bloqueio de compra e revenda imediata do mesmo item
- Registro de jogadores que mais influenciaram o preço
- Exclusão das ofertas de ouro monetário

### 24.18 Processamento diário dos preços

A recuperação diária deverá usar o mesmo fuso horário econômico dos juros.

Cada oferta e data terão um registro único. Reiniciar o servidor não poderá recuperar o mesmo nível duas vezes.

Se o servidor ficar desligado por vários dias, os dias sem atividade deverão ser processados até o preço se aproximar ou retornar ao preço-base.

### 24.19 Remoção da bancada

Uma Bancada do Banco com estoque não poderá ser quebrada normalmente.

O operador deverá:

- Retirar primeiro os itens
- Ou usar um comando administrativo de remoção forçada

```text
/economia admin bloco remover-forcado
```

Na remoção forçada:

- Os itens serão derrubados no chão
- A configuração será desativada no SQL
- A operação será registrada na auditoria
- A Reserva de Ouro continuará registrada e não será apagada com a remoção de uma bancada

---
