package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.Mapper.MediaMapper;
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
import java.util.List;
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
    public MediaDTO saveMedia(MultipartFile file, Long entityId, EntityType entityType, Long uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new InvalidInputException("O arquivo enviado está vazio ou inválido.");
        }

        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalArgumentException("O nome do bucket não foi configurado corretamente.");
        }


        try {
            String objectName = file.getOriginalFilename();
            log.info("Recebendo arquivo com o nome: {}", objectName);

            MediaType mediaType = determineMediaType(file.getContentType());
            if (mediaType == null) {
                throw new InvalidInputException("Tipo de mídia não suportado: " + file.getContentType());
            }

            // Upload para o MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Arquivo enviado para o MinIO com sucesso.");

            // Gerar URL do arquivo no MinIO
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(2, TimeUnit.HOURS)
                            .build()
            );
            log.info("URL gerada: {}", url);

            // Salvar no banco de dados
            Media media = Media.builder()
                    .url(url)
                    .mediaType(mediaType)
                    .uploadDate(LocalDateTime.now())
                    .entityId(entityId)
                    .entityType(entityType)
                    .uploadedBy(uploadedBy)
                    .build();

            Media savedMedia = mediaRepository.save(media);
            log.info("Mídia salva no banco com ID: {}", savedMedia.getMidiaId());

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

    private MediaType determineMediaType(String contentType) {
        if (contentType == null) {
            throw new InvalidInputException("O tipo de conteúdo do arquivo é nulo.");
        }
        if (contentType.startsWith("image/")) {
            return MediaType.IMAGE;
        } else if (contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        } else if (contentType.startsWith("audio/")) {
            return MediaType.AUDIO;
        }
        throw new InvalidInputException("Tipo de mídia não suportado: " + contentType);
    }

    @Override
    public List<MediaDTO> getMediaByUploader(Long uploadedBy) {
        List<Media> mediaList = mediaRepository.findByUploadedBy(uploadedBy);
        if (mediaList.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma mídia encontrada para o usuário com ID: " + uploadedBy);
        }
        return mediaList.stream()
                .map(mediaMapper::entityToDto)
                .toList();
    }

    @Override
    public List<String> getURLsOfThemeImages(Long themeId) {
        List<Media> mediaList = mediaRepository.findAllByEntityIdAndEntityTypeAndMediaType(themeId, EntityType.THEME, MediaType.IMAGE);
        if (mediaList.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma imagem encontrada para o tema com ID: " + themeId);
        }
        return mediaList.stream()
                .map(Media::getUrl)
                .toList();
    }

    @Override
    public List<String> getURLsOfChallengeImages(Long challengeId) {
        List<Media> mediaList = mediaRepository.findAllByEntityIdAndEntityTypeAndMediaType(challengeId, EntityType.CHALLENGE, MediaType.IMAGE);
        if (mediaList.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma imagem encontrada para o desafio com ID: " + challengeId);
        }
        return mediaList.stream()
                .map(Media::getUrl)
                .toList();
    }
}
