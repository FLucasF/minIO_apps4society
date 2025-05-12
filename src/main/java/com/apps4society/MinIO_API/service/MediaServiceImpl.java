package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.config.MinioConfig;
import com.apps4society.MinIO_API.exceptions.*;
import com.apps4society.MinIO_API.mapper.MediaMapper;
import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.MediaType;
import com.apps4society.MinIO_API.repository.MediaRepository;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final MediaMapper mediaMapper;
    private final MinioClient minioClient;
    private final String bucketName;

    public MediaServiceImpl(MediaRepository mediaRepository, MediaMapper mediaMapper, MinioClient minioClient, MinioConfig minioConfig) {
        this.mediaRepository = mediaRepository;
        this.mediaMapper = mediaMapper;
        this.minioClient = minioClient;
        this.bucketName = minioConfig.getBucketName();
    }

    @Override
    @Transactional
    public MediaResponse uploadMedia(MediaRequest mediaRequest,MultipartFile file) {
        log.info("Iniciando upload de mídia | Serviço: '{}'", mediaRequest.serviceName());

        validateFile(file);

        String originalFileName = file.getOriginalFilename();
        String objectName = mediaRequest.serviceName() + "/" + originalFileName;
        MediaType mediaType = determineMediaType(originalFileName);

        log.info("Nome do arquivo original: '{}'", originalFileName);
        log.info("Nome do objeto no MinIO: '{}'", objectName);
        log.info("Tipo de mídia detectado: '{}'", mediaType);

        if (mediaRepository.existsByFileNameAndServiceName(originalFileName, mediaRequest.serviceName())){
            log.error("Arquivo duplicado detectado: '{}' para o serviço '{}'", originalFileName, mediaRequest.serviceName());
            throw new DuplicateFileException("O arquivo com o nome '" + originalFileName + "' já foi enviado para o serviço '" + mediaRequest.serviceName() + "'.");
        }

        try {
            log.info("Enviando arquivo para MinIO - Bucket: '{}'", bucketName);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Arquivo '{}' salvo com sucesso no MinIO!", objectName);

            Media media = new Media(mediaRequest.uploadedBy(), originalFileName, mediaRequest.serviceName(), mediaType);

            //criando variavel apenas para debug
            Media savedMedia = mediaRepository.save(media);
            log.info("Mídia salva no banco | ID: '{}' | Nome: '{}'", savedMedia.getEntityId(), savedMedia.getFileName());

            return mediaMapper.toResponse(media);
        } catch (Exception e) {
            log.error("Erro ao armazenar mídia no MinIO!", e);
            throw new FileStorageException("Erro ao salvar mídia no armazenamento.", e);
        }
    }


    @Override
    public String getMediaUrl(String serviceName, Long mediaId) {
        log.info("Buscando URL da mídia ID '{}' no serviço '{}'", mediaId, serviceName);

        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do serviço não pode ser vazio ou nulo.");
        }

        if (mediaId == null || mediaId <= 0) {
            throw new IllegalArgumentException("O ID do material deve ser maior que 0.");
        }

        Media media = mediaRepository.findByEntityIdAndServiceNameAndActiveTrue(mediaId, serviceName)
                .orElseThrow(() -> new MediaNotFoundException("Mídia não encontrada ou inativa."));

        try {
            String objectPath = media.getServiceName() + "/" + media.getFileName();
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectPath)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            throw new MinIOConnectionException("Erro ao gerar URL assinada da mídia.", e);
        }
    }

    @Override
    public List<MediaResponse> listMediaByUploadedBy(String serviceName, Long uploadedBy) {
        log.info("Buscando mídias para o serviço '{}' e usuário '{}'", serviceName, uploadedBy);

        List<Media> midias = mediaRepository.findByServiceNameAndUploadedByAndActiveTrue(serviceName, uploadedBy);

        if (midias.isEmpty()) {
            log.warn("Nenhuma mídia encontrada para o serviço '{}' e usuário '{}'", serviceName, uploadedBy);
        } else {
            log.info("{} mídias encontradas para o serviço '{}' e usuário '{}'", midias.size(), serviceName, uploadedBy);
        }

        return midias.stream().map(mediaMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MediaResponse updateMedia(Long entityId, MediaRequest mediaRequest, MultipartFile file) {
        String serviceName = mediaRequest.serviceName();

        log.info("Atualizando mídia ID '{}' no serviço '{}'", entityId, serviceName);

        Media media = mediaRepository.findByEntityIdAndServiceNameAndActiveTrue(entityId, serviceName)
                .orElseThrow(() -> {
                    log.warn("Mídia ID '{}' não encontrada no serviço '{}' ou inativa.", entityId, serviceName);
                    return new MediaNotFoundException("Mídia não encontrada ou inativa.");
                });

        log.info("Media: {}", media);

        validateFile(file);

        if (mediaRepository.existsByFileNameAndServiceName(file.getOriginalFilename(), serviceName)) {
            log.error("Arquivo duplicado detectado: '{}' para o serviço '{}'", file.getOriginalFilename(), serviceName);
            throw new DuplicateFileException("O arquivo com o nome '" + file.getOriginalFilename() + "' já foi enviado para o serviço '" + serviceName + "'.");
        }

        try {
            String oldObjectName = serviceName + "/" + media.getFileName();
            String newObjectName = "arquivos_desativados/" + serviceName + "/" + media.getFileName();

            log.info("Movendo arquivo antigo para '{}'", newObjectName);
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(newObjectName)
                            .source(CopySource.builder().bucket(bucketName).object(oldObjectName).build())
                            .build()
            );
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(oldObjectName)
                            .build()
            );
            log.info("Arquivo antigo '{}' movido para '{}'", oldObjectName, newObjectName);

            String newFileName = file.getOriginalFilename();
            String newFileObjectName = serviceName + "/" + newFileName;
            MediaType newMediaType = determineMediaType(newFileObjectName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(newFileObjectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Novo arquivo '{}' enviado para MinIO!", newFileObjectName);

            media.setFileName(newFileName);
            media.setMediaType(newMediaType);

            Media updatedMedia = mediaRepository.save(media);
            log.info("Mídia atualizada no banco | ID: '{}' | Nome: '{}'", updatedMedia.getEntityId(), updatedMedia.getFileName());

            return mediaMapper.toResponse(media);
        } catch (Exception e) {
            log.error("Erro ao atualizar a mídia no MinIO!", e);
            throw new FileStorageException("Erro ao atualizar a mídia no armazenamento.", e);
        }
    }

    @Override
    @Transactional
    public void disableMedia(String serviceName, Long mediaId) {
        log.info("Desativando mídia ID '{}' no serviço '{}'", mediaId, serviceName);

        Media media = mediaRepository.findByEntityIdAndServiceNameAndActiveTrue(mediaId, serviceName)
                .orElseThrow(() -> {
                    log.warn("Mídia ID '{}' não encontrada no serviço '{}'!", mediaId, serviceName);
                    return new MediaNotFoundException("Mídia não encontrada ou inativa.");
                });

        try {
            String newObjectName = "arquivos_desativados/" + serviceName + "/" + media.getFileName();
            log.info("Movendo arquivo para '{}'", newObjectName);

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(newObjectName)
                            .source(CopySource.builder().bucket(bucketName).object(serviceName + "/" + media.getFileName()).build())
                            .build()
            );

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(serviceName + "/" + media.getFileName())
                            .build()
            );

            log.info("Arquivo '{}' movido para '{}'", media.getFileName(), newObjectName);
        } catch (Exception e) {
            log.error("Erro ao mover mídia!", e);
            throw new FileStorageException("Erro ao mover mídia no armazenamento.", e);
        }

        media.disable();
        mediaRepository.save(media);
        log.info("Mídia ID '{}' desativada no banco para o serviço '{}'!", mediaId, serviceName);
    }

    /**
     * Determina o tipo de mídia com base na extensão do arquivo.
     */
    private MediaType determineMediaType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg", "png" -> MediaType.IMAGE;
            case "mp4" -> MediaType.VIDEO;
            case "mp3" -> MediaType.AUDIO;
            default -> throw new UnsupportedMediaTypeException("Tipo de mídia não suportado: " + extension);
        };
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new InvalidFileException("O arquivo enviado para upload está vazio ou sem nome.");
        }
    }
}