package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.exceptions.MediaNotFoundException;
import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.service.MediaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MediaControllerUpdateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    private final String serviceName = "educAPI";
    private final Long mediaId = 100L;
    private final Long uploadedBy = 1L;
    private final Long entityId = 42L;

    @Test
    @DisplayName("PUT /api/media/{serviceName}/{mediaId} - Sucesso (200)")
    public void testUpdateMedia_Success() throws Exception {
        // Criando um arquivo válido para atualização
        MockMultipartFile file = new MockMultipartFile("file", "updated.png", "image/png", "updatedContent".getBytes());

        // Criando um objeto de resposta esperado
        MediaResponse response = new MediaResponse(mediaId, serviceName, "updated.png", "http://localhost/media/updated.png");

        // Configurando o serviço para retornar a resposta esperada
        when(mediaService.updateMedia(eq(serviceName), eq(mediaId), any(MediaRequest.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/media/{serviceName}/{mediaId}", serviceName, mediaId)
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; }) // Define o método como PUT
                        .param("uploadedBy", uploadedBy.toString())
                        .param("entityId", entityId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())  // Deve retornar 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(mediaId))
                .andExpect(jsonPath("$.serviceName").value(serviceName))
                .andExpect(jsonPath("$.fileName").value("updated.png"));
    }

    @Test
    @DisplayName("PUT /api/media/{serviceName}/{mediaId} - Mídia não encontrada (404)")
    public void testUpdateMedia_NotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "updated.png", "image/png", "updatedContent".getBytes());

        // 🔹 Simular o lançamento da exceção `MediaNotFoundException`
        when(mediaService.updateMedia(eq(serviceName), eq(mediaId), any(MediaRequest.class)))
                .thenThrow(new MediaNotFoundException("Mídia não encontrada ou inativa."));

        mockMvc.perform(multipart("/api/media/{serviceName}/{mediaId}", serviceName, mediaId)
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .param("uploadedBy", uploadedBy.toString())
                        .param("entityId", entityId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Mídia não encontrada ou inativa."));
    }


    @Test
    @DisplayName("PUT /api/media/{serviceName}/{mediaId} - Arquivo ausente (400)")
    public void testUpdateMedia_MissingFile() throws Exception {
        mockMvc.perform(multipart("/api/media/{serviceName}/{mediaId}", serviceName, mediaId)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .param("uploadedBy", uploadedBy.toString())
                        .param("entityId", entityId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest()); // Deve retornar 400
    }

    @Test
    @DisplayName("PUT /api/media/{serviceName}/{mediaId} - Parâmetros obrigatórios ausentes (400)")
    public void testUpdateMedia_MissingParams() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "updated.png", "image/png", "updatedContent".getBytes());

        mockMvc.perform(multipart("/api/media/{serviceName}/{mediaId}", serviceName, mediaId)
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest()); // Deve retornar 400
    }

    @Test
    @DisplayName("PUT /api/media/{serviceName}/{mediaId} - Erro interno no serviço (500)")
    public void testUpdateMedia_InternalServerError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "updated.png", "image/png", "updatedContent".getBytes());

        when(mediaService.updateMedia(eq(serviceName), eq(mediaId), any(MediaRequest.class)))
                .thenThrow(new RuntimeException("Erro inesperado"));

        mockMvc.perform(multipart("/api/media/{serviceName}/{mediaId}", serviceName, mediaId)
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .param("uploadedBy", uploadedBy.toString())
                        .param("entityId", entityId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError()); // Deve retornar 500
    }
}
