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
    private Long mediaIdentifier;
    private MediaType mediaType;
    private EntityType entityType;
    private Long uploadedBy;
    private LocalDateTime uploadDate;
    private String url;
}
