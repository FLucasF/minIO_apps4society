package com.apps4society.MinIO_API.model.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "Dados para o upload de um arquivo de mídia no MinIO.")
public record MediaRequest(
        @Schema(description = "Nome do serviço ao qual a mídia pertence.", example = "educAPI")
        @NotBlank(message = "O nome do serviço não pode ser vazio")
        String serviceName,

        @Schema(description = "ID do usuário que está realizando o upload.", example = "42")
        @Min(value = 1, message = "O campo uploadedBy deve ser maior que 0")
        @NotNull(message = "O id do professor é obrigatório")
        Long uploadedBy

) {}
