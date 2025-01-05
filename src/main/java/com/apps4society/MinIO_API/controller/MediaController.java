package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/upload")
    public ResponseEntity<MediaDTO> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("entityId") Long entityId,
            @RequestParam("entityType") EntityType entityType,
            @RequestParam("uploadedBy") Long uploadedBy) {
        log.info("Iniciando upload de arquivo para entidade {} com ID {}", entityType, entityId);
        MediaDTO mediaDTO = mediaService.saveMedia(file, entityId, entityType, uploadedBy);
        return ResponseEntity.ok(mediaDTO);
    }

    @GetMapping("/uploader/{uploadedBy}")
    public ResponseEntity<List<MediaDTO>> getMediaByUploader(@PathVariable Long uploadedBy) {
        List<MediaDTO> mediaList = mediaService.getMediaByUploader(uploadedBy);
        return ResponseEntity.ok(mediaList);
    }

    @GetMapping("/theme/{themeId}/images")
    public ResponseEntity<List<String>> getURLsOfThemeImages(@PathVariable Long themeId) {
        List<String> imageUrls = mediaService.getURLsOfThemeImages(themeId);
        return ResponseEntity.ok(imageUrls);
    }

    @GetMapping("/challenge/{challengeId}/images")
    public ResponseEntity<List<String>> getURLsOfChallengeImages(@PathVariable Long challengeId) {
        List<String> imageUrls = mediaService.getURLsOfChallengeImages(challengeId);
        return ResponseEntity.ok(imageUrls);
    }

}
