package com.apps4society.MinIO_API.model.DTO;

import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representação de um arquivo de mídia no MinIO.")
public class MediaDTO {

    @Schema(description = "Identificador único da mídia.", example = "1")
    private Long id;

    @Schema(description = "Nome do serviço ao qual a mídia pertence.", example = "educAPI")
    private String serviceName;

    @Schema(description = "Tipo de mídia (ex: IMAGE, VIDEO, AUDIO).", example = "IMAGE")
    private MediaType mediaType;

    @Schema(description = "Tipo da entidade associada à mídia.", example = "THEME")
    private EntityType entityType;

    @Schema(description = "ID do usuário que fez o upload.", example = "42")
    private Long uploadedBy;

    @Schema(description = "Nome do arquivo original.", example = "foto_perfil.png")
    private String fileName;

    @Schema(description = "Tag opcional para classificar a mídia.", example = "perfil")
    private String tag;
}
