package com.escolaamerica.ninho.arquivo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.escolaamerica.ninho.TestcontainersConfiguration;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Import(TestcontainersConfiguration.class)
class StorageServiceTest {

    @Autowired
    StorageService storage;

    @Test
    void enviaBaixaPorUrlPreAssinadaERemove() throws Exception {
        String chave = "curriculos/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".pdf";
        byte[] conteudo = "%PDF-1.7 teste".getBytes(StandardCharsets.UTF_8);

        storage.enviar(chave, new ByteArrayInputStream(conteudo), conteudo.length, "application/pdf");
        assertThat(storage.existe(chave)).isTrue();

        String url = storage.urlDownload(chave, "Currículo Maria.pdf");
        HttpResponse<byte[]> resposta = HttpClient.newHttpClient()
            .send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body()).isEqualTo(conteudo);
        assertThat(resposta.headers().firstValue("Content-Disposition")).hasValueSatisfying(v -> assertThat(v).startsWith("attachment"));

        storage.remover(chave);
        assertThat(storage.existe(chave)).isFalse();
    }
}
