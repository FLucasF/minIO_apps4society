//package com.apps4society.MinIO_API.service;
//
//import com.apps4society.MinIO_API.exceptions.MediaNotFoundException;
//import com.apps4society.MinIO_API.model.DTO.MediaResponse;
//import com.apps4society.MinIO_API.model.entity.Media;
//import com.apps4society.MinIO_API.model.enums.MediaType;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//public class MediaServiceImplGetTest extends BaseMediaServiceImplTest {
//
//    private final Long mediaId = 1L;
//    private Media mediaMock;
//
//    @BeforeEach
//    void setup() {
//        ReflectionTestUtils.setField(mediaService, "bucketName", "test-bucket");
//
//        // Criando um mock da mídia ativa
//        mediaMock = Media.builder()
//                .id(mediaId)
//                .serviceName("educAPI")
//                .fileName("test-image.png") // Apenas o nome do arquivo
//                .mediaType(MediaType.IMAGE)
//                .active(true)
//                .build();
//    }
//
//    @Test
//    void testGetMedia_mediaNotFound_throwsMediaNotFoundException() {
//        when(mediaRepository.findByIdAndActiveTrue(mediaId))
//                .thenReturn(Optional.empty());
//
//        MediaNotFoundException exception = assertThrows(MediaNotFoundException.class, () ->
//                mediaService.getMedia(mediaId));
//
//        assertEquals("Mídia não encontrada ou inativa.", exception.getMessage());
//    }
//
//    @Test
//    void testGetMedia_successful() {
//        when(mediaRepository.findByIdAndActiveTrue(mediaId))
//                .thenReturn(Optional.of(mediaMock));
//
//        MediaResponse result = mediaService.getMedia(mediaId);
//
//        assertNotNull(result);
//        assertAll("Validando resposta",
//                () -> assertEquals(mediaMock.getId(), result.id()),
//                () -> assertEquals(mediaMock.getServiceName(), result.serviceName()),
//                () -> assertEquals(mediaMock.getFileName(), result.fileName())
//        );
//
//        verify(mediaRepository, times(1)).findByIdAndActiveTrue(mediaId);
//    }
//}
