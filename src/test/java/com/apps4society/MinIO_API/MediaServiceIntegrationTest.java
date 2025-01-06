package com.apps4society.MinIO_API;

import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import com.apps4society.MinIO_API.repository.MediaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class MediaServiceIntegrationTest {

    @Autowired
    private MediaRepository mediaRepository;

    @Test
    void testDatabaseInteraction() {
        Media media = Media.builder()
                .url("http://localhost:9000/test/image.jpg")
                .mediaType(MediaType.IMAGE)
                .uploadDate(LocalDateTime.now())
                .entityId(1L)
                .entityType(EntityType.THEME)
                .uploadedBy(1L)
                .build();
        Media savedMedia = mediaRepository.save(media);
        assertThat(savedMedia).isNotNull();
        assertThat(savedMedia.getMidiaId()).isGreaterThan(0);
    }
}
