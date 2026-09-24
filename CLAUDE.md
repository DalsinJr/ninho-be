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

Fase F0 em andamento: esqueleto executável, sem migrations de domínio. MinIO já integrado (`arquivo/service/StorageService`, health "storage"). Próximo passo: `V1__create_iam.sql`, `V2__seed_usuario_admin.sql` e login (SPEC §7.1).
