# Checklist - Cartoes, tooltip e feedback de lojas

Codigo: `CHK-DEV-23`
Historia base: `HIST-DEV-04`, `HIST-DEV-05`
Historias gerais relacionadas: `HIST-GERAL-03`, `HIST-GERAL-05`
Escopo: descricao de cartoes, selecao de pagamento em lojas e mensagens de erro.

- [x] Cartao: gravar limite de credito no item quando o cartao for criado.
- [x] Cartao: atualizar limite de credito gravado no item ao ajustar limite no ATM.
- [x] Cartao: mostrar tipo do cartao na descricao.
- [x] Cartao: mostrar limite de credito na descricao quando possuir funcao credito.
- [x] Loja de venda: adicionar seletor Debito/Credito para cartao com as duas funcoes.
- [x] Loja de venda: aplicar a forma de pagamento selecionada no calculo de compra e no pagamento real.
- [x] Loja de compra: manter mensagens claras para item errado, limite atingido, armazem cheio ou loja sem saldo.
- [x] Ambas lojas: permitir clique no botao de troca para retornar erro simples quando a operacao nao puder concluir.
- [x] Revisar referencias de payload/menu para manter compatibilidade com chamadas antigas.
- [x] Validar JSON de idioma e checagem basica de diff.
