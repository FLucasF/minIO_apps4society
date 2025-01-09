package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.model.DTO.MediaDTO;

import com.apps4society.MinIO_API.model.enums.EntityType;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {
    MediaDTO saveMedia(MultipartFile file, Long entityId, EntityType entityType, Long uploadedBy);
    MediaDTO getMediaById(Long mediaId, EntityType entityType);
    MediaDTO updateMedia(Long id, MultipartFile file, Long uploadedBy);
}
