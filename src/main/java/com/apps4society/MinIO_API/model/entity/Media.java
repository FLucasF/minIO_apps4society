package com.apps4society.MinIO_API.model.entity;

import com.apps4society.MinIO_API.model.enums.MediaType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "media")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Size(max = 255, message = "O nome do arquivo não pode exceder 255 caracteres.")
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "entity_id", nullable = false)
    private Long entityId; // Identifica a entidade à qual a mídia está associada

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public void disable() {
        this.active = false;
    }
}
