//package com.apps4society.MinIO_API.integration;
//
//import com.apps4society.MinIO_API.repository.MediaRepository;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.test.web.reactive.server.WebTestClient;
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.http.client.MultipartBodyBuilder;
//
//import java.time.Duration;
//
//import static org.springframework.web.reactive.function.BodyInserters.fromMultipartData;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles("test")
//public class MediaIntegrationTest {
//
//    @Autowired
//    private WebTestClient webClient;
//
//    @Autowired
//    private MediaRepository mediaRepository;
//
//    @DynamicPropertySource
//    static void registerProperties(DynamicPropertyRegistry registry) {
//        // Configura o MinIO real rodando localmente
//        registry.add("minio.url", () -> "http://127.0.0.1:9000");
//    }
//
//    @BeforeAll
//    static void setup() {
//        System.out.println("📡 Conectando ao MinIO real em http://127.0.0.1:9000");
//    }
//
//    @AfterEach
//    void cleanup() {
//        TestUtil.cleanupTestData("educaAPI/screenshot.png");
//        TestUtil.cleanupTestData("educaAPI/updated_screenshot.png");
//    }
//
//    /**
//     * 🟢 Teste de sucesso - Upload de mídia
//     */
//    @Test
//    @DisplayName("Teste de integração - Upload de mídia")
//    void testUploadMedia() {
//        MultipartBodyBuilder builder = new MultipartBodyBuilder();
//        builder.part("serviceName", "educaAPI");
//        builder.part("tag", "teste");
//        builder.part("entityType", "THEME");
//        builder.part("uploadedBy", "1");
//        builder.part("file", new ByteArrayResource("conteudoDaImagem".getBytes()) {
//            @Override
//            public String getFilename() {
//                return "screenshot.png";
//            }
//        }).header("Content-Type", "image/png");
//
//        System.out.println("🚀 Enviando requisição para /api/media/post");
//
//        webClient.mutate().responseTimeout(Duration.ofSeconds(10)).build()
//                .post()
//                .uri("/api/media/post")
//                .header("api-key", "123")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(fromMultipartData(builder.build()))
//                .exchange()
//                .expectStatus().isCreated()
//                .expectBody()
//                .jsonPath("$.serviceName").isEqualTo("educaAPI")
//                .jsonPath("$.tag").isEqualTo("teste")
//                .jsonPath("$.entityType").isEqualTo("THEME")
//                .jsonPath("$.uploadedBy").isEqualTo(1);
//
//        System.out.println("✅ Upload realizado com sucesso!");
//    }
//
//    /**
//     * 🟢 Teste de sucesso - Obter URL de mídia
//     */
//    @Test
//    @DisplayName("Teste de integração - Obter URL de mídia")
//    void testGetMediaUrl() {
//        // ✅ Criando mídia antes do teste
//        TestUtil.createTestMedia("educaAPI/screenshot.png");
//
//        webClient.get()
//                .uri("/api/media/get/educaAPI/1")
//                .header("api-key", "123")
//                .exchange()
//                .expectStatus().isOk()
//                .expectBody()
//                .jsonPath("$.url").exists(); // Confirma que uma URL foi gerada
//
//        System.out.println("✅ URL assinada gerada com sucesso!");
//    }
//
//    /**
//     * 🟢 Teste de sucesso - Atualizar mídia
//     */
//    @Test
//    @DisplayName("Teste de integração - Atualizar mídia")
//    void testUpdateMedia() {
//        // ✅ Criando mídia antes do teste
//        TestUtil.createTestMedia("educaAPI/screenshot.png");
//
//        MultipartBodyBuilder builder = new MultipartBodyBuilder();
//        builder.part("entityType", "THEME");
//        builder.part("tag", "novoTeste");
//        builder.part("mediaType", "IMAGE");
//        builder.part("file", new ByteArrayResource("novoConteudoImagem".getBytes()) {
//            @Override
//            public String getFilename() {
//                return "updated_screenshot.png";
//            }
//        }).header("Content-Type", "image/png");
//
//        webClient.put()
//                .uri("/api/media/update/educaAPI/1")
//                .header("api-key", "123")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(fromMultipartData(builder.build()))
//                .exchange()
//                .expectStatus().isOk()
//                .expectBody()
//                .jsonPath("$.tag").isEqualTo("novoTeste")
//                .jsonPath("$.fileName").value(fileName -> {
//                    assert fileName.toString().contains("updated_screenshot.png");
//                });
//
//        System.out.println("✅ Mídia atualizada com sucesso!");
//    }
//
//    /**
//     * 🟢 Teste de sucesso - Desativar mídia
//     */
//    @Test
//    @DisplayName("Teste de integração - Desativar mídia")
//    void testDisableMedia() {
//        // ✅ Criando mídia antes do teste
//        TestUtil.createTestMedia("educaAPI/screenshot.png");
//
//        webClient.delete()
//                .uri("/api/media/educaAPI/1")
//                .header("api-key", "123")
//                .exchange()
//                .expectStatus().isNoContent(); // Espera HTTP 204
//
//        System.out.println("✅ Mídia desativada com sucesso!");
//    }
//
////    @Test
////    @DisplayName("❌ Upload de mídia sem arquivo - Falha")
////    void testUploadMediaWithoutFile() {
////        MultipartBodyBuilder builder = new MultipartBodyBuilder();
////        builder.part("serviceName", "educaAPI");
////        builder.part("tag", "teste");
////        builder.part("entityType", "THEME");
////        builder.part("uploadedBy", "1");
////
////        webClient.post()
////                .uri("/api/media/post")
////                .header("api-key", "123")
////                .contentType(MediaType.MULTIPART_FORM_DATA)
////                .body(fromMultipartData(builder.build()))
////                .exchange()
////                .expectStatus().isBadRequest();
////
////        System.out.println("❌ Upload falhou corretamente!");
////    }
//
//    @Test
//    @DisplayName("❌ Obter URL de mídia inexistente - Falha")
//    void testGetNonexistentMediaUrl() {
//        webClient.get()
//                .uri("/api/media/get/educaAPI/999")
//                .header("api-key", "123")
//                .exchange()
//                .expectStatus().isNotFound();
//
//        System.out.println("❌ Erro 404 corretamente retornado!");
//    }
//
//    @Test
//    @DisplayName("❌ Atualizar mídia inexistente - Falha")
//    void testUpdateNonexistentMedia() {
//        MultipartBodyBuilder builder = new MultipartBodyBuilder();
//        builder.part("entityType", "THEME");
//        builder.part("tag", "novoTeste");
//        builder.part("mediaType", "IMAGE");
//        builder.part("file", new ByteArrayResource("novoConteudoImagem".getBytes()) {
//            @Override
//            public String getFilename() {
//                return "updated_screenshot.png";
//            }
//        }).header("Content-Type", "image/png");
//
//        webClient.put()
//                .uri("/api/media/update/educaAPI/999")
//                .header("api-key", "123")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(fromMultipartData(builder.build()))
//                .exchange()
//                .expectStatus().isNotFound();
//
//        System.out.println("❌ Erro 404 corretamente retornado!");
//    }
//
//    @Test
//    @DisplayName("❌ Desativar mídia inexistente - Falha")
//    void testDisableNonexistentMedia() {
//        webClient.delete()
//                .uri("/api/media/educaAPI/999")
//                .header("api-key", "123")
//                .exchange()
//                .expectStatus().isNotFound();
//
//        System.out.println("❌ Erro 404 corretamente retornado!");
//    }
//}
