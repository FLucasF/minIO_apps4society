package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.exceptions.MediaNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MediaControllerDisableTest extends BaseMediaControllerTest {

    @Test
    @DisplayName("DELETE /api/media/{serviceName}/{mediaId} - Sucesso (204)")
    public void testDisableMedia_Success() throws Exception {
        // 🔹 Simula a desativação bem-sucedida da mídia
        doNothing().when(mediaService).disableMedia(eq(serviceName), eq(mediaId));

        mockMvc.perform(delete("/api/media/{serviceName}/{mediaId}", serviceName, mediaId)
                        .header("api-key", "123"))
                .andExpect(status().isNoContent()); // ✅ Deve retornar 204 NO CONTENT
    }

    @Test
    @DisplayName("DELETE /api/media/{serviceName}/{mediaId} - Mídia não encontrada (404)")
    public void testDisableMedia_NotFound() throws Exception {
        // 🔹 Simula a exceção `MediaNotFoundException`
        doThrow(new MediaNotFoundException("Mídia não encontrada ou inativa."))
                .when(mediaService).disableMedia(eq(serviceName), eq(mediaId));

        mockMvc.perform(delete("/api/media/{serviceName}/{mediaId}", serviceName, mediaId)
                        .header("api-key", "123"))
                .andExpect(status().isNotFound()) // ✅ Deve retornar 404 NOT FOUND
                .andExpect(jsonPath("$.message").value("Mídia não encontrada ou inativa."));

    }
}


