package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.exceptions.FileStorageException;
import com.apps4society.MinIO_API.exceptions.InvalidFileException;
import com.apps4society.MinIO_API.exceptions.UnsupportedMediaTypeException;
import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.MediaType;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
class MediaServiceImplUploadTest extends BaseMediaServiceImplTest {

    private MockMultipartFile file;
    private MediaRequest mediaRequest;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(mediaService, "bucketName", "test-bucket");

        file = new MockMultipartFile("file", "test-image.png", "image/png", "dummy".getBytes());

        mediaRequest = new MediaRequest(
                "educAPI", // serviceName
                42L
        );
    }

    @Test
    void testUploadMedia_nullFile_throwsInvalidFileException() {
        MediaRequest requestSemArquivo = new MediaRequest("educAPI", 42L);

        assertThrows(NullPointerException.class, () -> mediaService.uploadMedia(requestSemArquivo, null));
    }

    @Test
    void testUploadMedia_emptyFile_throwsInvalidFileException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        MediaRequest requestComArquivoVazio = new MediaRequest("educAPI", 42L);

        assertThrows(InvalidFileException.class, () -> mediaService.uploadMedia(requestComArquivoVazio, emptyFile));
    }

    @Test
    void testUploadMedia_nullServiceName_throwsFileStorageException() {
        MediaRequest requestSemService = new MediaRequest(null, 42L);

        assertThrows(FileStorageException.class, () -> mediaService.uploadMedia(requestSemService, file));
    }

    @Test
    void testUploadMedia_emptyServiceName_throwsFileStorageException() {
        MediaRequest requestComServiceVazio = new MediaRequest("", 42L);

        assertThrows(FileStorageException.class, () -> mediaService.uploadMedia(requestComServiceVazio, file));
    }

    @Test
    void testUploadMedia_invalidFileName_throwsUnsupportedMediaTypeException() {
        MockMultipartFile invalidFile = new MockMultipartFile("file", "", "image/png", "dummy".getBytes());
        MediaRequest requestArquivoSemNome = new MediaRequest("educAPI", 42L);

        assertThrows(UnsupportedMediaTypeException.class, () -> mediaService.uploadMedia(requestArquivoSemNome, invalidFile));
    }

    @Test
    void testUploadMedia_minioFailure_throwsFileStorageException() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("Erro ao salvar no MinIO"));

        assertThrows(FileStorageException.class, () -> mediaService.uploadMedia(mediaRequest, file));
    }

    @Test
    void testUploadMedia_successful() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenReturn(mock(ObjectWriteResponse.class));
        log.info("Mock do MinIO configurado.");

        Media mediaMock = new Media();
        mediaMock.setEntityId(4L);
        mediaMock.setFileName("Screenshot from 2025-03-28 17-33-28.png");
        mediaMock.setServiceName("educAPI");

        when(mediaRepository.save(any(Media.class))).thenReturn(mediaMock);
        log.info("Mock do mediaRepository configurado.");

        MediaResponse expectedResponse = new MediaResponse(
                mediaMock.getEntityId(),
                mediaMock.getServiceName(),
                mediaMock.getFileName(),
                "https://minio.example.com/educAPI/Screenshot%20from%202025-03-28%2017-33-28.png"
        );
        when(mediaMapper.toResponse(any(Media.class))).thenReturn(expectedResponse);
        log.info("Mock do mediaMapper configurado.");

        log.info("Executando o método uploadMedia...");
        MediaResponse actualResponse = mediaService.uploadMedia(mediaRequest, file);

        log.info("Verificando se a resposta não é nula...");
        assertNotNull(actualResponse, "A resposta não pode ser nula");

        log.info("Validando a resposta...");
        assertEquals(expectedResponse.entityId(), actualResponse.entityId(), "ID da mídia não corresponde");
        assertEquals(expectedResponse.serviceName(), actualResponse.serviceName(), "Nome do serviço não corresponde");
        assertEquals(expectedResponse.fileName(), actualResponse.fileName(), "Nome do arquivo não corresponde");
        assertEquals(expectedResponse.url(), actualResponse.url(), "URL não corresponde");

        log.info("Verificando as interações com os mocks...");
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
        verify(mediaRepository, times(1)).save(any(Media.class));
        verify(mediaMapper, times(1)).toResponse(any(Media.class));

        log.info("Teste testUploadMedia_successful concluído com sucesso.");
    }







}
