package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.exceptions.MediaNotFoundException;
import com.apps4society.MinIO_API.exceptions.MinIOConnectionException;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.MediaType;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.errors.ErrorResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MediaServiceImplGetTest extends BaseMediaServiceImplTest {

    private final Long mediaId = 1L;
    private final String serviceName = "educAPI";
    private Media mediaMock;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(mediaService, "bucketName", "test-bucket");

        // Criando um mock da mídia ativa
        mediaMock = Media.builder()
                .id(mediaId)
                .serviceName(serviceName)
                .fileName("test-image.png") // Apenas o nome do arquivo
                .mediaType(MediaType.IMAGE)
                .active(true)
                .build();
    }

    @Test
    void testGetMedia_mediaNotFound_throwsMediaNotFoundException() {
        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName))
                .thenReturn(Optional.empty());

        MediaNotFoundException exception = assertThrows(MediaNotFoundException.class, () ->
                mediaService.getMediaUrl(serviceName, mediaId));

        assertEquals("Mídia não encontrada ou inativa.", exception.getMessage());
    }

    @Test
    void testGetMedia_successful() throws Exception {
        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName))
                .thenReturn(Optional.of(mediaMock));

        // Mock para a URL assinada do MinIO
        String expectedUrl = "https://minio.example.com/educAPI/test-image.png";
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn(expectedUrl);

        String result = mediaService.getMediaUrl(serviceName, mediaId);

        assertNotNull(result);
        assertEquals(expectedUrl, result);

        verify(mediaRepository, times(1)).findByIdAndServiceNameAndActiveTrue(mediaId, serviceName);
        verify(minioClient, times(1)).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    void testGetMediaUrl_minioFailure_throwsMinIOConnectionException() throws Exception {
        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName))
                .thenReturn(Optional.of(mediaMock));

        // 🔥 Simular falha no MinIO lançando uma exceção realista
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new IOException("Erro simulado ao acessar MinIO"));

        MinIOConnectionException exception = assertThrows(MinIOConnectionException.class, () ->
                mediaService.getMediaUrl(serviceName, mediaId));

        assertEquals("Erro ao gerar URL assinada da mídia.", exception.getMessage());

        verify(mediaRepository, times(1)).findByIdAndServiceNameAndActiveTrue(mediaId, serviceName);
        verify(minioClient, times(1)).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

}
