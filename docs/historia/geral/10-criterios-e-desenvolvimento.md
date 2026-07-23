# Parte 10 — Critérios de aceite e desenvolvimento

Esta parte integra a história funcional completa do Mod de Economia para Minecraft Java 1.21.1 com NeoForge 21.1.234+. As regras gerais e referências entre módulos continuam válidas.

---

## 38. Critérios de aceite

### Conta

- Um jogador consegue criar apenas uma conta.
- O nome bancário é único.
- A senha não aparece nos logs.
- O saldo permanece após reiniciar o servidor.
- Conta bloqueada não movimenta dinheiro.

### Crédito e juros

- O principal e os juros são armazenados separadamente.
- O limite efetivo nunca ultrapassa o valor elegível pela faixa do saldo.
- A dívida total bloqueia parte do saldo.
- Não é possível sacar o valor que garante a dívida.
- Juros são aplicados uma única vez por cartão e por dia.
- O resto de cálculo evita arredondamento abusivo de dívidas pequenas.
- Dias desligados são recuperados sem duplicação.
- Pagamento parcial quita juros antes do principal.
- Fatura perdida pode ser reemitida.
- Pagamento de faturas segue da mais antiga para a mais nova.
- Existe botão para pagar todas as faturas em aberto.
- Cartão desativado continua com dívida e juros registrados.
- Juros podem ultrapassar o limite, mas não liberam novas compras.
- Crédito só é concedido quando o jogador pede crédito e não possui dívida aberta.
- Pedido de crédito respeita as faixas de 40%, 60%, 80% e 95% do saldo.

### Cartões

- Limites de quantidade são respeitados.
- Cartões possuem UUID.
- Outro jogador consegue usar o cartão.
- Cartão falso é recusado.
- Cartão mostra tipo, conta e limites aplicáveis na descrição.
- Cartão possui nome com número sequencial de criação e data.
- Cartão pode ser bloqueado pela lista do Caixa Eletrônico.
- Cartão desativado deixa de funcionar.
- Todas as cópias do mesmo cartão são desativadas juntas.

### Caixa Eletrônico

- Permite depósito.
- Permite saque.
- Não debita quando o inventário está cheio.
- Permite comprar cartões com taxa debitada do saldo disponível.
- Mostra lista rolável dos cartões criados.
- Permite bloquear e desativar cartões da conta.
- Mantém apenas a hotbar nas abas de Cartões e Crédito.
- Permite login por cartão e credenciais.
- Somente funções permitidas são mostradas em login por cartão.

### Loja de Venda

- Possui 9 slots de estoque.
- Possui caixa físico separado.
- Não vende sem estoque.
- Pagamento por cartão vai para a conta vinculada.
- Cartão de débito e crédito exige seleção explícita entre Débito e Crédito.
- Mostra mensagem simples quando a compra não puder ser concluída.
- Somente o proprietário configura.

### Loja de Compra

- Possui um item de referência.
- Respeita a quantidade máxima.
- Para quando fica sem dinheiro.
- Para quando fica sem saldo bancário.
- Para quando fica sem crédito.
- Não compra se o inventário estiver cheio.
- Bloqueia item diferente do item de referência.
- Permite vender tudo com shift sem estourar limite ou armazém.
- Permite retirar itens comprados enquanto a loja está ativa.
- Mostra mensagem simples quando a venda não puder ser concluída.

### Bancada do Banco, ouro e preços

- Não possui craft.
- Somente administrador consegue obtê-la e colocá-la.
- Possui 16 ofertas.
- Usa uma tesouraria SQL para itens comuns.
- Não compra item comum sem saldo na tesouraria.
- Aceita pepita, lingote e bloco de ouro com conversão exata.
- Vender ouro emite moeda lastreada e aumenta a Reserva de Ouro.
- Comprar ouro recolhe moeda e reduz a reserva.
- O bloco e o lingote não permitem lucro por conversão de crafting.
- Ofertas dinâmicas aumentam o preço quando há grande demanda diária.
- O preço pago pelo banco pode cair quando há excesso de oferta.
- Dias sem movimentação aproximam o preço do valor-base.
- A mesma rotina diária não é executada duas vezes.
- Ouro monetário tem preço fixo por padrão.
- Não vende nenhum item sem estoque.
- Não pode ser quebrada com estoque pela forma normal.

### SQL

- Operações utilizam transações.
- Linhas financeiras são bloqueadas durante alteração.
- Existe chave de idempotência.
- Nenhum saldo fica negativo.
- Transações concluídas não são apagadas.
- Estornos geram novas transações.
- Falha do SQL bloqueia operações.
- Reinício durante compra não faz o jogador perder dinheiro ou item.
- Juros diários possuem chave única por cartão e data.
- Estatísticas de preço possuem chave única por oferta e data.
- Emissões por ouro possuem lançamento contábil próprio.
- Reserva de ouro e níveis de preço nunca ficam negativos.

---

## 39. Ordem de desenvolvimento

### Fase 1 — Banco de dados

- Conexão SQL
- Migrações
- Contas do sistema
- Contas dos jogadores
- Ledger
- Auditoria

### Fase 2 — Dinheiro e contas

- Notas
- Comandos
- Login
- Senhas
- Saldo
- Depósito
- Saque

### Fase 3 — Caixa Eletrônico

- Bloco
- Interface
- Orientação
- Compra de cartões
- Sessões

### Fase 4 — Cartões, crédito e juros

- Débito
- Crédito
- Cartão combinado
- Limites
- Separação entre principal e juros
- Faturas
- Rotina diária de juros
- Pagamentos parciais
- Recuperação de dias pendentes
- Desativação

### Fase 5 — Loja de Venda

- Propriedade
- Estoque
- Caixa físico
- Troco
- Pagamentos bancários

### Fase 6 — Loja de Compra

- Item de referência
- Quantidade desejada
- Dinheiro físico
- Saldo bancário
- Financiamento por crédito

### Fase 7 — Bancada do Banco, ouro e mercado

- Comando de obtenção
- Permissões
- Ofertas
- Estoque
- Tesouraria
- Reserva de Ouro
- Emissão e resgate monetário
- Compra e venda
- Preço dinâmico
- Estatísticas diárias
- Recuperação gradual dos preços

### Fase 8 — Recuperação e segurança

- Idempotência
- Operações pendentes
- Recuperação após queda
- Concorrência
- Estornos
- Testes de duplicação
- Testes de juros após reinício
- Testes de compra atravessando níveis de preço
- Testes de conversão entre pepita, lingote e bloco
- Testes de emissão e resgate de moeda
