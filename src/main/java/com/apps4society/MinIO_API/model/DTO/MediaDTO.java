package com.apps4society.MinIO_API.model.DTO;

import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaDTO {
    private Long id;
    private String url;
    private MediaType mediaType;
    private Long entityId;
    private EntityType entityType;
    private Long uploadedBy;
    private LocalDateTime uploadDate;
}
