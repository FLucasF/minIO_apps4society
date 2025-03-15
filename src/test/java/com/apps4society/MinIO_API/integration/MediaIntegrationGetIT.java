//package com.apps4society.MinIO_API.integration;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.util.Map;
//
//import static io.restassured.RestAssured.given;
//import static org.hamcrest.Matchers.*;
//
//public class MediaIntegrationGetIT extends BaseMediaIntegrationTest {
//
//    @Test
//    @DisplayName("GET /api/media/{serviceName}/{mediaId} - Buscar URL de mídia existente (200 OK)")
//    void testGetMediaUrl_Success_ShouldReturn200() {
//        // 🔹 Criando mídia de teste
//        Map<String, Object> testMedia = createTestMedia();
//
//        // 🔹 Buscando a URL da mídia enviada
//        given()
//                .pathParam("serviceName", testMedia.get("serviceName"))
//                .pathParam("mediaId", testMedia.get("id"))
//                .header("api-key", API_KEY)
//                .accept("text/plain") // 🔹 Aceita resposta `text/plain`
//                .when()
//                .get("/api/media/{serviceName}/{mediaId}")
//                .then()
//                .statusCode(200) // 🔹 Deve retornar 200 OK
//                .contentType("text/plain") // 🔹 Confirma que a resposta é `text/plain`
//                .body(not(emptyOrNullString())) // 🔹 Verifica que não está vazia
//                .body(matchesPattern("http.*")); // 🔹 Verifica que a resposta contém uma URL válida
//
//        // 🔹 Limpando a mídia após o teste
//        cleanupTestMedia(TEST_IMAGE_NAME);
//    }
//
//    @Test
//    @DisplayName("GET /api/media/{serviceName}/{mediaId} - Mídia não encontrada (404 NOT FOUND)")
//    void testGetMedia_NotFound_ShouldReturn404() {
//        given()
//                .pathParam("serviceName", SERVICE_NAME)
//                .pathParam("mediaId", 99999L) // ID inexistente
//                .header("api-key", API_KEY)
//                .when()
//                .get("/api/media/{serviceName}/{mediaId}")
//                .then()
//                .statusCode(404);
//    }
//}
