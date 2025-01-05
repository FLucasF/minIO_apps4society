package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.model.DTO.MediaDTO;

import com.apps4society.MinIO_API.model.enums.EntityType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaService {
    MediaDTO saveMedia(MultipartFile file, Long entityId, EntityType entityType, Long uploadedBy);
    List<MediaDTO> getMediaByUploader(Long uploadedBy);
    List<String> getURLsOfThemeImages(Long themeId);
    List<String> getURLsOfChallengeImages(Long challengeId);
}
