# Fases de implementacao

## Fase 0 - Scaffold NeoForge

Objetivo:

- Criar estrutura do mod.
- Configurar Java 21.
- Configurar NeoForge 21.1.234+.
- Definir `modid`.
- Registrar config basica.

Entrega:

- Mod carrega em cliente e servidor dedicado.
- Logs indicam sistema economico desativado ate SQL estar configurado.

## Fase 1 - Banco de dados

Objetivo:

- Conexao SQL.
- Pool.
- Migracoes.
- Contas do sistema.
- Ledger.
- Auditoria.

Entrega:

- Servidor aplica migracoes.
- Cria tesouraria e contas de sistema.
- Bloqueia economia quando SQL falha.

## Fase 2 - Dinheiro e contas

Objetivo:

- Registrar notas.
- Criar comandos de conta.
- Implementar senha segura.
- Criar sessoes em memoria.
- Deposito e saque por servico.

Entrega:

- Jogador cria conta.
- Login nao expoe senha.
- Saldo persiste.
- Deposito e saque funcionam com idempotencia.

## Fase 3 - Caixa Eletronico

Objetivo:

- Bloco orientavel.
- Block entity com UUID.
- Menu e tela.
- Deposito, saque, saldo e compra de cartao.

Entrega:

- Caixa pode ser criado por craft.
- Responsavel pelo bloco e registrado.
- Quebra respeita permissao.

## Fase 4 - Cartoes e credito

Objetivo:

- Cartoes com UUID.
- Validacao SQL.
- Debito.
- Credito.
- Faturas.
- Juros diarios.
- Pagamentos parciais.

Entrega:

- Cartao falso e recusado.
- Limites sao respeitados.
- Juros sao idempotentes.
- Dias pendentes sao recuperados.

## Fase 5 - Loja de Venda

Objetivo:

- Bloco orientavel.
- Estoque de 9 slots.
- Caixa fisico de 9 slots.
- Configuracao de produtos.
- Pagamento em dinheiro, debito e credito.
- Selecao Debito/Credito para cartao combinado.

Entrega:

- Loja nao vende sem estoque.
- Pagamento por cartao cai na conta vinculada.
- Clique em compra bloqueada mostra erro simples.

## Fase 6 - Loja de Compra

Objetivo:

- Item de referencia.
- Quantidade desejada.
- Reserva fisica.
- Financiamento por saldo.
- Financiamento por credito.

Entrega:

- Loja para quando falta dinheiro, saldo, credito ou espaco.
- Jogador recebe pagamento em notas ou na conta do cartao inserido.
- Itens comprados ficam persistidos.

## Fase 7 - Bancada do Banco

Objetivo:

- Comando administrativo.
- Bloco sem craft.
- Ofertas.
- Estoque do banco.
- Tesouraria.
- Reserva de ouro.
- Emissao e resgate.
- Precos dinamicos.

Entrega:

- Bancada so pode ser obtida e colocada por admin.
- Ouro converte por pepita, lingote e bloco sem arbitragem.
- Tesouraria limita compra de itens comuns.
- Preco dinamico altera por demanda e oferta.

## Fase 8 - Recuperacao e seguranca

Objetivo:

- Recuperacao de operacoes pendentes.
- Estornos.
- Concorrencia.
- Testes de duplicacao.
- Testes de reinicio.

Entrega:

- Reinicio nao duplica item ou dinheiro.
- Operacoes pendentes sao resolvidas.
- Rotinas diarias nao processam a mesma data duas vezes.

## Prioridade tecnica

Implementar primeiro o nucleo financeiro antes de telas sofisticadas:

```text
SQL -> dominio -> comandos simples -> itens/blocos -> menus -> fluxos completos
```

Isso permite validar seguranca e consistencia antes de expandir interfaces.
