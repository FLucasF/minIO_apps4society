//package com.apps4society.MinIO_API.controller;
//
//import com.apps4society.MinIO_API.model.DTO.MediaDTO;
//import com.apps4society.MinIO_API.model.enums.EntityType;
//import com.apps4society.MinIO_API.model.enums.MediaType;
//import com.apps4society.MinIO_API.service.MediaService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.Map;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
//import org.hamcrest.Matchers;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(MediaController.class)
//@AutoConfigureMockMvc(addFilters = false) // Desabilita os filtros de segurança para isolar o teste do controller
//public class MediaControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private MediaService mediaService;
//
//    // ----- Teste GET: Buscar URL assinada -----
//    @Test
//    @DisplayName("Teste de buscar URL assinada com retorno dinâmico")
//    public void testGetMediaUrlDynamic() throws Exception {
//        // Exemplo de URL assinada retornada pelo serviço (valor dinâmico)
//        String dynamicUrl = "http://localhost:9000/dev-bucket/educaAPI/Screenshot%20from%202025-02-14%2012-33-13.png?" +
//                "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20250226%2Fus-east-1%2Fs3%2Faws4_request" +
//                "&X-Amz-Date=20250226T105917Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=e75f51c1b236bb25a9d1b99af4430ddeef7f3091d7edb84bb99e1dce1d071ad6";
//
//        // O MediaService retornará esse Map simulando o retorno real
//        when(mediaService.getMediaUrl(eq("educaAPI"), eq(1L)))
//                .thenReturn(Map.of("url", dynamicUrl));
//
//        // Realiza a requisição GET e verifica diversos aspectos da resposta
//        mockMvc.perform(get("/api/media/get/educaAPI/1")
//                        .header("api-key", "123"))
//                .andExpect(status().isOk())
//                // Verifica que o content type é JSON
//                .andExpect(content().contentTypeCompatibleWith("application/json"))
//                // Verifica que a URL não está vazia
//                .andExpect(jsonPath("$.url").isNotEmpty())
//                // Verifica que a URL contém partes esperadas
//                .andExpect(jsonPath("$.url").value(Matchers.containsString("educaAPI/Screenshot")))
//                .andExpect(jsonPath("$.url").value(Matchers.containsString("X-Amz-Algorithm=")))
//                // Verifica que a URL começa com "http://localhost:9000/"
//                .andExpect(jsonPath("$.url").value(Matchers.startsWith("http://localhost:9000/")));
//    }
//
//    @Test
//    @DisplayName("Teste de buscar URL assinada - não encontrada")
//    public void testGetMediaUrlNotFound() throws Exception {
//        when(mediaService.getMediaUrl(eq("educaAPI"), eq(43L))).thenReturn(null);
//
//        mockMvc.perform(get("/api/media/get/educaAPI/43")
//                        .header("api-key", "123"))
//                .andExpect(status().isNotFound());
//    }
//
//    // ----- Teste PUT: Atualizar mídia -----
//    @Test
//    @DisplayName("Teste de atualizar mídia - encontrada")
//    public void testUpdateMediaFound() throws Exception {
//        // Cria um arquivo simulado para o update
//        MockMultipartFile file = new MockMultipartFile(
//                "file", "updated.png", "image/png", "conteudoAtualizado".getBytes());
//
//        // Cria o DTO simulado para atualização
//        MediaDTO updatedDTO = MediaDTO.builder()
//                .id(43L)
//                .serviceName("educaAPI")
//                .mediaType(MediaType.IMAGE)
//                .entityType(EntityType.THEME)
//                .uploadedBy(1L)
//                .fileName("educaAPI/updated.png")
//                .tag("testeAtualizado")
//                .active(true)
//                .build();
//
//        when(mediaService.updateMedia(eq("educaAPI"), eq(43L), eq(EntityType.THEME),
//                eq("testeAtualizado"), eq(MediaType.IMAGE), any()))
//                .thenReturn(updatedDTO);
//
//        // Como o endpoint PUT recebe multipart, usamos .with(...) para forçar o método PUT
//        mockMvc.perform(multipart("/api/media/update/educaAPI/43")
//                        .file(file)
//                        .with(request -> {
//                            request.setMethod("PUT");
//                            return request;
//                        })
//                        .header("api-key", "123")
//                        .param("entityType", "THEME")
//                        .param("tag", "testeAtualizado")
//                        .param("mediaType", "IMAGE"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(43))
//                .andExpect(jsonPath("$.serviceName").value("educaAPI"))
//                .andExpect(jsonPath("$.mediaType").value("IMAGE"))
//                .andExpect(jsonPath("$.entityType").value("THEME"))
//                .andExpect(jsonPath("$.uploadedBy").value(1))
//                .andExpect(jsonPath("$.fileName").value("educaAPI/updated.png"))
//                .andExpect(jsonPath("$.tag").value("testeAtualizado"))
//                .andExpect(jsonPath("$.active").value(true));
//    }
//
//    @Test
//    @DisplayName("Teste de atualizar mídia - não encontrada")
//    public void testUpdateMediaNotFound() throws Exception {
//        MockMultipartFile file = new MockMultipartFile(
//                "file", "updated.png", "image/png", "conteudoAtualizado".getBytes());
//
//        when(mediaService.updateMedia(eq("educaAPI"), eq(43L), eq(EntityType.THEME),
//                eq("testeAtualizado"), eq(MediaType.IMAGE), any()))
//                .thenReturn(null);
//
//        mockMvc.perform(multipart("/api/media/update/educaAPI/43")
//                        .file(file)
//                        .with(request -> {
//                            request.setMethod("PUT");
//                            return request;
//                        })
//                        .header("api-key", "123")
//                        .param("entityType", "THEME")
//                        .param("tag", "testeAtualizado")
//                        .param("mediaType", "IMAGE"))
//                .andExpect(status().isNotFound());
//    }
//
//    // ----- Teste DELETE: Desativar mídia -----
//    @Test
//    @DisplayName("Teste de desativar mídia - encontrada")
//    public void testDisableMediaFound() throws Exception {
//        MediaDTO disabledMedia = MediaDTO.builder()
//                .id(43L)
//                .active(false)
//                .build();
//        doReturn(disabledMedia).when(mediaService).disableMedia(eq("educaAPI"), eq(43L));
//
//        mockMvc.perform(delete("/api/media/educaAPI/43")
//                        .header("api-key", "123"))
//                .andExpect(status().isNoContent());
//    }
//
//
//    @Test
//    @DisplayName("Teste de desativar mídia - não encontrada")
//    public void testDisableMediaNotFound() throws Exception {
//        when(mediaService.disableMedia(eq("educaAPI"), eq(43L))).thenReturn(null);
//
//        mockMvc.perform(delete("/api/media/educaAPI/43")
//                        .header("api-key", "123"))
//                .andExpect(status().isNotFound());
//    }
//}
