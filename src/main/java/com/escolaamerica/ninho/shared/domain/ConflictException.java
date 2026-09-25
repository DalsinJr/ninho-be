package com.escolaamerica.ninho.shared.domain;

/** Conflito com o estado atual (unicidade, versão): responde 409 com a mensagem do serviço (SPEC §5.1). */
public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}
