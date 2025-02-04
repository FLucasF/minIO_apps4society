package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.mapper.MediaMapper;
import com.apps4society.MinIO_API.exceptions.*;
import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import com.apps4society.MinIO_API.repository.MediaRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final MediaMapper mediaMapper;
    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    public MediaServiceImpl(MediaRepository mediaRepository, MediaMapper mediaMapper, MinioClient minioClient) {
        this.mediaRepository = mediaRepository;
        this.mediaMapper = mediaMapper;
        this.minioClient = minioClient;
    }

    @Override
    public MediaDTO saveMedia(String serviceName, MultipartFile file, String tag, EntityType entityType, Long uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new InvalidInputException("O arquivo enviado está vazio ou inválido.");
        }

        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalArgumentException("O nome do bucket não foi configurado corretamente.");
        }

        try {
            String objectName = file.getOriginalFilename();
            log.info("Recebendo arquivo com o nome: {}", objectName);

            MediaType mediaType = determineMediaType(objectName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Arquivo enviado para o MinIO com sucesso.");

            Media media = Media.builder()
                    .serviceName(serviceName)
                    .tag(tag)
                    .fileName(objectName)
                    .mediaType(mediaType)
                    .entityType(entityType)
                    .uploadedBy(uploadedBy)
                    .build();

            Media savedMedia = mediaRepository.save(media);
            log.info("Mídia salva no banco com ID: {}", savedMedia.getId());

            return mediaMapper.entityToDto(savedMedia);

        } catch (Exception e) {
            log.error("Erro ao salvar mídia: {}", e.getMessage(), e);
            throw new ExternalServiceException("Erro ao salvar mídia.", e);
        }
    }

    @Override
    public Map<String, String> getMediaUrl(String serviceName, Long mediaId) {
        Media media = mediaRepository.findByIdAndServiceName(mediaId, serviceName)
                .orElseThrow(() -> new ResourceNotFoundException("Mídia não encontrada para o serviço informado."));

        try {
            // Gera a URL assinada para o arquivo no MinIO
            String signedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(media.getFileName())
                            .expiry(1, TimeUnit.HOURS) // URL válida por 1 hora
                            .build()
            );

            return Map.of("url", signedUrl);

        } catch (Exception e) {
            log.error("Erro ao gerar a URL assinada: {}", e.getMessage(), e);
            throw new ExternalServiceException("Erro ao gerar a URL assinada da mídia.", e);
        }
    }

    @Override
    public MediaDTO updateMedia(String serviceName, Long mediaId, EntityType entityType, String tag, MediaType mediaType, MultipartFile file) {
        // Busca a mídia pelo ID e valida se pertence ao serviço
        Media media = mediaRepository.findByIdAndServiceName(mediaId, serviceName)
                .orElseThrow(() -> new ResourceNotFoundException("Mídia não encontrada para o serviço especificado."));

        try {
            String objectName = serviceName + "/" + file.getOriginalFilename();
            log.info("Atualizando mídia no serviço: {}, Nome do arquivo: {}", serviceName, objectName);

            // Envia o novo arquivo para o MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Arquivo atualizado no MinIO com sucesso.");

            // Atualiza as informações no banco de dados
            media.setFileName(objectName);
            media.setEntityType(entityType);
            media.setTag(tag);
            media.setMediaType(mediaType);

            Media updatedMedia = mediaRepository.save(media);
            log.info("Mídia atualizada no banco com ID: {}", updatedMedia.getId());

            // Retorna o DTO atualizado
            return MediaDTO.builder()
                    .id(updatedMedia.getId())
                    .serviceName(updatedMedia.getServiceName())
                    .mediaType(updatedMedia.getMediaType())
                    .entityType(updatedMedia.getEntityType())
                    .uploadedBy(updatedMedia.getUploadedBy())
                    .fileName(updatedMedia.getFileName())
                    .tag(updatedMedia.getTag())
                    .build();

        } catch (Exception e) {
            log.error("Erro ao atualizar mídia: {}", e.getMessage(), e);
            throw new ExternalServiceException("Erro ao atualizar a mídia.", e);
        }
    }

    private MediaType determineMediaType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new InvalidInputException("O arquivo não possui uma extensão válida.");
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        MediaType mediaType = EXTENSION_TO_MEDIA_TYPE_MAP.get(extension);

        if (mediaType == null) {
            throw new InvalidInputException("Tipo de mídia não suportado para a extensão: " + extension);
        }

        return mediaType;
    }

    private static final Map<String, MediaType> EXTENSION_TO_MEDIA_TYPE_MAP = Map.of(
            "jpg", MediaType.IMAGE,
            "jpeg", MediaType.IMAGE,
            "png", MediaType.IMAGE,
            "pdf", MediaType.IMAGE,
            "mp4", MediaType.VIDEO,
            "mp3", MediaType.AUDIO
    );
}
