# Checklist - Lojas, cartoes, fatura e UI

Codigo: `CHK-DEV-17`
Historia base: `HIST-DEV-02`, `HIST-DEV-03`, `HIST-DEV-04`, `HIST-DEV-05`
Historias gerais relacionadas: `HIST-GERAL-03`, `HIST-GERAL-04`, `HIST-GERAL-05`, `HIST-GERAL-06`
Escopo: compras em lote, limites de loja, cartoes, fatura e ajustes de UI.

- [x] Loja de compra: permitir shift + vender para processar tudo sem passar do limite de compra.
- [x] Loja de compra: impedir lote que estoure o armazem de itens comprados.
- [x] Loja de compra: permitir ao dono retirar itens comprados mesmo com a loja ativa.
- [x] Loja de venda: mostrar estoque disponivel para o comprador.
- [x] Loja de venda: permitir campo opcional de quantidade para compra.
- [x] Loja de venda: permitir shift + comprar tudo sem passar do dinheiro/credito disponivel.
- [x] Verificar suporte monetario acima de trilhoes.
- [x] Remover corte de preco da loja em 32 bits para aceitar valores altos.
- [x] Cartao de debito: adicionar limite diario configuravel no ATM e descricao no item.
- [x] Cartao de credito: manter compras de credito sem autorizar debito.
- [x] Pedido de credito: aplicar faixas de 40%, 60%, 80% e 95% sem concessao automatica.
- [x] ATM: adicionar pagamento de fatura na aba de credito respeitando janela configuravel antes do vencimento.
- [x] ATM: adicionar item e slot de fatura para pagar somente a fatura da conta conectada.
- [x] ATM: manter fatura pagavel depois do dia de vencimento dentro do mesmo mes.
- [x] Bancada do Banco: ajustar ordem dos textos e posicao do valor pago estimado.
- [x] Revisar alinhamento das interfaces afetadas.
- [x] Revisar impacto final sem alterar arquivos fora do escopo.
