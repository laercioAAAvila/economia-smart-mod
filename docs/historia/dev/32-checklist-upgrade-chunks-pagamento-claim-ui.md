# Checklist — Upgrade de chunks, pagamento do claim e UI

Referência: `HIST-DEV-31`.

## Configuração e cálculo

- [x] Adicionar `claimMinChunks` com padrão `4`.
- [x] Adicionar `claimMaxChunks` com padrão `20`.
- [x] Adicionar `claimUpgradeBasePrice` com padrão `10000`.
- [x] Adicionar `claimUpgradeMinPercentage` com padrão `10`.
- [x] Adicionar `claimUpgradeMaxPercentage` com padrão `30`.
- [x] Validar relações entre mínimo, máximo e percentuais.
- [x] Remover preços e limites antigos que contradigam a configuração vigente.
- [x] Calcular a quantidade total de upgrades sem valores fixos.
- [x] Interpolar o percentual do mínimo ao máximo.
- [x] Calcular cada preço sobre o preço do nível anterior.
- [x] Usar cálculo monetário determinístico sem `float` ou `double`.
- [x] Definir arredondamento para valores monetários inteiros.
- [x] Tratar zero ou um upgrade sem divisão por zero.
- [x] Recalcular a sequência quando a configuração mudar.

## Persistência e segurança do upgrade

- [x] Persistir o limite atual de chunks de cada grupo.
- [x] Recalcular preço e percentual no servidor antes da cobrança.
- [x] Validar cargo ou proprietário autorizado no servidor.
- [x] Implementar compra idempotente do upgrade.
- [x] Impedir cobrança ou concessão duplicada por repetição de pacote.
- [x] Tratar concorrência entre duas compras do mesmo nível.
- [x] Bloquear upgrade ao alcançar `claimMaxChunks`.
- [x] Atualizar a interface após compra aprovada ou preço invalidado.
- [x] Registrar a operação financeira e sua auditoria.

## Tela de upgrade

- [x] Exibir limite atual e próximo limite.
- [x] Exibir percentual do próximo nível.
- [x] Exibir preço calculado do próximo nível.
- [x] Habilitar `Comprar Upgrade` somente quando a compra for válida.
- [x] Exibir estado de limite máximo e desabilitar a compra.
- [x] Manter `Voltar` e `ESC` no fluxo correto.
- [x] Reutilizar o componente de pagamento existente.

## Pagamento do claim

- [x] Fazer `Dar Claim` abrir o modal de pagamento antes da ativação.
- [x] Manter a âncora pendente enquanto o pagamento não for aprovado.
- [x] Oferecer escolha entre cartão e dinheiro.
- [x] No cartão, exigir escolha explícita entre crédito e débito.
- [x] Validar propriedade, conta, cartão, saldo e limite no servidor.
- [x] No dinheiro, considerar somente itens inseridos no menu.
- [x] Preservar a regra de pagamento exato e sem troco.
- [x] Não retirar dinheiro do inventário normal do jogador.
- [x] Devolver cartão e dinheiro ao cancelar ou fechar.
- [x] Ativar o território somente após confirmação financeira idempotente.
- [x] Recuperar falha posterior ao pagamento sem perda financeira.
- [x] Definir compatibilidade com boletos de terreno pendentes já persistidos.

## Inventário e autenticação

- [x] Criar componente reutilizável para desenhar slots do Minecraft.
- [x] Desenhar individualmente slots de cartão, dinheiro e itens.
- [x] Garantir alinhamento entre slot visual e slot lógico do menu.
- [x] Ocultar e desativar o slot de cartão após autenticação.
- [x] Não renderizar o item autenticador na tela autenticada.
- [x] Manter o cartão protegido no servidor até o fechamento.
- [x] Devolver o cartão em `Sair`, `ESC` ou invalidação do menu.
- [x] Revisar as telas de Clã e Propriedade Privada mostradas nas referências visuais.

## Texturas e recursos

- [x] Inventariar todos os novos blocos e itens sem textura própria.
- [x] Criar texturas finais para Clã, Propriedade Privada e Claims.
- [x] Remover placeholders e recursos de depuração.
- [x] Validar modelos, blockstates, itens, loot tables e receitas.
- [x] Confirmar que nenhuma textura existente foi substituída ou quebrada.

## Responsividade e validação

- [x] Reutilizar botões e componentes equivalentes entre os fluxos.
- [x] Validar textos longos em português e inglês.
- [x] Validar escalas de GUI e resoluções suportadas.
- [x] Confirmar ausência de sobreposição, corte e controles fora da tela.
- [x] Validar visualmente todos os slots interativos.
- [x] Validar pagamento de claim por crédito, débito e dinheiro.
- [x] Validar progressão completa do limite mínimo ao máximo.
- [x] Validar configurações alternativas às configurações padrão.
- [ ] Executar validação em jogo quando solicitada.
