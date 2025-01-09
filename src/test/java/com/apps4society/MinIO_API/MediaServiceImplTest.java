package com.apps4society.MinIO_API;

import com.apps4society.MinIO_API.repository.MediaRepository;
import com.apps4society.MinIO_API.service.MediaServiceImpl;
import com.apps4society.MinIO_API.mapper.MediaMapper;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;


//ESTUDANDO SOBRE, NÃO LEVAR A SERIO, PODE ESTAR TOTALMENTE ERRADO
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class MediaServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MinioClient minioClient;

    @Mock
    private MediaMapper mediaMapper;

    @InjectMocks
    private MediaServiceImpl mediaService;

    @BeforeEach
    void setUp() {
        // Injetar o nome do bucket manualmente
        ReflectionTestUtils.setField(mediaService, "bucketName", "test-bucket");
    }

}
