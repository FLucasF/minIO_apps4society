package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.service.MediaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MediaControllerListTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    private final String serviceName = "educAPI";
    private final Long entityId = 1001L;

    @Test
    @DisplayName("GET /api/media/lists/{serviceName}/{entityId} - Sucesso (200)")
    public void testListMediaByEntity_Success() throws Exception {
        // Criando mídias fictícias para retorno
        List<MediaResponse> mediaList = List.of(
                new MediaResponse(1L, serviceName, "file1.png", "http://localhost/media/file1.png"),
                new MediaResponse(2L, serviceName, "file2.png", "http://localhost/media/file2.png")
        );

        when(mediaService.listMediaByEntity(serviceName, entityId)).thenReturn(mediaList);

        mockMvc.perform(get("/api/media/lists/{serviceName}/{entityId}", serviceName, entityId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())  // Deve retornar 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(mediaList.size())) // Garante que retornou 2 mídias
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].fileName").value("file1.png"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].fileName").value("file2.png"));
    }

    @Test
    @DisplayName("GET /api/media/lists/{serviceName}/{entityId} - Nenhuma mídia encontrada (200, lista vazia)")
    public void testListMediaByEntity_NoContent() throws Exception {
        when(mediaService.listMediaByEntity(serviceName, entityId)).thenReturn(List.of());

        mockMvc.perform(get("/api/media/lists/{serviceName}/{entityId}", serviceName, entityId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())  // Ainda retorna 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0)); // Lista vazia
    }

    @Test
    @DisplayName("GET /api/media/lists/{serviceName}/{entityId} - Erro no serviço (500)")
    public void testListMediaByEntity_ServiceError() throws Exception {
        when(mediaService.listMediaByEntity(anyString(), anyLong()))
                .thenThrow(new RuntimeException("Erro inesperado"));

        mockMvc.perform(get("/api/media/lists/{serviceName}/{entityId}", serviceName, entityId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError()); // Deve retornar 500
    }
}
