package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MediaControllerUploadTest extends BaseMediaControllerTest {

    @Test
    @DisplayName("POST /api/media - Sucesso (201)")
    public void testUploadFileSuccess_201() throws Exception {
        MediaResponse response = createMediaResponse();
        when(mediaService.uploadMedia(any())).thenReturn(response);

        mockMvc.perform(multipart("/api/media")
                        .file(validFile)
                        .param("serviceName", serviceName)
                        .param("uploadedBy", String.valueOf(uploadedBy))
                        .param("entityId", String.valueOf(entityId))
                        .contentType("multipart/form-data"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.id").value(mediaId))
                .andExpect(jsonPath("$.serviceName").value(serviceName))
                .andExpect(jsonPath("$.fileName").value(fileName));
    }

    @Test
    @DisplayName("POST /api/media - Falha (400) - Sem Arquivo")
    public void testUploadFileMissingFile_400() throws Exception {
        mockMvc.perform(multipart("/api/media")
                        .param("serviceName", serviceName)
                        .param("uploadedBy", String.valueOf(uploadedBy))
                        .param("entityId", String.valueOf(entityId))
                        .contentType("multipart/form-data"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/media - Falha (400) - Parâmetro 'serviceName' ausente")
    public void testUploadFileMissingServiceName_400() throws Exception {
        mockMvc.perform(multipart("/api/media")
                        .file(validFile)
                        .param("uploadedBy", String.valueOf(uploadedBy))
                        .param("entityId", String.valueOf(entityId))
                        .contentType("multipart/form-data"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/media - Falha (500) - Erro no Serviço")
    public void testUploadFileServiceException_500() throws Exception {
        when(mediaService.uploadMedia(any())).thenThrow(new RuntimeException("Erro interno"));

        mockMvc.perform(multipart("/api/media")
                        .file(validFile)
                        .param("serviceName", serviceName)
                        .param("uploadedBy", String.valueOf(uploadedBy))
                        .param("entityId", String.valueOf(entityId))
                        .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError());
    }
}
