package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.config.MinioConfig;
import com.apps4society.MinIO_API.mapper.MediaMapper;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.MediaType;
import com.apps4society.MinIO_API.repository.MediaRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.when;

public abstract class BaseMediaServiceImplTest {

    @Mock
    protected MediaRepository mediaRepository;

    @Mock
    protected MediaMapper mediaMapper;

    @Mock
    protected MinioClient minioClient;

    @Mock
    protected MinioConfig minioConfig;

    protected MediaServiceImpl mediaService;

    // 🔹 Arquivos de teste reutilizáveis
    protected MockMultipartFile validFile;
    protected MockMultipartFile emptyFile;
    protected MockMultipartFile fileWithoutName;

    // 🔹 Mídia simulada reutilizável
    protected final String serviceName = "educAPI";
    protected final Long mediaId = 1L;
    protected Media existingMedia;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(minioConfig.getBucketName()).thenReturn("test-bucket");

        mediaService = new MediaServiceImpl(mediaRepository, mediaMapper, minioClient, minioConfig);

        validFile = new MockMultipartFile("file", "updated-image.png", "image/png", "dummy".getBytes());
        emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        fileWithoutName = new MockMultipartFile("file", "", "image/png", "dummy".getBytes());

        existingMedia = Media.builder()
                .id(mediaId)
                .serviceName(serviceName)
                .mediaType(MediaType.IMAGE)
                .fileName("old-image.png")
                .entityId(1001L)
                .active(true)
                .build();
    }
}
