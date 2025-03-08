package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.MediaType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaServiceImplListTest extends BaseMediaServiceImplTest {

    private final String serviceName = "educAPI";
    private final Long entityId = 1001L;

    @Test
    void testListMediaByEntity_successful() {
        // Criando mocks das mídias associadas à entidade
        Media media1 = Media.builder()
                .id(1L)
                .serviceName(serviceName)
                .fileName("image1.png")
                .mediaType(MediaType.IMAGE)
                .entityId(entityId)
                .active(true)
                .build();

        Media media2 = Media.builder()
                .id(2L)
                .serviceName(serviceName)
                .fileName("image2.png")
                .mediaType(MediaType.IMAGE)
                .entityId(entityId)
                .active(true)
                .build();

        List<Media> mockMedias = List.of(media1, media2);

        when(mediaRepository.findByServiceNameAndEntityIdAndActiveTrue(serviceName, entityId))
                .thenReturn(mockMedias);

        // Simulando o mapeamento para MediaResponse
        when(mediaMapper.toResponse(media1)).thenReturn(new MediaResponse(
                media1.getId(),
                media1.getServiceName(),
                "image1.png", // Apenas o nome do arquivo
                "https://minio.example.com/educAPI/image1.png"
        ));

        when(mediaMapper.toResponse(media2)).thenReturn(new MediaResponse(
                media2.getId(),
                media2.getServiceName(),
                "image2.png",
                "https://minio.example.com/educAPI/image2.png"
        ));

        // Executando o método
        List<MediaResponse> result = mediaService.listMediaByEntity(serviceName, entityId);

        // Validações
        assertNotNull(result);
        assertEquals(2, result.size());

        assertAll("Validando resposta da listagem",
                () -> assertEquals(1L, result.get(0).id()),
                () -> assertEquals(serviceName, result.get(0).serviceName()),
                () -> assertEquals("image1.png", result.get(0).fileName()),

                () -> assertEquals(2L, result.get(1).id()),
                () -> assertEquals(serviceName, result.get(1).serviceName()),
                () -> assertEquals("image2.png", result.get(1).fileName())
        );

        // Verifica chamadas
        verify(mediaRepository, times(1)).findByServiceNameAndEntityIdAndActiveTrue(serviceName, entityId);
        verify(mediaMapper, times(2)).toResponse(any(Media.class));
    }

    @Test
    void testListMediaByEntity_noMediaFound_returnsEmptyList() {
        when(mediaRepository.findByServiceNameAndEntityIdAndActiveTrue(serviceName, entityId))
                .thenReturn(List.of());

        List<MediaResponse> result = mediaService.listMediaByEntity(serviceName, entityId);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(mediaRepository, times(1)).findByServiceNameAndEntityIdAndActiveTrue(serviceName, entityId);
        verify(mediaMapper, never()).toResponse(any(Media.class));
    }
}
