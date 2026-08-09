# Checklist — Compra de chunk e mapa texturizado

Referência: `HIST-DEV-35`.

## Blocos de Claim

- [x] Mostrar contagem e limite de chunks.
- [x] Mostrar o preço estimado do próximo chunk.
- [x] Adicionar `Comprar chunk` ao claim do clã e da propriedade privada.
- [x] Restringir o botão ao responsável e à capacidade disponível.

## Seleção no mapa

- [x] Abrir o mapa centralizado na âncora que iniciou a compra.
- [x] Exibir botão para cancelar.
- [x] Destacar o chunk sob o ponteiro.
- [x] Mostrar coordenadas e preço exato do chunk apontado.
- [x] Permitir arraste com o botão direito e zoom durante a seleção.
- [x] Encerrar o modo de seleção depois do clique de compra.

## Servidor e persistência

- [x] Revalidar dono, dimensão, limite, disponibilidade e distância no servidor.
- [x] Exigir adjacência lateral ao território da âncora escolhida.
- [x] Bloquear adição gratuita pelo mapa comum.
- [x] Somar o preço ao valor e à dívida do terreno.
- [x] Criar e entregar um boleto `LAND` por compra.
- [x] Fazer cada boleto reduzir somente sua parcela da dívida.
- [x] Serializar compras concorrentes pelo grupo.

## Texturas e interface

- [x] Renderizar texturas de superfície dos chunks carregados.
- [x] Aumentar a amostragem visual conforme o zoom.
- [x] Manter grade para chunks não carregados.
- [x] Preservar camadas de claims, jogador e localizações.
- [x] Adicionar textos em português e inglês.

## Validação manual

- [ ] Comprar um chunk adjacente para clã e propriedade privada.
- [ ] Tentar chunk diagonal, ocupado, distante e de outra dimensão.
- [ ] Confirmar bloqueio ao atingir o limite.
- [ ] Confirmar entrega, reemissão e pagamento de dois boletos de chunks.
- [ ] Confirmar que remover um chunk não remove a âncora.
- [ ] Verificar mapa em zoom mínimo e máximo com chunks carregados e não carregados.

