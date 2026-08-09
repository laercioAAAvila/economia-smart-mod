# Checklist — Mapa, clãs, propriedades privadas, claims e permissões

> Upgrade progressivo, pagamento direto de `Dar Claim`, slots, autenticação visual e texturas passam a ser acompanhados em `CHK-DEV-32`.

## História e arquitetura

- [x] Registrar a história técnica consolidada.
- [x] Preservar as proteções específicas dos blocos comerciais.
- [x] Definir políticas extensíveis para blocos comuns, de dono, de grupo e de sistema.
- [x] Isolar parâmetros de negócio ainda indefinidos.

## Configuração e persistência

- [x] Adicionar configurações iniciais de clã, propriedade privada, claims e inatividade.
- [x] Limitar territórios independentes de clã e propriedade privada por configuração, com padrão 3 e mínimo 1.
- [x] Criar migration para grupos, membros, convites, permissões e contas coletivas.
- [x] Criar migration para claims, âncoras, localizações e relógio ativo.
- [x] Registrar migrations no catálogo e na limpeza administrativa.
- [x] Persistir tempo de servidor ativo e última atividade.

## Clãs e propriedades privadas

- [x] Implementar criação, entrada, convite, recusa, saída e encerramento no servidor.
- [x] Expor criação, convites, membros, permissões, banco e configurações pelas interfaces.
- [x] Validar unicidade de participação, limites, cargos e nomes.
- [x] Restringir convites a jogadores online no serviço usado pela interface.
- [x] Implementar cargos e permissões individuais do clã.
- [x] Implementar permissões padrão da propriedade privada.
- [x] Implementar sucessão e encerramento seguro por inatividade do clã.
- [x] Reutilizar contas, ledger e transações para tesouraria, fundo e conta da propriedade privada.

## Claims

- [x] Implementar criação e remoção server-side de claims.
- [x] Validar sobreposição, distância externa, limite e adjacência ortogonal.
- [x] Aplicar a distância configurável entre territórios separados da mesma propriedade privada.
- [x] Impedir remoção direta do chunk da âncora.
- [x] Direcionar novas expansões ao fluxo pago `Comprar chunk` do Bloco de Claim.
- [x] Recalcular componentes e remover territórios sem âncora.
- [x] Proteger colocação, quebra e interação em território reclamado.
- [x] Bloquear explosões e movimentos por pistão em território protegido.

## Blocos e propriedade

- [x] Registrar os blocos de claim de clã e propriedade privada.
- [x] Registrar blocos de gerenciamento e baús de clã/propriedade privada.
- [x] Implementar colocação, ativação e quebra das âncoras conforme cargo.
- [x] Adicionar receitas definidas e manter receitas de claim pendentes.
- [x] Corrigir recuperação perigosa de dono ao abrir Loja ou Correio sem SQL.
- [x] Permitir override de liderança somente no claim do próprio grupo.
- [x] Manter Bancada do Banco fora de qualquer override territorial.

## Mapa, localizações e chat

- [x] Registrar teclas configuráveis `M` e `P`.
- [x] Implementar mapa responsivo, arraste, zoom e camadas de jogador, chunks, claims e localizações.
- [x] Implementar CRUD e compartilhamento confirmado de localizações.
- [x] Implementar canais Geral, Clã e Propriedade Privada pela interface.
- [x] Implementar mensagem clicável que abre o mapa sem salvar automaticamente.

## Interfaces

- [x] Criar componentes reutilizáveis e navegação em cascata.
- [x] Implementar autenticação por cartão nos blocos de gerenciamento.
- [x] Implementar telas de clã, propriedade privada, membros, permissões, convites e upgrades.
- [x] Implementar telas de contas coletivas, tesouraria e fundo de apoio.
- [x] Garantir devolução do cartão e logout em Sair ou `ESC`.
- [x] Manter textos em UTF-8 e traduções em `pt_br`/`en_us`.

## Revisão

- [x] Revisar impactos nos sistemas existentes.
- [ ] Validar em jogo quando solicitado.
