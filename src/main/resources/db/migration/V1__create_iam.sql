-- SPEC §4.4 — acesso ao backoffice. Um usuário pode acumular papéis.
CREATE TABLE iam_usuario (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  nome          text NOT NULL,
  email         text NOT NULL CHECK (email = lower(email)),
  senha_hash    text NOT NULL,                 -- BCrypt
  ativo         boolean NOT NULL DEFAULT true,
  ultimo_acesso timestamptz,
  criado_em     timestamptz NOT NULL DEFAULT now(),
  atualizado_em timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_iam_usuario_email ON iam_usuario (email);

CREATE TABLE iam_usuario_papel (
  usuario_id uuid NOT NULL REFERENCES iam_usuario(id),
  papel      text NOT NULL CHECK (papel IN ('RH_ADMIN','RH_RECRUTADOR','GESTOR','DIRETOR','DPO')),
  PRIMARY KEY (usuario_id, papel)
);
