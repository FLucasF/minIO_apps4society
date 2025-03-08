package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.exceptions.FileStorageException;
import com.apps4society.MinIO_API.exceptions.InvalidFileException;
import com.apps4society.MinIO_API.exceptions.UnsupportedMediaTypeException;
import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.MediaType;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.ObjectWriteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MediaServiceImplUploadTest extends BaseMediaServiceImplTest {

    private MockMultipartFile file;
    private MediaRequest mediaRequest;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(mediaService, "bucketName", "test-bucket");

        // Simulando um arquivo válido
        file = new MockMultipartFile("file", "test-image.png", "image/png", "dummy".getBytes());

        // Criando um MediaRequest válido
        mediaRequest = new MediaRequest(
                "educAPI", // serviceName
                42L,       // uploadedBy
                1001L,     // entityId
                file       // file
        );
    }

    @Test
    void testUploadMedia_nullFile_throwsInvalidFileException() {
        MediaRequest requestSemArquivo = new MediaRequest("educAPI", 42L, 1001L, null);

        assertThrows(NullPointerException.class, () -> mediaService.uploadMedia(requestSemArquivo));
    }

    @Test
    void testUploadMedia_emptyFile_throwsInvalidFileException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        MediaRequest requestComArquivoVazio = new MediaRequest("educAPI", 42L, 1001L, emptyFile);

        assertThrows(InvalidFileException.class, () -> mediaService.uploadMedia(requestComArquivoVazio));
    }

    @Test
    void testUploadMedia_nullServiceName_throwsInvalidFileException() {
        MediaRequest requestSemService = new MediaRequest(null, 42L, 1001L, file);

        assertThrows(FileStorageException.class, () -> mediaService.uploadMedia(requestSemService));
    }

    @Test
    void testUploadMedia_emptyServiceName_throwsInvalidFileException() {
        MediaRequest requestComServiceVazio = new MediaRequest("", 42L, 1001L, file);

        assertThrows(FileStorageException.class, () -> mediaService.uploadMedia(requestComServiceVazio));
    }

    @Test
    void testUploadMedia_invalidFileName_throwsInvalidFileException() {
        MockMultipartFile invalidFile = new MockMultipartFile("file", "", "image/png", "dummy".getBytes());
        MediaRequest requestArquivoSemNome = new MediaRequest("educAPI", 42L, 1001L, invalidFile);

        assertThrows(UnsupportedMediaTypeException.class, () -> mediaService.uploadMedia(requestArquivoSemNome));
    }

    @Test
    void testUploadMedia_minioFailure_throwsFileStorageException() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("Erro ao salvar no MinIO"));

        assertThrows(FileStorageException.class, () -> mediaService.uploadMedia(mediaRequest));
    }

    @Test
    void testUploadMedia_successful() throws Exception {
        // Simulando a resposta do MinIO
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));

        // Criando uma mídia simulada salva no banco
        Media savedMedia = new Media(1L, "educAPI", MediaType.IMAGE, "educAPI/test-image.png", 1001L, true);
        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

        // Criando um mock da resposta esperada
        MediaResponse expectedResponse = new MediaResponse(
                1L, "educAPI", "test-image.png", "https://minio.example.com/educAPI/test-image.png"
        );
        when(mediaMapper.toResponse(savedMedia)).thenReturn(expectedResponse);

        // Executando o método
        MediaResponse result = mediaService.uploadMedia(mediaRequest);

        // Validações dos atributos retornados
        assertAll("Validando resposta do upload",
                () -> assertEquals(expectedResponse.id(), result.id()),
                () -> assertEquals(expectedResponse.serviceName(), result.serviceName()),
                () -> assertEquals(expectedResponse.fileName(), result.fileName()),
                () -> assertEquals(expectedResponse.url(), result.url())
        );

        // Verificando se os métodos foram chamados corretamente
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
        verify(mediaRepository, times(1)).save(any(Media.class));
    }
}
