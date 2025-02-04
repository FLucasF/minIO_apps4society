//package com.apps4society.MinIO_API;
//
//import io.restassured.RestAssured;
//import io.restassured.http.ContentType;
//import io.restassured.response.Response;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import java.io.File;
//import static io.restassured.RestAssured.given;
//import static org.hamcrest.Matchers.equalTo;
//import static org.hamcrest.Matchers.*;
//
//public class MediaControllerTest {
//
//    @Test
//    @DisplayName("teste upload")
//    public void testUploadMedia() { // Ver o motivo de falhar (black box)
//        Response response = given()
//                .baseUri("https://localhost:8080/api/media")
//                .relaxedHTTPSValidation()
//                .header("api-key", "123")
//                .contentType(ContentType.MULTIPART)
//                .multiPart("file", new File("src/test/resources/test-image.png"))
//                .formParam("tag", "teste")
//                .formParam("entityType", "THEME")
//                .formParam("uploadedBy", "1")
//                .when()
//                .post("/post/educAPI")
//                .then()
//                .statusCode(200) // Espera um código de sucesso
//                .body("serviceName", equalTo("educAPI"))
//                .body("mediaType", equalTo("IMAGE"))
//                .body("tag", equalTo("teste"))
//                .body("entityType", equalTo("THEME"))
//                .extract()
//                .response();
//
//        // Como apagar imagem no Rest assured
//        // Criar um banco exclusivo para teste
//        System.out.println("Upload response: " + response.asString());
//    }
//
//    @Test
//    public void testGetMediaUrl() {
//        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
//
//        Response response = given()
//                .baseUri("https://localhost:8080/api/media")
//                .relaxedHTTPSValidation()
//                .header("api-key", "123")
//                .when()
//                .get("/get/educAPI/3")
//                .then()
//                .statusCode(200)
//                .body("url", notNullValue())
//                .body("url", startsWith("http://localhost:9000/"))
//                .body("url", containsString("educAPI"))
//                .body("url", containsString("?X-Amz-Algorithm="))
//                .extract()
//                .response();
//
//        System.out.println("GET response: " + response.asString());
//    }
//
//    @Test
//    public void testUpdateMedia() {
//        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
//
//        File testFile = new File("src/test/resources/test-image-updated.png");
//
//        Response response = given()
//                .baseUri("https://localhost:8080/api/media")
//                .relaxedHTTPSValidation()
//                .header("api-key", "123")
//                .contentType(ContentType.MULTIPART)
//                .multiPart("file", testFile)
//                .formParam("entityType", "THEME")
//                .formParam("tag", "testeAtualizado")
//                .formParam("mediaType", "IMAGE")
//                .when()
//                .put("/update/educAPI/3")
//                .then()
//                .statusCode(200)
//                .body("serviceName", equalTo("educAPI"))
//                .body("mediaType", equalTo("IMAGE"))
//                .body("tag", equalTo("testeAtualizado"))
//                .body("entityType", equalTo("THEME"))
//                .extract()
//                .response();
//
//        System.out.println("Update response: " + response.asString());
//    }
//}
