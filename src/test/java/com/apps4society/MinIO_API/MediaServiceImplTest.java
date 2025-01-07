package com.apps4society.MinIO_API;

import com.apps4society.MinIO_API.exceptions.InvalidInputException;
import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import com.apps4society.MinIO_API.repository.MediaRepository;
import com.apps4society.MinIO_API.service.MediaServiceImpl;
import com.apps4society.MinIO_API.Mapper.MediaMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


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
