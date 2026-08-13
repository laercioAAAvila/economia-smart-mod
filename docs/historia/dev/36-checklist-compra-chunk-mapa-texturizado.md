# Checklist — Compra de chunk, taxas e mapa de superfície

Referência: `HIST-DEV-35`.

## Autenticação do Bloco de Claim

- [x] Exigir cartão de uma conta ativa pertencente ao jogador.
- [x] Proteger o cartão no menu durante a sessão.
- [x] Encerrar a autenticação ao fechar com `Sair`, `ESC` ou invalidar o menu.
- [x] Exigir nova autenticação em cada abertura.
- [x] Bloquear ações do claim enquanto a persistência SQL estiver indisponível.

## Compra de chunk

- [x] Mostrar contagem, limite e preço estimado do próximo chunk.
- [x] Disponibilizar `Comprar chunk` no claim de clã e propriedade privada.
- [x] Restringir a compra ao líder do clã ou proprietário privado.
- [x] Abrir o mapa centralizado na âncora que iniciou a compra.
- [x] Mostrar coordenadas e preço exato do chunk apontado.
- [x] Abrir confirmação com valor, `Confirmar`, `Cancelar` e aviso de irreversibilidade.
- [x] Revalidar dono, âncora, dimensão, limite, disponibilidade, distância e adjacência.
- [x] Exigir adjacência lateral ao mesmo território.
- [x] Debitar o cartão autenticado e creditar o tesouro.
- [x] Executar débito e gravação do claim na mesma transação SQL.
- [x] Reverter a cobrança se a gravação ou revalidação do claim falhar.
- [x] Não criar boleto nem dívida para uma nova compra de chunk.
- [x] Somar o preço pago ao valor patrimonial do terreno.
- [x] Vincular a sessão temporária ao jogador e à âncora.
- [x] Bloquear expansão gratuita pelo mapa comum.
- [x] Serializar alterações concorrentes do mesmo grupo.

## Taxas e boletos

- [x] Exibir taxa atual, total pendente, quantidade e intervalo.
- [x] Permitir imprimir a taxa atual.
- [x] Permitir consolidar todas as taxas pendentes em um boleto.
- [x] Aplicar taxa maior ao clã por multiplicador configurável, padrão `200%`.
- [x] Manter boletos de venda separados das taxas.
- [x] Preservar compatibilidade com boletos `LAND` já persistidos.
- [x] Impedir que novas compras de chunk gerem boleto `LAND`.

## Mapa e interface

- [x] Renderizar a superfície apenas de chunks carregados no cliente.
- [x] Usar cor do bloco superior e tonalidade do bioma quando disponível.
- [x] Representar água, vegetação, areia, pedra e construções pela amostra da superfície.
- [x] Variar a densidade de amostragem conforme o zoom.
- [x] Limitar a região amostrada e manter cache limitado de cores.
- [x] Limitar novas amostras por frame para evitar queda brusca de FPS.
- [x] Preservar grade para chunks não carregados.
- [x] Preservar camadas de claims, jogador e localizações.
- [x] Remover a camada de desfoque da tela e da confirmação.
- [x] Manter painel, informações e botões nítidos.
- [x] Adicionar textos em português e inglês.

## Permissões e administração

- [x] Manter lojas de compra e venda acessíveis ao público dentro ou fora de claims.
- [x] Preservar ao dono da loja o controle e a remoção do bloco comercial.
- [x] Permitir configurar por convidado privado `USAR`, `COLOCAR` e `REMOVER`.
- [x] Aplicar as permissões territoriais dos cargos e membros do clã.
- [x] Permitir que administrador com nível de permissão 2 quebre o Bloco de Claim.
- [x] Ao administrador quebrar a âncora, remover território e chunks vinculados.
- [x] Manter proteção contra explosões, pistões e falhas do banco.

## Persistência e diagnóstico

- [x] Registrar as migrações 25 e 26 no catálogo.
- [x] Preservar o checksum da migração 25 já publicada.
- [x] Aplicar o ajuste do vínculo de boletos consolidados somente na migração 26.
- [x] Bloquear operações financeiras quando a inicialização SQL falhar.
- [x] Registrar etapa de inicialização, destino seguro, SQLState e código SQL.
- [x] Registrar ação, UUID do jogador e UUID da âncora em falhas do claim.
- [x] Não registrar senha, hash, salt ou conteúdo sensível do cartão.
- [x] Persistir UUID econômico dentro de cada save do Minecraft.
- [x] Gerar novo UUID quando o mundo for apagado e recriado.
- [x] Escopar grupos, membros, convites, claims, âncoras, territórios e localizações.
- [x] Criar índices SQL por `server_uuid` para consultas de mapa e proteção.
- [x] Escopar blocos comerciais e sua unicidade de coordenadas por `server_uuid`.
- [x] Isolar por `server_uuid` o relógio de tempo ativo usado nas taxas.
- [x] Isolar registros legados sem associá-los automaticamente ao mundo novo.
- [x] Abrir o mapa com estado vazio até receber os dados do servidor atual.
- [x] Evitar consulta extra por tick, movimento ou frame para detectar troca de mundo.
- [x] Limpar sessão temporária de compra no logout e ao parar o servidor.
- [x] Atualizar chunks forçados e sessões após reset administrativo do banco.
- [x] Tornar atualizações em lote dos inventários comerciais atômicas em caso de conflito.

## Validação manual pendente

- [ ] Comprar um chunk adjacente para clã e propriedade privada.
- [ ] Tentar chunk diagonal, ocupado, distante e de outra dimensão.
- [ ] Confirmar cancelamento sem cobrança e compra confirmada no débito.
- [ ] Confirmar que não é emitido boleto após a compra.
- [ ] Verificar taxa atual, boleto consolidado e multiplicador do clã.
- [ ] Validar água, vegetação e construções no mapa em diferentes níveis de zoom.
- [ ] Confirmar ausência de desfoque e queda anormal de FPS.
- [ ] Quebrar cada Bloco de Claim como administrador e confirmar a remoção territorial.
