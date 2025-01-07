package com.apps4society.MinIO_API;

import com.apps4society.MinIO_API.controller.MediaController;
import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(MediaController.class)
public class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        Mockito.reset(mediaService);
    }

}
