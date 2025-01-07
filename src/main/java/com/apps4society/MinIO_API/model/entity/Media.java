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

import java.time.LocalDateTime;

@Entity
@Table(name = "media")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull(message = "O ID do arquivo é obrigatório.")
    @Column(name = "mediaIdentifier", nullable = false, unique = true)
    private Long mediaIdentifier;

    @NotNull(message = "O tipo da entidade é obrigatório.")
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    @NotNull(message = "O ID do usuário que enviou é obrigatório.")
    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @NotNull(message = "A URL da mídia é obrigatória.")
    @Size(max = 2083, message = "A URL não pode exceder 2083 caracteres.")
    @Column(name = "url", nullable = false, length = 2083)
    private String url;

    @NotNull(message = "A data de upload é obrigatória.")
    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    @NotNull(message = "O tipo de mídia é obrigatório.")
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;
}
