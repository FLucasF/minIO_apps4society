package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.exceptions.MediaNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MediaControllerGetTest extends BaseMediaControllerTest {

    @Test
    @DisplayName("GET /api/media/{serviceName}/{mediaId} - Sucesso (200)")
    public void testGetMediaSuccess_200() throws Exception {
        when(mediaService.getMediaUrl(eq(serviceName), eq(mediaId))).thenReturn(mediaUrl);

        mockMvc.perform(get("/api/media/{serviceName}/{mediaId}", serviceName, mediaId))
                .andExpect(status().isOk())
                .andExpect(content().string(mediaUrl));
    }

    @Test
    @DisplayName("GET /api/media/{serviceName}/{mediaId} - Mídia não encontrada (404)")
    public void testGetMediaNotFound_404() throws Exception {
        when(mediaService.getMediaUrl(eq(serviceName), eq(nonExistingMediaId)))
                .thenThrow(new MediaNotFoundException("Mídia não encontrada ou inativa."));

        mockMvc.perform(get("/api/media/{serviceName}/{mediaId}", serviceName, nonExistingMediaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Mídia não encontrada ou inativa."));
    }


    @Test
    @DisplayName("GET /api/media/{serviceName}/{mediaId} - Parâmetro inválido (400)")
    public void testGetMediaInvalidParameter_400() throws Exception {
        mockMvc.perform(get("/api/media/{serviceName}/{mediaId}", serviceName, "invalidId"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/media/{serviceName}/{mediaId} - Erro interno no serviço (500)")
    public void testGetMediaServiceException_500() throws Exception {
        when(mediaService.getMediaUrl(eq(serviceName), eq(mediaId))).thenThrow(new RuntimeException("Erro interno"));

        mockMvc.perform(get("/api/media/{serviceName}/{mediaId}", serviceName, mediaId))
                .andExpect(status().isInternalServerError());
    }
}
