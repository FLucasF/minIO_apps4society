//package com.apps4society.MinIO_API.service;
//
//import com.apps4society.MinIO_API.exceptions.FileStorageException;
//import com.apps4society.MinIO_API.exceptions.MediaNotFoundException;
//import com.apps4society.MinIO_API.model.DTO.MediaRequest;
//import com.apps4society.MinIO_API.model.DTO.MediaResponse;
//import com.apps4society.MinIO_API.model.entity.Media;
//import com.apps4society.MinIO_API.model.enums.MediaType;
//import io.minio.ObjectWriteResponse;
//import io.minio.PutObjectArgs;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//class MediaServiceImplUpdateTest extends BaseMediaServiceImplTest {
//
//    private MockMultipartFile file;
//    private final String serviceName = "educAPI";
//    private final Long mediaId = 1L;
//    private Media existingMedia;
//
//    @BeforeEach
//    void init() {
//        ReflectionTestUtils.setField(mediaService, "bucketName", "test-bucket");
//
//        file = new MockMultipartFile("file", "new-image.png", "image/png", "dummy".getBytes());
//
//        existingMedia = Media.builder()
//                .id(mediaId)
//                .serviceName(serviceName)
//                .mediaType(MediaType.IMAGE)
//                .fileName("educAPI/old-image.png")
//                .entityId(1001L)
//                .active(true)
//                .build();
//    }
//
//    @Test
//    void testUpdateMedia_mediaNotFound_throwsMediaNotFoundException() {
//        when(mediaRepository.findByIdAndActiveTrue(mediaId)).thenReturn(Optional.empty());
//
//        MediaNotFoundException exception = assertThrows(MediaNotFoundException.class, () ->
//                mediaService.updateMedia(mediaId, new MediaRequest(serviceName, 42L, 1001L, file)));
//
//        assertEquals("Mídia não encontrada ou inativa.", exception.getMessage());
//    }
//
//    @Test
//    void testUpdateMedia_successful() throws Exception {
//        when(mediaRepository.findByIdAndActiveTrue(mediaId)).thenReturn(Optional.of(existingMedia));
//
//        // Mock correto para `putObject`
//        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));
//
//        // Criar mídia atualizada
//        Media updatedMedia = Media.builder()
//                .id(existingMedia.getId())
//                .serviceName(existingMedia.getServiceName())
//                .mediaType(MediaType.IMAGE)
//                .fileName("educAPI/new-image.png") // Nome salvo no banco
//                .entityId(existingMedia.getEntityId())
//                .active(true)
//                .build();
//
//        when(mediaRepository.save(any(Media.class))).thenReturn(updatedMedia);
//
//        // Simular a conversão para `MediaResponse`
//        when(mediaMapper.toResponse(any(Media.class))).thenReturn(new MediaResponse(
//                updatedMedia.getId(),
//                updatedMedia.getServiceName(),
//                "new-image.png", // Apenas o nome do arquivo
//                "https://minio.example.com/educAPI/new-image.png"
//        ));
//
//        // Executar a atualização da mídia
//        MediaResponse result = mediaService.updateMedia(mediaId, new MediaRequest(serviceName, 42L, 1001L, file));
//
//        // Validar a resposta gerada
//        assertAll("Validando resposta da atualização",
//                () -> assertEquals(updatedMedia.getId(), result.id()),
//                () -> assertEquals(updatedMedia.getServiceName(), result.serviceName()),
//                () -> assertEquals("new-image.png", result.fileName()), // Agora retorna apenas o nome do arquivo
//                () -> assertEquals("https://minio.example.com/educAPI/new-image.png", result.url())
//        );
//
//        // Verifica chamadas nos mocks
//        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
//        verify(mediaRepository, times(1)).save(any(Media.class));
//        verify(mediaMapper, times(1)).toResponse(any(Media.class)); // Confirma uso do DTO
//    }
//
//
//}
