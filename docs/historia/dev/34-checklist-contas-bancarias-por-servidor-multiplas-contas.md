# Checklist — Contas por servidor, múltiplas contas e taxa de abertura

Referência: `HIST-DEV-33`.

## Configuração e identidade

- [x] Criar `serverUuid` estável e automático nos dados salvos do mundo.
- [x] Gerar outro `serverUuid` ao apagar e recriar o save.
- [x] Configurar `maxAccountsPerPlayer`, padrão 3, de 1 até o limite suportado pelo servidor.
- [x] Configurar `openingFee`, padrão R$ 1.000 e mínimo zero.
- [x] Salvar UUID e nome atual do jogador nas contas novas.

## Persistência

- [x] Adicionar `server_uuid`, taxa e requisição de abertura à conta.
- [x] Remover a unicidade global de jogador e usuário bancário.
- [x] Tornar o usuário bancário único por servidor.
- [x] Adotar contas legadas sem alterar saldo, senha, cartões ou número.
- [x] Manter abertura idempotente com estado pendente até o pagamento.

## Fluxos bancários

- [x] Permitir múltiplas contas por jogador até o limite do servidor.
- [x] Restringir login, cartões, consultas e recuperação ao servidor atual.
- [x] Recuperar pela combinação servidor, jogador e usuário bancário.
- [x] Atualizar somente a conta escolhida durante recuperação ou exclusão.
- [x] Definir a conta ativa mais antiga como padrão nos fluxos sem seleção explícita.

## Caixa Eletrônico

- [x] Mostrar a taxa na tela de criação de conta.
- [x] Exibir slots para pagamento em notas.
- [x] Exigir o valor exato e devolver notas ao fechar ou sair da criação.
- [x] Ativar a conta somente depois do pagamento confirmado.
- [x] Aceitar abertura gratuita quando a taxa for zero.

## Segurança e observabilidade

- [x] Registrar o UUID da conta em falhas nas quais a conta foi localizada.
- [x] Não registrar senha, hash, salt, cartão ou credenciais completas.
- [x] Não apagar registros compartilhados de outras contas do jogador.

## Validação manual

- [ ] Reiniciar um singleplayer e confirmar que o `serverUuid` permanece igual.
- [ ] Usar o mesmo banco em dois servidores com UUIDs diferentes e criar uma conta em cada um.
- [ ] Criar três contas no mesmo servidor e confirmar o bloqueio da quarta.
- [ ] Alterar o limite no config e confirmar o novo comportamento.
- [ ] Pagar a taxa exata, tentar valor insuficiente e tentar valor acima da taxa.
- [ ] Recuperar separadamente duas contas do mesmo jogador.
- [ ] Confirmar no log o `accountUuid` em uma tentativa com senha incorreta.
