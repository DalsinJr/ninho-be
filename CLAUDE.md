# ninho-be — mapa

Regras e fluxo de git: `AGENTS.md`. Fonte de verdade: `docs/spec_software_eav.md`.

## Onde fica cada coisa

| Caminho                                         | Conteúdo                                                   |
| ----------------------------------------------- | ---------------------------------------------------------- |
| `compose.yaml`                                  | Postgres :15440, MinIO :19000/:19001, Mailpit :11025/:18025 |
| `src/main/resources/application.yml`           | configuração; API na porta 18090                            |
| `src/main/resources/db/migration/`              | Flyway `V{n}__descricao.sql` (plano: SPEC §4.3)             |
| `src/main/java/com/escolaamerica/ninho/config/` | Security, CORS, OpenAPI, relógio (America/Sao_Paulo)        |
| `.../shared/`                                   | api (erros), domain, evento (outbox), job (fila), auditoria |
| `.../iam`, `estrutura`, `pessoa`, `recrutamento`, `triagem`, `lgpd`, `arquivo`, `notificacao`, `relatorio` | features (SPEC §3.3) |

## Comandos

```bash
./mvnw spring-boot:run   # sobe o Compose e a API
./mvnw test              # testes (Testcontainers: precisa de Docker)
```

## Estado

Fatia S1 (fundação e acesso) implementada no backend: migrations V1–V3, login por sessão (`/auth/*`), cadastro de usuários (`/usuarios`) com as regras da §7.1, `AutorizacaoService`, `AuditoriaService`, logs sem PII e `ArquiteturaTest`. Admin local: `admin@ninho.local` / `ninho123`.

Planos das fatias: `../../docs/spec/ninho/mvp/` (workspace). Próxima fatia: S2 (estrutura organizacional).

Testes de integração estendem `IntegracaoApiBase` (Testcontainers condicionado a Docker; helpers `sessaoDe`, `criarUsuario`). Sem jenv no shell, rodar com `JAVA_HOME` apontando para o JDK 21.
