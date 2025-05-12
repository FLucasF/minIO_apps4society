package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = false)
public abstract class BaseMediaControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    protected MediaService mediaService;

    protected final String serviceName = "educAPI";
    protected final Long uploadedBy = 1L;
    protected final Long entityId = 42L;
    protected final Long mediaId = 100L;
    protected final String fileName = "test.png";
    protected final String fileType = "image/png";
    protected final String mediaUrl = "http://localhost:9000/educAPI/" + fileName;

    protected MockMultipartFile validFile;
    protected MockMultipartFile emptyFile;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validFile = new MockMultipartFile("file", fileName, fileType, "dummyContent".getBytes());
        emptyFile = new MockMultipartFile("file", "empty.png", fileType, new byte[0]);
    }

    protected MediaRequest createMediaRequest() {
        return new MediaRequest(serviceName, uploadedBy);
    }

    protected MediaResponse createMediaResponse() {
        return new MediaResponse(mediaId, serviceName, fileName, mediaUrl);
    }
}
