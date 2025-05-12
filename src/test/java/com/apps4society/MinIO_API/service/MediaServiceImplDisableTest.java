package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.exceptions.FileStorageException;
import com.apps4society.MinIO_API.exceptions.MediaNotFoundException;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.MediaType;
import io.minio.CopyObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MediaServiceImplDisableTest extends BaseMediaServiceImplTest {

    @Test
    void testDisableMedia_mediaNotFound_throwsMediaNotFoundException() {
        when(mediaRepository.findByEntityIdAndServiceNameAndActiveTrue(mediaId, serviceName))
                .thenReturn(Optional.empty());

        MediaNotFoundException exception = assertThrows(MediaNotFoundException.class, () ->
                mediaService.disableMedia(serviceName, mediaId));

        assertEquals("Mídia não encontrada ou inativa.", exception.getMessage());
    }

    @Test
    void testDisableMedia_successful() throws Exception {
        when(mediaRepository.findByEntityIdAndServiceNameAndActiveTrue(mediaId, serviceName))
                .thenReturn(Optional.of(existingMedia));

        when(minioClient.copyObject(any(CopyObjectArgs.class)))
                .thenReturn(mock(io.minio.ObjectWriteResponse.class));

        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

        ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
        when(mediaRepository.save(mediaCaptor.capture())).thenReturn(existingMedia);

        mediaService.disableMedia(serviceName, mediaId);

        Media savedMedia = mediaCaptor.getValue();
        assertFalse(savedMedia.isActive(), "A mídia deveria estar desativada após o método disableMedia.");

        verify(mediaRepository, times(1)).findByEntityIdAndServiceNameAndActiveTrue(mediaId, serviceName);
        verify(minioClient, times(1)).copyObject(any(CopyObjectArgs.class));
        verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
        verify(mediaRepository, times(1)).save(any(Media.class));
    }


    @Test
    void testDisableMedia_minioFailure_throwsFileStorageException() throws Exception {
        when(mediaRepository.findByEntityIdAndServiceNameAndActiveTrue(mediaId, serviceName))
                .thenReturn(Optional.of(existingMedia));

        doThrow(new RuntimeException("Erro simulado no MinIO"))
                .when(minioClient).copyObject(any(CopyObjectArgs.class));

        FileStorageException exception = assertThrows(FileStorageException.class, () ->
                mediaService.disableMedia(serviceName, mediaId));

        assertEquals("Erro ao mover mídia no armazenamento.", exception.getMessage());

        verify(mediaRepository, times(1)).findByEntityIdAndServiceNameAndActiveTrue(mediaId, serviceName);
        verify(minioClient, times(1)).copyObject(any(CopyObjectArgs.class));
        verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
        verify(mediaRepository, never()).save(any(Media.class));
    }

}
