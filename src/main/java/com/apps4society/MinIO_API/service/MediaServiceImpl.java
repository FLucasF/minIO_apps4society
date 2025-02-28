package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.config.MinioConfig;
import com.apps4society.MinIO_API.exceptions.*;
import com.apps4society.MinIO_API.mapper.MediaMapper;
import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import com.apps4society.MinIO_API.repository.MediaRepository;
import io.minio.errors.ErrorResponseException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
        log.info("🔍 Bucket utilizado: '{}'", bucketName);  // <-- Log do bucket


        if (file == null || file.isEmpty()) {
            log.warn("O arquivo enviado está vazio ou inválido.");
            throw new InvalidFileException("O arquivo enviado está vazio ou inválido.");
        }

        if (bucketName == null || bucketName.isEmpty()) {
            log.error("Falha ao salvar mídia: Nome do bucket não está configurado.");
            throw new BucketNotFoundException("O nome do bucket não foi configurado corretamente.");
        }

        // Gera o nome do objeto de forma consistente (ex: "educAPI/test-image.png")
        String objectName = buildObjectName(serviceName, file.getOriginalFilename());

        // Verifica duplicidade usando o nome completo
        if (mediaRepository.existsByFileNameAndActive(objectName, true)) {
            log.warn("Tentativa de cadastrar um arquivo já existente e ativo: {}", objectName);
            throw new DuplicateFileException("Já existe um arquivo ativo com esse nome.");
        }

        try {
            MediaType mediaType = determineMediaType(objectName);
            log.info("Tipo de mídia identificado: {}", mediaType);

            // Tenta enviar o arquivo para o MinIO
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
                    .active(true)
                    .build();

            Media savedMedia = mediaRepository.save(media);
            if (savedMedia == null) {
                log.error("Erro ao salvar mídia: A resposta do repositório foi nula.");
                // Lançamos uma exceção de banco de dados, já que é um erro de persistência
                throw new DatabaseException("Erro ao salvar mídia no banco de dados.", new NullPointerException("Repositório retornou null"));
            }
            log.info("Mídia salva no banco com ID: {}, Nome: {}", savedMedia.getId(), savedMedia.getFileName());

            return mediaMapper.entityToDto(savedMedia);

        } catch (ErrorResponseException ere) {
            // Se a exceção do MinIO indicar um erro específico, mapeamos para exceções customizadas
            int statusCode = Integer.parseInt(ere.errorResponse().code()); // Supondo que esse método exista
            if (statusCode == 412) {
                throw new PreconditionRequiredCustomException("Precondition required: " + ere.getMessage());
            } else if (statusCode == 408) {
                throw new RequestTimeoutCustomException("Request timeout: " + ere.getMessage());
            } else if (statusCode == 429) {
                throw new TooManyRequestsCustomException("Too many requests: " + ere.getMessage());
            } else {
                throw new FileStorageException("Erro ao salvar mídia no armazenamento.", ere);
            }
        } catch (Exception e) {
            log.error("Erro ao salvar mídia: {}", e.getMessage(), e);
            throw new FileStorageException("Erro ao salvar mídia no armazenamento.", e);
        }
    }

    @Override
    public Map<String, String> getMediaUrl(String serviceName, Long mediaId) {
        log.info("Gerando URL assinada para mídia ID: {} no serviço '{}'", mediaId, serviceName);

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

        } catch (Exception e) {
            log.error("Erro ao gerar URL assinada para mídia '{}': {}", media.getFileName(), e.getMessage(), e);
            throw new MinIOConnectionException("Erro ao gerar a URL assinada da mídia.", e);
        }
    }

    @Override
    public MediaDTO updateMedia(String serviceName, Long mediaId, EntityType entityType, String tag, MediaType mediaType, MultipartFile file) {
        log.info("Recebendo requisição para atualizar mídia ID: {} no serviço '{}'", mediaId, serviceName);

        if (file == null || file.isEmpty()) {
            log.warn("O arquivo enviado para atualização está vazio ou inválido.");
            throw new InvalidFileException("O arquivo enviado está vazio ou inválido.");
        }

        if (bucketName == null || bucketName.isEmpty()) {
            log.error("Falha ao atualizar mídia: Nome do bucket não está configurado.");
            throw new BucketNotFoundException("O nome do bucket não foi configurado corretamente.");
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
            if (updatedMedia == null) {
                log.error("Erro ao atualizar mídia: A resposta do repositório foi nula.");
                throw new DatabaseException("Erro ao atualizar a mídia no banco de dados.", new NullPointerException("Repositório retornou null"));
            }
            log.info("Mídia atualizada no banco com ID: {}", updatedMedia.getId());

            return mediaMapper.entityToDto(updatedMedia);

        } catch (ErrorResponseException ere) {
            int statusCode = Integer.parseInt(ere.errorResponse().code());
            if (statusCode == 412) {
                throw new PreconditionRequiredCustomException("Precondition required: " + ere.getMessage());
            } else if (statusCode == 408) {
                throw new RequestTimeoutCustomException("Request timeout: " + ere.getMessage());
            } else if (statusCode == 429) {
                throw new TooManyRequestsCustomException("Too many requests: " + ere.getMessage());
            } else {
                throw new FileStorageException("Erro ao atualizar a mídia no armazenamento.", ere);
            }
        } catch (Exception e) {
            log.error("Erro ao atualizar mídia '{}': {}", media.getFileName(), e.getMessage(), e);
            throw new FileStorageException("Erro ao atualizar a mídia no armazenamento.", e);
        }
    }

    @Override
    public MediaDTO disableMedia(String serviceName, Long mediaId) {
        log.info("Recebendo requisição para desativar mídia ID: {} no serviço '{}'", mediaId, serviceName);

        Media media = mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName)
                .orElseThrow(() -> {
                    log.warn("Mídia não encontrada para o ID {} no serviço '{}'", mediaId, serviceName);
                    return new MediaNotFoundException("Mídia não encontrada para o serviço informado.");
                });

        if (!media.isActive()) {
            log.warn("A mídia ID {} já está desativada.", mediaId);
            throw new MediaAlreadyDisabledException("A mídia já está desativada.");
        }

        media.setActive(false);
        Media updatedMedia = mediaRepository.save(media);
        if (updatedMedia == null) {
            throw new DatabaseException("Erro ao desativar a mídia no banco de dados.", new NullPointerException("Repositório retornou null"));
        }
        log.info("Mídia ID {} foi desativada com sucesso.", mediaId);
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
