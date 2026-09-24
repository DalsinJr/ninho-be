# Ninho BE

Este arquivo vale para todo o repositório.

## Fluxo de Git

**Sempre commitar e push direto na `main`.** Não crie branches de feature, não abra PR, não faça merge.

- `git commit` direto em `main`, seguido de `git push origin main`.
- Só crie um branch separado se o usuário pedir **explicitamente** ("crie um branch", "abra um PR", "trabalhe em uma feature branch"). Sem essa instrução clara, o default é `main`.
- Se você se encontrar em um branch que não é `main` sem ter sido instruído, pare e pergunte antes de continuar.

## Documentação obrigatória

Fonte de verdade: `docs/spec_software_eav.md` (SPEC v2.0). O mesmo arquivo é copiado para `ninho-fe/docs/`.

Prioridade:

1. regra de negócio desta SPEC
2. contrato e experiência
3. nada mais

Toda decisão estrutural nova é registrada na §2 da SPEC antes de ser implementada.

## Stack alvo

Java 21 · Spring Boot 3.5.6 · Spring Web/Security/Validation/Data JPA · PostgreSQL 17 · Flyway · OpenAPI (springdoc) · Testcontainers. Versões e bibliotecas por fase: SPEC §3.2.

## Regras obrigatórias

- backend é a fonte de verdade para autorização e regra de negócio
- monolito por feature (`api`, `domain`, `repository`, `service`); uma feature nunca escreve pelo `repository` de outra (SPEC §3.4)
- não existe `candidato` nem `funcionario`: existe `core_pessoa` (SPEC §4.1)
- a IA nunca reprova: toda decisão é humana e registrada (SPEC D6)
- o Portal nunca revela existência, quantidade ou detalhe de vagas (SPEC D7)
- nenhum dado pessoal em log; chave de IA só em variável de ambiente
- migrations Flyway são imutáveis depois de aplicadas; correção entra em nova versão
- nenhum `DELETE` físico em tabela de negócio (SPEC §4.2)
- documentar no OpenAPI qualquer mudança material de contrato
- cada fase inclui pelo menos um smoke E2E com Testcontainers cobrindo o fluxo crítico liberado nela
- se o ambiente local não tiver Docker, manter o teste condicionado em vez de remover a cobertura
