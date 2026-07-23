# Parte 1 — Visão geral, itens e dinheiro físico

Esta parte integra a história funcional completa do Mod de Economia para Minecraft Java 1.21.1 com NeoForge 21.1.234+. As regras gerais e referências entre módulos continuam válidas.

---

## 1. Informações do projeto

**Minecraft:** Java Edition 1.21.1  
**Mod Loader:** NeoForge 21.1.234 ou superior  
**Java:** Java 21  
**Execução:** Cliente e servidor  
**Persistência financeira:** Banco de dados SQL  
**Moeda:** Real brasileiro  
**Centavos:** Não suportados inicialmente

O mod deverá funcionar corretamente em servidor dedicado e impedir que o cliente altere diretamente saldos, cartões, lojas ou transações.

Todas as decisões financeiras serão tomadas pelo servidor.

---

## Organização da documentação

A história completa permanece neste arquivo, mas também foi separada em arquivos menores, sem remover regras ou alterar o fluxo funcional:

1. Visão geral, itens e dinheiro físico
2. Contas, autenticação e saldo
3. Crédito, cartões, juros e faturas
4. Caixa Eletrônico
5. Loja de Venda e Loja de Compra
6. Bancada do Banco, ouro e preços dinâmicos
7. Orientação dos blocos e modelo SQL
8. Integridade, concorrência e recuperação
9. Permissões, comandos e administração
10. Critérios de aceite e ordem de desenvolvimento

---

## 2. Objetivo

Criar um sistema de economia completo para Minecraft, contendo:

- Contas bancárias
- Autenticação
- Dinheiro físico
- Cartões de débito
- Cartões de crédito
- Cartões de débito e crédito
- Caixa Eletrônico
- Loja de Venda
- Loja de Compra
- Bancada comercial do banco
- Crédito com limite garantido pelo saldo
- Faturas
- Juros diários configuráveis
- Ouro como lastro e origem oficial de moeda
- Preços dinâmicos por oferta e procura
- Histórico financeiro
- Persistência SQL
- Proteção contra duplicação

---

## 3. Blocos e itens existentes

### 3.1 Itens de mão

Os seguintes itens não poderão ser colocados no chão:

#### Dinheiro

- Nota de R$ 1
- Nota de R$ 2
- Nota de R$ 5
- Nota de R$ 10
- Nota de R$ 20
- Nota de R$ 50
- Nota de R$ 100
- Nota de R$ 200

#### Cartões

- Cartão de débito
- Cartão de crédito
- Cartão de débito e crédito

Todos deverão possuir textura própria.

### 3.2 Blocos

Existirão quatro blocos:

- Caixa Eletrônico
- Loja de Venda
- Loja de Compra
- Bancada do Banco

O sistema utilizará apenas o nome **Caixa Eletrônico** em comandos, interfaces, arquivos de tradução, código e documentação.

---

## 4. Dinheiro físico

### 4.1 Regras das notas

Cada valor será um item registrado separadamente.

Todas as notas deverão:

- Possuir textura própria
- Ter stack máximo de 64
- Não ser colocáveis como bloco
- Ser aceitas por caixas, lojas e bancadas
- Mostrar o valor no nome do item
- Ser validadas pelo servidor

Exemplo:

```text
Nota de R$ 50
Valor monetário: R$ 50
```

### 4.2 Armazenamento dos valores

Os valores serão armazenados como números inteiros utilizando `BIGINT` no SQL e `long` no Java.

Exemplo:

```text
1000 representa R$ 1.000
```

Não deverá ser utilizado `float` ou `double` para valores monetários.

### 4.3 Cálculo do dinheiro

O sistema deverá multiplicar o valor de cada nota pela quantidade presente.

Exemplo:

```text
2 notas de R$ 100 = R$ 200
3 notas de R$ 20 = R$ 60
5 notas de R$ 2 = R$ 10

Total: R$ 270
```

---
