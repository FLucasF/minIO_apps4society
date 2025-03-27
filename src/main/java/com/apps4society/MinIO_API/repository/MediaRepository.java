package com.apps4society.MinIO_API.repository;

import com.apps4society.MinIO_API.model.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    Optional<Media> findByIdAndServiceNameAndActiveTrue(Long id, String serviceName);

    List<Media> findByServiceNameAndEntityIdAndActiveTrue(String serviceName, Long entityId);

    boolean existsByFileNameAndServiceName(String fileName, String serviceName);

}
