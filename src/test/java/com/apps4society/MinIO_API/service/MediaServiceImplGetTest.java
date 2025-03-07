package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.exceptions.MediaNotFoundException;
import com.apps4society.MinIO_API.exceptions.MinIOConnectionException;
import com.apps4society.MinIO_API.model.entity.Media;
import io.minio.GetPresignedObjectUrlArgs;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class MediaServiceImplGetTest extends BaseMediaServiceImplTest{
   @Test
void testGetMediaUrl_nullServiceName_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () ->
            mediaService.getMediaUrl(null, 1L));
}

@Test
void testGetMediaUrl_emptyServiceName_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () ->
            mediaService.getMediaUrl("", 1L));
}

@Test
void testGetMediaUrl_nullMediaId_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () ->
            mediaService.getMediaUrl("educAPI", null));
}

@Test
void testGetMediaUrl_mediaNotFound_throwsMediaNotFoundException() {
    when(mediaRepository.findByIdAndServiceNameAndActiveTrue(1L, "educAPI")).thenReturn(Optional.empty());

    MediaNotFoundException exception = assertThrows(MediaNotFoundException.class, () ->
            mediaService.getMediaUrl("educAPI", 1L));

    assertEquals("Mídia não encontrada para o serviço informado.", exception.getMessage());
}

@Test
void testGetMediaUrl_successful() throws Exception {
    Media media = Media.builder()
            .id(1L)
            .serviceName("educAPI")
            .fileName("educAPI/test-image.png")
            .active(true)
            .build();

    when(mediaRepository.findByIdAndServiceNameAndActiveTrue(1L, "educAPI")).thenReturn(Optional.of(media));

    String signedUrl = "http://minio/test-image.png";
    when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(signedUrl);

    Map<String, String> result = mediaService.getMediaUrl("educAPI", 1L);

    assertNotNull(result);
    assertTrue(result.containsKey("url"));
    assertEquals(signedUrl, result.get("url"));
}

    @Test
    void testGetMediaUrl_minioFails_throwsMinIOConnectionException() throws Exception {
        Media media = Media.builder()
                .id(1L)
                .serviceName("educAPI")
                .fileName("educAPI/test-image.png")
                .active(true)
                .build();

        when(mediaRepository.findByIdAndServiceNameAndActiveTrue(1L, "educAPI")).thenReturn(Optional.of(media));
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("Falha ao conectar ao MinIO"));

        MinIOConnectionException exception = assertThrows(MinIOConnectionException.class, () ->
                mediaService.getMediaUrl("educAPI", 1L));

        assertEquals("Erro ao gerar URL assinada da mídia.", exception.getMessage());
    }

}
