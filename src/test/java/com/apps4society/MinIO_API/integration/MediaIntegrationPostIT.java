//package com.apps4society.MinIO_API.integration;
//
//import io.restassured.http.ContentType;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.util.Map;
//
//import static io.restassured.RestAssured.given;
//import static org.hamcrest.Matchers.*;
//
//public class MediaIntegrationPostIT extends BaseMediaIntegrationTest {
//
//    @Test
//    @DisplayName("POST /api/media - Upload de mídia bem-sucedido (201 CREATED)")
//    void testUploadMedia_Success_ShouldReturn201() {
//        // 🔹 Criando e enviando mídia de teste
//        Map<String, Object> testMedia = createTestMedia();
//
//        // 🔹 Recuperando URL assinada da mídia enviada
//        given()
//                .pathParam("serviceName", testMedia.get("serviceName"))
//                .pathParam("mediaId", testMedia.get("id"))
//                .header("api-key", API_KEY)
//                .accept(ContentType.TEXT)  // 🔹 Indica que esperamos um 'text/plain'
//                .when()
//                .get("/api/media/{serviceName}/{mediaId}")
//                .then()
//                .statusCode(200) // 🔹 Deve retornar 200 OK
//                .contentType(ContentType.TEXT) // 🔹 Confirma que o retorno é `text/plain`
//                .body(not(emptyOrNullString())) // 🔹 Verifica se a resposta não é vazia
//                .body(matchesPattern("http.*")); // 🔹 Verifica se é uma URL válida (começa com http)
//
//        cleanupTestMedia(TEST_IMAGE_NAME); // 🔹 Remove a mídia após o teste
//    }
//}
