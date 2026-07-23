# Parte 4 — Caixa Eletrônico

Esta parte integra a história funcional completa do Mod de Economia para Minecraft Java 1.21.1 com NeoForge 21.1.234+. As regras gerais e referências entre módulos continuam válidas.

---

## 21. Ajuste implementado da interface

A interface do Caixa Eletronico deve separar claramente os estados de acesso:

- Antes do login: abas de Login, Criar conta e Recuperar senha
- Depois do login: abas de Dinheiro, Cartoes, Credito, Transferir, Seguranca e Ouro

Na tela de Login, o jogador pode entrar com usuario e senha ou usando um cartao valido no slot de cartao.

Nas telas de Criar conta e Recuperar senha nao deve existir slot de cartao nem inventario do jogador.

Depois do login, a tela de Dinheiro mostra saldo e saldo disponivel acima do campo de valor. O saldo deve ser atualizado automaticamente em intervalo controlado para evitar consulta excessiva ao banco.

A aba Cartoes deve manter somente a hotbar do jogador, mostrar os botoes de emissao em linha horizontal com o valor abaixo de cada tipo, permitir configurar limite diario de debito, e exibir uma lista rolavel dos cartoes criados para bloquear ou desativar.

A aba Credito deve manter somente a hotbar do jogador, permitir pedir credito, ajustar limite do cartao inserido, emitir/reemitir fatura, pagar a fatura emitida e pagar todas as faturas em aberto.

Ao fechar com ESC, a sessao continua em memoria ate expirar, desconectar ou trocar de mundo.

## 17. Caixa Eletrônico

### 17.1 Bloco

O Caixa Eletrônico será um bloco com:

- Textura própria
- Modelo próprio
- Frente identificável
- Orientação horizontal
- Interface própria

Direções permitidas:

- Norte
- Sul
- Leste
- Oeste

Não poderá ficar voltado para cima ou para baixo.

### 17.2 Colocação

Qualquer jogador poderá:

- Criar o Caixa Eletrônico pelo craft
- Colocar o bloco
- Usar o bloco

O jogador que colocar o Caixa Eletrônico será registrado como responsável pelo bloco.

Somente poderão removê-lo:

- Jogador que colocou
- Operador
- Administrador

O responsável pelo bloco não terá acesso aos dados bancários dos jogadores.

### 17.3 Funções

O Caixa Eletrônico deverá permitir:

- Criar conta
- Login com usuário e senha
- Login com cartão
- Consultar saldo
- Consultar limite
- Depositar
- Sacar
- Comprar cartões
- Alterar nome dos cartões
- Alterar limites
- Desativar cartões
- Bloquear cartões
- Listar cartões criados
- Consultar faturas
- Reemitir faturas perdidas
- Pagar faturas
- Pagar todas as faturas em aberto
- Encerrar sessão

### 17.4 Acesso por cartão

O acesso por cartão deverá ser limitado às funções do cartão.

#### Débito

- Consultar saldo
- Depositar
- Sacar
- Ver movimentações feitas pelo cartão

#### Crédito

- Consultar limite
- Consultar fatura
- Pagar fatura

#### Débito e crédito

- Todas as funções anteriores

As funções abaixo sempre exigirão usuário e senha:

- Emitir novos cartões
- Alterar limite global
- Alterar limite de outros cartões
- Desativar cartões
- Bloquear cartões
- Alterar senha
- Consultar todos os cartões da conta

---

## 18. Depósito

A interface terá slots específicos para notas.

Somente notas válidas serão aceitas.

Ao confirmar:

1. O servidor bloqueia a operação.
2. Valida novamente os itens.
3. Calcula o valor.
4. Remove as notas.
5. Credita o saldo.
6. Registra o depósito.

Itens diferentes deverão ser recusados automaticamente.

---

## 19. Saque

O jogador informará um valor inteiro.

O sistema deverá:

- Confirmar que o valor é maior que zero
- Confirmar que o jogador tem saldo disponível
- Confirmar que o inventário possui espaço
- Gerar as notas utilizando as maiores denominações
- Retirar o saldo
- Entregar as notas
- Registrar o saque

Exemplo de saque de R$ 376:

```text
1 × R$ 200
1 × R$ 100
1 × R$ 50
1 × R$ 20
1 × R$ 5
1 × R$ 1
```

Se o inventário não possuir espaço suficiente, nada será debitado.

---

## 20. Craft do Caixa Eletrônico

Receita inicial:

```text
F O F
F R F
F O F
```

Legenda:

- `F`: lingote de ferro
- `O`: lingote de ouro
- `R`: bloco de redstone

Resultado:

```text
1 Caixa Eletrônico
```

---
