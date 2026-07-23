# Parte 3 — Crédito, cartões, juros e faturas

Esta parte integra a história funcional completa do Mod de Economia para Minecraft Java 1.21.1 com NeoForge 21.1.234+. As regras gerais e referências entre módulos continuam válidas.

---

## 8. Sistema de crédito

### 8.1 Crédito solicitado e garantido pelo saldo

O limite da conta não será concedido automaticamente. O jogador deverá pedir crédito no Caixa Eletrônico.

Ao pedir crédito, o servidor verificará se existe dívida de crédito aberta. Se existir principal ou juros em aberto, o pedido será recusado.

Quando não houver dívida, o limite elegível será calculado por faixa do saldo bancário:

```text
Saldo abaixo de R$ 250.000: 40%
R$ 250.000 até abaixo de R$ 5.000.000: 60%
R$ 5.000.000 até abaixo de R$ 500.000.000: 80%
R$ 500.000.000 ou mais: 95%
```

O limite efetivo da conta será sempre o menor valor entre o limite configurado e o limite permitido pelo saldo atual.

Exemplo:

```text
Saldo da conta: R$ 1.000
Faixa aplicada: 40%
Limite aprovado ao pedir crédito: R$ 400
```

O jogador poderá ajustar o limite configurado manualmente para um valor menor, mas nunca acima do valor elegível pela faixa do saldo.

### 8.2 Separação entre principal e juros

A dívida deverá ser dividida em:

- **Principal em aberto:** compras realizadas no crédito que ainda não foram pagas
- **Juros acumulados:** juros diários já lançados e ainda não pagos
- **Dívida total:** principal em aberto mais juros acumulados

Fórmula:

```text
Dívida total = principal em aberto + juros acumulados
```

Essa separação é obrigatória porque os juros podem fazer a dívida ultrapassar o limite originalmente concedido. Isso não aumenta o limite nem permite novas compras.

### 8.3 Crédito disponível

O crédito disponível deverá considerar a dívida total, e não apenas o principal.

```text
Crédito global disponível =
max(0, limite efetivo da conta - dívida total da conta)
```

Para um cartão específico:

```text
Crédito disponível do cartão =
max(0, menor valor entre:
  limite individual do cartão - dívida total do cartão,
  crédito global disponível da conta
)
```

Uma compra no crédito somente poderá ser aprovada quando a dívida total após a compra continuar dentro:

- Do limite individual do cartão
- Do limite efetivo da conta
- Do saldo bancário que garante o crédito

### 8.4 Saldo bloqueado pela dívida

O jogador não poderá sacar ou gastar no débito o valor que garante a dívida total do crédito.

```text
Saldo disponível para saque ou débito =
max(0, saldo bancário - dívida total)
```

Exemplo:

```text
Saldo bancário: R$ 1.000
Principal em aberto: R$ 300
Juros acumulados: R$ 20
Dívida total: R$ 320
Disponível para saque ou débito: R$ 680
```

Isso impede que o mesmo dinheiro seja utilizado como garantia do crédito e depois retirado da conta.

### 8.5 Redução automática do limite efetivo

Sempre que uma operação reduzir o saldo e o limite configurado ficar acima do valor elegível pela faixa atual, o limite efetivo deverá ser reduzido automaticamente até esse valor elegível.

Exemplo:

```text
Saldo anterior: R$ 1.000
Limite anterior: R$ 400
Pagamento de fatura: R$ 300
Saldo posterior: R$ 700
Novo limite elegível: R$ 280
```

Os juros poderão fazer a dívida total ultrapassar o limite ou até o saldo. Nesse caso:

- A dívida não será apagada
- Nenhuma nova compra no crédito será autorizada
- O saldo disponível para saque e débito poderá ficar em zero
- O jogador deverá pagar a dívida ou depositar mais dinheiro

### 8.6 Regras principais

Antes de autorizar uma nova compra no crédito, deverá ser verdadeiro:

```text
limite efetivo = min(limite configurado, limite elegível por faixa do saldo)

dívida total após a compra <= limite efetivo

dívida total do cartão após a compra <= limite individual do cartão
```

Depois da aplicação de juros, a dívida total poderá ultrapassar o limite. Essa situação será aceita apenas como dívida existente e bloqueará novas compras.

---

## 9. Cartões

### 9.1 Tipos

#### Cartão de débito

Permite:

- Compras no débito
- Consulta de saldo
- Depósitos
- Saques no Caixa Eletrônico

#### Cartão de crédito

Permite:

- Compras no crédito
- Consulta de limite
- Consulta de fatura
- Pagamento de fatura

Não permite saque do saldo bancário.

#### Cartão de débito e crédito

Permite todas as funções dos dois tipos.

O usuário deverá escolher débito ou crédito no momento da compra.

### 9.2 Propriedades dos cartões

Cada cartão deverá possuir:

- Identificador UUID próprio
- Identificador da conta
- Número sequencial de criação na conta
- Tipo
- Nome personalizado
- Estado
- Limite individual, quando possuir crédito
- Limite diário de débito
- Valor gasto no débito no dia
- Data do gasto diário de débito
- Principal em aberto
- Juros acumulados
- Resto matemático do cálculo de juros
- Data de criação
- Data de desativação
- Versão de segurança

O item deverá guardar apenas os dados necessários para localizar o cartão e exibir informações úteis na descrição:

- Tipo do cartão
- Número da conta
- Limite diário de débito, quando configurado
- Limite de crédito, quando possuir função crédito

A autorização verdadeira será consultada no SQL.

### 9.3 Stack

Todos os cartões terão stack máximo de 1.

### 9.4 Uso por outra pessoa

Qualquer jogador que estiver com o cartão poderá utilizá-lo.

O mod não verificará se o jogador que está usando o cartão é o dono.

Isso permitirá:

- Emprestar um cartão
- Roubar um cartão
- Utilizar um cartão encontrado

O proprietário poderá desativá-lo pelo Caixa Eletrônico.

### 9.5 Cópias de cartão

Cartões falsificados ou com identificadores inexistentes serão recusados.

Caso duas cópias possuam exatamente o mesmo identificador, ambas serão tratadas como o mesmo cartão.

Ao desativar o cartão, todas as cópias com aquele identificador deixarão de funcionar.

### 9.6 Nome do cartão

Cada cartão receberá um nome no formato:

```text
numeroDeCriacao-dd-MM-aaaa
```

Exemplo:

```text
1-23-07-2026
2-23-07-2026
```

O número de criação nunca será reutilizado. Se o cartão 3 for desativado, o próximo cartão criado será o 4.

O proprietário poderá definir um nome personalizado de até 32 caracteres quando o fluxo permitir.

Exemplos:

- Cartão principal
- Cartão da loja
- Cartão de compras
- Reserva

O nome não poderá alterar o identificador interno.

---

## 10. Quantidade máxima de cartões

Cada conta poderá possuir:

- Até 3 cartões de débito ativos
- Até 3 cartões de crédito ativos
- Até 1 cartão de débito e crédito ativo

Cartões desativados não contarão para o limite de cartões ativos.

Entretanto, dívidas de cartões desativados continuarão vinculadas à conta.

---

## 11. Limites individuais

Cada cartão com função crédito poderá possuir um limite próprio.

A soma dos valores reservados pelos cartões não poderá superar o limite total da conta.

Para cartões ativos:

```text
valor reservado = limite individual
```

Para cartões desativados com dívida:

```text
valor reservado = dívida total ainda não paga
```

Exemplo:

```text
Limite total da conta: R$ 1.000

Cartão A ativo: limite R$ 500
Cartão B ativo: limite R$ 300
Cartão C desativado: dívida R$ 200

Total reservado: R$ 1.000
```

Não poderá ser criado outro limite até que parte da dívida seja paga ou outro limite seja reduzido.

---

## 12. Aquisição de cartões

Os cartões somente poderão ser adquiridos no Caixa Eletrônico.

Requisitos:

- Ter conta ativa
- Estar autenticado com usuário e senha
- Não ter atingido o limite daquele tipo
- Possuir saldo disponível suficiente para a taxa de emissão
- Ter espaço no inventário

Os preços serão configuráveis.

Exemplo inicial:

```text
Cartão de débito: R$ 20
Cartão de crédito: R$ 30
Cartão de débito e crédito: R$ 50
```

Caso não exista espaço no inventário para receber o cartão, o item deverá ser entregue no mundo sem apagar o registro do cartão.

---

## 13. Desativação dos cartões

Um cartão poderá ser bloqueado ou desativado pelo proprietário da conta no Caixa Eletrônico.

O Caixa Eletrônico deverá listar os cartões da conta em uma tabela com rolagem quando necessário.

Bloquear cartão:

- Impede novas operações
- Não libera a vaga de cartão ativo
- Não apaga dívida ou fatura

Desativar cartão:

Ao desativar:

- Novas compras serão bloqueadas
- Novos saques serão bloqueados
- A dívida continuará existindo
- O histórico continuará disponível
- O cartão liberará uma vaga de emissão
- O limite não utilizado será liberado
- O principal e os juros ainda não pagos continuarão vinculados à conta

Se o cartão desativado possuir dívida de crédito, a dívida continuará aparecendo na ordem de pagamento das faturas.

Nenhuma dívida, fatura, histórico ou reserva necessária para cobrança poderá ficar órfã.

---

## 14. Compras no débito

Uma compra no débito deverá:

1. Validar o cartão no SQL.
2. Confirmar que está ativo.
3. Confirmar que possui função débito.
4. Confirmar que a conta está ativa.
5. Bloquear temporariamente a linha da conta no SQL.
6. Verificar o saldo disponível.
7. Debitar a conta do comprador.
8. Creditar o vendedor.
9. Entregar os itens.
10. Registrar a transação.

O saldo disponível deverá considerar toda a dívida do crédito.

```text
saldo disponível = max(0, saldo - principal em aberto - juros acumulados)
```

---

## 15. Compras no crédito

Uma compra no crédito deverá:

1. Validar o cartão.
2. Verificar se possui função crédito.
3. Verificar se está ativo.
4. Verificar o limite individual.
5. Verificar o limite global.
6. Aumentar o principal em aberto do cartão.
7. Aumentar o principal em aberto da conta.
8. Creditar o vendedor.
9. Registrar a compra na fatura com a data de elegibilidade para juros.
10. Entregar os itens.

A compra deverá ser recusada se ultrapassar:

- Limite individual do cartão
- Limite global da conta
- Saldo que garante o crédito
- Limite restante

---

## 16. Faturas e juros diários

### 16.1 Consulta da fatura

O Caixa Eletrônico deverá mostrar:

- Cartão
- Nome personalizado
- Limite individual
- Principal em aberto
- Juros acumulados
- Dívida total
- Crédito disponível
- Compras realizadas
- Juros lançados por dia
- Pagamentos realizados
- Data e hora de cada lançamento
- Loja ou bancada relacionada
- Taxa de juros diária vigente
- Próxima data de aplicação de juros

### 16.2 Juros diários

O cartão de crédito e o cartão de débito e crédito terão juros diários sobre a dívida não paga.

A taxa deverá ser configurável pelo servidor em pontos-base:

```text
1 ponto-base = 0,01%
50 pontos-base = 0,50% ao dia
100 pontos-base = 1,00% ao dia
```

O valor apresentado acima é apenas exemplo. A taxa real será definida na configuração ou por comando administrativo.

### 16.3 Período de carência

Para evitar juros quase imediatos em compras feitas perto da virada do dia, cada compra terá uma data a partir da qual poderá receber juros.

Configuração:

```text
credit.interest.graceDays
```

Valor inicial sugerido:

```text
1 dia
```

Com um dia de carência, uma compra não receberá juros no mesmo dia em que foi realizada.

### 16.4 Fechamento diário

O servidor executará uma rotina diária usando o fuso horário configurado.

Configurações:

```text
economy.timeZone=America/Araguaina
credit.interest.applicationHour=0
```

A rotina deverá:

1. Localizar cartões com dívida elegível.
2. Bloquear a conta e o cartão no SQL.
3. Verificar se os juros daquele cartão e daquela data já foram aplicados.
4. Calcular os juros.
5. Criar um lançamento de fatura do tipo `DAILY_INTEREST`.
6. Atualizar os juros acumulados do cartão e da conta.
7. Registrar a transação e a auditoria.
8. Marcar aquela data como processada.

A aplicação deverá ser idempotente. Reiniciar o servidor não poderá cobrar o mesmo dia duas vezes.

### 16.5 Cálculo sem centavos visíveis

A moeda física não terá centavos, mas o cálculo dos juros não deverá arredondar todo valor pequeno para R$ 1 por dia.

Será mantido um resto matemático em pontos-base.

Exemplo com dívida de R$ 100 e taxa de 0,50%:

```text
Primeiro dia:
100 × 50 = 5.000 unidades de cálculo
Juro inteiro lançado: R$ 0
Resto acumulado: 5.000

Segundo dia:
100 × 50 + 5.000 = 10.000
Juro inteiro lançado: R$ 1
Resto acumulado: 0
```

Assim, o jogo continua usando valores inteiros em reais, sem cobrar R$ 1 indevidamente todos os dias sobre dívidas pequenas.

### 16.6 Juros simples ou compostos

O modo será configurável:

- `SIMPLE`: calcula juros apenas sobre o principal em aberto
- `COMPOUND`: calcula juros sobre principal mais juros acumulados

Modo inicial sugerido:

```text
COMPOUND
```

A interface deverá informar claramente qual modo está ativo.

### 16.7 Servidor desligado durante vários dias

Ao iniciar, o servidor deverá identificar dias ainda não processados e aplicar cada fechamento pendente uma única vez.

O cálculo deverá considerar:

- Data de elegibilidade da dívida
- Datas já processadas
- Alterações históricas da taxa
- Pagamentos realizados no período, quando houver registros intermediários

Não será permitido cobrar duas vezes nem ignorar dias apenas porque o servidor estava desligado.

### 16.8 Pagamento da fatura

O jogador poderá:

- Reemitir a fatura mais antiga em aberto, caso tenha perdido o item de fatura
- Pagar a fatura emitida no slot de fatura do Caixa Eletrônico
- Pagar todas as faturas em aberto de uma vez

O pagamento será retirado do saldo bancário.

A fatura deverá ficar disponível a partir de uma quantidade configurável de dias antes do vencimento. O padrão poderá ser 1 dia antes.

Quando houver mais de uma fatura em aberto, a emissão e o pagamento deverão seguir sempre da mais antiga para a mais nova. Ao pagar uma fatura, a próxima em aberto poderá ser emitida no mesmo local.

A ordem de pagamento será:

1. Juros mais antigos
2. Principal mais antigo
3. Demais lançamentos na ordem cronológica

Ao pagar:

- O saldo bancário diminui
- Os juros e/ou principal em aberto diminuem
- O crédito disponível aumenta quando a dívida fica abaixo do limite
- O limite configurado é reduzido automaticamente se ficar maior que o limite elegível pelo saldo

Exemplo:

```text
Saldo antes: R$ 1.000
Principal: R$ 300
Juros: R$ 20
Dívida total: R$ 320

Pagamento: R$ 320

Saldo depois: R$ 680
Principal depois: R$ 0
Juros depois: R$ 0
```

### 16.9 Pagamento parcial

O valor deverá ser maior que zero e não poderá superar a dívida selecionada.

Em pagamentos gerais da conta, o sistema pagará primeiro os lançamentos mais antigos, respeitando a prioridade de juros antes do principal.

### 16.10 Cartão desativado

Cartões desativados continuarão recebendo juros enquanto possuírem dívida, salvo se uma configuração administrativa específica congelar os juros.

Desativar um cartão impede novas compras, mas não cancela compras anteriores, juros ou pagamentos pendentes.

---
