package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.MediaType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaServiceImplListTest extends BaseMediaServiceImplTest {

    @Test
    void testListMediaByUploadedBy_successful() {
        Media media1 = new Media(1L, "image1.png", serviceName, MediaType.IMAGE);
        media1.setEntityId(1L);
        media1.setActive(true);

        Media media2 = new Media(1L, "image2.png", serviceName, MediaType.IMAGE);
        media2.setEntityId(2L);
        media2.setActive(true);

        List<Media> mockMedias = List.of(media1, media2);

        when(mediaRepository.findByServiceNameAndUploadedByAndActiveTrue(serviceName, 1L))
                .thenReturn(mockMedias);

        when(mediaMapper.toResponse(media1)).thenReturn(new MediaResponse(
                media1.getEntityId(),
                media1.getServiceName(),
                media1.getFileName(),
                "https://minio.example.com/educAPI/image1.png"
        ));

        when(mediaMapper.toResponse(media2)).thenReturn(new MediaResponse(
                media2.getEntityId(),
                media2.getServiceName(),
                media2.getFileName(),
                "https://minio.example.com/educAPI/image2.png"
        ));

        List<MediaResponse> result = mediaService.listMediaByUploadedBy(serviceName, 1L);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertAll("Validando resposta da listagem",
                () -> assertEquals(1L, result.get(0).entityId()),
                () -> assertEquals(serviceName, result.get(0).serviceName()),
                () -> assertEquals("image1.png", result.get(0).fileName()),

                () -> assertEquals(2L, result.get(1).entityId()),
                () -> assertEquals(serviceName, result.get(1).serviceName()),
                () -> assertEquals("image2.png", result.get(1).fileName())
        );

        verify(mediaRepository, times(1)).findByServiceNameAndUploadedByAndActiveTrue(serviceName, 1L);
        verify(mediaMapper, times(2)).toResponse(any(Media.class));
    }

    @Test
    void testListMediaByUploadedBy_noMediaFound_returnsEmptyList() {
        when(mediaRepository.findByServiceNameAndUploadedByAndActiveTrue(serviceName, 1L))
                .thenReturn(List.of());

        List<MediaResponse> result = mediaService.listMediaByUploadedBy(serviceName, 1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(mediaRepository, times(1)).findByServiceNameAndUploadedByAndActiveTrue(serviceName, 1L);
        verify(mediaMapper, never()).toResponse(any(Media.class));
    }
}
