package com.apps4society.MinIO_API.integration;

import com.apps4society.MinIO_API.repository.MediaRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.web.reactive.function.BodyInserters.fromMultipartData;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class MediaIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private MediaRepository mediaRepository;

    @BeforeEach
    public void setUp() {
        // Limpa a base antes de cada teste
        mediaRepository.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        // Limpa a base após cada teste
        mediaRepository.deleteAll();
    }

    @Test
    @DisplayName("Teste de integração - Upload de mídia")
    public void testUploadMediaIntegration() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("serviceName", "educaAPI");
        builder.part("tag", "teste");
        builder.part("entityType", "THEME");
        builder.part("uploadedBy", "1");
        builder.part("file", new ByteArrayResource("conteudoDaImagem".getBytes()) {
            @Override
            public String getFilename() {
                return "screenshot.png";
            }
        }).header("Content-Type", "image/png");

        // Executa a requisição POST para o endpoint de upload
        webTestClient.post()
                .uri("/api/media/post")
                .header("api-key", "123")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.serviceName").isEqualTo("educaAPI")
                .jsonPath("$.tag").isEqualTo("teste")
                .jsonPath("$.entityType").isEqualTo("THEME")
                .jsonPath("$.uploadedBy").isEqualTo(1)
                .jsonPath("$.fileName").value(fileName ->
                        Assertions.assertThat(fileName.toString()).startsWith("educaAPI/")
                );

        // Verifica se o registro foi realmente persistido no banco H2
        Assertions.assertThat(mediaRepository.findAll()).hasSize(1);
    }
}
