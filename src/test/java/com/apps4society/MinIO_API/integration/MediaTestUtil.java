package com.apps4society.MinIO_API.integration;

import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType; // Use seu enum de mídia, não o org.springframework.http.MediaType
import com.apps4society.MinIO_API.repository.MediaRepository;
import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

@Component
public class MediaTestUtil {

    private final MediaRepository mediaRepository;
    private final MinioClient minioClient;
    private final String bucketName = "test-bucket";

    @Autowired
    public MediaTestUtil(MediaRepository mediaRepository, MinioClient minioClient) {
        this.mediaRepository = mediaRepository;
        this.minioClient = minioClient;
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar/criar bucket: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Map<String, Object> createTestMedia(String fileName) {
        ensureBucketExists();

        try {
            byte[] content = "conteudo para teste".getBytes();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType("image/png")
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer upload no MinIO: " + e.getMessage(), e);
        }

        Media media = Media.builder()
                .serviceName("educaAPI")
                .fileName(fileName)
                .entityType(EntityType.THEME)
                .mediaType(MediaType.IMAGE)
                .uploadedBy(1L)
                .tag("teste")
                .active(true)
                .build();

        Media saved = mediaRepository.save(media);

        if (saved.getId() == null) {
            throw new RuntimeException("Erro ao salvar mídia no banco.");
        }

        return Map.of(
                "id", saved.getId(),
                "fileName", saved.getFileName(),
                "serviceName", saved.getServiceName(),
                "mediaType", saved.getMediaType().toString(),
                "entityType", saved.getEntityType().toString(),
                "uploadedBy", saved.getUploadedBy(),
                "tag", saved.getTag(),
                "active", saved.isActive()
        );
    }

    public Media findMediaById(Long mediaId) {
        return mediaRepository.findById(mediaId).orElse(null);
    }

    @Transactional
    public void cleanupTestMedia(String fileName) {
        // Tenta remover o arquivo do MinIO
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            System.out.println("Arquivo removido do MinIO: " + fileName);
        } catch (Exception e) {
            System.err.println("Erro ao remover arquivo do MinIO: " + e.getMessage());
        }

        List<Media> allMedia = mediaRepository.findAll();
        allMedia.stream()
                .filter(m -> fileName.equals(m.getFileName()) && m.isActive())
                .forEach(mediaRepository::delete);
        System.out.println("Registros de mídia com fileName '" + fileName + "' removidos do banco (se existiam).");
    }

    public boolean checkFileExists(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucketName).object(fileName).build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }



}
