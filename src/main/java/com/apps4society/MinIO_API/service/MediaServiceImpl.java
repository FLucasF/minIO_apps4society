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

    /**
     * Realiza o upload de um arquivo de mídia no MinIO e registra a mídia no banco de dados.
     * Caso o arquivo seja inválido, lança uma exceção.
     * @param request Objeto contendo os dados do arquivo a ser enviado.
     * @return MediaResponse com informações da mídia salva.
     * @throws InvalidFileException Caso o arquivo seja inválido.
     * @throws FileStorageException Caso ocorra erro ao salvar no MinIO ou no banco de dados.
     */
    @Override
    public MediaResponse uploadMedia(MediaRequest request) {
        log.info("Iniciando upload de mídia | Entidade: '{}' | Serviço: '{}'", request.entityId(), request.serviceName());

        if (request.file().isEmpty() || request.file().getOriginalFilename() == null) {
            log.warn("❌ [UPLOAD] Arquivo inválido - Está vazio ou sem nome.");
            throw new InvalidFileException("O arquivo enviado está vazio ou sem nome.");
        }

        String originalFileName = request.file().getOriginalFilename();
        String objectName = request.serviceName() + "/" + originalFileName;
        MediaType mediaType = determineMediaType(originalFileName);

        log.info("Nome do arquivo original: '{}'", originalFileName);
        log.info("Nome do objeto no MinIO: '{}'", objectName);
        log.info("Tipo de mídia detectado: '{}'", mediaType);

        try {
            log.info("Enviando arquivo para MinIO - Bucket: '{}'", bucketName);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(request.file().getInputStream(), request.file().getSize(), -1)
                            .contentType(request.file().getContentType())
                            .build()
            );
            log.info("Arquivo '{}' salvo com sucesso no MinIO!", objectName);

            Media media = new Media(null, request.serviceName(), mediaType, originalFileName, request.entityId(), true);
            Media savedMedia = mediaRepository.save(media);
            log.info("Mídia salva no banco | ID: '{}' | Nome: '{}'", savedMedia.getId(), savedMedia.getFileName());

            return mediaMapper.toResponse(savedMedia);
        } catch (Exception e) {
            log.error("Erro ao armazenar mídia no MinIO!", e);
            throw new FileStorageException("Erro ao salvar mídia no armazenamento.", e);
        }
    }

    /**
     * Retorna a URL assinada de um arquivo de mídia armazenado no MinIO.
     * A URL tem validade de 1 hora e pode ser usada para acessar o arquivo diretamente.
     * @param serviceName Nome do serviço associado à mídia.
     * @param mediaId ID da mídia para a qual a URL será gerada.
     * @return URL assinada para acesso à mídia.
     * @throws MediaNotFoundException Caso a mídia não seja encontrada ou esteja inativa.
     * @throws MinIOConnectionException Caso ocorra erro ao gerar a URL assinada.
     */
    @Override
    public String getMediaUrl(String serviceName, Long mediaId) {
        log.info("Buscando URL da mídia ID '{}' no serviço '{}'", mediaId, serviceName);

        Media media = mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName)
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

    /**
     * Lista todas as mídias ativas associadas a uma entidade específica.
     * @param serviceName Nome do serviço ao qual as mídias pertencem.
     * @param entityId ID da entidade associada às mídias.
     * @return Lista de MediaResponse contendo URLs assinadas e informações das mídias.
     */
    @Override
    public List<MediaResponse> listMediaByEntity(String serviceName, Long entityId) {
        log.info("Buscando mídias para Entidade '{}' | Serviço '{}'", entityId, serviceName);

        List<Media> midias = mediaRepository.findByServiceNameAndEntityIdAndActiveTrue(serviceName, entityId);

        if (midias.isEmpty()) {
            log.warn("Nenhuma mídia encontrada para a entidade '{}' e serviço '{}'", entityId, serviceName);
        } else {
            log.info("{} mídias encontradas para a entidade '{}'", midias.size(), entityId);
        }

        return midias.stream().map(mediaMapper::toResponse).collect(Collectors.toList());
    }

    /**
     * Atualiza uma mídia existente no MinIO e no banco de dados com um novo arquivo.
     * A mídia antiga será desabilitada no MinIO, mas permanecerá ativa no banco de dados.
     * @param serviceName Nome do serviço associado à mídia.
     * @param mediaId ID da mídia que será atualizada.
     * @param request Objeto contendo os dados do novo arquivo a ser enviado.
     * @return MediaResponse com informações da mídia atualizada.
     * @throws MediaNotFoundException Caso a mídia não seja encontrada ou esteja inativa.
     * @throws InvalidFileException Caso o arquivo enviado para atualização seja inválido.
     * @throws FileStorageException Caso ocorra erro ao salvar no MinIO ou no banco de dados.
     */
    @Override
    public MediaResponse updateMedia(String serviceName, Long mediaId, MediaRequest request) {
        log.info("Atualizando mídia ID '{}' no serviço '{}'", mediaId, serviceName);

        Media media = mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName)
                .orElseThrow(() -> {
                    log.warn("Mídia ID '{}' não encontrada no serviço '{}' ou inativa.", mediaId, serviceName);
                    return new MediaNotFoundException("Mídia não encontrada ou inativa.");
                });

        if (request.file().isEmpty() || request.file().getOriginalFilename() == null) {
            throw new InvalidFileException("O arquivo enviado para atualização está vazio ou sem nome.");
        }

        try {
            String newFileName = request.file().getOriginalFilename();
            String newObjectName = serviceName + "/" + newFileName;
            MediaType newMediaType = determineMediaType(newObjectName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(newObjectName)
                            .stream(request.file().getInputStream(), request.file().getSize(), -1)
                            .contentType(request.file().getContentType())
                            .build()
            );

            String oldObjectName = serviceName + "/" + media.getFileName();
            log.info("Desabilitando arquivo antigo '{}'", oldObjectName);
            try {
                // Mover o arquivo antigo para a pasta de desativados no MinIO
                String disabledObjectName = "arquivos_desativados/" + oldObjectName;
                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(bucketName)
                                .object(disabledObjectName)
                                .source(CopySource.builder().bucket(bucketName).object(oldObjectName).build())
                                .build()
                );
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(oldObjectName)
                                .build()
                );
                log.info("Arquivo antigo '{}' movido para '{}'", oldObjectName, disabledObjectName);
            } catch (Exception e) {
                log.error("Erro ao mover mídia antiga no MinIO", e);
                throw new FileStorageException("Erro ao desabilitar mídia antiga no MinIO.", e);
            }

            media.setFileName(newFileName);
            media.setMediaType(newMediaType);
            Media updatedMedia = mediaRepository.save(media);

            return mediaMapper.toResponse(updatedMedia);
        } catch (Exception e) {
            throw new FileStorageException("Erro ao atualizar a mídia no armazenamento.", e);
        }
    }

    /**
     * Desativa uma mídia existente e move o arquivo para uma pasta de "arquivos desativados" no MinIO.
     * A mídia também é marcada como inativa no banco de dados.
     * @param serviceName Nome do serviço associado à mídia.
     * @param mediaId ID da mídia que será desativada.
     * @throws MediaNotFoundException Caso a mídia não seja encontrada ou esteja inativa.
     * @throws FileStorageException Caso ocorra erro ao mover a mídia no MinIO.
     */
    @Override
    @Transactional
    public void disableMedia(String serviceName, Long mediaId) {
        log.info("Desativando mídia ID '{}' no serviço '{}'", mediaId, serviceName);

        Media media = mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName)
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
     * @param fileName Nome do arquivo para o qual o tipo será determinado.
     * @return MediaType correspondente ao tipo de mídia (imagem, vídeo, áudio).
     * @throws UnsupportedMediaTypeException Caso o tipo de mídia não seja suportado.
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

}
