-- SPEC §7.1 — uso LOCAL. Senha inicial: ninho123 (trocar em /app/perfil no primeiro acesso).
INSERT INTO iam_usuario (id, nome, email, senha_hash)
VALUES ('00000000-0000-0000-0000-000000000001', 'Administrador', 'admin@ninho.local',
        '$2a$10$YEoeN6Re/J6lmZE8maK0ouDwKxQPISMzkkr0OspC8HxfVD5jkBxM.');
INSERT INTO iam_usuario_papel (usuario_id, papel) VALUES
  ('00000000-0000-0000-0000-000000000001', 'RH_ADMIN'),
  ('00000000-0000-0000-0000-000000000001', 'DPO');
