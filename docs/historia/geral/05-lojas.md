# Parte 5 — Loja de Venda e Loja de Compra

Esta parte integra a história funcional completa do Mod de Economia para Minecraft Java 1.21.1 com NeoForge 21.1.234+. As regras gerais e referências entre módulos continuam válidas.

---

## 24. Ajuste implementado das lojas de jogador

### 24.1 Padrao de abertura

Ao colocar uma Loja de Venda ou Loja de Compra, o estado inicial sera sempre desativado.

A interface nao deve abrir duas vezes no mesmo clique. O bloco processa apenas a interacao da mao principal.

### 24.2 Dono

O jogador que colocou o bloco e o dono da loja.

Para o dono, a interface mostra:

- Slot de referencia do item negociado
- Campo de preco
- Campo de limite de compra, quando for Loja de Compra
- Botao unico de estado, alternando entre ativado e desativado
- Inventario de caixa para notas recebidas ou usadas como reserva
- Inventario de estoque da loja

O estoque so aceita itens iguais ao item de referencia, comparando item e componentes para preservar NBT, encantamentos, acessorios e dados de outros mods.

### 24.3 Cliente

Para jogadores que nao sao donos, a interface mostra somente:

- Item de referencia
- Slot de pagamento ou entrega
- Slot de cartao
- Botao de troca
- Inventario do jogador

Na Loja de Venda, o cliente pode pagar com dinheiro fisico ou cartao valido. No pagamento por cartao, o valor vai para a conta ativa do dono.

Quando o cliente usar cartao de debito e credito na Loja de Venda, a interface deve mostrar um botao de selecao acima de Comprar alternando entre Debito e Credito.

Na Loja de Compra, o cliente entrega apenas itens iguais ao item de referencia usando o inventario temporario de entrega. O pagamento pode vir da reserva fisica da loja ou da conta do dono para o cartao inserido pelo cliente.

Se o botao Comprar ou Vender nao puder concluir a operacao, o clique deve mostrar uma mensagem simples e compreensivel, como loja desativada, item errado, falta de estoque, limite atingido, saldo insuficiente, credito insuficiente, limite diario atingido ou armazem cheio.

### 24.4 Regras de seguranca

Antes de concluir uma troca, o servidor deve validar novamente:

- Loja ativa
- Preco maior que zero
- Quantidade maior que zero
- Item de referencia existente
- Estoque suficiente para venda
- Espaco de estoque suficiente para compra
- Dinheiro fisico suficiente quando a loja precisa pagar
- Saldo bancario suficiente quando a loja paga por conta vinculada
- Pagamento fisico ou cartao valido quando o cliente precisa pagar
- Funcao debito ou credito conforme a selecao do cliente

Se qualquer validacao falhar, a troca e cancelada sem alterar os itens do jogador.

## 21. Loja de Venda

### 21.1 Propriedade

Qualquer jogador poderá criar e colocar uma Loja de Venda.

O jogador que colocar será o proprietário.

Somente poderão configurar ou remover:

- Proprietário
- Operador
- Administrador

### 21.2 Inventários

A Loja de Venda terá:

#### Estoque

- 9 slots
- Stacks normais do Minecraft
- Acesso administrativo somente pelo proprietário

#### Caixa físico

- 9 slots
- Aceita somente notas
- Guarda dinheiro recebido
- Acesso somente pelo proprietário

#### Pagamento do cliente

- Slots temporários para colocar notas
- Slot temporário para cartão

### 21.3 Configuração dos produtos

Cada oferta persistente poderá possuir:

- Item vendido
- Quantidade entregue
- Preço
- Estado ativo

Exemplo:

```text
Item: pão
Quantidade por compra: 16
Preço: R$ 20
```

### 21.4 Pagamento em dinheiro

A loja deverá:

1. Somar as notas inseridas.
2. Verificar o estoque.
3. Confirmar que o valor cobre o preco.
4. Transferir o pagamento para o caixa da loja.
5. Entregar o produto.
6. Registrar a venda.

### 21.5 Vinculação bancária

O proprietário poderá vincular a loja à própria conta usando:

- Cartão de crédito ativo
- Cartão de débito e crédito ativo

O cartão será utilizado somente para autorizar a vinculação.

Após a vinculação, a loja armazenará o identificador da conta, não o identificador do cartão.

A conta do cartão deverá pertencer ao proprietário da loja.

### 21.6 Recebimento por cartão

Compras feitas em débito ou crédito serão enviadas diretamente para a conta vinculada.

Se a loja não possuir conta vinculada:

- Pagamento em dinheiro continuará funcionando
- Pagamento por cartão ficará desativado

---

## 22. Loja de Compra

### 22.1 Propriedade

Qualquer jogador poderá criar e colocar uma Loja de Compra.

O jogador que colocar será o proprietário.

Somente proprietário, operador ou administrador poderá configurá-la.

### 22.2 Configuração

A loja terá:

- 1 slot de item de referência
- Campo de preço por unidade
- Campo de quantidade máxima desejada
- Quantidade já comprada
- Quantidade restante
- Botão para ativar
- Botão para desativar
- Inventario temporario do cliente para entregar itens
- Slot de cartao para pagamento em conta quando aplicavel

### 22.3 Inventários

#### Itens comprados

- 9 slots
- Guarda itens recebidos dos jogadores

#### Reserva de dinheiro

- 9 slots
- Aceita somente notas
- Utilizada para pagar vendedores

### 22.4 Formas de financiamento

#### Dinheiro físico

O dinheiro será retirado da reserva da loja.

A loja parará de comprar quando:

- Não houver dinheiro suficiente
- O inventário estiver cheio
- A quantidade desejada tiver sido atingida

#### Saldo bancário

O proprietário poderá vincular sua conta.

A cada compra:

1. O valor será debitado do saldo disponível.
2. O valor será enviado para o cartao/conta do vendedor.
3. Os itens serão guardados na loja.
4. A transação será registrada.

Essa operação funcionará como transferencia da conta do proprietário para a conta do vendedor.

#### Crédito

Para usar crédito, o proprietário deverá selecionar um cartão ativo com função crédito.

O cartão será utilizado para autorizar e identificar a dívida.

A cada compra:

- O valor aumentará a dívida do cartão
- O pagamento será entregue ao vendedor conforme o modo da loja
- O limite disponível será reduzido
- A operação aparecerá na fatura

Quando o limite acabar, a loja parará de comprar.

### 22.5 Venda para a loja

O jogador deverá:

1. Colocar os itens na interface.
2. Visualizar a quantidade aceita.
3. Visualizar o pagamento.
4. Confirmar a operação.
5. Receber as notas.

A transação será recusada caso o inventário do jogador não tenha espaço para receber o pagamento.

### 22.6 Comparação dos itens

Por padrão, a loja deverá comparar:

- Identificador do item
- Componentes
- Encantamentos
- Durabilidade
- Nome personalizado
- Outros dados relevantes

O proprietário poderá ativar uma opção para ignorar componentes e comparar somente o identificador do item.

---

## 23. Craft das lojas

### 23.1 Loja de Venda

```text
M F M
M R M
M M M
```

### 23.2 Loja de Compra

```text
M M M
M R M
M F M
```

Legenda:

- `M`: qualquer tábua de madeira
- `F`: lingote de ferro
- `R`: bloco de redstone

---
