package com.apps4society.MinIO_API.integration;

import io.minio.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MediaIntegrationIT {
    // 🔹 Constantes reutilizáveis
    protected static final String SERVICE_NAME = "educAPI";
    protected static final Long UPLOADED_BY = 1L;
    protected static final Long ENTITY_ID = 42L;
    protected static final String API_KEY = "123";
    protected static final String BUCKET_NAME = "test-bucket";
    protected static final String TEST_IMAGE_NAME = "test-image.jpg";
    protected static final String TEST_UPDATED_IMAGE_NAME = "test-image-updated.jpg";

    @LocalServerPort
    protected int port;

    @Container
    public static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    public static GenericContainer<?> minioContainer = new GenericContainer<>("minio/minio:latest")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server /data");

    @BeforeAll
    static void setupAll() {
        log.info("Iniciando containers de teste...");

        mysqlContainer.start();
        minioContainer.start();

        log.info("✅ Containers rodando!");

        String minioUrl = "http://" + minioContainer.getHost() + ":" + minioContainer.getMappedPort(9000);
        System.setProperty("minio.url", minioUrl);
        RestAssured.baseURI = "http://localhost";

        criarBucketMinio(minioUrl);
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String mysqlUrl = mysqlContainer.getJdbcUrl();
        log.info("Definindo spring.datasource.url = {}", mysqlUrl);

        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("MYSQL_HOST", mysqlContainer::getHost);
        registry.add("MYSQL_PORT", () -> mysqlContainer.getMappedPort(3306));
    }

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    static void criarBucketMinio(String minioUrl) {
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(minioUrl)
                    .credentials("minioadmin", "minioadmin")
                    .build();

            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
                log.info("Bucket '{}' criado com sucesso!", BUCKET_NAME);
            } else {
                log.info("Bucket '{}' já existe.", BUCKET_NAME);
            }
        } catch (Exception e) {
            log.error("Erro ao criar bucket no MinIO: ", e);
        }
    }

    protected InputStream createImageFile() {
        log.info("Criando imagem de teste.");
        byte[] fakeImage = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        return new ByteArrayInputStream(fakeImage);
    }

    protected Map<String, Object> createTestMedia() {
        log.info("Enviando mídia de teste.");

        InputStream imageStream = createImageFile();

        var response = given()
                .multiPart("file", TEST_IMAGE_NAME, imageStream, "image/jpeg")
                .formParam("serviceName", SERVICE_NAME)
                .formParam("uploadedBy", UPLOADED_BY)
                .formParam("entityId", ENTITY_ID)
                .header("api-key", API_KEY)
                .contentType("multipart/form-data")
                .when()
                .post("/api/media");

        response.then()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue());

        Long mediaId = response.jsonPath().getLong("id");
        log.info("Mídia criada com sucesso: ID {}", mediaId);

        return Map.of(
                "id", mediaId,
                "serviceName", SERVICE_NAME,
                "fileName", TEST_IMAGE_NAME,
                "uploadedBy", UPLOADED_BY
        );
    }

    protected void cleanupTestMedia(String fileName) {
        log.info("Removendo mídia de teste do MinIO: {}", fileName);

        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(System.getProperty("minio.url"))
                    .credentials("minioadmin", "minioadmin")
                    .build();

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(fileName)
                            .build()
            );

            log.info("Mídia '{}' removida com sucesso.", fileName);
        } catch (Exception e) {
            log.error("Erro ao remover mídia '{}': ", fileName, e);
        }
    }

    @Test
    @DisplayName("POST /api/media - Upload de mídia bem-sucedido (201 CREATED)")
    void testUploadMedia_Success_ShouldReturn201() {
        Map<String, Object> testMedia = createTestMedia();

        given()
                .pathParam("serviceName", testMedia.get("serviceName"))
                .pathParam("mediaId", testMedia.get("id"))
                .header("api-key", API_KEY)
                .accept(ContentType.TEXT)  //
                .when()
                .get("/api/media/{serviceName}/{mediaId}")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(not(emptyOrNullString()))
                .body(matchesPattern("http.*"));

        cleanupTestMedia(TEST_IMAGE_NAME);
    }

    //---

    @Test
    @DisplayName("PUT /api/media/{serviceName}/{mediaId} - Atualizar mídia existente (200 OK)")
    void testUpdateMedia_Success_ShouldReturn200() {
        Map<String, Object> testMedia = createTestMedia();

        InputStream updatedImageStream = createImageFile();

        given()
                .pathParam("serviceName", testMedia.get("serviceName"))
                .pathParam("mediaId", testMedia.get("id"))
                .formParam("uploadedBy", UPLOADED_BY)
                .formParam("entityId", ENTITY_ID)
                .multiPart("file", TEST_UPDATED_IMAGE_NAME, updatedImageStream, "image/jpeg")
                .header("api-key", API_KEY)
                .contentType(ContentType.MULTIPART)
                .when()
                .put("/api/media/{serviceName}/{mediaId}")
                .then()
                .statusCode(200)
                .body("fileName", notNullValue());

        cleanupTestMedia(TEST_UPDATED_IMAGE_NAME);
    }

    @Test
    @DisplayName("PUT /api/media/{serviceName}/{mediaId} - Mídia não encontrada (404 NOT FOUND)")
    void testUpdateMedia_NotFound_ShouldReturn404() {
        InputStream updatedImageStream = createImageFile();

        given()
                .pathParam("serviceName", SERVICE_NAME)
                .pathParam("mediaId", 99999L)
                .formParam("uploadedBy", UPLOADED_BY)
                .formParam("entityId", ENTITY_ID)
                .multiPart("file", TEST_UPDATED_IMAGE_NAME, updatedImageStream, "image/jpeg")
                .header("api-key", API_KEY)
                .contentType(ContentType.MULTIPART)
                .when()
                .put("/api/media/{serviceName}/{mediaId}")
                .then()
                .statusCode(404);
    }

    // ---

    @Test
    @DisplayName("GET /api/media/{serviceName}/{mediaId} - Buscar URL de mídia existente (200 OK)")
    void testGetMediaUrl_Success_ShouldReturn200() {
        // 🔹 Criando mídia de teste
        Map<String, Object> testMedia = createTestMedia();

        // 🔹 Buscando a URL da mídia enviada
        given()
                .pathParam("serviceName", testMedia.get("serviceName"))
                .pathParam("mediaId", testMedia.get("id"))
                .header("api-key", API_KEY)
                .accept("text/plain")
                .when()
                .get("/api/media/{serviceName}/{mediaId}")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(not(emptyOrNullString()))
                .body(matchesPattern("http.*"));

        cleanupTestMedia(TEST_IMAGE_NAME);
    }

    @Test
    @DisplayName("GET /api/media/{serviceName}/{mediaId} - Mídia não encontrada (404 NOT FOUND)")
    void testGetMedia_NotFound_ShouldReturn404() {
        given()
                .pathParam("serviceName", SERVICE_NAME)
                .pathParam("mediaId", 99999L)
                .header("api-key", API_KEY)
                .when()
                .get("/api/media/{serviceName}/{mediaId}")
                .then()
                .statusCode(404);
    }

    // --

    @Test
    @DisplayName("DELETE /api/media/{serviceName}/{mediaId} - Desativar mídia existente (204 No Content)")
    void testDisableMedia_Success_ShouldReturn204() {
        Map<String, Object> testMedia = createTestMedia();

        given()
                .pathParam("serviceName", testMedia.get("serviceName"))
                .pathParam("mediaId", testMedia.get("id"))
                .header("api-key", API_KEY)
                .when()
                .delete("/api/media/{serviceName}/{mediaId}")
                .then()
                .statusCode(204);

        int responseStatus = given()
                .header("api-key", API_KEY)
                .when()
                .get("/api/media/{serviceName}/{mediaId}", testMedia.get("serviceName"), testMedia.get("id"))
                .then()
                .extract()
                .statusCode();

        assertEquals(404, responseStatus, "A mídia ainda existe no MinIO após a desativação!");
    }


    @Test
    @DisplayName("DELETE /api/media/{serviceName}/{mediaId} - Mídia não encontrada (404 NOT FOUND)")
    void testDisableMedia_NotFound_ShouldReturn404() {
        given()
                .pathParam("serviceName", SERVICE_NAME)
                .pathParam("mediaId", 99999L) // ID inexistente
                .header("api-key", API_KEY)
                .when()
                .delete("/api/media/{serviceName}/{mediaId}")
                .then()
                .statusCode(404);
    }
}
