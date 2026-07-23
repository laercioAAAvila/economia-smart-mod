# Checklist de implementacao

Codigo: `CHK-DEV-07`
Historia base: `HIST-DEV-01`, `HIST-DEV-02`, `HIST-DEV-03`, `HIST-DEV-04`, `HIST-DEV-05`, `HIST-DEV-06`
Historias gerais relacionadas: `HIST-GERAL-01` a `HIST-GERAL-10`
Escopo: checklist principal de implementacao do mod.

## Fase 0 - Scaffold NeoForge

- [x] Criar projeto Gradle/NeoForge.
- [x] Configurar Minecraft 1.21.1, NeoForge 21.1.234+ e Java 21.
- [x] Criar classe principal do mod.
- [x] Registrar configuracao de servidor.
- [x] Criar `.gitignore` basico.
- [x] Criar pack metadata e manifest `neoforge.mods.toml`.
- [x] Modularizar servicos grandes de pagamento, fatura e juros.

## Fase 1 - Banco de dados

- [x] Criar migrações SQL versionadas iniciais.
- [x] Criar controle de estado do banco.
- [x] Criar catalogo de migracoes carregavel pelo mod.
- [x] Implementar conexao SQL.
- [x] Implementar executor de migracoes.
- [x] Criar contas do sistema na inicializacao.
- [x] Bloquear operacoes quando SQL estiver indisponivel.

## Fase 2 - Dinheiro e contas

- [x] Registrar itens de notas.
- [x] Criar utilitario de denominacoes.
- [x] Criar utilitario de soma e montagem de notas.
- [x] Implementar hash de senha.
- [x] Criar servico SQL base de criacao de conta.
- [x] Criar servico SQL base de autenticacao.
- [x] Criar servico de sessao bancaria em memoria.
- [x] Implementar comando de criacao de conta por tela segura.
- [x] Implementar login por tela segura com sessao em memoria.
- [x] Implementar logout de sessao em memoria.
- [x] Implementar consulta de saldo.
- [x] Criar servico SQL transacional de deposito.
- [x] Criar servico SQL transacional de saque.
- [x] Criar servico de inventario para dinheiro fisico.
- [x] Criar coordenador de deposito com dinheiro fisico.
- [x] Criar coordenador de saque com dinheiro fisico.
- [x] Implementar deposito pela interface.
- [x] Implementar saque pela interface.

## Fase 3 - Caixa Eletronico

- [x] Registrar bloco do Caixa Eletronico.
- [x] Criar receita do Caixa Eletronico.
- [x] Aplicar orientacao horizontal.
- [x] Criar block entity com UUID comercial.
- [x] Registrar/remover Caixa Eletronico no SQL.
- [x] Criar menu e tela.
- [x] Implementar deposito pela interface.
- [x] Implementar saque pela interface.
- [x] Implementar compra de cartoes.

## Fase 4 - Cartoes, credito e juros

- [x] Registrar itens de cartao.
- [x] Criar enum tecnico de tipo de cartao.
- [x] Persistir UUID e versao de seguranca no item.
- [x] Criar servico SQL de emissao de cartoes.
- [x] Criar servico SQL de validacao de cartoes.
- [x] Implementar compra/emissao de cartoes pela interface.
- [x] Implementar nucleo SQL de debito.
- [x] Implementar nucleo SQL de credito.
- [x] Criar consulta SQL de faturas.
- [x] Implementar pagamento parcial de fatura.
- [x] Implementar processador de juros diarios idempotentes.
- [x] Implementar pagamento parcial.

## Fase 5 - Loja de Venda

- [x] Registrar bloco da Loja de Venda.
- [x] Criar receita da Loja de Venda.
- [x] Aplicar orientacao horizontal.
- [x] Criar block entity com UUID comercial.
- [x] Registrar/remover Loja de Venda no SQL.
- [x] Criar base SQL de inventario persistente da Loja de Venda.
- [x] Criar inventario persistente de estoque.
- [x] Criar inventario persistente de caixa fisico.
- [x] Implementar venda em dinheiro.
- [x] Implementar venda por cartao.

## Fase 6 - Loja de Compra

- [x] Registrar bloco da Loja de Compra.
- [x] Criar receita da Loja de Compra.
- [x] Aplicar orientacao horizontal.
- [x] Criar block entity com UUID comercial.
- [x] Registrar/remover Loja de Compra no SQL.
- [x] Criar base SQL de inventario persistente da Loja de Compra.
- [x] Implementar item de referencia.
- [x] Implementar compra com reserva fisica.
- [x] Implementar compra com saldo bancario.
- [x] Implementar compra com credito.

## Fase 7 - Bancada do Banco

- [x] Registrar bloco da Bancada do Banco.
- [x] Nao criar receita da Bancada do Banco.
- [x] Criar comando administrativo para entregar Bancada do Banco.
- [x] Bloquear colocacao por jogador sem permissao.
- [x] Bloquear quebra por jogador sem permissao.
- [x] Criar block entity com UUID comercial.
- [x] Registrar/remover Bancada do Banco no SQL.
- [x] Criar base SQL de inventario persistente da Bancada do Banco.
- [x] Criar ofertas administrativas.
- [x] Implementar tesouraria.
- [x] Implementar reserva de ouro.
- [x] Criar IDs fixos das contas do sistema.
- [x] Criar consulta basica da tesouraria.
- [x] Inicializar resumo oficial da Reserva de Ouro.
- [x] Criar conversor tecnico de ouro monetario.
- [x] Criar nucleo SQL de emissao monetaria por ouro.
- [x] Criar nucleo SQL de resgate monetario por ouro.
- [x] Implementar emissao e resgate pela interface.
- [x] Implementar precos dinamicos.
- [x] Criar calculadora tecnica de precos dinamicos.

## Fase 8 - Recuperacao e seguranca

- [x] Criar tabela de operacoes pendentes.
- [x] Criar base de idempotencia para deposito e saque.
- [x] Implementar chaves de idempotencia em deposito e saque.
- [x] Criar controle idempotente de rotina diaria de juros.
- [x] Implementar retomada de operacoes pendentes.
- [x] Implementar estornos.
- [x] Implementar rotina diaria idempotente de juros.
- [x] Cobrir cenarios de queda entre SQL e inventario.
