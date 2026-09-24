package com.escolaamerica.ninho.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ninho")
public class NinhoProperties {

    private String frontendUrl;
    private Cors cors = new Cors();
    private Storage storage = new Storage();

    @Getter
    @Setter
    public static class Cors {

        /** Origens separadas por vírgula; aceita padrões (ex.: {@code http://localhost:*}). */
        private String allowedOrigins;
    }

    @Getter
    @Setter
    public static class Storage {

        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
        /** Validade das URLs pré-assinadas de download (SPEC D9: 5 minutos). */
        private Duration presignTtl = Duration.ofMinutes(5);
    }
}
