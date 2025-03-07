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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MediaServiceImplSaveTest extends BaseMediaServiceImplTest {

    private MockMultipartFile file;
    private final String serviceName = "educAPI";

    @BeforeEach
    void init() {
        file = new MockMultipartFile("file", "test-image.png", "image/png", "dummy".getBytes());
    }

    @Test
    void testSaveMedia_nullServiceName_throwsFileStorageException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.saveMedia(null, file, "tag", EntityType.THEME, 1L));
    }

    @Test
    void testSaveMedia_emptyServiceName_throwsFileStorageException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.saveMedia("", file, "tag", EntityType.THEME, 1L));
    }

    @Test
    void testSaveMedia_nullEntityType_throwsFileStorageException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.saveMedia(serviceName, file, "tag", null, 1L));
    }

    @Test
    void testSaveMedia_emptyEntityType_throwsFileStorageException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.saveMedia(serviceName, file, "tag", EntityType.valueOf(""), 1L));
    }

    @Test
    void testSaveMedia_nullTag_throwsFileStorageException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.saveMedia(serviceName, file, null, EntityType.THEME, 1L));
    }

    @Test
    void testSaveMedia_emptyTag_throwsFileStorageException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.saveMedia(serviceName, file, "", EntityType.THEME, 1L));
    }

    @Test
    void testSaveMedia_nullUploadedBy_throwsFileStorageException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.saveMedia(serviceName, file, "tag", EntityType.THEME, null));
    }

    @Test
    void testSaveMedia_successful() throws Exception {
        mockMediaRepository(false);

        // Simulando retorno do MinIO
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));

        Media savedMedia = createMockMedia();
        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);
        when(mediaMapper.entityToDto(savedMedia)).thenReturn(createMockMediaDTO());

        MediaDTO result = mediaService.saveMedia(serviceName, file, "tag", EntityType.THEME, 1L);

        assertAll("MediaDTO attributes",
                () -> assertEquals(1L, result.getId()),
                () -> assertEquals(serviceName, result.getServiceName()),
                () -> assertEquals(EntityType.THEME, result.getEntityType()),
                () -> assertEquals(MediaType.IMAGE, result.getMediaType()),
                () -> assertEquals(1L, result.getUploadedBy()),
                () -> assertEquals("educAPI/test-image.png", result.getFileName()),
                () -> assertEquals("tag", result.getTag()),
                () -> assertTrue(result.getActive())
        );
    }



    @Test
    void testSaveMedia_nullFile_throwsInvalidFileException() {
        assertThrows(InvalidFileException.class,
                () -> mediaService.saveMedia(serviceName, null, "tag", EntityType.THEME, 1L));
    }

    @Test
    void testSaveMedia_emptyFile_throwsInvalidFileException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        assertThrows(InvalidFileException.class, () -> mediaService.saveMedia(serviceName, emptyFile, "tag", EntityType.THEME, 1L));
    }

    @Test
    void testSaveMedia_nullServiceName_throwsInvalidFileException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.saveMedia(null, file, "tag", EntityType.THEME, 1L));
    }

    @Test
    void testSaveMedia_emptyServiceName_throwsInvalidFileException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.saveMedia("", file, "tag", EntityType.THEME, 1L));
    }

    @Test
    void testSaveMedia_nullEntityType_throwsInvalidFileException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.saveMedia(serviceName, file, "tag", null, 1L));
    }

    @Test
    void testSaveMedia_nullTag_throwsInvalidFileException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.saveMedia(serviceName, file, null, EntityType.THEME, 1L));
    }

    @Test
    void testSaveMedia_emptyTag_throwsInvalidFileException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.saveMedia(serviceName, file, "", EntityType.THEME, 1L));
    }

    @Test
    void testSaveMedia_nullUploadedBy_throwsInvalidFileException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.saveMedia(serviceName, file, "tag", EntityType.THEME, null));
    }

    @Test
    void testSaveMedia_invalidFileName_throwsInvalidFileException() {
        MockMultipartFile invalidFile = new MockMultipartFile("file", "", "image/png", "dummy".getBytes());
        assertThrows(InvalidFileException.class, () ->
                mediaService.saveMedia(serviceName, invalidFile, "tag", EntityType.THEME, 1L));
    }


    @Test
    void testSaveMedia_noBucketConfigured_throwsBucketNotFoundException() {
        ReflectionTestUtils.setField(mediaService, "bucketName", "");
        assertThrows(BucketNotFoundException.class, () -> mediaService.saveMedia(serviceName, file, "tag", EntityType.THEME, 1L));
    }

    @Test
    void testSaveMedia_duplicateFile_throwsDuplicateFileException() {
        mockMediaRepository(true);
        assertThrows(DuplicateFileException.class, () -> mediaService.saveMedia(serviceName, file, "tag", EntityType.THEME, 1L));
        verify(mediaRepository, never()).save(any());
    }

    @Test
    void testSaveMedia_minioFailure_throwsFileStorageException() throws Exception {
        when(mediaRepository.existsByFileNameAndServiceNameAndActiveTrue("educAPI/test-image.png", "educAPI"))
                .thenReturn(false);

        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new IOException("Erro ao armazenar arquivo"));

        FileStorageException exception = assertThrows(FileStorageException.class, () ->
                mediaService.saveMedia("educAPI", file, "tag", EntityType.THEME, 1L));

        assertEquals("Erro ao salvar mídia no armazenamento.", exception.getMessage());
    }

    private void mockMediaRepository(boolean fileExists) {
        when(mediaRepository.existsByFileNameAndServiceNameAndActiveTrue("educAPI/test-image.png", serviceName))
                .thenReturn(fileExists);
    }

    private Media createMockMedia() {
        return Media.builder()
                .id(1L)
                .serviceName(serviceName)
                .entityType(EntityType.THEME)
                .mediaType(MediaType.IMAGE)
                .uploadedBy(1L)
                .fileName("educAPI/test-image.png")
                .tag("tag")
                .active(true)
                .build();
    }

    private MediaDTO createMockMediaDTO() {
        return MediaDTO.builder()
                .id(1L)
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
