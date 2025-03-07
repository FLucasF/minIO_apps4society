package com.apps4society.MinIO_API.integration;

import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.service.MediaService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public class MediaIntegrationIT {

    private static final Logger logger = LoggerFactory.getLogger(MediaIntegrationIT.class);

    @LocalServerPort
    private int port;

    @Autowired
    private MediaTestUtil mediaTestUtil;

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
    static void setup() {
        logger.info("Iniciando configuração dos containers");
        mysqlContainer.start();
        minioContainer.start();
        logger.info("Containers iniciados!");

        String minioUrl = "http://" + minioContainer.getHost() + ":" + minioContainer.getMappedPort(9000);
        System.setProperty("minio.url", minioUrl);

        int mysqlPort = mysqlContainer.getMappedPort(3306);
        System.setProperty("TESTCONTAINERS_MYSQL_PORT", String.valueOf(mysqlPort));

        RestAssured.baseURI = "http://localhost";

        logger.info("MySQL rodando em {}:{}", mysqlContainer.getHost(), mysqlPort);
        logger.info("MinIO rodando em {}", minioUrl);

        verificarBancoDeDados();
        criarBucketMinio(minioUrl);

    }

    static void criarBucketMinio(String minioUrl) {
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(minioUrl)
                    .credentials("minioadmin", "minioadmin")
                    .build();

            String bucketName = "test-bucket";

            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                logger.info("Bucket '{}' criado com sucesso!", bucketName);
            } else {
                logger.info("Bucket '{}' já existe.", bucketName);
            }
        } catch (Exception e) {
            logger.error("Erro ao criar bucket no MinIO: ", e);
        }
    }

    static void verificarBancoDeDados() {
        try (Connection connection = DriverManager.getConnection(mysqlContainer.getJdbcUrl(), "test", "test");
             Statement statement = connection.createStatement()) {

            ResultSet resultSet = statement.executeQuery("SHOW TABLES");
            while (resultSet.next()) {
                logger.info("Tabela encontrada no banco de testes: {}", resultSet.getString(1));
            }
        } catch (Exception e) {
            logger.error("Erro ao verificar o banco de dados de teste: ", e);
        }
    }

    @BeforeEach
    void setUpTest() {
        RestAssured.port = port;
        logger.info("Iniciando teste na porta: {}", port);
    }

    @Test
    @DisplayName("Verificação do ambiente de testes")
    void testEnvironmentSetup() {
        logger.info("Testando se o ambiente de testes está configurado corretamente...");
        assertTrue(mysqlContainer.isRunning(), "O container do MySQL não está rodando.");
        assertTrue(minioContainer.isRunning(), "O container do MinIO não está rodando.");
        logger.info("Ambiente de testes configurado corretamente.");
    }

    @Test
    @DisplayName("POST /api/media/post - Sucesso (201 CREATED)")
    void testSaveMedia_WithValidData_ShouldReturn201Created() {
        logger.info("Iniciando upload de mídia...");

        String fileName = "test-image.jpg";

        var response = given()
                .log().all()
                .multiPart("file", new File("src/test/resources/test-image.jpg"))
                .formParam("serviceName", "educaAPI")
                .formParam("tag", "profile")
                .formParam("entityType", "THEME")
                .formParam("uploadedBy", "1")
                .header("api-key", "123")
                .contentType(ContentType.MULTIPART)
                .when()
                .post("/api/media/post")
                .thenReturn();

        logger.info("Resposta do servidor: {} - {}", response.getStatusCode(), response.getBody().asString());

        response.then()
                .log().all()
                .statusCode(201)
                .body("id", notNullValue());

        assertNotNull(response.jsonPath().get("fileName"), "O fileName retornado não deveria ser nulo.");
        assertNotNull(response.jsonPath().get("mediaType"), "O mediaType retornado não deveria ser nulo.");
        assertNotNull(response.jsonPath().get("tag"), "A tag retornado não deveria ser nulo.");
        assertNotNull(response.jsonPath().get("active"), "O active retornado não deveria ser nulo.");
        assertNotNull(response.jsonPath().get("uploadedBy"), "O uploadedBy retornado não deveria ser nulo.");
        assertNotNull(response.jsonPath().get("entityType"), "O entityType retornado não deveria ser nulo.");
        assertNotNull(response.jsonPath().get("serviceName"), "O serviceName retornado não deveria ser nulo.");
        assertNotNull(response.jsonPath().get("id"), "O id retornado não deveria ser nulo.");

        assertEquals(201, response.getStatusCode(), "Código de status não corresponde ao esperado.");
        assertEquals("educaAPI", response.jsonPath().get("serviceName"), "O serviceName retornado não corresponde ao esperado.");
        assertEquals("profile", response.jsonPath().get("tag"), "A tag retornada não corresponde ao esperado.");
        assertEquals("THEME", response.jsonPath().get("entityType"), "O entityType retornado não corresponde ao esperado.");
        assertEquals(1L, response.jsonPath().getLong("uploadedBy"), "O uploadedBy retornado não corresponde ao esperado.");
        assertTrue(response.jsonPath().getBoolean("active"), "O active retornado não corresponde ao esperado.");

        logger.info("Upload de mídia realizado com sucesso.");

        mediaTestUtil.cleanupTestMedia(fileName);
        assertFalse(mediaTestUtil.checkFileExists(fileName), "A mídia não foi removida corretamente.");
    }

    @Test
    @DisplayName("GET /api/media/get/{serviceName}/{mediaId} - Buscar URL assinada com mídia criada deve retornar 200 OK")
    void testGetMediaUrl_AfterUploadingMedia_ShouldReturn200OK() {
        logger.info("Criando mídia de teste...");

        Map<String, Object> testMedia = mediaTestUtil.createTestMedia("educaAPI/test-image.png");

        logger.info("Mídia criada: {}", testMedia);

        logger.info("Solicitando URL assinada para mídia ID: {}", testMedia.get("id"));

        var response = given()
                .log().all() // Logando a requisição para depuração
                .pathParam("serviceName", testMedia.get("serviceName"))
                .pathParam("mediaId", testMedia.get("id"))
                .header("api-key", "123")
                .when()
                .get("/api/media/get/{serviceName}/{mediaId}")
                .thenReturn();

        response.then().log().all(); // Logando a resposta para verificar o retorno

        assertEquals(200, response.getStatusCode(), "Código de status não corresponde ao esperado.");
        assertNotNull(response.jsonPath().get("url"), "A URL retornada não deveria ser nula.");

        logger.info("URL assinada obtida com sucesso.");

        // Cleanup - Remover a mídia após o teste
        mediaTestUtil.cleanupTestMedia((String) testMedia.get("fileName"));
        assertFalse(mediaTestUtil.checkFileExists((String) testMedia.get("fileName")), "A mídia não foi removida corretamente.");
    }


    @Test
    @DisplayName("PUT /api/media/update/{serviceName}/{mediaId} - Atualizar mídia existente deve retornar 200 OK")
    void testUpdateMedia_WithValidData_ShouldReturn200OK() throws Exception {
        logger.info("Criando mídia de teste...");

        Map<String, Object> testMedia = mediaTestUtil.createTestMedia("educaAPI/test-image.png");

        logger.info("Mídia criada: {}", testMedia);

        logger.info("Verificando se o arquivo de atualização existe...");
        File updatedFile = new File("src/test/resources/test-image-updated.jpg");

        if (!updatedFile.exists() || updatedFile.length() == 0) {
            logger.warn("⚠️ Arquivo não encontrado ou vazio! Criando arquivo temporário...");
            try (FileOutputStream fos = new FileOutputStream(updatedFile)) {
                fos.write("conteudo de teste".getBytes()); // Garante que o arquivo tem algum conteúdo
            }
        }

        logger.info("Iniciando atualização de mídia ID: {}", testMedia.get("id"));

        var response = given()
                .log().all() // Logando a requisição para depuração
                .pathParam("serviceName", testMedia.get("serviceName"))
                .pathParam("mediaId", testMedia.get("id"))
                .formParam("entityType", "THEME")
                .formParam("tag", "updated-profile")
                .formParam("mediaType", "IMAGE")
                .multiPart("file", updatedFile) // Arquivo garantido para o teste
                .header("api-key", "123")
                .contentType(ContentType.MULTIPART)
                .when()
                .put("/api/media/update/{serviceName}/{mediaId}")
                .thenReturn();

        response.then().log().all(); // Logando a resposta para depuração

        assertEquals(200, response.getStatusCode(), "Código de status não corresponde ao esperado.");
        assertNotNull(response.jsonPath().get("fileName"), "O fileName retornado não deveria ser nulo.");
        assertNotNull(response.jsonPath().get("mediaType"), "O mediaType retornado não deveria ser nulo.");
        assertNotNull(response.jsonPath().get("tag"), "A tag retornada não deveria ser nula.");
        assertNotNull(response.jsonPath().get("active"), "O active retornado não deveria ser nulo.");
        assertNotNull(response.jsonPath().get("uploadedBy"), "O uploadedBy retornado não deveria ser nulo.");
        assertNotNull(response.jsonPath().get("entityType"), "O entityType retornado não deveria ser nulo.");
        assertNotNull(response.jsonPath().get("serviceName"), "O serviceName retornado não deveria ser nulo.");
        assertNotNull(response.jsonPath().get("id"), "O id retornado não deveria ser nulo.");

        assertEquals("educaAPI", response.jsonPath().get("serviceName"), "O serviceName retornado não corresponde ao esperado.");
        assertEquals("updated-profile", response.jsonPath().get("tag"), "A tag retornada não corresponde ao esperado.");
        assertEquals("THEME", response.jsonPath().get("entityType"), "O entityType retornado não corresponde ao esperado.");
        assertEquals(testMedia.get("uploadedBy"), response.jsonPath().getLong("uploadedBy"), "O uploadedBy retornado não corresponde ao esperado.");
        assertTrue(response.jsonPath().getBoolean("active"), "O active retornado não corresponde ao esperado.");

        logger.info("✅ Mídia atualizada com sucesso.");

        mediaTestUtil.cleanupTestMedia((String) testMedia.get("fileName"));
        assertFalse(mediaTestUtil.checkFileExists((String) testMedia.get("fileName")), "A mídia não foi removida corretamente.");
    }

    @Test
    @DisplayName("DELETE /api/media/delete/{serviceName}/{mediaId} - Desabilitar mídia deve atualizar banco e bucket")
    void testDisableMedia_ShouldDisableInDatabaseAndBucket() {
        logger.info("Criando mídia de teste...");

        Map<String, Object> testMedia = mediaTestUtil.createTestMedia("educaAPI/test-image.png");

        logger.info("🟡 Mídia criada: {}", testMedia);

        logger.info("Desabilitando mídia ID: {}", testMedia.get("id"));

        var response = given()
                .log().all() // Logando a requisição para depuração
                .pathParam("serviceName", testMedia.get("serviceName"))
                .pathParam("mediaId", testMedia.get("id"))
                .header("api-key", "123")
                .when()
                .delete("/api/media/delete/{serviceName}/{mediaId}")
                .thenReturn();

        response.then().log().all().statusCode(204); // Verifica apenas o status da resposta

        Media mediaFromDb = mediaTestUtil.findMediaById((Long) testMedia.get("id"));
        assertNotNull(mediaFromDb, "A mídia deveria existir no banco.");
        assertFalse(mediaFromDb.isActive(), "A mídia deveria estar desativada no banco de dados.");

        boolean existsInActiveFolder = mediaTestUtil.checkFileExists("educaAPI/test-image.png");
        boolean existsInDisabledFolder = mediaTestUtil.checkFileExists("arquivos_desativados/educaAPI/test-image.png");

        assertFalse(existsInActiveFolder, "O arquivo ativo não deveria existir mais no MinIO.");
        assertTrue(existsInDisabledFolder, "O arquivo deveria estar movido para a pasta de desativados.");

        logger.info("✅ Mídia desativada corretamente no banco e no MinIO.");
    }








}
