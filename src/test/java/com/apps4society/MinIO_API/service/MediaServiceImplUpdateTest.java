package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.exceptions.FileStorageException;
import com.apps4society.MinIO_API.exceptions.InvalidFileException;
import com.apps4society.MinIO_API.exceptions.MediaNotFoundException;
import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.MediaType;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MediaServiceImplUpdateTest extends BaseMediaServiceImplTest {

    private MockMultipartFile file;
    private final Long entityId = 1001L;
    private Media existingMedia;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(mediaService, "bucketName", "test-bucket");

        file = new MockMultipartFile("file", "new-image.png", "image/png", "dummy".getBytes());

        existingMedia = new Media(42L, "old-image.png", serviceName, MediaType.IMAGE);
        existingMedia.setEntityId(entityId);
        existingMedia.setActive(true);
    }

    @Test
    void testUpdateMedia_mediaNotFound_throwsMediaNotFoundException() {
        when(mediaRepository.findByEntityIdAndServiceNameAndActiveTrue(entityId, serviceName))
                .thenReturn(Optional.empty());

        MediaRequest request = new MediaRequest(serviceName, 42L);

        MediaNotFoundException exception = assertThrows(MediaNotFoundException.class, () ->
                mediaService.updateMedia(entityId, request, file));

        assertEquals("Mídia não encontrada ou inativa.", exception.getMessage());
    }

    @Test
    void testUpdateMedia_successful() throws Exception {
        when(mediaRepository.findByEntityIdAndServiceNameAndActiveTrue(entityId, serviceName))
                .thenReturn(Optional.of(existingMedia));

        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));

        existingMedia.setFileName("new-image.png"); // Nome salvo no banco

        when(mediaRepository.save(any(Media.class))).thenReturn(existingMedia);

        MediaResponse expectedResponse = new MediaResponse(
                existingMedia.getEntityId(),
                existingMedia.getServiceName(),
                "new-image.png",
                "https://minio.example.com/educAPI/new-image.png"
        );

        when(mediaMapper.toResponse(any(Media.class))).thenReturn(expectedResponse);

        MediaRequest request = new MediaRequest(serviceName, 42L);

        MediaResponse result = mediaService.updateMedia(entityId, request, file);

        assertAll("Validando resposta da atualização",
                () -> assertEquals(existingMedia.getEntityId(), result.entityId()),
                () -> assertEquals(existingMedia.getServiceName(), result.serviceName()),
                () -> assertEquals("new-image.png", result.fileName()),
                () -> assertEquals("https://minio.example.com/educAPI/new-image.png", result.url())
        );

        // Verifica chamadas nos mocks
        verify(mediaRepository, times(1)).findByEntityIdAndServiceNameAndActiveTrue(entityId, serviceName);
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
        verify(mediaRepository, times(1)).save(any(Media.class));
        verify(mediaMapper, times(1)).toResponse(any(Media.class));
    }

    @Test
    void testUpdateMedia_emptyFile_throwsInvalidFileException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        MediaRequest request = new MediaRequest(serviceName, 42L);  // Ajuste para o novo construtor

        when(mediaRepository.findByEntityIdAndServiceNameAndActiveTrue(entityId, serviceName))
                .thenReturn(Optional.of(existingMedia));

        InvalidFileException exception = assertThrows(InvalidFileException.class, () ->
                mediaService.updateMedia(entityId, request, emptyFile));

        assertEquals("O arquivo enviado para upload está vazio ou sem nome.", exception.getMessage());

        verify(mediaRepository, times(1)).findByEntityIdAndServiceNameAndActiveTrue(entityId, serviceName);
        verifyNoInteractions(minioClient);
        verifyNoInteractions(mediaMapper);
    }

    @Test
    void testUpdateMedia_minioFailure_throwsFileStorageException() throws Exception {
        MediaRequest request = new MediaRequest(serviceName, 42L);  // Ajuste para o novo construtor

        when(mediaRepository.findByEntityIdAndServiceNameAndActiveTrue(entityId, serviceName))
                .thenReturn(Optional.of(existingMedia));

        // Simula um erro ao enviar para o MinIO
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("Erro simulado ao salvar no MinIO"));

        FileStorageException exception = assertThrows(FileStorageException.class, () ->
                mediaService.updateMedia(entityId, request, file));

        assertEquals("Erro ao atualizar a mídia no armazenamento.", exception.getMessage());

        verify(mediaRepository, times(1)).findByEntityIdAndServiceNameAndActiveTrue(entityId, serviceName);
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
        verifyNoInteractions(mediaMapper);
    }
}
