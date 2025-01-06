package com.apps4society.MinIO_API;

import com.apps4society.MinIO_API.controller.MediaController;
import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringJUnitConfig(MediaControllerTest.Config.class)
@WebMvcTest(MediaController.class)
public class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        Mockito.reset(mediaService);
    }

    @Test
    void testUploadFile_Success() throws Exception {
        // Configure o mock
        when(mediaService.saveMedia(any(), anyLong(), any(), anyLong()))
                .thenReturn(MediaDTO.builder().id(1L).url("http://localhost:9000/test/image.jpg").build());

        // Execute a requisição e valide a resposta
        mockMvc.perform(multipart("/api/media/upload")
                        .file("file", "content".getBytes())
                        .param("entityId", "1")
                        .param("entityType", "THEME")
                        .param("uploadedBy", "1")
                        .contentType("multipart/form-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.url").value("http://localhost:9000/test/image.jpg"));
    }

    @Test
    void testGetMediaByUploader_NotFound() throws Exception {
        when(mediaService.getMediaByUploader(1L)).thenThrow(new RuntimeException("No media found"));

        mockMvc.perform(get("/api/media/uploader/1"))
                .andExpect(status().isNotFound());
    }

    @Configuration
    static class Config {
        @Bean
        public MediaService mediaService() {
            return mock(MediaService.class);
        }
    }
}
