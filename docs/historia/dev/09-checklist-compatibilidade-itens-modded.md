# Checklist de compatibilidade com itens de outros mods

Codigo: `CHK-DEV-09`
Historia base: `HIST-DEV-04`, `HIST-DEV-05`
Historias gerais relacionadas: `HIST-GERAL-05`, `HIST-GERAL-10`
Escopo: compatibilidade de lojas com itens, componentes e dados de outros mods.

## Lojas

- [x] Confirmar suporte a `item_id` com namespace de outros mods.
- [x] Preservar componentes completos do `ItemStack` em estoque/ofertas.
- [x] Restaurar itens vendidos/comprados com os componentes originais.
- [x] Manter comparacao por componentes para itens especiais.

## Modularizacao

- [x] Isolar serializacao de `ItemStack` em servico pequeno.
- [x] Evitar aumentar servicos de loja.
