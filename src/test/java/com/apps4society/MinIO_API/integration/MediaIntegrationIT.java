package com.apps4society.MinIO_API.integration;

import com.apps4society.MinIO_API.exceptions.DuplicateFileException;
import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.repository.MediaRepository;
import com.apps4society.MinIO_API.service.MediaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MediaIntegrationIT {
    // 🔹 Constantes reutilizáveis
    @Mock
    private MediaRepository mediaRepository; // Mock do MediaRepository

    @Mock
    private MediaService mediaService; // Mock do MediaService
    protected static final String SERVICE_NAME = "educAPI";
    protected static final Long UPLOADED_BY = 1L;
    protected static final Long ENTITY_ID = 1L;
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

        log.info("Containers rodando!");

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
        MediaRequest mediaRequest = new MediaRequest(SERVICE_NAME, UPLOADED_BY);
        ObjectMapper objectMapper = new ObjectMapper();
        String mediaRequestJson = null;
        try {
            mediaRequestJson = objectMapper.writeValueAsString(mediaRequest);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar mediaRequest para JSON", e);
            throw new RuntimeException("Erro ao serializar mediaRequest", e);
        }

        byte[] fileBytes = null;
        try {
            fileBytes = Files.readAllBytes(Paths.get("src/test/resources/" + TEST_IMAGE_NAME));
        } catch (IOException e) {
            log.error("Erro ao ler arquivo de teste: ", e);
            throw new RuntimeException("Erro ao ler o arquivo", e);
        }

        given()
                .contentType("multipart/form-data")
                .header("api-key", API_KEY)
                .multiPart("mediaRequest", "mediaRequest.json", mediaRequestJson.getBytes(), "application/json")
                .multiPart("file", TEST_IMAGE_NAME, fileBytes, "image/jpeg")
                .when()
                .post("/api/media")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("entityId", is(ENTITY_ID.intValue()))
                .body("serviceName", is(SERVICE_NAME))
                .body("fileName", is(TEST_IMAGE_NAME));
        cleanupTestMedia(TEST_IMAGE_NAME);
    }

    @Test
    @DisplayName("POST /api/media - Arquivo já existente (409 CONFLICT)")
    void testUploadMedia_Conflict_ShouldReturn409() {
        // Criar mediaRequest
        MediaRequest mediaRequest = new MediaRequest(SERVICE_NAME, UPLOADED_BY);
        String mediaRequestJson = serializeToJson(mediaRequest); // Serializa a MediaRequest em JSON
        byte[] fileBytes = readFile(TEST_IMAGE_NAME); // Lê o arquivo de teste

        // Simulando o comportamento do repositório para quando o arquivo já existir
        when(mediaRepository.existsByFileNameAndServiceName(TEST_IMAGE_NAME, SERVICE_NAME)).thenReturn(true);


        // Simulando o comportamento esperado do método uploadMedia, onde ele deve lançar DuplicateFileException
        when(mediaService.uploadMedia(
                ArgumentMatchers.any(MediaRequest.class),
                ArgumentMatchers.any(MultipartFile.class)
        )).thenThrow(new DuplicateFileException("O arquivo com o nome '" + TEST_IMAGE_NAME + "' já foi enviado para o serviço '" + SERVICE_NAME + "'"));

        // Realizando o teste
        given()
                .log().all()
                .contentType("multipart/form-data")
                .header("api-key", API_KEY)
                .multiPart("mediaRequest", "mediaRequest.json", mediaRequestJson.getBytes(), "application/json") // Passando o JSON da MediaRequest
                .multiPart("file", TEST_IMAGE_NAME, fileBytes, "image/jpeg") // Passando o arquivo
                .when()
                .post("/api/media")
                .then()
                .statusCode(HttpStatus.CONFLICT.value()) // Verifica se o status é 409
                .body("message", containsString("O arquivo com o nome")); // Verifica a mensagem da resposta

        // Certificando-se de que o método cleanup será executado
        cleanupTestMedia(TEST_IMAGE_NAME);
    }





    @Test
    @DisplayName("POST /api/media - Parâmetro ausente (400 BAD REQUEST)")
    void testUploadMedia_MissingParameter_ShouldReturn400() {
        byte[] fileBytes = readFile(TEST_IMAGE_NAME); // Arquivo sem mediaRequest

        given()
                .contentType("multipart/form-data")
                .header("api-key", API_KEY)
                .multiPart("file", TEST_IMAGE_NAME, fileBytes, "image/jpeg")
                .when()
                .post("/api/media")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value()) // Verifica se o status é 400
                .body("message", containsString("Parâmetro ausente"));
    }

    @Test
    @DisplayName("POST /api/media - Acesso negado (403 FORBIDDEN)")
    void testUploadMedia_Forbidden_ShouldReturn403() {
        // Simulando a exceção AccessDeniedException
        MediaRequest mediaRequest = new MediaRequest(SERVICE_NAME, UPLOADED_BY);
        String mediaRequestJson = serializeToJson(mediaRequest);
        byte[] fileBytes = readFile(TEST_IMAGE_NAME);

        given()
                .contentType("multipart/form-data")
                .header("api-key", API_KEY)
                .multiPart("mediaRequest", "mediaRequest.json", mediaRequestJson.getBytes(), "application/json")
                .multiPart("file", TEST_IMAGE_NAME, fileBytes, "image/jpeg")
                .when()
                .post("/api/media")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value()) // Verifica se o status é 403
                .body("message", containsString("Access Denied"));
    }

    @Test
    @DisplayName("POST /api/media - Mídia não encontrada (404 NOT FOUND)")
    void testUploadMedia_NotFound_ShouldReturn404() {
        MediaRequest mediaRequest = new MediaRequest(SERVICE_NAME, UPLOADED_BY);
        String mediaRequestJson = serializeToJson(mediaRequest);
        byte[] fileBytes = readFile(TEST_IMAGE_NAME);

        // Simulando MediaNotFoundException no serviço
        given()
                .contentType("multipart/form-data")
                .header("api-key", API_KEY)
                .multiPart("mediaRequest", "mediaRequest.json", mediaRequestJson.getBytes(), "application/json")
                .multiPart("file", TEST_IMAGE_NAME, fileBytes, "image/jpeg")
                .when()
                .post("/api/media")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value()) // Verifica se o status é 404
                .body("message", containsString("Mídia não encontrada"));
    }

    @Test
    @DisplayName("POST /api/media - Método não permitido (405 METHOD NOT ALLOWED)")
    void testUploadMedia_MethodNotAllowed_ShouldReturn405() {
        MediaRequest mediaRequest = new MediaRequest(SERVICE_NAME, UPLOADED_BY);
        String mediaRequestJson = serializeToJson(mediaRequest);
        byte[] fileBytes = readFile(TEST_IMAGE_NAME);

        given()
                .contentType("multipart/form-data")
                .header("api-key", API_KEY)
                .multiPart("mediaRequest", "mediaRequest.json", mediaRequestJson.getBytes(), "application/json")
                .multiPart("file", TEST_IMAGE_NAME, fileBytes, "image/jpeg")
                .when()
                .post("/api/media") // Supondo que o verbo POST esteja errado aqui
                .then()
                .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value()) // Verifica se o status é 405
                .body("message", containsString("Method Not Allowed"));
    }

    @Test
    @DisplayName("POST /api/media - Tipo de mídia não aceitável (406 NOT ACCEPTABLE)")
    void testUploadMedia_UnsupportedMediaType_ShouldReturn406() {
        MediaRequest mediaRequest = new MediaRequest(SERVICE_NAME, UPLOADED_BY);
        String mediaRequestJson = serializeToJson(mediaRequest);
        byte[] fileBytes = readFile(TEST_IMAGE_NAME);

        // Simulando uma exceção UnsupportedMediaTypeException
        given()
                .contentType("multipart/form-data")
                .header("api-key", API_KEY)
                .multiPart("mediaRequest", "mediaRequest.json", mediaRequestJson.getBytes(), "application/json")
                .multiPart("file", TEST_IMAGE_NAME, fileBytes, "application/xml") // Envia um tipo de mídia inválido
                .when()
                .post("/api/media")
                .then()
                .statusCode(HttpStatus.NOT_ACCEPTABLE.value()) // Verifica se o status é 406
                .body("message", containsString("Unsupported Media Type"));
    }

    @Test
    @DisplayName("POST /api/media - Arquivo grande demais (413 PAYLOAD TOO LARGE)")
    void testUploadMedia_PayloadTooLarge_ShouldReturn413() {
        MediaRequest mediaRequest = new MediaRequest(SERVICE_NAME, UPLOADED_BY);
        String mediaRequestJson = serializeToJson(mediaRequest);

        // Aqui você pode simular o envio de um arquivo muito grande
        byte[] fileBytes = new byte[10 * 1024 * 1024]; // Um arquivo de 10 MB

        given()
                .contentType("multipart/form-data")
                .header("api-key", API_KEY)
                .multiPart("mediaRequest", "mediaRequest.json", mediaRequestJson.getBytes(), "application/json")
                .multiPart("file", TEST_IMAGE_NAME, fileBytes, "image/jpeg")
                .when()
                .post("/api/media")
                .then()
                .statusCode(HttpStatus.PAYLOAD_TOO_LARGE.value()) // Verifica se o status é 413
                .body("message", containsString("O arquivo enviado excede o limite permitido"));
    }

    @Test
    @DisplayName("POST /api/media - Erro no serviço (503 SERVICE UNAVAILABLE)")
    void testUploadMedia_ServiceUnavailable_ShouldReturn503() {
        MediaRequest mediaRequest = new MediaRequest(SERVICE_NAME, UPLOADED_BY);
        String mediaRequestJson = serializeToJson(mediaRequest);
        byte[] fileBytes = readFile(TEST_IMAGE_NAME);

        // Simulando FileStorageException (serviço temporariamente indisponível)
        given()
                .contentType("multipart/form-data")
                .header("api-key", API_KEY)
                .multiPart("mediaRequest", "mediaRequest.json", mediaRequestJson.getBytes(), "application/json")
                .multiPart("file", TEST_IMAGE_NAME, fileBytes, "image/jpeg")
                .when()
                .post("/api/media")
                .then()
                .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value()) // Verifica se o status é 503
                .body("message", containsString("Erro ao salvar mídia"));
    }


    // Método para serializar o objeto MediaRequest para JSON
    private String serializeToJson(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar objeto para JSON", e);
            throw new RuntimeException("Erro ao serializar objeto", e);
        }
    }

    // Método para ler um arquivo e retornar seus bytes
    private byte[] readFile(String fileName) {
        try {
            return Files.readAllBytes(Paths.get("src/test/resources/" + fileName));
        } catch (IOException e) {
            log.error("Erro ao ler arquivo: " + fileName, e);
            throw new RuntimeException("Erro ao ler arquivo", e);
        }
    }

}
