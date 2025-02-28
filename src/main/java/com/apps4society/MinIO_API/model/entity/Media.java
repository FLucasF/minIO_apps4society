package com.apps4society.MinIO_API.model.entity;

import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "media", uniqueConstraints = {
        @UniqueConstraint(name = "unique_active_file", columnNames = {"file_name", "active"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull(message = "O nome do serviço é obrigatório.")
    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @NotNull(message = "O tipo da entidade é obrigatório.")
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    @NotNull(message = "O tipo de mídia é obrigatório.")
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @NotNull(message = "O ID do usuário que enviou é obrigatório.")
    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @NotNull(message = "O nome do arquivo é obrigatório.")
    @Size(max = 255, message = "O nome do arquivo não pode exceder 255 caracteres.")
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @NotNull(message = "A tag é obrigatória.")
    @Size(max = 255, message = "O nome do arquivo não pode exceder 255 caracteres.")
    @Column(name = "tag", nullable = false)
    private String tag;

    @Column(name = "active", nullable = false)
    private boolean active = true;

}