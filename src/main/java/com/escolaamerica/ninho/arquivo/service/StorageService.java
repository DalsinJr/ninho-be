package com.escolaamerica.ninho.arquivo.service;

import com.escolaamerica.ninho.config.NinhoProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import java.io.InputStream;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Arquivos no bucket privado do MinIO (SPEC D9). Nenhum objeto público: download só por URL
 * pré-assinada, com {@code Content-Disposition: attachment}. A chave do objeto é gerada pelo
 * servidor; o nome original serve só para exibição.
 */
@Service
@RequiredArgsConstructor
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final MinioClient minio;
    private final NinhoProperties properties;

    /** O Compose já cria o bucket (minio-init); isto cobre ambientes sem ele, como os testes. */
    @EventListener(ApplicationReadyEvent.class)
    public void garantirBucket() throws Exception {
        String bucket = bucket();
        if (!minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Bucket {} criado", bucket);
        }
    }

    public boolean bucketDisponivel() throws Exception {
        return minio.bucketExists(BucketExistsArgs.builder().bucket(bucket()).build());
    }

    public void enviar(String chave, InputStream conteudo, long tamanho, String contentType) throws Exception {
        minio.putObject(PutObjectArgs.builder()
            .bucket(bucket())
            .object(chave)
            .stream(conteudo, tamanho, -1)
            .contentType(contentType)
            .build());
    }

    public boolean existe(String chave) throws Exception {
        try {
            minio.statObject(StatObjectArgs.builder().bucket(bucket()).object(chave).build());
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            throw e;
        }
    }

    public String urlDownload(String chave, String nomeExibicao) throws Exception {
        String disposition = "attachment; filename=\"" + nomeExibicao.replace("\"", "") + "\"";
        return minio.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket(bucket())
            .object(chave)
            .expiry((int) properties.getStorage().getPresignTtl().toSeconds())
            .extraQueryParams(Map.of("response-content-disposition", disposition))
            .build());
    }

    public void remover(String chave) throws Exception {
        minio.removeObject(RemoveObjectArgs.builder().bucket(bucket()).object(chave).build());
    }

    private String bucket() {
        return properties.getStorage().getBucket();
    }
}
