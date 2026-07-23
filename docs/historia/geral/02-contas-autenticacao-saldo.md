# Parte 2 — Contas, autenticação e saldo

Esta parte integra a história funcional completa do Mod de Economia para Minecraft Java 1.21.1 com NeoForge 21.1.234+. As regras gerais e referências entre módulos continuam válidas.

---

## 5. Conta bancária

### 5.1 Regras gerais

Cada UUID de jogador poderá possuir somente uma conta bancária pessoal.

A conta será vinculada a:

- UUID do jogador
- Nome bancário
- Senha
- Identificador interno da conta

O nome bancário deverá ser único, ignorando diferenças entre letras maiúsculas e minúsculas.

Exemplo:

```text
Laercio
laercio
LAERCIO
```

Os três nomes serão considerados iguais.

### 5.2 Estados da conta

Uma conta poderá estar:

- `ACTIVE`: ativa
- `BLOCKED`: bloqueada
- `CLOSED`: encerrada

Contas bloqueadas ou encerradas não poderão realizar movimentações.

Uma conta não deverá ser excluída fisicamente do SQL caso já possua transações.

---

## 6. Criação e acesso à conta

### 6.1 Criação segura

Comando:

```text
/economia conta criar <usuario>
```

Após executar o comando, será aberta uma interface para:

- Informar a senha
- Confirmar a senha
- Confirmar a criação da conta

A senha não deverá ser digitada diretamente no chat ou no comando, porque comandos podem aparecer no histórico do cliente ou nos logs do servidor.

### 6.2 Login seguro

Comando:

```text
/economia login <usuario>
```

Após executar, será aberta uma interface para digitar a senha.

### 6.3 Logout

```text
/economia logout
```

### 6.4 Consulta de saldo

```text
/economia saldo
```

Exemplo de resposta:

```text
Saldo bancário: R$ 1.500
Saldo disponível para saque ou débito: R$ 1.180
Limite de crédito configurado: R$ 600
Principal em aberto: R$ 300
Juros acumulados: R$ 20
Dívida total: R$ 320
Crédito disponível: R$ 280
```

### 6.5 Alteração de senha

```text
/economia conta alterar-senha
```

O comando abrirá uma interface solicitando:

- Senha atual
- Nova senha
- Confirmação

### 6.6 Sessão bancária

A sessão deverá ser encerrada quando:

- O jogador executar logout
- O jogador desconectar
- O servidor reiniciar
- O tempo configurado expirar

As sessões poderão ser mantidas somente na memória do servidor.

Não será necessário persistir senhas ou tokens de sessão no cliente.

### 6.7 Segurança da senha

A senha deverá:

- Ser armazenada usando hash seguro
- Possuir salt individual
- Nunca ser armazenada em texto puro
- Nunca ser enviada de volta ao cliente
- Nunca aparecer em logs

O SQL deverá armazenar somente:

- Hash
- Salt, quando exigido pelo algoritmo
- Versão do algoritmo utilizado

---

## 7. Saldo bancário

O saldo deverá ser persistido no SQL.

O saldo poderá ser alterado por:

- Depósito
- Saque
- Compra no débito
- Recebimento de vendas
- Pagamento de fatura
- Operações realizadas por lojas
- Operações da Bancada do Banco
- Emissão monetária lastreada em ouro
- Resgate de ouro com recolhimento de moeda
- Juros e pagamentos de fatura
- Ajustes administrativos
- Estornos

O saldo nunca poderá ficar negativo.

---
