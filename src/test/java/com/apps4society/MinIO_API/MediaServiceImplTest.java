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

    @Test
    void testSaveMedia_Success() throws Exception {
        // Mock do MultipartFile
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("image.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(InputStream.nullInputStream());
        when(file.getSize()).thenReturn(1024L);

        // Mock da entidade Media
        Media media = Media.builder()
                .midiaId(1L)
                .url("http://localhost:9000/test/image.jpg")
                .mediaType(MediaType.IMAGE)
                .uploadDate(LocalDateTime.now())
                .entityId(1L)
                .entityType(EntityType.THEME)
                .uploadedBy(1L)
                .build();

        MediaDTO mediaDTO = MediaDTO.builder()
                .id(1L)
                .url("http://localhost:9000/test/image.jpg")
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(media);
        when(mediaMapper.entityToDto(any(Media.class))).thenReturn(mediaDTO);

        // Executando o método a ser testado
        MediaDTO result = mediaService.saveMedia(file, 1L, EntityType.THEME, 1L);

        // Verificações
        verify(mediaRepository).save(any(Media.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
        verify(mediaMapper).entityToDto(any(Media.class));

        // Asserções
        assert result != null;
        assert result.getId() == 1L;
        assert result.getUrl().equals("http://localhost:9000/test/image.jpg");
    }

    @Test
    void testSaveMedia_InvalidFile() {
        // Mock do MultipartFile vazio
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        // Asserção para verificar a exceção
        assertThrows(InvalidInputException.class, () -> {
            mediaService.saveMedia(file, 1L, EntityType.THEME, 1L);
        });
    }
}
