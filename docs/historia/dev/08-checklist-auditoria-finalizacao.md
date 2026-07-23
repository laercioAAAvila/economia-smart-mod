# Checklist de auditoria e finalizacao

Codigo: `CHK-DEV-08`
Historia base: `HIST-DEV-02`, `HIST-DEV-03`, `HIST-DEV-05`
Historias gerais relacionadas: `HIST-GERAL-05`, `HIST-GERAL-08`, `HIST-GERAL-10`
Escopo: auditoria de consistencia financeira, lojas, precos, ouro e finalizacao.

## Consistencia financeira

- [x] Corrigir recuperacao de deposito com SQL commitado antes da remocao de notas.
- [x] Corrigir recuperacao de saque com SQL commitado antes da entrega de notas.
- [x] Evitar pagamento da Loja de Compra quando o item nao for recebido.
- [x] Evitar venda da Loja de Venda quando o dinheiro nao for removido do jogador.

## Lojas e precos

- [x] Manter preco das lojas definido pelo jogador/proprietario.
- [x] Aplicar preco dinamico somente nas ofertas do banco.
- [x] Atualizar estatisticas de oferta com novo nivel de demanda/oferta.
- [x] Criar caminho tecnico para vincular conta/cartao da loja.
- [x] Criar caminho tecnico para configurar ofertas de loja.

## Interface e seguranca

- [x] Remover senha trafegando por comando de chat.
- [x] Rebaixar login por comando/tela atual para modo temporario nao seguro.
- [x] Restringir comandos de ouro a jogador diante da Bancada do Banco.
- [x] Validar menus por distancia/bloco quando possivel.

## Modularizacao

- [x] Reduzir `CommercialBlockEvents`.
- [x] Evitar crescimento de handlers de comando.
- [x] Manter novos servicos abaixo de tamanho moderado.
