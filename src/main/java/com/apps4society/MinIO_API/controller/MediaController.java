package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import com.apps4society.MinIO_API.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    //http://localhost:8080/api/media/upload
    @PostMapping("/upload")
    public ResponseEntity<MediaDTO> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("mediaIdentifier") Long mediaIdentifier,
            @RequestParam("entityType") EntityType entityType,
            @RequestParam("uploadedBy") Long uploadedBy) {
        log.info("Iniciando upload de arquivo para entidade {} com ID {}", entityType, mediaIdentifier);
        MediaDTO mediaDTO = mediaService.saveMedia(file, mediaIdentifier, entityType, uploadedBy);
        return ResponseEntity.ok(mediaDTO);
    }

    //http://localhost:8080/api/media/VIDEO/1?entityType=THEME
    @GetMapping("/{mediaType}/{id}")
    public ResponseEntity<Map<String, String>> getMediaById(
            @PathVariable MediaType mediaType,
            @PathVariable Long id,
            @RequestParam EntityType entityType) {
        String url = mediaService.getMediaById(id, entityType).getUrl();
        return ResponseEntity.ok(Map.of("url", url));
    }

    //https://localhost:8080/api/media/{id}/update?uploadedBy={uploadedBy}
    @PutMapping("/{id}/update")
    public ResponseEntity<MediaDTO> updateMedia(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam("uploadedBy") Long uploadedBy) {
        log.info("Atualizando a mídia com ID {}", id);
        MediaDTO updatedMedia = mediaService.updateMedia(id, file, uploadedBy);
        return ResponseEntity.ok(updatedMedia);
    }
}

