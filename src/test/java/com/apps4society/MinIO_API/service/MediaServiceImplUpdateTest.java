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
    private final String serviceName = "educAPI";
    private final Long mediaId = 1L;
    private Media existingMedia;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(mediaService, "bucketName", "test-bucket");

        file = new MockMultipartFile("file", "new-image.png", "image/png", "dummy".getBytes());

        existingMedia = Media.builder()
                .id(mediaId)
                .serviceName(serviceName)
                .mediaType(MediaType.IMAGE)
                .fileName("old-image.png")
                .entityId(1001L)
                .active(true)
                .build();
    }

    @Test
    void testUpdateMedia_mediaNotFound_throwsMediaNotFoundException() {
        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName))
                .thenReturn(Optional.empty());

        MediaRequest request = new MediaRequest(serviceName, 42L, 1001L, file);

        MediaNotFoundException exception = assertThrows(MediaNotFoundException.class, () ->
                mediaService.updateMedia(serviceName, mediaId, request));

        assertEquals("Mídia não encontrada ou inativa.", exception.getMessage());
    }

    @Test
    void testUpdateMedia_successful() throws Exception {
        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName))
                .thenReturn(Optional.of(existingMedia));

        // Mock correto para `putObject`
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));

        // Criar mídia atualizada
        Media updatedMedia = Media.builder()
                .id(existingMedia.getId())
                .serviceName(existingMedia.getServiceName())
                .mediaType(MediaType.IMAGE)
                .fileName("new-image.png") // Nome salvo no banco
                .entityId(existingMedia.getEntityId())
                .active(true)
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(updatedMedia);

        // Simular a conversão para `MediaResponse`
        MediaResponse expectedResponse = new MediaResponse(
                updatedMedia.getId(),
                updatedMedia.getServiceName(),
                "new-image.png",
                "https://minio.example.com/educAPI/new-image.png"
        );

        when(mediaMapper.toResponse(any(Media.class))).thenReturn(expectedResponse);

        MediaRequest request = new MediaRequest(serviceName, 42L, 1001L, file);

        // Executar a atualização da mídia
        MediaResponse result = mediaService.updateMedia(serviceName, mediaId, request);

        // Validar a resposta gerada
        assertAll("Validando resposta da atualização",
                () -> assertEquals(updatedMedia.getId(), result.id()),
                () -> assertEquals(updatedMedia.getServiceName(), result.serviceName()),
                () -> assertEquals("new-image.png", result.fileName()),
                () -> assertEquals("https://minio.example.com/educAPI/new-image.png", result.url())
        );

        // Verifica chamadas nos mocks
        verify(mediaRepository, times(1)).findByIdAndServiceNameAndActiveTrue(mediaId, serviceName);
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
        verify(mediaRepository, times(1)).save(any(Media.class));
        verify(mediaMapper, times(1)).toResponse(any(Media.class));
    }

    @Test
    void testUpdateMedia_emptyFile_throwsInvalidFileException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        MediaRequest request = new MediaRequest(serviceName, 42L, 1001L, emptyFile);

        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName))
                .thenReturn(Optional.of(existingMedia));

        InvalidFileException exception = assertThrows(InvalidFileException.class, () ->
                mediaService.updateMedia(serviceName, mediaId, request));

        assertEquals("O arquivo enviado para atualização está vazio ou sem nome.", exception.getMessage());

        verify(mediaRepository, times(1)).findByIdAndServiceNameAndActiveTrue(mediaId, serviceName);
        verifyNoInteractions(minioClient);
        verifyNoInteractions(mediaMapper);
    }

    @Test
    void testUpdateMedia_nullFileName_throwsInvalidFileException() {
        MockMultipartFile fileWithoutName = new MockMultipartFile("file", null, "image/png", "dummy".getBytes());
        MediaRequest request = new MediaRequest(serviceName, 42L, 1001L, fileWithoutName);

        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName))
                .thenReturn(Optional.of(existingMedia));

        FileStorageException exception = assertThrows(FileStorageException.class, () ->
                mediaService.updateMedia(serviceName, mediaId, request));

        assertEquals("Erro ao atualizar a mídia no armazenamento.", exception.getMessage());

        verify(mediaRepository, times(1)).findByIdAndServiceNameAndActiveTrue(mediaId, serviceName);
        verifyNoInteractions(minioClient);
        verifyNoInteractions(mediaMapper);
    }

    @Test
    void testUpdateMedia_minioFailure_throwsFileStorageException() throws Exception {
        MediaRequest request = new MediaRequest(serviceName, 42L, 1001L, validFile);

        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName))
                .thenReturn(Optional.of(existingMedia));

        // 🔥 Simula um erro ao enviar para o MinIO
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("Erro simulado ao salvar no MinIO"));

        FileStorageException exception = assertThrows(FileStorageException.class, () ->
                mediaService.updateMedia(serviceName, mediaId, request));

        assertEquals("Erro ao atualizar a mídia no armazenamento.", exception.getMessage());

        verify(mediaRepository, times(1)).findByIdAndServiceNameAndActiveTrue(mediaId, serviceName);
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
        verifyNoInteractions(mediaMapper);
    }
}
