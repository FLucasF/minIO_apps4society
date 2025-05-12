package com.apps4society.MinIO_API.integration;

import io.minio.*;
import io.restassured.RestAssured;
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
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class BaseMediaIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseMediaIntegrationTest.class);

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
        logger.info("🚀 Iniciando containers de teste...");

        mysqlContainer.start();
        minioContainer.start();

        logger.info("✅ Containers rodando!");

        // 🔹 Configuração do MinIO
        String minioUrl = "http://" + minioContainer.getHost() + ":" + minioContainer.getMappedPort(9000);
        System.setProperty("minio.url", minioUrl);
        RestAssured.baseURI = "http://localhost";

        criarBucketMinio(minioUrl);
    }

    /**
     * 🔹 Injeta as propriedades no contexto Spring antes da inicialização.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String mysqlUrl = mysqlContainer.getJdbcUrl();
        logger.info("🔗 Definindo spring.datasource.url = {}", mysqlUrl);

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
                logger.info("✅ Bucket '{}' criado com sucesso!", BUCKET_NAME);
            } else {
                logger.info("✅ Bucket '{}' já existe.", BUCKET_NAME);
            }
        } catch (Exception e) {
            logger.error("❌ Erro ao criar bucket no MinIO: ", e);
        }
    }

    protected InputStream createImageFile() {
        logger.info("📸 Criando imagem de teste...");
        byte[] fakeImage = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        return new ByteArrayInputStream(fakeImage);
    }

    protected Map<String, Object> createTestMedia() {
        logger.info("📤 Enviando mídia de teste...");

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
        logger.info("✅ Mídia criada com sucesso: ID {}", mediaId);

        return Map.of(
                "id", mediaId,
                "serviceName", SERVICE_NAME,
                "fileName", TEST_IMAGE_NAME,
                "uploadedBy", UPLOADED_BY
        );
    }

    protected void cleanupTestMedia(String fileName) {
        logger.info("🗑️ Removendo mídia de teste do MinIO: {}", fileName);

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

            logger.info("✅ Mídia '{}' removida com sucesso.", fileName);
        } catch (Exception e) {
            logger.error("❌ Erro ao remover mídia '{}': ", fileName, e);
        }
    }
}
