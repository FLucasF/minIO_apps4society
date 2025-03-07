package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.exceptions.*;
import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
class MediaServiceImplUpdateTest extends BaseMediaServiceImplTest {

    private MockMultipartFile file;
    private final String serviceName = "educAPI";

    @BeforeEach
    void init() {
        file = new MockMultipartFile("file", "test-image.png", "image/png", "dummy".getBytes());
    }

    @Test
    void testUpdateMedia_nullServiceName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.updateMedia(null, 1L, EntityType.THEME, "tag", MediaType.IMAGE, file));
    }

    @Test
    void testUpdateMedia_emptyServiceName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.updateMedia("", 1L, EntityType.THEME, "tag", MediaType.IMAGE, file));
    }

    @Test
    void testUpdateMedia_nullMediaId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.updateMedia(serviceName, null, EntityType.THEME, "tag", MediaType.IMAGE, file));
    }

    @Test
    void testUpdateMedia_nullEntityType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.updateMedia(serviceName, 1L, null, "tag", MediaType.IMAGE, file));
    }

    @Test
    void testUpdateMedia_nullTag_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.updateMedia(serviceName, 1L, EntityType.THEME, null, MediaType.IMAGE, file));
    }

    @Test
    void testUpdateMedia_emptyTag_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.updateMedia(serviceName, 1L, EntityType.THEME, "", MediaType.IMAGE, file));
    }

    @Test
    void testUpdateMedia_nullFile_throwsInvalidFileException() {
        assertThrows(InvalidFileException.class, () ->
                mediaService.updateMedia(serviceName, 1L, EntityType.THEME, "tag", MediaType.IMAGE, null));
    }

    @Test
    void testUpdateMedia_emptyFile_throwsInvalidFileException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThrows(InvalidFileException.class, () ->
                mediaService.updateMedia(serviceName, 1L, EntityType.THEME, "tag", MediaType.IMAGE, emptyFile));
    }

    @Test
    void testUpdateMedia_noBucketConfigured_throwsBucketNotFoundException() {
        ReflectionTestUtils.setField(mediaService, "bucketName", "");

        assertThrows(BucketNotFoundException.class, () ->
                mediaService.updateMedia(serviceName, 1L, EntityType.THEME, "tag", MediaType.IMAGE, file));
    }

    @Test
    void testUpdateMedia_mediaNotFound_throwsMediaNotFoundException() {
        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(1L, serviceName)).thenReturn(Optional.empty());

        MediaNotFoundException exception = assertThrows(MediaNotFoundException.class, () ->
                mediaService.updateMedia(serviceName, 1L, EntityType.THEME, "tag", MediaType.IMAGE, file));

        assertEquals("Mídia não encontrada para o serviço informado.", exception.getMessage());
    }

    @Test
    void testUpdateMedia_minioFailure_throwsFileStorageException() throws Exception {
        Media media = Media.builder()
                .id(1L)
                .serviceName(serviceName)
                .fileName("educAPI/test-image.png")
                .active(true)
                .build();

        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(1L, serviceName)).thenReturn(Optional.of(media));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new IOException("Erro ao atualizar mídia"));

        FileStorageException exception = assertThrows(FileStorageException.class, () ->
                mediaService.updateMedia(serviceName, 1L, EntityType.THEME, "tag", MediaType.IMAGE, file));

        assertEquals("Erro ao atualizar a mídia no armazenamento.", exception.getMessage());
    }

    @Test
    void testUpdateMedia_successful() throws Exception {
        Media media = Media.builder()
                .id(1L)
                .serviceName(serviceName)
                .fileName("educAPI/test-image.png")
                .entityType(EntityType.THEME)
                .mediaType(MediaType.IMAGE)
                .tag("oldTag")
                .active(true)
                .build();

        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(1L, serviceName)).thenReturn(Optional.of(media));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));

        Media updatedMedia = Media.builder()
                .id(1L)
                .serviceName(serviceName)
                .fileName("educAPI/test-image.png")
                .entityType(EntityType.THEME)
                .mediaType(MediaType.IMAGE)
                .tag("tag")
                .active(true)
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(updatedMedia);
        when(mediaMapper.entityToDto(updatedMedia)).thenReturn(MediaDTO.builder()
                .id(1L)
                .serviceName(serviceName)
                .fileName("educAPI/test-image.png")
                .entityType(EntityType.THEME)
                .mediaType(MediaType.IMAGE)
                .tag("tag")
                .active(true)
                .build());

        MediaDTO result = mediaService.updateMedia(serviceName, 1L, EntityType.THEME, "tag", MediaType.IMAGE, file);

        assertAll("MediaDTO attributes",
                () -> assertEquals(1L, result.getId()),
                () -> assertEquals(serviceName, result.getServiceName()),
                () -> assertEquals(EntityType.THEME, result.getEntityType()),
                () -> assertEquals(MediaType.IMAGE, result.getMediaType()),
                () -> assertEquals("tag", result.getTag()),
                () -> assertEquals("educAPI/test-image.png", result.getFileName()),
                () -> assertTrue(result.getActive())
        );
    }
}
