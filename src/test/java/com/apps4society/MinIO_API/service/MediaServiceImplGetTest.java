package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.exceptions.MediaNotFoundException;
import com.apps4society.MinIO_API.exceptions.MinIOConnectionException;
import io.minio.GetPresignedObjectUrlArgs;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MediaServiceImplGetTest extends BaseMediaServiceImplTest {

    @Test
    void testGetMedia_mediaNotFound_throwsMediaNotFoundException() {
        when(mediaRepository.findByEntityIdAndServiceNameAndActiveTrue(mediaId, serviceName))
                .thenReturn(Optional.empty());

        MediaNotFoundException exception = assertThrows(MediaNotFoundException.class, () ->
                mediaService.getMediaUrl(serviceName, mediaId));

        assertEquals("Mídia não encontrada ou inativa.", exception.getMessage());
    }

    @Test
    void testGetMedia_successful() throws Exception {
        when(mediaRepository.findByEntityIdAndServiceNameAndActiveTrue(mediaId, serviceName))
                .thenReturn(Optional.of(existingMedia));

        String expectedUrl = "https://minio.example.com/educAPI/test-image.png";
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn(expectedUrl);

        String result = mediaService.getMediaUrl(serviceName, mediaId);

        assertNotNull(result);
        assertEquals(expectedUrl, result);

        verify(mediaRepository, times(1)).findByEntityIdAndServiceNameAndActiveTrue(mediaId, serviceName);
        verify(minioClient, times(1)).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    void testGetMediaUrl_minioFailure_throwsMinIOConnectionException() throws Exception {
        when(mediaRepository.findByEntityIdAndServiceNameAndActiveTrue(mediaId, serviceName))
                .thenReturn(Optional.of(existingMedia));

        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("Erro simulado ao acessar MinIO"));

        MinIOConnectionException exception = assertThrows(MinIOConnectionException.class, () ->
                mediaService.getMediaUrl(serviceName, mediaId));

        assertEquals("Erro ao gerar URL assinada da mídia.", exception.getMessage());

        verify(mediaRepository, times(1)).findByEntityIdAndServiceNameAndActiveTrue(mediaId, serviceName);
        verify(minioClient, times(1)).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }
}
