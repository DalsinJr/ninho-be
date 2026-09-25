package com.escolaamerica.ninho.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** Formato único de erro da API (SPEC §5.1). {@code fields} só aparece em erro de validação. */
public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<CampoInvalido> fields
) {
}
