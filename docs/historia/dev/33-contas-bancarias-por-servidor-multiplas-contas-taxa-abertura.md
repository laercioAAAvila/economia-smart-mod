# História — Contas por servidor, múltiplas contas e taxa de abertura

## 1. Objetivo

Permitir que o mesmo jogador use um único banco de dados em mundos ou servidores diferentes sem que uma conta criada em um ambiente impeça a criação de outra conta no segundo ambiente.

Também permitir múltiplas contas bancárias por jogador no mesmo servidor e cobrar uma taxa configurável ao abrir cada conta.

## 2. Identidade bancária

Uma conta de jogador pertence à combinação:

- UUID estável do servidor ou mundo;
- UUID do jogador fornecido pelo Minecraft;
- nome bancário normalizado e exclusivo naquele servidor.

O nome atual do perfil Minecraft também é salvo para suporte e atualização cadastral, mas não substitui o UUID do jogador.

Cada mundo ou servidor gera automaticamente seu `serverUuid` na configuração do mod. Servidores diferentes que usam o mesmo banco devem manter UUIDs diferentes. Copiar intencionalmente o mesmo `serverUuid` faz os ambientes compartilharem o mesmo escopo bancário.

## 3. Regras funcionais

### 3.1 Contas por jogador

- O limite padrão é de 3 contas ativas ou pendentes por jogador em cada servidor.
- `bank.accounts.maxAccountsPerPlayer` permite configurar de 1 até o limite suportado pelo servidor.
- O mesmo jogador pode criar contas adicionais até o limite.
- O mesmo nome bancário não pode ser repetido dentro do mesmo servidor.
- Servidores diferentes podem reutilizar o mesmo nome bancário e o mesmo UUID de jogador.

### 3.2 Conta padrão

Fluxos antigos que conhecem apenas o UUID do jogador usam como conta padrão a conta ativa mais antiga daquele jogador no servidor atual. Fluxos autenticados, cartões e sessões operam sobre a conta escolhida no login.

### 3.3 Taxa de abertura

- `bank.accounts.openingFee` define a taxa, com padrão de R$ 1.000 e mínimo zero.
- O Caixa Eletrônico mostra o valor antes da confirmação.
- O pagamento da primeira e das demais contas é feito com notas no espaço exibido na tela de criação.
- O valor deve ser exato; não há troco.
- Taxa zero cria a conta gratuitamente.
- A taxa é registrada na conta e creditada ao tesouro somente uma vez.
- A conta fica pendente até o pagamento ser confirmado, evitando uma conta ativa sem pagamento.

### 3.4 Login e recuperação

- Login por usuário e senha procura somente contas do servidor atual.
- Recuperação exige a combinação UUID do servidor, UUID do jogador conectado e nome bancário informado.
- A recuperação de uma conta não altera outra conta do mesmo jogador.
- Após redefinir a senha com sucesso, a sessão dessa conta é aberta automaticamente.

### 3.5 Logs seguros

- Falhas conhecidas podem registrar `accountUuid` para diagnóstico.
- Senhas, hashes, salts, números de cartão e dados completos de autenticação nunca são registrados.
- Quando nenhuma conta é localizada, o log informa apenas o motivo sem expor credenciais.

## 4. Migração e compatibilidade

- Contas existentes recebem o UUID do primeiro servidor que iniciar após a migração.
- Nenhum saldo, senha, cartão ou número de conta é recriado durante a adoção.
- A exclusão administrativa é limitada ao nome bancário no servidor atual e não remove dados das outras contas do jogador.

## 5. Critérios de aceite

1. Um jogador cria uma conta no singleplayer e outra em um servidor diferente usando o mesmo banco de dados.
2. As contas não entram em conflito quando os `serverUuid` são diferentes.
3. Um jogador cria até o limite configurado de contas no mesmo servidor.
4. A tentativa seguinte é recusada sem cobrança.
5. O Caixa Eletrônico mostra R$ 1.000 por padrão e exige o pagamento exato.
6. Login, cartões e recuperação não atravessam o escopo do servidor.
7. O log identifica a conta por UUID quando ela foi localizada, sem registrar informações sensíveis.
