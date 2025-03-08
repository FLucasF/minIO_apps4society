package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.exceptions.InvalidFileException;
import com.apps4society.MinIO_API.exceptions.UnsupportedMediaTypeException;
import com.apps4society.MinIO_API.model.enums.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MediaServiceImplDetermineTypeTest extends BaseMediaServiceImplTest{

    @Test
    void testDetermineMediaType_validExtensions() {
        assertEquals(MediaType.IMAGE, ReflectionTestUtils.invokeMethod(mediaService, "determineMediaType", "test.jpg"));
        assertEquals(MediaType.IMAGE, ReflectionTestUtils.invokeMethod(mediaService, "determineMediaType", "test.jpeg"));
        assertEquals(MediaType.IMAGE, ReflectionTestUtils.invokeMethod(mediaService, "determineMediaType", "test.png"));
        assertEquals(MediaType.VIDEO, ReflectionTestUtils.invokeMethod(mediaService, "determineMediaType", "test.mp4"));
        assertEquals(MediaType.AUDIO, ReflectionTestUtils.invokeMethod(mediaService, "determineMediaType", "test.mp3"));
    }

    @Test
    void testDetermineMediaType_invalidExtension_throwsUnsupportedMediaTypeException() {
        assertThrows(UnsupportedMediaTypeException.class, () ->
                ReflectionTestUtils.invokeMethod(mediaService, "determineMediaType", "test.xyz"));
    }

    @Test
    void testDetermineMediaType_uppercaseExtensions() {
        assertEquals(MediaType.IMAGE, ReflectionTestUtils.invokeMethod(mediaService, "determineMediaType", "test.JPG"));
        assertEquals(MediaType.VIDEO, ReflectionTestUtils.invokeMethod(mediaService, "determineMediaType", "test.MP4"));
    }

    @Test
    void testDetermineMediaType_noExtension_throwsInvalidFileException() {
        assertThrows(UnsupportedMediaTypeException.class, () ->
                ReflectionTestUtils.invokeMethod(mediaService, "determineMediaType", "testfile"));
    }


}
