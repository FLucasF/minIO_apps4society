package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MediaControllerUploadTest extends BaseMediaControllerTest {


    @Test
    @DisplayName("POST /api/media - Sucesso (201)")
    public void testUploadFileSuccess_201() throws Exception {
        MediaResponse response = createMediaResponse();
        when(mediaService.uploadMedia(any(MediaRequest.class), any(MultipartFile.class))).thenReturn(response);

        MediaRequest mediaRequest = new MediaRequest(serviceName, uploadedBy);

        ObjectMapper objectMapper = new ObjectMapper();
        String mediaRequestJson = objectMapper.writeValueAsString(mediaRequest);

        MockMultipartFile mediaRequestPart = new MockMultipartFile(
                "mediaRequest",
                "mediaRequest.json",
                "application/json",
                mediaRequestJson.getBytes()
        );

        mockMvc.perform(multipart("/api/media")
                        .file(validFile)
                        .file(mediaRequestPart)
                        .contentType("multipart/form-data"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.entityId").value(100))
                .andExpect(jsonPath("$.serviceName").value(serviceName))
                .andExpect(jsonPath("$.fileName").value(fileName));
    }

    @Test
    @DisplayName("POST /api/media - Falha (400) - Sem Arquivo")
    public void testUploadFileMissingFile_400() throws Exception {
        mockMvc.perform(multipart("/api/media")
                        .param("mediaRequest", "{\"serviceName\":\"" + serviceName + "\",\"uploadedBy\":" + uploadedBy + "}")
                        .contentType("multipart/form-data"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/media - Falha (400) - Parâmetro 'mediaRequest' ausente")
    public void testUploadFileMissingMediaRequest_400() throws Exception {
        mockMvc.perform(multipart("/api/media")
                        .file(validFile)
                        .contentType("multipart/form-data"))
                .andExpect(status().isBadRequest()); // Espera-se status 400 (Bad Request)
    }

//    @Test
//    @DisplayName("POST /api/media - Falha (500) - Erro no Serviço")
//    public void testUploadFileServiceException_500() throws Exception {
//        when(mediaService.uploadMedia(any(MediaRequest.class), any(MultipartFile.class)))
//                .thenThrow(new RuntimeException("Erro interno"));
//
//        mockMvc.perform(multipart("/api/media")
//                        .file("file", validFile.getBytes())
//                        .param("mediaRequest", "{\"serviceName\":\"" + serviceName + "\",\"uploadedBy\":" + uploadedBy + "}") // Enviando o mediaRequest como JSON
//                        .contentType("multipart/form-data"))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.message").value("Erro interno"));
//    }

}
