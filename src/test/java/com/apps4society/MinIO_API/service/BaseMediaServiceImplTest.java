package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.config.MinioConfig;
import com.apps4society.MinIO_API.mapper.MediaMapper;
import com.apps4society.MinIO_API.repository.MediaRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(minioConfig.getBucketName()).thenReturn("test-bucket");
        mediaService = new MediaServiceImpl(mediaRepository, mediaMapper, minioClient, minioConfig);
    }
}
