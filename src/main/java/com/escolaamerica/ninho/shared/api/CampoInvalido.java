package com.escolaamerica.ninho.shared.api;

/**
 * Campo recusado numa validação (SPEC §5.1). {@code code} é um de {@code OBRIGATORIO}, {@code TAMANHO},
 * {@code FORMATO} ou {@code INVALIDO}.
 */
public record CampoInvalido(String field, String code) {

    public static final String OBRIGATORIO = "OBRIGATORIO";
    public static final String TAMANHO = "TAMANHO";
    public static final String FORMATO = "FORMATO";
    public static final String INVALIDO = "INVALIDO";

    /** Traduz o nome da anotação de Bean Validation (ex.: {@code NotBlank}) no código da §5.1. */
    public static String codigoDaRestricao(String restricao) {
        if (restricao == null) {
            return INVALIDO;
        }
        return switch (restricao) {
            case "NotNull", "NotBlank", "NotEmpty" -> OBRIGATORIO;
            case "Size", "Min", "Max", "DecimalMin", "DecimalMax", "Length" -> TAMANHO;
            case "Email", "Pattern" -> FORMATO;
            default -> INVALIDO;
        };
    }
}
