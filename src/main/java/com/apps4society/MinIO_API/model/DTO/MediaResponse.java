package com.apps4society.MinIO_API.model.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Representação de uma mídia armazenada no MinIO.")
public record MediaResponse(

        @Schema(description = "Identificador único da mídia.", example = "1")
        Long entityId,

        @Schema(description = "Nome do serviço ao qual a mídia pertence.", example = "educAPI")
        String serviceName,

        @Schema(description = "Nome do arquivo armazenado.", example = "foto_perfil.png")
        String fileName,

        @Schema(description = "URL gerada pelo MinIO para acesso ao arquivo.", example = "https://minio.example.com/educAPI/foto_perfil.png")
        String url
) {}
