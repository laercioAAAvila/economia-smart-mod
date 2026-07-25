# Checklist - Bloco Correio

## Registro e assets

- [x] Registrar bloco, item, menu e BlockEntity do Correio.
- [x] Adicionar o Correio na aba criativa e nos nomes localizados.
- [x] Criar blockstate, modelos, receita e texturas do Correio.

## Persistencia

- [x] Registrar o Correio como tipo comercial proprio.
- [x] Persistir dono, nome do dono, conta vinculada, numero da conta, dimensao e coordenadas.
- [x] Criar armazenamento dos destinatarios por Correio.
- [x] Criar inventario persistente de recebimento com 18 slots.

## Bloco e permissoes

- [x] Tornar o jogador que colocou o bloco dono do Correio.
- [x] Permitir quebra somente para quem colocou ou jogador em modo criativo.
- [x] Abrir a tela de nome somente para o dono enquanto o Correio estiver sem nome.

## Interface

- [x] Criar tela de nome inicial com confirmar e cancelar.
- [x] Criar tela principal com pesquisa, adicionar e lista de destinatarios.
- [x] Criar tela de envio com destinatario, 18 slots, pagamento e enviar.
- [x] Criar modal de pagamento com 1 slot de cartao e 6 slots de dinheiro.
- [x] Criar aviso de troco sem cartao com voltar e confirmar.
- [x] Manter slots e textos contidos nos limites da tela.

## Regras de destinatarios

- [x] Buscar Correios por nome somente na mesma dimensao.
- [x] Mostrar erro quando a caixa nao existir ou estiver em outra dimensao.
- [x] Diferenciar internamente por nome e coordenadas, sem mostrar coordenadas na interface.
- [x] Permitir adicionar por qualquer jogador.
- [x] Permitir excluir apenas pelo dono do Correio de origem.

## Envio

- [x] Criar 18 slots temporarios de envio.
- [x] Retornar itens temporarios ao fechar/cancelar antes do envio.
- [x] Validar destinatario selecionado, itens, pagamento e espaco.
- [x] Transferir itens para os 18 slots de recebimento do destinatario.
- [x] Permitir retirada de recebimentos apenas pelo dono.

## Pagamento

- [x] Configurar valor por slot ocupado.
- [x] Calcular distancia horizontal usando X/Z.
- [x] Aplicar faixas progressivas de acrescimo.
- [x] Pagar com dinheiro exato.
- [x] Pagar com dinheiro acima do valor e trocar via cartao.
- [x] Avisar quando houver troco sem cartao e permitir confirmar para conta do dono.
- [x] Pagar com cartao em debito ou credito.
- [x] Retornar cartao e dinheiro temporario quando aplicavel.

## Revisao

- [x] Revisar impactos em registros, migracoes, rede e menus.
- [ ] Validar em jogo nas resolucoes da historia.

## Correcao de interface e pagamento

- [x] Bloquear fechamento pela tecla de inventario enquanto inputs do Correio estiverem focados.
- [x] Remover hover/click/render de slots escondidos em todas as telas e modais.
- [x] Ocultar slots de envio/recebimento/inventario nas telas onde eles nao pertencem.
- [x] Reposicionar/desenhar slots do pagamento acima do fundo do modal.
- [x] Garantir 1 slot de cartao e 6 slots de dinheiro visiveis no pagamento.
- [x] Enviar a encomenda automaticamente quando o pagamento for concluido.
- [x] Revisar alinhamento das paginas e modal apos os ajustes.

## Correcao pagina de envio

- [x] Remover slots de recebidos da tela apos escolher destinatario.
- [x] Reposicionar slots de recebidos para caberem no card da tela principal.
- [x] Remover o botao Enviar da tela de envio.
- [x] Fechar o modal de pagamento e retornar aos slots de envio apos pagamento/envio.
- [x] Sincronizar limpeza dos slots de envio depois do envio automatico.

## Correcao modal de troco e pagamento

- [x] Fazer Voltar do aviso de troco fechar o aviso e abrir o pagamento.
- [x] Bloquear shift-click de dinheiro/cartao para slots de pagamento quando o modal nao estiver aberto.
- [x] Ampliar modal de pagamento para a esquerda.
- [x] Reposicionar valor total para nao sobrepor Cartao.
- [x] Corrigir acentos dos textos pt_br do Correio.
- [x] Revisar estados de abrir/fechar pagamento entre cliente e servidor.

## Alinhamento modal de pagamento

- [x] Separar coluna de texto, slots e botoes no modal.
- [x] Mover slot de cartao para nao encostar nos botoes.
- [x] Mover 6 slots de notas para nao ficarem sob os botoes.
- [x] Manter valor total livre de sobreposicao com Cartao e Notas.
- [x] Revisar visual do modal contra o inventario do jogador.

## Correcao Correio, bancada e historico do caixa

- [x] Expandir o modal de pagamento do Correio para a esquerda.
- [x] Realinhar Total, Cartao, Notas e botoes sem sobreposicao.
- [x] Ajustar o slot de cartao do menu para a nova posicao visual.
- [x] Adicionar 3px de espaco entre Banco compra e Resgatar ouro.
- [x] Criar historico de operacoes da conta no Caixa Eletronico.
- [x] Mostrar entradas e saidas de troco da loja, troco do Correio, transferencias e saques.
- [x] Limitar o historico exibido aos ultimos 5 dias uteis reais.
- [x] Limpar o historico exibido quando a sessao do caixa terminar.

## Alinhamento final do pagamento

- [x] Remover o card escuro interno do pagamento.
- [x] Usar o card padrao da tela do Correio como fundo do pagamento.
- [x] Realinhar texto, slot de cartao, slots de notas e botoes no card padrao.

## Permissao e drop de blocos comerciais

- [x] Bloquear quebra da Bancada do Banco para nao administradores.
- [x] Permitir quebra de Loja de Compra, Loja de Venda, Caixa Eletronico e Correio apenas para quem colocou ou modo criativo.
- [x] Usar o jogador que colocou como dono do Caixa Eletronico.
- [x] Manter fallback para blocos antigos usando quem colocou o bloco.
- [x] Adicionar drop do proprio item para Loja de Compra, Loja de Venda, Caixa Eletronico, Bancada do Banco e Correio.

## Ajuste final de quebra e drops

- [x] Permitir quebrar a Bancada do Banco somente em modo criativo.
- [x] Permitir quebrar Loja de Compra, Loja de Venda, Correio e Caixa Eletronico somente para quem colocou ou em modo criativo.
- [x] Dropar itens persistidos dos inventarios comerciais antes de remover o bloco.
- [x] Limpar slots persistidos apos preparar os drops para evitar duplicacao.
- [x] Manter drop do proprio bloco por loot table.
- [x] Ajustar tempo de quebra do Caixa Eletronico para ficar igual ao das lojas.
