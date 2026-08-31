# Validacao do pacote 0.5.1

Data: 2026-08-31

## Concluido

- SQLite baseline executado em banco vazio.
- `PRAGMA integrity_check`: `ok`.
- `PRAGMA foreign_key_check`: sem violacoes.
- Schema SQLite: 30 tabelas e 51 indices (mesmo conjunto final de indices esperado das migrations PostgreSQL, com equivalentes SQLite quando necessario).
- Testada unicidade de `username_normalized` por `server_uuid` em SQLite.
- Funcoes Java independentes compiladas e executadas com Java 21:
  - aliases `sql/sqlite` e `pgsql/postgresql`;
  - adaptacao `FOR UPDATE` e `LEAST/GREATEST`;
  - fingerprint SHA-256 deterministico;
  - validacao de idempotency key;
  - splitter de migration preservando `;` em strings/comentarios/dollar blocks.
- Varredura de todas as fontes Java com `javac` sem classpath NeoForge nao encontrou diagnosticos de sintaxe; os erros restantes sao simbolos/dependencias Minecraft/NeoForge ausentes nesse modo de validacao.
- Nenhuma referencia restou para os servicos legados de loja removidos.
- Nenhum marcador de conflito de merge encontrado.
- Arquivos `run/`, logs e bancos locais removidos do pacote final.

## Limitacao do ambiente

A tentativa de `./gradlew compileJava --no-daemon` nao chegou a iniciar o Gradle porque o wrapper precisava baixar `gradle-9.3.0-bin.zip` e o ambiente nao possui resolucao/acesso a `services.gradle.org` (`UnknownHostException`).

Antes de publicar o JAR, execute em uma maquina com rede/cache Gradle:

```bash
./gradlew clean build
```

Essa limitacao nao invalida os testes locais acima, mas significa que o pacote fonte nao deve ser anunciado como "Gradle build confirmado" ate essa compilacao ser feita.

## Website / token web - 0.5.1
- Login web nao envia senha: usa ticket de uso unico gerado no ATM.
- Ticket testado com Java 21: 16 caracteres/80 bits, expira e nao pode ser reutilizado.
- Bearer testado com Java 21: 32 bytes aleatorios (256 bits), Base64 URL-safe.
- JavaScript de `web/js/` validado com `node --check`.
- Referencias HTML/CSS/JS e assets verificadas localmente.
- Todas as paginas/arquivos estaticos principais responderam HTTP 200 em servidor local.
- Website nao possui paginas de saque, deposito ou troca de senha.
- API web continua desativada por padrao e limitada a PostgreSQL + bind loopback.
