//package com.apps4society.MinIO_API.service;
//
//import com.apps4society.MinIO_API.exceptions.FileStorageException;
//import com.apps4society.MinIO_API.exceptions.MediaNotFoundException;
//import com.apps4society.MinIO_API.model.entity.Media;
//import com.apps4society.MinIO_API.model.enums.MediaType;
//import io.minio.CopyObjectArgs;
//import io.minio.MinioClient;
//import io.minio.RemoveObjectArgs;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//class MediaServiceImplDisableTest extends BaseMediaServiceImplTest {
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
//                .mediaType(MediaType.IMAGE)
//                .fileName("educAPI/test-image.png")
//                .active(true)
//                .build();
//    }
//
//    @Test
//    void testDisableMedia_mediaNotFound_throwsMediaNotFoundException() {
//        // Configura o repositório para retornar vazio (simulando mídia inexistente)
//        when(mediaRepository.findByIdAndActiveTrue(mediaId))
//                .thenReturn(Optional.empty());
//
//        // Executa o teste e verifica se a exceção correta é lançada
//        MediaNotFoundException exception = assertThrows(MediaNotFoundException.class, () ->
//                mediaService.disableMedia(mediaId));
//
//        assertEquals("Mídia não encontrada ou inativa.", exception.getMessage());
//    }
//
//    @Test
//    void testDisableMedia_moveFails_throwsFileStorageException() throws Exception {
//        // Simula que a mídia existe no banco
//        when(mediaRepository.findByIdAndActiveTrue(mediaId))
//                .thenReturn(Optional.of(mediaMock));
//
//        // Simula um erro ao mover o arquivo no MinIO
//        doThrow(new RuntimeException("MinIO copy error"))
//                .when(minioClient).copyObject(any(CopyObjectArgs.class));
//
//        // Verifica se a exceção correta é lançada
//        FileStorageException exception = assertThrows(FileStorageException.class, () ->
//                mediaService.disableMedia(mediaId));
//
//        assertEquals("Erro ao mover mídia no armazenamento.", exception.getMessage());
//
//        // Verifica que o removeObject NÃO foi chamado (pois o move falhou antes)
//        verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
//        verify(mediaRepository, never()).save(any(Media.class));
//    }
//
//    @Test
//    void testDisableMedia_successful() throws Exception {
//        // Simula que a mídia existe no banco
//        when(mediaRepository.findByIdAndActiveTrue(mediaId))
//                .thenReturn(Optional.of(mediaMock));
//
//        // Executa o método disableMedia
//        assertDoesNotThrow(() -> mediaService.disableMedia(mediaId));
//
//        // Verifica se os métodos do MinIO foram chamados corretamente
//        verify(minioClient, times(1)).copyObject(any(CopyObjectArgs.class));
//        verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
//
//        // Verifica se o banco de dados foi atualizado corretamente
//        verify(mediaRepository, times(1)).save(Mockito.argThat(media ->
//                !media.isActive() // A mídia deve estar desativada (active = false)
//        ));
//    }
//}
