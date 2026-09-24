# ninho-be

Backend do **Ninho** — Plataforma de Talentos Escola América.

## Pré-requisitos

- JDK 21 (Temurin); o Maven Wrapper cuida do Maven
- Docker com Compose v2

## Subir

```bash
./mvnw spring-boot:run
```

O Spring sobe o `compose.yaml` (Postgres, MinIO e Mailpit) e aplica as migrations.

| Serviço          | Endereço                               |
| ---------------- | -------------------------------------- |
| API + Swagger    | http://localhost:18090/swagger-ui.html |
| Health           | http://localhost:18090/actuator/health |
| Console do MinIO | http://localhost:19001 (ninho / ninho12345) |
| Caixa de e-mails | http://localhost:18025                 |

Especificação: [docs/spec_software_eav.md](docs/spec_software_eav.md).
