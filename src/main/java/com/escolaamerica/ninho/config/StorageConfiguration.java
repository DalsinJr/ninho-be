package com.escolaamerica.ninho.config;

import com.escolaamerica.ninho.arquivo.service.StorageService;
import io.minio.MinioClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfiguration {

    @Bean
    MinioClient minioClient(NinhoProperties properties) {
        NinhoProperties.Storage storage = properties.getStorage();
        return MinioClient.builder()
            .endpoint(storage.getEndpoint())
            .credentials(storage.getAccessKey(), storage.getSecretKey())
            .build();
    }

    /** Aparece em /actuator/health como "storage". */
    @Bean
    HealthIndicator storageHealthIndicator(StorageService storage, NinhoProperties properties) {
        return () -> {
            String bucket = properties.getStorage().getBucket();
            try {
                return storage.bucketDisponivel()
                    ? Health.up().withDetail("bucket", bucket).build()
                    : Health.down().withDetail("bucket", bucket).withDetail("motivo", "bucket inexistente").build();
            } catch (Exception e) {
                return Health.down(e).withDetail("bucket", bucket).build();
            }
        };
    }
}
