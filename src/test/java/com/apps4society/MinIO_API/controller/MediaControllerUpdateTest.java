package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.exceptions.MediaNotFoundException;
import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.service.MediaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;

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
    private final Long entityId = 42L;
    private final Long uploadedBy = 1L;

    @Test
    @DisplayName("PUT /api/media/{entityId} - Sucesso (200)")
    public void testUpdateMedia_Success() throws Exception, JsonProcessingException {
        MockMultipartFile file = new MockMultipartFile("file", "updated.png", "image/png", "updatedContent".getBytes());

        MediaRequest mediaRequest = new MediaRequest(serviceName, uploadedBy);

        MediaResponse response = new MediaResponse(entityId, serviceName, "updated.png", "http://localhost/media/updated.png");

        when(mediaService.updateMedia(eq(entityId), eq(mediaRequest), eq(file)))
                .thenReturn(response);

        String mediaRequestJson = new ObjectMapper().writeValueAsString(mediaRequest);

        MockMultipartFile mediaRequestPart = new MockMultipartFile(
                "mediaRequest",
                "mediaRequest.json",
                "application/json",
                mediaRequestJson.getBytes()
        );

        mockMvc.perform(multipart("/api/media/{entityId}", entityId)
                        .file(file)
                        .file(mediaRequestPart)
                        .param("uploadedBy", uploadedBy.toString())
                        .param("serviceName", serviceName)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.entityId").value(entityId))
                .andExpect(jsonPath("$.serviceName").value(serviceName))
                .andExpect(jsonPath("$.fileName").value("updated.png"))
                .andExpect(jsonPath("$.url").value("http://localhost/media/updated.png"));
    }



    @Test
    @DisplayName("PUT /api/media/{entityId} - Mídia não encontrada (404)")
    public void testUpdateMedia_NotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "updated.png", "image/png", "updatedContent".getBytes());

        MediaRequest mediaRequest = new MediaRequest(serviceName, uploadedBy);

        when(mediaService.updateMedia(eq(entityId), eq(mediaRequest), eq(file)))
                .thenThrow(new MediaNotFoundException("Mídia não encontrada ou inativa."));

        String mediaRequestJson = new ObjectMapper().writeValueAsString(mediaRequest);

        MockMultipartFile mediaRequestPart = new MockMultipartFile(
                "mediaRequest",
                "mediaRequest.json",
                "application/json",
                mediaRequestJson.getBytes()
        );

        mockMvc.perform(multipart("/api/media/{entityId}", entityId)
                        .file(file)
                        .file(mediaRequestPart)
                        .param("uploadedBy", uploadedBy.toString())
                        .param("serviceName", serviceName)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .contentType("multipart/form-data"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Mídia não encontrada ou inativa."));  // Verificando a mensagem de erro
    }



    @Test
    @DisplayName("PUT /api/media/{entityId} - Arquivo ausente (400)")
    public void testUpdateMedia_MissingFile() throws Exception {
        mockMvc.perform(multipart("/api/media/{entityId}", entityId)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("uploadedBy", uploadedBy.toString())
                        .param("serviceName", serviceName)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/media/{entityId} - Parâmetros obrigatórios ausentes (400)")
    public void testUpdateMedia_MissingParams() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "updated.png", "image/png", "updatedContent".getBytes());

        mockMvc.perform(multipart("/api/media/{entityId}", entityId)
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/media/{entityId} - Erro interno no serviço (500)")
    public void testUpdateMedia_InternalServerError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "updated.png", "image/png", "updatedContent".getBytes());

        MediaRequest mediaRequest = new MediaRequest(serviceName, uploadedBy);

        when(mediaService.updateMedia(eq(entityId), eq(mediaRequest), eq(file)))
                .thenThrow(new RuntimeException("Erro inesperado"));

        String mediaRequestJson = new ObjectMapper().writeValueAsString(mediaRequest);

        MockMultipartFile mediaRequestPart = new MockMultipartFile(
                "mediaRequest",
                "mediaRequest.json",
                "application/json",
                mediaRequestJson.getBytes()
        );

        mockMvc.perform(multipart("/api/media/{entityId}", entityId)
                        .file(file)
                        .file(mediaRequestPart)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError());
    }
}