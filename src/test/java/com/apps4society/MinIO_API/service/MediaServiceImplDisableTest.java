package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.exceptions.FileStorageException;
import com.apps4society.MinIO_API.exceptions.MediaNotFoundException;
import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import io.minio.CopyObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MediaServiceImplDisableTest extends BaseMediaServiceImplTest {

    private final String serviceName = "educAPI";
    private final Long mediaId = 1L;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(mediaService, "bucketName", "test-bucket");
    }

    @Test
    void testDisableMedia_nullServiceName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.disableMedia(null, 1L));
    }


    @Test
    void testDisableMedia_emptyServiceName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.disableMedia("", 1L));
    }


    @Test
    void testDisableMedia_mediaNotFound_throwsMediaNotFoundException() {
        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName)).thenReturn(Optional.empty());

        MediaNotFoundException exception = assertThrows(MediaNotFoundException.class, () ->
                mediaService.disableMedia(serviceName, mediaId));

        assertEquals("Mídia não encontrada para o serviço informado.", exception.getMessage());
    }

    @Test
    void testDisableMedia_moveFails_throwsFileStorageException() throws Exception {
        Media media = createMockMedia();

        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(mediaId, serviceName)).thenReturn(Optional.of(media));

        doThrow(new RuntimeException("MinIO copy error")).when(minioClient).copyObject(any(CopyObjectArgs.class));

        FileStorageException exception = assertThrows(FileStorageException.class, () ->
                mediaService.disableMedia(serviceName, mediaId));

        assertEquals("Erro ao mover mídia no armazenamento.", exception.getMessage());
    }

    @Test
    void testDisableMedia_successful() throws Exception {
        // Arrange - Criando mock da mídia ativa
        Media media = Media.builder()
                .id(1L)
                .serviceName("educAPI")
                .entityType(EntityType.THEME)
                .mediaType(MediaType.IMAGE)
                .uploadedBy(1L)
                .fileName("educAPI/test-image.png")
                .tag("tag")
                .active(true)
                .build();

        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(media.getId(), media.getServiceName()))
                .thenReturn(Optional.of(media));

        // Criando manualmente um novo objeto com active = false
        Media disabledMedia = Media.builder()
                .id(media.getId())
                .serviceName(media.getServiceName())
                .entityType(media.getEntityType())
                .mediaType(media.getMediaType())
                .uploadedBy(media.getUploadedBy())
                .fileName(media.getFileName())
                .tag(media.getTag())
                .active(false) // Agora desativado
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(disabledMedia);

        // Criando mock do DTO retornado
        MediaDTO expectedDto = new MediaDTO(
                disabledMedia.getId(),
                disabledMedia.getServiceName(),
                disabledMedia.getMediaType(),
                disabledMedia.getEntityType(),
                disabledMedia.getUploadedBy(),
                disabledMedia.getFileName(),
                disabledMedia.getTag(),
                disabledMedia.isActive()
        );

        when(mediaMapper.entityToDto(disabledMedia)).thenReturn(expectedDto);

        // Act - Desativando a mídia
        MediaDTO result = mediaService.disableMedia(media.getServiceName(), media.getId());

        // Assert - Verificando os atributos retornados
        assertAll("Disabled MediaDTO attributes",
                () -> assertEquals(disabledMedia.getId(), result.getId()),
                () -> assertEquals(disabledMedia.getServiceName(), result.getServiceName()),
                () -> assertEquals(disabledMedia.getEntityType(), result.getEntityType()),
                () -> assertEquals(disabledMedia.getMediaType(), result.getMediaType()),
                () -> assertEquals(disabledMedia.getUploadedBy(), result.getUploadedBy()),
                () -> assertEquals(disabledMedia.getFileName(), result.getFileName()),
                () -> assertEquals(disabledMedia.getTag(), result.getTag()),
                () -> assertFalse(result.getActive()) // Confirma que a mídia foi desativada
        );

        // Verifica se os métodos necessários foram chamados
        verify(minioClient, times(1)).copyObject(any(CopyObjectArgs.class));
        verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
        verify(mediaRepository, times(1)).save(any(Media.class));
    }


    private Media createMockMedia() {
        return Media.builder()
                .id(mediaId)
                .serviceName(serviceName)
                .entityType(EntityType.THEME)
                .mediaType(MediaType.IMAGE)
                .uploadedBy(1L)
                .fileName("educAPI/test-image.png")
                .tag("tag")
                .active(true)
                .build();
    }
}
