package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.service.MediaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
public class MediaControllerUploadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    @Test
    @DisplayName("Teste de upload de mídia com header api-key")
    public void testUploadMediaWithApiKey() throws Exception {
        // Cria um arquivo simulado (ex: screenshot ou imagem)
        MockMultipartFile file = new MockMultipartFile(
                "file", "screenshot.png", "image/png", "conteudoDaImagem".getBytes());

        // Cria um DTO simulado que será retornado pelo MediaService
        MediaDTO mediaDTO = new MediaDTO();
        mediaDTO.setId(1L);
        mediaDTO.setUrl("http://example.com/media/1");

        // Configura o comportamento do serviço para retornar o DTO esperado
        when(mediaService.saveMedia(eq("educaAPI"), any(), eq("teste"), eq(EntityType.THEME), eq(1L)))
                .thenReturn(mediaDTO);

        // Executa a requisição multipart simulando os parâmetros e o header "api-key"
        mockMvc.perform(multipart("/api/media/post")
                        .file(file)
                        .header("api-key", "123")
                        .param("tag", "teste")
                        .param("entityType", "THEME")
                        .param("uploadedBy", "1")
                        .param("serviceName", "educaAPI"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.url").value("http://example.com/media/1"));
    }
}
