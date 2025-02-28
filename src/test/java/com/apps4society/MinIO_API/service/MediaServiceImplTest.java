//package com.apps4society.MinIO_API.service;
//
//import com.apps4society.MinIO_API.exceptions.*;
//import com.apps4society.MinIO_API.mapper.MediaMapper;
//import com.apps4society.MinIO_API.model.DTO.MediaDTO;
//import com.apps4society.MinIO_API.model.entity.Media;
//import com.apps4society.MinIO_API.model.enums.EntityType;
//import com.apps4society.MinIO_API.model.enums.MediaType;
//import com.apps4society.MinIO_API.repository.MediaRepository;
//import io.minio.GetPresignedObjectUrlArgs;
//import io.minio.MinioClient;
//import io.minio.ObjectWriteResponse;
//import io.minio.PutObjectArgs;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.*;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.util.Map;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//class MediaServiceImplTest {
//
//    @Mock
//    private MediaRepository mediaRepository;
//
//    @Mock
//    private MediaMapper mediaMapper;
//
//    @Mock
//    private MinioClient minioClient;
//
//    @InjectMocks
//    private MediaServiceImpl mediaService;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//        // Injetando o valor do bucket para os testes
//        ReflectionTestUtils.setField(mediaService, "bucketName", "test-bucket");
//    }
//
//    // ---------------------------
//    // Testes para saveMedia(...)
//    // ---------------------------
//    @Test
//    void testSaveMedia_nullFile_throwsInvalidFileException() {
//        assertThrows(InvalidFileException.class, () ->
//                mediaService.saveMedia("educAPI", null, "tag", EntityType.THEME, 1L));
//    }
//
//    @Test
//    void testSaveMedia_emptyFile_throwsInvalidFileException() {
//        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
//        assertThrows(InvalidFileException.class, () ->
//                mediaService.saveMedia("educAPI", emptyFile, "tag", EntityType.THEME, 1L));
//    }
//
//    @Test
//    void testSaveMedia_noBucketConfigured_throwsBucketNotFoundException() {
//        // Forçando bucket vazio
//        ReflectionTestUtils.setField(mediaService, "bucketName", "");
//        MockMultipartFile file = new MockMultipartFile("file", "test-image.png", "image/png", "dummy".getBytes());
//        assertThrows(BucketNotFoundException.class, () ->
//                mediaService.saveMedia("educAPI", file, "tag", EntityType.THEME, 1L));
//    }
//
//    @Test
//    void testSaveMedia_duplicateFile_throwsDuplicateFileException() {
//        // Cria o arquivo de teste
//        MockMultipartFile file = new MockMultipartFile("file", "test-image.png", "image/png", "dummy".getBytes());
//
//        // O método buildObjectName irá gerar "educAPI/test-image.png"
//        when(mediaRepository.existsByFileNameAndActive("educAPI/test-image.png", true)).thenReturn(true);
//
//        // Verifica se a DuplicateFileException é lançada
//        assertThrows(DuplicateFileException.class, () ->
//                mediaService.saveMedia("educAPI", file, "tag", EntityType.THEME, 1L));
//    }
//
//
//    @Test
//    void testSaveMedia_unsupportedFileType_throwsFileStorageExceptionWithCause() {
//        // Extensão "xyz" não está mapeada
//        MockMultipartFile file = new MockMultipartFile("file", "test-file.xyz", "application/octet-stream", "dummy".getBytes());
//        when(mediaRepository.existsByFileNameAndActive("test-file.xyz", true)).thenReturn(false);
//
//        FileStorageException exception = assertThrows(FileStorageException.class, () ->
//                mediaService.saveMedia("educAPI", file, "tag", EntityType.THEME, 1L));
//
//        // Verifica se a causa é uma UnsupportedMediaTypeException e se a mensagem está correta
//        assertNotNull(exception.getCause());
//        assertTrue(exception.getCause() instanceof UnsupportedMediaTypeException);
//        assertEquals("Tipo de mídia não suportado: xyz", exception.getCause().getMessage());
//    }
//
//    @Test
//    void testSaveMedia_successful() throws Exception {
//        // ARRANGE
//        MockMultipartFile file = new MockMultipartFile("file", "test-image.png", "image/png", "dummy".getBytes());
//        when(mediaRepository.existsByFileNameAndActive("test-image.png", true)).thenReturn(false);
//
//        // Simula a resposta do método putObject, que retorna um ObjectWriteResponse
//        ObjectWriteResponse mockResponse = mock(ObjectWriteResponse.class);
//        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mockResponse);
//
//        Media savedMedia = Media.builder()
//                .id(1L)
//                .serviceName("educAPI")
//                .fileName("test-image.png")
//                .mediaType(MediaType.IMAGE)
//                .entityType(EntityType.THEME)
//                .uploadedBy(1L)
//                .tag("tag")
//                .active(true)
//                .build();
//        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);
//
//        MediaDTO expectedDto = MediaDTO.builder().fileName("educAPI/test-image.png").build();
//        when(mediaMapper.entityToDto(savedMedia)).thenReturn(expectedDto);
//
//        // ACT
//        MediaDTO result = mediaService.saveMedia("educAPI", file, "tag", EntityType.THEME, 1L);
//
//        // ASSERT
//        assertEquals("educAPI/test-image.png", result.getFileName());
//    }
//
//    // ---------------------------
//    // Testes para getMediaUrl(...)
//    // ---------------------------
//    @Test
//    void testGetMediaUrl_mediaNotFound_throwsMediaNotFoundException() {
//        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(1L, "educAPI")).thenReturn(Optional.empty());
//        assertThrows(MediaNotFoundException.class, () ->
//                mediaService.getMediaUrl("educAPI", 1L));
//    }
//
//    @Test
//    void testGetMediaUrl_successful() throws Exception {
//        Media media = Media.builder()
//                .id(1L)
//                .serviceName("educAPI")
//                .fileName("test-image.png")
//                .build();
//        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(1L, "educAPI")).thenReturn(Optional.of(media));
//
//        String signedUrl = "http://minio/test-image.png";
//        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(signedUrl);
//
//        Map<String, String> result = mediaService.getMediaUrl("educAPI", 1L);
//        assertEquals(signedUrl, result.get("url"));
//    }
//
//    // ---------------------------
//    // Testes para updateMedia(...)
//    // ---------------------------
//    @Test
//    void testUpdateMedia_nullFile_throwsInvalidFileException() {
//        assertThrows(InvalidFileException.class, () ->
//                mediaService.updateMedia("educAPI", 1L, EntityType.THEME, "tag", MediaType.IMAGE, null));
//    }
//
//    @Test
//    void testUpdateMedia_noBucketConfigured_throwsBucketNotFoundException() {
//        ReflectionTestUtils.setField(mediaService, "bucketName", "");
//        MockMultipartFile file = new MockMultipartFile("file", "updated-image.png", "image/png", "dummy".getBytes());
//        assertThrows(BucketNotFoundException.class, () ->
//                mediaService.updateMedia("educAPI", 1L, EntityType.THEME, "tag", MediaType.IMAGE, file));
//    }
//
//    @Test
//    void testUpdateMedia_mediaNotFound_throwsMediaNotFoundException() {
//        MockMultipartFile file = new MockMultipartFile("file", "updated-image.png", "image/png", "dummy".getBytes());
//        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(1L, "educAPI")).thenReturn(Optional.empty());
//        assertThrows(MediaNotFoundException.class, () ->
//                mediaService.updateMedia("educAPI", 1L, EntityType.THEME, "tag", MediaType.IMAGE, file));
//    }
//
//    @Test
//    void testUpdateMedia_successful() throws Exception {
//        // ARRANGE
//        MockMultipartFile file = new MockMultipartFile("file", "updated-image.png", "image/png", "dummy".getBytes());
//        Media existingMedia = Media.builder()
//                .id(1L)
//                .serviceName("educAPI")
//                .fileName("old-image.png")
//                .mediaType(MediaType.IMAGE)
//                .entityType(EntityType.THEME)
//                .uploadedBy(1L)
//                .tag("oldTag")
//                .active(true)
//                .build();
//        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(1L, "educAPI")).thenReturn(Optional.of(existingMedia));
//
//        // Simula a resposta do método putObject
//        ObjectWriteResponse mockResponse = mock(ObjectWriteResponse.class);
//        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mockResponse);
//
//        // Supondo que o novo arquivo é salvo com o nome "educAPI/updated-image.png"
//        Media updatedMedia = Media.builder()
//                .id(1L)
//                .serviceName("educAPI")
//                .fileName("educAPI/updated-image.png")
//                .mediaType(MediaType.IMAGE)
//                .entityType(EntityType.THEME)
//                .uploadedBy(1L)
//                .tag("newTag")
//                .active(true)
//                .build();
//        when(mediaRepository.save(existingMedia)).thenReturn(updatedMedia);
//        MediaDTO expectedDto = MediaDTO.builder().fileName("educAPI/updated-image.png").build();
//        when(mediaMapper.entityToDto(updatedMedia)).thenReturn(expectedDto);
//
//        // ACT
//        MediaDTO result = mediaService.updateMedia("educAPI", 1L, EntityType.THEME, "newTag", MediaType.IMAGE, file);
//
//        // ASSERT
//        assertEquals("educAPI/updated-image.png", result.getFileName());
//    }
//
//    // ---------------------------
//    // Testes para disableMedia(...)
//    // ---------------------------
//    @Test
//    void testDisableMedia_mediaNotFound_throwsMediaNotFoundException() {
//        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(1L, "educAPI")).thenReturn(Optional.empty());
//        assertThrows(MediaNotFoundException.class, () ->
//                mediaService.disableMedia("educAPI", 1L));
//    }
//
//    @Test
//    void testDisableMedia_alreadyDisabled_throwsMediaAlreadyDisabledException() {
//        Media media = Media.builder()
//                .id(1L)
//                .serviceName("educAPI")
//                .active(false)
//                .build();
//        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(1L, "educAPI")).thenReturn(Optional.of(media));
//        assertThrows(MediaAlreadyDisabledException.class, () ->
//                mediaService.disableMedia("educAPI", 1L));
//    }
//
//    @Test
//    void testDisableMedia_successful() {
//        // ARRANGE
//        Media media = Media.builder()
//                .id(1L)
//                .serviceName("educAPI")
//                .active(true)
//                .build();
//        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(1L, "educAPI")).thenReturn(Optional.of(media));
//
//        Media disabledMedia = Media.builder()
//                .id(1L)
//                .serviceName("educAPI")
//                .active(false)
//                .build();
//        when(mediaRepository.save(media)).thenReturn(disabledMedia);
//
//        MediaDTO expectedDto = MediaDTO.builder().build();
//        when(mediaMapper.entityToDto(disabledMedia)).thenReturn(expectedDto);
//
//        // ACT
//        MediaDTO result = mediaService.disableMedia("educAPI", 1L);
//
//        // ASSERT
//        assertFalse(disabledMedia.isActive());
//        assertNotNull(result);
//    }
//}
