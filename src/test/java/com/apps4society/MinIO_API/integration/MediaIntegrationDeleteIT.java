//package com.apps4society.MinIO_API.integration;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.util.Map;
//
//import static io.restassured.RestAssured.given;
//import static org.hamcrest.Matchers.*;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//
//public class MediaIntegrationDeleteIT extends BaseMediaIntegrationTest {
//
//    @Test
//    @DisplayName("DELETE /api/media/{serviceName}/{mediaId} - Desativar mídia existente (204 No Content)")
//    void testDisableMedia_Success_ShouldReturn204() {
//        // 🔹 Criando mídia de teste
//        Map<String, Object> testMedia = createTestMedia();
//
//        // 🔹 Desativando a mídia
//        given()
//                .pathParam("serviceName", testMedia.get("serviceName"))
//                .pathParam("mediaId", testMedia.get("id"))
//                .header("api-key", API_KEY)
//                .when()
//                .delete("/api/media/{serviceName}/{mediaId}")
//                .then()
//                .statusCode(204); // ✅ 204 No Content indica sucesso
//
//        // 🔹 Tentando buscar a mídia no MinIO
//        int responseStatus = given()
//                .header("api-key", API_KEY)
//                .when()
//                .get("/api/media/{serviceName}/{mediaId}", testMedia.get("serviceName"), testMedia.get("id"))
//                .then()
//                .extract()
//                .statusCode();
//
//        // 🔹 O esperado é que a mídia não seja encontrada (404 NOT FOUND)
//        assertEquals(404, responseStatus, "A mídia ainda existe no MinIO após a desativação!");
//    }
//
//
//    @Test
//    @DisplayName("DELETE /api/media/{serviceName}/{mediaId} - Mídia não encontrada (404 NOT FOUND)")
//    void testDisableMedia_NotFound_ShouldReturn404() {
//        given()
//                .pathParam("serviceName", SERVICE_NAME)
//                .pathParam("mediaId", 99999L) // ID inexistente
//                .header("api-key", API_KEY)
//                .when()
//                .delete("/api/media/{serviceName}/{mediaId}")
//                .then()
//                .statusCode(404);
//    }
//}
