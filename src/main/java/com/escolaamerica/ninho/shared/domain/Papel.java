package com.escolaamerica.ninho.shared.domain;

/** Papéis do backoffice e seu escopo (SPEC §7.2). Um usuário pode acumular papéis. */
public enum Papel {
    RH_ADMIN("Tudo, incluindo usuários, estrutura e configuração de IA"),
    RH_RECRUTADOR("Vagas, banco de talentos, pipeline, candidaturas e decisões"),
    GESTOR("Requisições próprias; nas vagas que gere, só as etapas liberadas ao gestor"),
    DIRETOR("Aprovação de requisições e relatórios"),
    DPO("LGPD: pedidos de titulares, exportação, anonimização, retenção e auditoria");

    private final String descricao;

    Papel(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
