package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.config.MinioConfig;
import com.apps4society.MinIO_API.exceptions.*;
import com.apps4society.MinIO_API.mapper.MediaMapper;
import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import com.apps4society.MinIO_API.repository.MediaRepository;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final MediaMapper mediaMapper;
    private final MinioClient minioClient;
    private final String bucketName;

    public MediaServiceImpl(
            MediaRepository mediaRepository,
            MediaMapper mediaMapper,
            MinioClient minioClient,
            MinioConfig minioConfig) {
        this.mediaRepository = mediaRepository;
        this.mediaMapper = mediaMapper;
        this.minioClient = minioClient;
        this.bucketName = minioConfig.getBucketName();
    }

    @Override
    public MediaDTO saveMedia(String serviceName, MultipartFile file, String tag, EntityType entityType, Long uploadedBy) {
        log.info("Recebendo requisição para salvar mídia no serviço '{}'", serviceName);
        log.info("🔍 Bucket utilizado: '{}'", bucketName);

        if (file == null || file.isEmpty() || file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
            log.warn("O arquivo enviado está vazio, inválido ou sem nome.");
            throw new InvalidFileException("O arquivo enviado está vazio, inválido ou sem nome.");
        }

        if (bucketName == null || bucketName.isEmpty()) {
            log.error("Falha ao salvar mídia: Nome do bucket não está configurado.");
            throw new BucketNotFoundException("O nome do bucket não foi configurado corretamente.");
        }

        if (serviceName == null || serviceName.trim().isEmpty() ||
                tag == null || tag.trim().isEmpty() ||
                entityType == null ||
                uploadedBy == null) {
            log.warn("Parâmetros inválidos ao salvar mídia.");
            throw new IllegalArgumentException("Um ou mais parâmetros obrigatórios estão inválidos.");
        }

        String objectName = buildObjectName(serviceName, file.getOriginalFilename());

        boolean existsActiveMedia = mediaRepository.existsByFileNameAndServiceNameAndActiveTrue(objectName, serviceName);
        if (existsActiveMedia) {
            log.warn("❌ Já existe um arquivo ativo com esse nome no serviço '{}'", serviceName);
            throw new DuplicateFileException("Já existe um arquivo ativo com esse nome.");
        }

        try {
            MediaType mediaType = determineMediaType(objectName);
            log.info("Tipo de mídia identificado: {}", mediaType);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("✅ Arquivo enviado para o MinIO com sucesso.");

            Media media = Media.builder()
                    .serviceName(serviceName)
                    .tag(tag)
                    .fileName(objectName)
                    .mediaType(mediaType)
                    .entityType(entityType)
                    .uploadedBy(uploadedBy)
                    .active(true)
                    .build();

            Media savedMedia = mediaRepository.save(media);
            log.info("✅ Mídia salva no banco com ID: {}, Nome: {}", savedMedia.getId(), savedMedia.getFileName());

            return mediaMapper.entityToDto(savedMedia);
        } catch (Exception e) {
            log.error("Erro inesperado ao armazenar arquivo no MinIO: {}", e.getMessage(), e);
            throw new FileStorageException("Erro ao salvar mídia no armazenamento.", e);
        }
    }

    @Override
    public Map<String, String> getMediaUrl(String serviceName, Long mediaId) {
        log.info("Gerando URL assinada para mídia ID: {} no serviço '{}'", mediaId, serviceName);

        if (serviceName == null || serviceName.trim().isEmpty()) {
            log.warn("Nome do serviço não pode ser nulo ou vazio.");
            throw new IllegalArgumentException("Nome do serviço é obrigatório.");
        }

        if (mediaId == null) {
            log.warn("ID da mídia não pode ser nulo.");
            throw new IllegalArgumentException("ID da mídia é obrigatório.");
        }

        Media media = mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName)
                .orElseThrow(() -> {
                    log.warn("Mídia não encontrada para o ID {} no serviço '{}'", mediaId, serviceName);
                    return new MediaNotFoundException("Mídia não encontrada para o serviço informado.");
                });

        try {
            String signedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(media.getFileName())
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
            log.info("URL assinada gerada com sucesso para mídia '{}'", media.getFileName());
            return Map.of("url", signedUrl);
        } catch (MinioException e) {
            log.error("Erro ao conectar com o MinIO: {}", e.getMessage(), e);
            throw new MinIOConnectionException("Erro ao conectar com o MinIO ao gerar URL assinada.", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao gerar URL assinada: {}", e.getMessage(), e);
            throw new MinIOConnectionException("Erro ao gerar URL assinada da mídia.", e);
        }
    }

    @Override
    public List<Map<String, String>> listarMidiasComUrl(String serviceName, EntityType entityType, Long entityId) {
        log.info("Buscando todas as mídias com URL do serviço '{}' para a entidade '{}'", serviceName, entityType);

        if (serviceName == null || serviceName.trim().isEmpty()) {
            log.warn("Nome do serviço não pode ser nulo ou vazio.");
            throw new IllegalArgumentException("Nome do serviço é obrigatório.");
        }

        if (entityType == null || entityId == null) {
            log.warn("Tipo de entidade ou ID não podem ser nulos.");
            throw new IllegalArgumentException("Tipo de entidade e ID são obrigatórios.");
        }

        // Busca as mídias no banco de dados
        List<Media> midias = mediaRepository.findByServiceNameAndEntityTypeAndEntityIdAndActiveTrue(
                serviceName, entityType, entityId);

        if (midias.isEmpty()) {
            log.warn("Nenhuma mídia encontrada para a entidade '{}' no serviço '{}'", entityType, serviceName);
            return Collections.emptyList();
        }

        // Para cada mídia encontrada, chamamos getMediaUrl para obter a URL assinada
        return midias.stream()
                .map(media -> {
                    Map<String, String> urlMap = getMediaUrl(serviceName, media.getId());
                    return Map.of(
                            "id", String.valueOf(media.getId()),
                            "fileName", media.getFileName(),
                            "url", urlMap.get("url")
                    );
                })
                .toList();
    }


    @Override
    public MediaDTO updateMedia(String serviceName, Long mediaId, EntityType entityType, String tag, MediaType mediaType, MultipartFile file) {
        log.info("Recebendo requisição para atualizar mídia ID: {} no serviço '{}'", mediaId, serviceName);

        if (file == null || file.isEmpty() || file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
            log.warn("O arquivo enviado para atualização está vazio, inválido ou sem nome.");
            throw new InvalidFileException("O arquivo enviado para atualização está vazio, inválido ou sem nome.");
        }

        if (bucketName == null || bucketName.isEmpty()) {
            log.error("Falha ao atualizar mídia: Nome do bucket não está configurado.");
            throw new BucketNotFoundException("O nome do bucket não foi configurado corretamente.");
        }

        if (serviceName == null || serviceName.trim().isEmpty() ||
                tag == null || tag.trim().isEmpty() ||
                entityType == null ||
                mediaId == null) {
            log.warn("Parâmetros inválidos ao atualizar mídia.");
            throw new IllegalArgumentException("Um ou mais parâmetros obrigatórios estão inválidos.");
        }

        Media media = mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName)
                .orElseThrow(() -> {
                    log.warn("Mídia não encontrada para o ID {} no serviço '{}'", mediaId, serviceName);
                    return new MediaNotFoundException("Mídia não encontrada para o serviço informado.");
                });

        try {
            String objectName = buildObjectName(serviceName, file.getOriginalFilename());
            log.info("Atualizando mídia '{}', novo arquivo: '{}'", media.getFileName(), objectName);

            MediaType newMediaType = determineMediaType(objectName);
            log.info("Novo tipo de mídia identificado: {}", newMediaType);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Arquivo atualizado no MinIO com sucesso.");

            media.setFileName(objectName);
            media.setEntityType(entityType);
            media.setTag(tag);
            media.setMediaType(newMediaType);

            Media updatedMedia = mediaRepository.save(media);
            log.info("Mídia atualizada no banco com ID: {}", updatedMedia.getId());

            return mediaMapper.entityToDto(updatedMedia);
        } catch (Exception e) {
            log.error("Erro ao armazenar arquivo no MinIO: {}", e.getMessage(), e);
            throw new FileStorageException("Erro ao atualizar a mídia no armazenamento.", e);
        }
    }

    @Override
    @Transactional
    public MediaDTO disableMedia(String serviceName, Long mediaId) {
        log.info("Recebendo requisição para desativar mídia ID: {} no serviço '{}'", mediaId, serviceName);

        if (serviceName == null || serviceName.trim().isEmpty()) {
            log.warn("O nome do serviço está nulo ou vazio.");
            throw new IllegalArgumentException("O nome do serviço não pode ser nulo ou vazio.");
        }

        Media media = mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName)
                .orElseThrow(() -> {
                    log.warn("Mídia não encontrada para o ID {} no serviço '{}'", mediaId, serviceName);
                    return new MediaNotFoundException("Mídia não encontrada para o serviço informado.");
                });

        try {
            String newObjectName = "arquivos_desativados/" + media.getFileName();

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(newObjectName)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(media.getFileName())
                                    .build())
                            .build()
            );

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(media.getFileName())
                            .build()
            );

            log.info("📁 Arquivo '{}' movido para '{}'", media.getFileName(), newObjectName);

        } catch (Exception e) {
            log.error("Erro ao mover a mídia no MinIO: {}", e.getMessage(), e);
            throw new FileStorageException("Erro ao mover mídia no armazenamento.", e);
        }

        media.setActive(false);
        Media updatedMedia = mediaRepository.save(media);
        log.info("✅ Mídia ID {} foi desativada no banco.", mediaId);

        return mediaMapper.entityToDto(updatedMedia);
    }

    /**
     * Constrói o nome do objeto para o armazenamento no MinIO, garantindo que ele inclua o prefixo do serviço.
     */
    private String buildObjectName(String serviceName, String fileName) {
        return serviceName + "/" + fileName;
    }

    private MediaType determineMediaType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            log.warn("O arquivo '{}' não possui uma extensão válida.", fileName);
            throw new InvalidFileException("O arquivo não possui uma extensão válida.");
        }
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        MediaType mediaType = EXTENSION_TO_MEDIA_TYPE_MAP.get(extension);
        if (mediaType == null) {
            log.warn("Tipo de mídia não suportado para a extensão '{}'", extension);
            throw new UnsupportedMediaTypeException("Tipo de mídia não suportado: " + extension);
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
