package com.apps4society.MinIO_API.integration;

import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import com.apps4society.MinIO_API.repository.MediaRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;

@Component
public class MediaTestUtil {

    private final MediaRepository mediaRepository;
    private final MinioClient minioClient;

    @Autowired
    public MediaTestUtil(MediaRepository mediaRepository, MinioClient minioClient) {
        this.mediaRepository = mediaRepository;
        this.minioClient = minioClient;
    }

    /**
     * Faz o upload de um arquivo no MinIO e cria um registro de mídia no banco (usando Spring Data JPA).
     *
     * @param fileName Nome do arquivo, por exemplo "educaAPI/screenshot.png"
     * @return O objeto Media criado e salvo.
     */
    @Transactional
    public Media createTestMedia(String fileName) {
        // Faz o upload do arquivo no MinIO.
        try {
            byte[] content = "conteudo para teste".getBytes();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket("test-bucket")
                            .object(fileName)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType("image/png")
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer upload no MinIO: " + e.getMessage(), e);
        }

        // Cria o registro de mídia no banco via JPA.
        Media media = Media.builder()
                .serviceName("educaAPI")
                .fileName(fileName)
                .entityType(EntityType.THEME)
                .mediaType(MediaType.IMAGE)
                .uploadedBy(1L)
                .tag("teste")
                .active(true)
                .build();
        return mediaRepository.save(media);
    }

    /**
     * Remove o arquivo do MinIO e o registro de mídia do banco.
     *
     * @param fileName Nome do arquivo, por exemplo "educaAPI/screenshot.png"
     */
    @Transactional
    public void cleanupTestMedia(String fileName) {
        // Remove o arquivo do MinIO.
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("test-bucket")
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            System.err.println("Erro ao remover arquivo do MinIO: " + e.getMessage());
        }
        // Remove o registro do banco (se existir).
        mediaRepository.findByFileNameAndActive(fileName, true)
                .ifPresent(mediaRepository::delete);
    }
}
