# Checklist — Atualização de claims e propriedades privadas

> O novo pagamento inicial de `Dar Claim` e o upgrade de chunks passam a ser acompanhados em `CHK-DEV-32`; os itens concluídos abaixo registram o fluxo anterior.

## História e configuração

- [x] Registrar a atualização funcional consolidada.
- [x] Atualizar a arquitetura principal com o novo modelo de propriedade.
- [x] Configurar preços por dimensão e distância.
- [x] Configurar preço, dias padrão e limite máximo da âncora.

## Propriedade privada e clã

- [x] Remover o conceito de líder da Propriedade Privada e registrar proprietário.
- [x] Permitir que qualquer jogador coloque uma âncora privada pendente.
- [x] Validar posição, sobreposição, distância e limite antes do claim.
- [x] Abrir tela de informações antes de criar o território.
- [x] Manter o claim do clã pendente até confirmação pela interface.
- [x] Persistir preço e dívida vinculados ao território.
- [x] Permitir membros convidados por propriedade.

## Âncora

- [x] Calcular o preço configurável da âncora.
- [x] Gerar boleto pela quantidade de dias escolhida.
- [x] Ativar carregamento do chunk somente após pagamento.
- [x] Permitir renovação sem ultrapassar o limite configurado.
- [x] Preservar dias pagos depois da venda.

## Boletos, venda e crédito

- [x] Persistir boletos de terreno, âncora e venda.
- [x] Permitir pagamento no Caixa Eletrônico.
- [x] Gerar venda com comprador, preço sugerido e preço editável.
- [x] Transferir propriedade e dívidas somente após pagamento confirmado.
- [x] Bloquear solicitação de crédito quando houver dívida de terreno.

## Interface e revisão

- [x] Implementar telas de informações, âncora e venda com `Voltar` e `ESC`.
- [x] Manter validações sensíveis no servidor e operações idempotentes.
- [x] Revisar impactos nos claims, proteções, contas e cartão de crédito.
- [ ] Validar em jogo quando solicitado.
