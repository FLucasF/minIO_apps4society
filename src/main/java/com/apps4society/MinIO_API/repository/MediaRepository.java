package com.apps4society.MinIO_API.repository;

import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByUploadedBy(Long uploadedBy);
    List<Media> findAllByEntityIdAndEntityTypeAndMediaType(Long entityId, EntityType entityType, MediaType mediaType);
}
