package com.escolaamerica.ninho.shared.api;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Pattern;

/**
 * Conversor do Logback que mascara e-mail e telefone na mensagem de log (SPEC §7.3): nenhum dado
 * pessoal em log, inclusive o que vier de bibliotecas. Usado em {@code logback-spring.xml}.
 */
public class MascaraPiiConverter extends ClassicConverter {

    private static final Pattern EMAIL =
        Pattern.compile("([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    /** Telefone brasileiro com DDD, com ou sem +55, parênteses, espaço e hífen; não pega pedaço de UUID. */
    private static final Pattern TELEFONE =
        Pattern.compile("(?<![\\w-])(?:\\+?55\\s?)?\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}(?![\\w-])");

    @Override
    public String convert(ILoggingEvent event) {
        return mascarar(event.getFormattedMessage());
    }

    public static String mascarar(String mensagem) {
        if (mensagem == null || mensagem.isEmpty()) {
            return mensagem;
        }
        String semEmail = EMAIL.matcher(mensagem).replaceAll("$1***@$2");
        return TELEFONE.matcher(semEmail).replaceAll("[telefone]");
    }
}
