package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface MediaService {
    MediaDTO saveMedia(String serviceName, MultipartFile file, String tag, EntityType entityType, Long uploadedBy);
    Map<String, String> getMediaUrl(String serviceName, Long mediaId);
    MediaDTO updateMedia(String serviceName, Long mediaId, EntityType entityType, String tag, MediaType mediaType, MultipartFile file);
    MediaDTO disableMedia(String serviceName, Long mediaId);
    List<Map<String, String>> listarMidiasComUrl(String serviceName, EntityType entityType, Long entityId);
}
