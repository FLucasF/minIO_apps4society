package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.service.MediaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;  // Spring's MediaType
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    @Test
    @DisplayName("POST /api/media/post - Sucesso (201)")
    public void testUploadFileSuccess_201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "upload.png", "image/png", "fileContent".getBytes());
        MediaDTO dto = MediaDTO.builder()
                .id(100L)
                .serviceName("educAPI")
                .mediaType(com.apps4society.MinIO_API.model.enums.MediaType.IMAGE)
                .entityType(EntityType.THEME)
                .uploadedBy(1L)
                .fileName("educAPI/upload.png")
                .tag("profile")
                .active(true)
                .build();

        when(mediaService.saveMedia(eq("educAPI"), any(), eq("profile"), eq(EntityType.THEME), eq(1L)))
                .thenReturn(dto);

        mockMvc.perform(multipart("/api/media/post")
                        .file(file)
                        .param("serviceName", "educAPI")
                        .param("tag", "profile")
                        .param("entityType", "THEME")
                        .param("uploadedBy", "1")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.serviceName").value("educAPI"))
                .andExpect(jsonPath("$.mediaType").value("IMAGE"))
                .andExpect(jsonPath("$.entityType").value("THEME"))
                .andExpect(jsonPath("$.uploadedBy").value(1))
                .andExpect(jsonPath("$.fileName").value("educAPI/upload.png"))
                .andExpect(jsonPath("$.tag").value("profile"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("POST /api/media/post - Exceção do Serviço (500)")
    public void testUploadFileServiceException_500() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "upload.png", "image/png", "fileContent".getBytes());
        when(mediaService.saveMedia(eq("educAPI"), any(), eq("profile"), eq(EntityType.THEME), eq(1L)))
                .thenThrow(new RuntimeException("Service failure"));

        mockMvc.perform(multipart("/api/media/post")
                        .file(file)
                        .param("serviceName", "educAPI")
                        .param("tag", "profile")
                        .param("entityType", "THEME")
                        .param("uploadedBy", "1")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /api/media/post - Parâmetro 'file' ausente (400)")
    public void testUploadFileMissingFile_400() throws Exception {
        mockMvc.perform(multipart("/api/media/post")
                        .param("serviceName", "educAPI")
                        .param("tag", "profile")
                        .param("entityType", "THEME")
                        .param("uploadedBy", "1")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/media/post - Parâmetro 'serviceName' ausente (400)")
    public void testUploadFileMissingServiceName_400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "upload.png", "image/png", "fileContent".getBytes());
        mockMvc.perform(multipart("/api/media/post")
                        .file(file)
                        .param("tag", "profile")
                        .param("entityType", "THEME")
                        .param("uploadedBy", "1")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/media/get/{serviceName}/{mediaId} - Sucesso (200)")
    public void testGetMediaUrlSuccess_200() throws Exception {
        // Arrange: configura o retorno do service com uma URL assinada
        String url = "http://localhost:9000/educAPI/file.png";
        when(mediaService.getMediaUrl(eq("educAPI"), eq(1L))).thenReturn(Map.of("url", url));

        mockMvc.perform(get("/api/media/get/educAPI/1")
                        .header("api-key", "123"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.url").value(url));
    }

    @Test
    @DisplayName("GET /api/media/get/{serviceName}/{mediaId} - Mídia não encontrada (404)")
    public void testGetMediaUrlNotFound_404() throws Exception {
        when(mediaService.getMediaUrl(eq("educAPI"), eq(43L))).thenReturn(null);

        mockMvc.perform(get("/api/media/get/educAPI/43")
                        .header("api-key", "123"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/media/get/{serviceName}/{mediaId} - Formato de mediaId inválido (400)")
    public void testGetMediaUrlInvalidMediaId_400() throws Exception {
        mockMvc.perform(get("/api/media/get/educAPI/invalidId")
                        .header("api-key", "123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/media/update/{serviceName}/{mediaId} - Sucesso (200)")
    public void testUpdateMediaSuccess_200() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "updated.png", "image/png", "updatedContent".getBytes());
        MediaDTO dto = MediaDTO.builder()
                .id(43L)
                .serviceName("educAPI")
                .mediaType(com.apps4society.MinIO_API.model.enums.MediaType.IMAGE)
                .entityType(EntityType.THEME)
                .uploadedBy(1L)
                .fileName("educAPI/updated.png")
                .tag("updatedTag")
                .active(true)
                .build();
        when(mediaService.updateMedia(eq("educAPI"), eq(43L), eq(EntityType.THEME),
                eq("updatedTag"), eq(com.apps4society.MinIO_API.model.enums.MediaType.IMAGE), any()))
                .thenReturn(dto);

        mockMvc.perform(multipart("/api/media/update/educAPI/43")
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .header("api-key", "123")
                        .param("entityType", "THEME")
                        .param("tag", "updatedTag")
                        .param("mediaType", "IMAGE")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.id").value(43))
                .andExpect(jsonPath("$.serviceName").value("educAPI"))
                .andExpect(jsonPath("$.mediaType").value("IMAGE"))
                .andExpect(jsonPath("$.entityType").value("THEME"))
                .andExpect(jsonPath("$.uploadedBy").value(1))
                .andExpect(jsonPath("$.fileName").value("educAPI/updated.png"))
                .andExpect(jsonPath("$.tag").value("updatedTag"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("PUT /api/media/update/{serviceName}/{mediaId} - Mídia não encontrada (404)")
    public void testUpdateMediaNotFound_404() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "updated.png", "image/png", "updatedContent".getBytes());
        when(mediaService.updateMedia(eq("educAPI"), eq(43L), eq(EntityType.THEME),
                eq("updatedTag"), eq(com.apps4society.MinIO_API.model.enums.MediaType.IMAGE), any()))
                .thenReturn(null);

        mockMvc.perform(multipart("/api/media/update/educAPI/43")
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .header("api-key", "123")
                        .param("entityType", "THEME")
                        .param("tag", "updatedTag")
                        .param("mediaType", "IMAGE")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/media/update/{serviceName}/{mediaId} - Arquivo ausente (400)")
    public void testUpdateMediaMissingFile_400() throws Exception {
        mockMvc.perform(multipart("/api/media/update/educAPI/43")
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .header("api-key", "123")
                        .param("entityType", "THEME")
                        .param("tag", "updatedTag")
                        .param("mediaType", "IMAGE")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/media/update/{serviceName}/{mediaId} - Valor inválido para mediaType (400)")
    public void testUpdateMediaInvalidMediaType_400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "updated.png", "image/png", "updatedContent".getBytes());
        mockMvc.perform(multipart("/api/media/update/educAPI/43")
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .header("api-key", "123")
                        .param("entityType", "THEME")
                        .param("tag", "updatedTag")
                        .param("mediaType", "INVALID_TYPE")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/media/delete/{serviceName}/{mediaId} - Sucesso (204)")
    public void testDisableMediaSuccess_204() throws Exception {
        MediaDTO dto = MediaDTO.builder()
                .id(43L)
                .active(false)
                .build();
        doReturn(dto).when(mediaService).disableMedia(eq("educAPI"), eq(43L));

        mockMvc.perform(delete("/api/media/delete/educAPI/43")
                        .header("api-key", "123"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/media/delete/{serviceName}/{mediaId} - Mídia não encontrada (404)")
    public void testDisableMediaNotFound_404() throws Exception {
        when(mediaService.disableMedia(eq("educAPI"), eq(43L))).thenReturn(null);

        mockMvc.perform(delete("/api/media/delete/educAPI/43")
                        .header("api-key", "123"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/media/delete/{serviceName}/{mediaId} - Formato inválido de mediaId (400)")
    public void testDisableMediaInvalidMediaId_400() throws Exception {
        mockMvc.perform(delete("/api/media/delete/educAPI/invalidId")
                        .header("api-key", "123"))
                .andExpect(status().isBadRequest());
    }
}
