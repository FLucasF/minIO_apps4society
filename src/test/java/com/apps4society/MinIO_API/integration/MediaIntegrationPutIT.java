//package com.apps4society.MinIO_API.integration;
//
//import io.restassured.http.ContentType;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.io.InputStream;
//import java.util.Map;
//
//import static io.restassured.RestAssured.given;
//import static org.hamcrest.Matchers.notNullValue;
//
//public class MediaIntegrationPutIT extends BaseMediaIntegrationTest {
//
//    @Test
//    @DisplayName("PUT /api/media/{serviceName}/{mediaId} - Atualizar mídia existente (200 OK)")
//    void testUpdateMedia_Success_ShouldReturn200() {
//        Map<String, Object> testMedia = createTestMedia(); // 🔹 Criando mídia de teste
//
//        InputStream updatedImageStream = createImageFile(); // 🔹 Criando imagem simulada para atualização
//
//        given()
//                .pathParam("serviceName", testMedia.get("serviceName"))
//                .pathParam("mediaId", testMedia.get("id"))
//                .formParam("uploadedBy", UPLOADED_BY)
//                .formParam("entityId", ENTITY_ID)
//                .multiPart("file", TEST_UPDATED_IMAGE_NAME, updatedImageStream, "image/jpeg")
//                .header("api-key", API_KEY)
//                .contentType(ContentType.MULTIPART)
//                .when()
//                .put("/api/media/{serviceName}/{mediaId}")
//                .then()
//                .statusCode(200)
//                .body("fileName", notNullValue());
//
//        cleanupTestMedia(TEST_UPDATED_IMAGE_NAME); // 🔹 Cleanup após o teste
//    }
//
//    @Test
//    @DisplayName("PUT /api/media/{serviceName}/{mediaId} - Mídia não encontrada (404 NOT FOUND)")
//    void testUpdateMedia_NotFound_ShouldReturn404() {
//        InputStream updatedImageStream = createImageFile(); // 🔹 Criando imagem simulada para atualização
//
//        given()
//                .pathParam("serviceName", SERVICE_NAME)
//                .pathParam("mediaId", 99999L) // 🔹 ID que não existe
//                .formParam("uploadedBy", UPLOADED_BY)
//                .formParam("entityId", ENTITY_ID)
//                .multiPart("file", TEST_UPDATED_IMAGE_NAME, updatedImageStream, "image/jpeg")
//                .header("api-key", API_KEY)
//                .contentType(ContentType.MULTIPART)
//                .when()
//                .put("/api/media/{serviceName}/{mediaId}")
//                .then()
//                .statusCode(404);
//    }
//}
