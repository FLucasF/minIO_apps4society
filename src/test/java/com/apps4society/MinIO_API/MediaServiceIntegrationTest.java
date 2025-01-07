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

}