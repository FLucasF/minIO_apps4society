package com.apps4society.MinIO_API.repository;

import com.apps4society.MinIO_API.model.entity.Media;
import com.apps4society.MinIO_API.model.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    Optional<Media> findByIdAndServiceNameAndActiveTrue(Long id, String serviceName);
    boolean existsByFileNameAndActive(String fileName, boolean active);

}


