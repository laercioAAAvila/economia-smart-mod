# Itens, blocos e inventarios

## Itens de dinheiro

Registrar oito itens separados:

- `banknote_1`
- `banknote_2`
- `banknote_5`
- `banknote_10`
- `banknote_20`
- `banknote_50`
- `banknote_100`
- `banknote_200`

Regras:

- Stack maximo 64.
- Nao colocavel como bloco.
- Valor fixo definido em enum.
- Validado pelo servidor.
- Aceito somente em slots financeiros permitidos.

Denominacoes para saque e pagamentos:

```text
200, 100, 50, 20, 10, 5, 2, 1
```

## Cartoes

Registrar tres itens:

- `debit_card`
- `credit_card`
- `debit_credit_card`

Regras:

- Stack maximo 1.
- Item guarda UUID do cartao e versao de seguranca.
- SQL valida existencia, estado, tipo e limite.
- Nome personalizado e apenas apresentacao.
- Copias com mesmo UUID representam o mesmo cartao.

Dados no item:

```text
card_id
security_version
display_name
```

Nao guardar saldo, limite ou senha no item.

## Blocos

Registrar quatro blocos:

- `atm`
- `sell_shop`
- `buy_shop`
- `bank_counter`

Todos usam orientacao horizontal:

```text
horizontal_facing = north | south | east | west
```

Nenhum bloco pode aceitar orientacao vertical.

## Block entities

Cada bloco comercial deve ter block entity com:

- `commercial_block_id`
- cache visual opcional
- estado temporario de carregamento

Dados financeiros e inventarios persistentes devem vir do SQL.

## Caixa Eletronico

Persistencia:

- Registro em `economy_commercial_blocks`
- Sem estoque interno
- Sem saldo interno

Permissao de quebra:

- Quem colocou
- Operador
- Administrador

Interface:

- Login por usuario e senha
- Login por cartao
- Deposito
- Saque
- Cartoes
- Saldo
- Limite
- Fatura
- Logout

## Loja de Venda

Inventarios persistidos:

- `PRODUCT_STOCK`: 9 slots
- `CASH_RESERVE`: 9 slots

Slots temporarios da tela:

- Pagamento em notas
- Cartao do cliente
- Seletor Debito/Credito quando o cartao possuir as duas funcoes

Configuracao por slot:

- Item
- Quantidade entregue
- Preco
- Ativo
- Comparacao

## Loja de Compra

Inventarios persistidos:

- `PURCHASED_ITEMS`: 9 slots
- `CASH_RESERVE`: 9 slots

Configuracao:

- Item de referencia
- Preco por unidade
- Quantidade maxima desejada
- Quantidade ja comprada
- Inventario temporario do cliente para entregar itens
- Slot de cartao para pagamento em conta
- Estado ativo

## Bancada do Banco

Obtencao:

- Sem craft.
- Apenas comando administrativo.
- Apenas operador ou administrador pode colocar.

Inventarios persistidos:

- `BANK_STOCK`
- `GOLD_RESERVE`

Ofertas:

- 16 ofertas configuraveis.
- Cada oferta pode comprar, vender ou ambos.
- Modo `FIXED`, `DYNAMIC` ou `MONETARY_GOLD`.

## Comparacao de itens

Modos:

- `FULL_COMPONENTS`
- `ITEM_ID_ONLY`

`FULL_COMPONENTS` deve considerar:

- Identificador do item
- Componentes
- Encantamentos
- Durabilidade
- Nome personalizado
- Outros dados persistentes relevantes da versao Minecraft

## Receitas

Caixa Eletronico:

```text
F O F
F R F
F O F
```

Loja de Venda:

```text
M F M
M R M
M M M
```

Loja de Compra:

```text
M M M
M R M
M F M
```

Bancada do Banco:

```text
sem receita
```
