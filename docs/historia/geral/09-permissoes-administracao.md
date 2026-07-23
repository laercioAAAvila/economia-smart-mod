# Parte 9 — Permissões, comandos e administração

Esta parte integra a história funcional completa do Mod de Economia para Minecraft Java 1.21.1 com NeoForge 21.1.234+. As regras gerais e referências entre módulos continuam válidas.

---

## 34. Permissões

### Jogador comum

Pode:

- Criar conta
- Fazer login
- Depositar
- Sacar
- Comprar cartões
- Usar cartões
- Colocar Caixa Eletrônico
- Colocar lojas
- Configurar as próprias lojas
- Comprar e vender
- Utilizar a Bancada do Banco

### Proprietário de loja

Pode:

- Gerenciar estoque
- Gerenciar caixa físico
- Alterar preços
- Vincular conta
- Selecionar cartão de financiamento
- Ativar ou desativar a loja
- Remover a própria loja

### Operador ou administrador

Pode:

- Administrar qualquer loja
- Dar a Bancada do Banco
- Colocar a Bancada do Banco
- Remover a Bancada do Banco
- Configurar ofertas do banco
- Alterar tesouraria
- Bloquear contas
- Bloquear cartões
- Realizar estornos
- Consultar auditorias

---

## 35. Comandos administrativos

### Consultar conta

```text
/economia admin conta consultar <usuario>
```

### Bloquear conta

```text
/economia admin conta bloquear <usuario>
```

### Desbloquear conta

```text
/economia admin conta desbloquear <usuario>
```

### Ajustar saldo

```text
/economia admin saldo adicionar <usuario> <valor>
/economia admin saldo remover <usuario> <valor>
/economia admin saldo definir <usuario> <valor>
```

### Ajustar tesouraria

```text
/economia admin tesouraria adicionar <valor>
/economia admin tesouraria remover <valor>
/economia admin tesouraria consultar
```

### Dar Bancada do Banco

```text
/economia admin bloco dar bancada-banco
/economia admin bloco dar bancada-banco <jogador>
```

### Desativar cartão

```text
/economia admin cartao desativar <cardId>
```

### Consultar transação

```text
/economia admin transacao consultar <transactionId>
```

### Estornar transação

```text
/economia admin transacao estornar <transactionId>
```

### Configurar juros

```text
/economia admin juros consultar
/economia admin juros definir <pontosBasePorDia>
/economia admin juros modo <simple|compound>
/economia admin juros processar <data>
```

O processamento manual de uma data deverá respeitar a idempotência e não poderá duplicar cobrança.

### Administrar ouro

```text
/economia admin ouro consultar
/economia admin ouro definir-valor-pepita <valor>
/economia admin ouro ajustar-reserva <adicionar|remover> <quantidadePepitas>
```

Alterar o valor-base do ouro afetará futuras trocas, sem modificar transações antigas.

### Administrar preços dinâmicos

```text
/economia admin preco consultar <offerId>
/economia admin preco resetar <offerId>
/economia admin preco recalcular <offerId>
/economia admin preco definir-modo <offerId> <fixed|dynamic|monetary_gold>
```

### Recarregar configurações

```text
/economia admin reload
```

Todos os comandos administrativos deverão gerar auditoria.

---

## 36. Quebra dos blocos

### Caixa Eletrônico

Pode ser removido por:

- Jogador que colocou
- Operador
- Administrador

Não possui saldo ou dinheiro interno.

### Loja de Venda e Loja de Compra

Podem ser removidas por:

- Proprietário
- Operador
- Administrador

Não poderão ser quebradas durante uma transação.

Ao remover:

- Estoque será devolvido ou derrubado
- Dinheiro será devolvido ou derrubado
- Vinculação bancária será removida
- Registro SQL será marcado como removido
- O bloco entregue não guardará a vinculação anterior

### Bancada do Banco

Pode ser removida somente por operador ou administrador.

Se possuir estoque, a quebra normal será bloqueada.

---

## 37. Texturas e modelos

Todos os itens deverão possuir textura:

- Oito notas
- Três cartões

Todos os blocos deverão possuir:

- Modelo
- Textura frontal
- Texturas laterais
- Textura traseira
- Textura superior
- Textura inferior
- Orientação horizontal correta

A Loja de Venda e a Loja de Compra deverão ser visualmente diferentes.

A Bancada do Banco deverá possuir aparência claramente administrativa ou bancária.

---
