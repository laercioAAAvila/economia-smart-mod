# Checklist ATM, Lojas e JEI

Codigo: `CHK-DEV-14`
Historia base: `HIST-DEV-04`, `HIST-DEV-05`
Historias gerais relacionadas: `HIST-GERAL-03`, `HIST-GERAL-04`, `HIST-GERAL-05`
Escopo: slots de cartao, senha, lojas e integracao JEI.

- [x] Restringir slot da aba Credito para cartoes com funcao credito.
- [x] Restringir slot da aba Seguranca para cartao bloqueado da propria conta.
- [x] Sincronizar contexto do slot de cartao entre cliente e servidor.
- [x] Forcar logout apos troca de senha.
- [x] Limitar senha para 4 a 12 caracteres.
- [x] Mostrar aviso de tamanho da senha na interface.
- [x] Desativar oferta de venda quando acabar o estoque.
- [x] Desativar oferta de compra quando o estoque de itens comprados ficar cheio.
- [x] Desativar botao de compra/venda quando a operacao nao couber no estoque.
- [x] Integrar JEI para definir item referencia sem possuir o item.

Observacao: JEI foi adicionado como dependencia opcional. Na loja de compra, o dono pode arrastar um item do JEI para o slot de referencia sem consumir item do inventario; o servidor valida dono, tipo da loja e oferta desativada antes de aceitar.
