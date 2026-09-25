-- SPEC §4.9 — trilha de auditoria. Nunca guarda valor de dado pessoal: campos de PII aparecem
-- só pelo NOME em antes/depois (ex.: {"telefone": "[alterado]"}), pelo RedatorPii.
-- As demais tabelas sys_ entram nas fatias que as usam (plano, ID-01).
CREATE TABLE sys_auditoria (
  id            bigserial PRIMARY KEY,
  usuario_id    uuid REFERENCES iam_usuario(id),
  acao          text NOT NULL,          -- "iam.usuario.papeis", "lgpd.pessoa.exportar"
  entidade      text NOT NULL,
  entidade_id   uuid,
  antes         text,                   -- JSON redigido
  depois        text,                   -- JSON redigido
  justificativa text,
  criado_em     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_sys_auditoria_entidade ON sys_auditoria (entidade, entidade_id, criado_em DESC);
