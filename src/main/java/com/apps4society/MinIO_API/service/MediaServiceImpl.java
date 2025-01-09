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
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.IOException;
import java.time.LocalDateTime;
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
    public MediaDTO saveMedia(MultipartFile file, Long mediaIdentifier, EntityType entityType, Long uploadedBy) {
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

            if (mediaType == null) {
                throw new InvalidInputException("Tipo de mídia não suportado: " + file.getContentType());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Arquivo enviado para o MinIO com sucesso.");

            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(2, TimeUnit.HOURS)
                            .build()
            );
            log.info("URL gerada: {}", url);

            Media media = Media.builder()
                    .url(url)
                    .mediaType(mediaType)
                    .uploadDate(LocalDateTime.now())
                    .mediaIdentifier(mediaIdentifier)
                    .entityType(entityType)
                    .uploadedBy(uploadedBy)
                    .build();

            Media savedMedia = mediaRepository.save(media);
            log.info("Mídia salva no banco com ID: {}", savedMedia.getId());

            return mediaMapper.entityToDto(savedMedia);

        } catch (IOException e) {
            log.error("Erro ao ler o arquivo: {}", e.getMessage(), e);
            throw new ExternalServiceException("Erro ao processar o arquivo enviado.", e);
        } catch (MinioException e) {
            log.error("Erro ao interagir com o MinIO: {}", e.getMessage(), e);
            throw new ExternalServiceException("Erro ao enviar o arquivo para o MinIO.", e);
        } catch (DataIntegrityViolationException e) {
            log.error("Erro de integridade no banco de dados: {}", e.getMessage(), e);
            throw new DatabaseException("Erro ao salvar no banco de dados. Verifique os dados enviados.", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao salvar mídia: {}", e.getMessage(), e);
            throw new GenericServiceException("Erro inesperado ao salvar mídia.", e);
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

    @Override
    public MediaDTO getMediaById(Long id, EntityType entityType) {
        Media media = mediaRepository.findByIdAndEntityType(id, entityType)
                .orElseThrow(() -> new ResourceNotFoundException("Mídia não encontrada para o ID e tipo especificados."));
        return mediaMapper.entityToDto(media);
    }

    @Override
    public MediaDTO updateMedia(Long id, MultipartFile file, Long uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new InvalidInputException("O arquivo enviado está vazio ou inválido.");
        }

        Media existingMedia = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mídia não encontrada com ID: " + id));

        try {
            String objectName = file.getOriginalFilename();
            log.info("Atualizando arquivo com o nome: {}", objectName);

            MediaType newMediaType = determineMediaType(objectName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Arquivo atualizado no MinIO com sucesso.");

            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(2, TimeUnit.HOURS)
                            .build()
            );

            existingMedia.setUrl(url);
            existingMedia.setUploadDate(LocalDateTime.now());
            existingMedia.setMediaType(newMediaType);
            existingMedia.setUploadedBy(uploadedBy);

            Media updatedMedia = mediaRepository.save(existingMedia);
            log.info("Mídia atualizada no banco com sucesso.");

            return mediaMapper.entityToDto(updatedMedia);

        } catch (IOException e) {
            log.error("Erro ao processar o arquivo: {}", e.getMessage(), e);
            throw new ExternalServiceException("Erro ao processar o arquivo enviado.", e);
        } catch (MinioException e) {
            log.error("Erro ao interagir com o MinIO: {}", e.getMessage(), e);
            throw new ExternalServiceException("Erro ao atualizar o arquivo no MinIO.", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao atualizar a mídia: {}", e.getMessage(), e);
            throw new GenericServiceException("Erro inesperado ao atualizar a mídia.", e);
        }
    }


    private static final Map<String, MediaType> EXTENSION_TO_MEDIA_TYPE_MAP = Map.of(
            "jpg", MediaType.IMAGE,
            "jpeg", MediaType.IMAGE,
            "png", MediaType.IMAGE,
            "mp4", MediaType.VIDEO,
            "mp3", MediaType.AUDIO
    );
}
