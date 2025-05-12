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

    // Mocked dependencies
    @Mock
    protected MediaRepository mediaRepository;

    @Mock
    protected MediaMapper mediaMapper;

    @Mock
    protected MinioClient minioClient;

    @Mock
    protected MinioConfig minioConfig;

    // Service under test
    protected MediaServiceImpl mediaService;

    // Test file samples
    protected MockMultipartFile validFile;
    protected MockMultipartFile emptyFile;
    protected MockMultipartFile fileWithoutName;

    // Reusable Media entity
    protected final String serviceName = "educAPI";
    protected final Long mediaId = 1L;
    protected Media existingMedia;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Mock the MinioConfig bucket name
        when(minioConfig.getBucketName()).thenReturn("test-bucket");

        // Instantiate the service under test
        mediaService = new MediaServiceImpl(mediaRepository, mediaMapper, minioClient, minioConfig);

        // Initialize test files
        validFile = new MockMultipartFile("file", "updated-image.png", "image/png", "dummy".getBytes());
        emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        fileWithoutName = new MockMultipartFile("file", "", "image/png", "dummy".getBytes());

        existingMedia = new Media(42L, "old-image.png", "educAPI", MediaType.IMAGE);

    }
}