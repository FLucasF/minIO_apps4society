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
        @NotBlank(message = "O ID do usuario que subiu o arquivo não pode ser vazio")
        @Min(value = 1, message = "O campo uploadedBy deve ser maior que 0")
        Long uploadedBy,

        @Schema(description = "ID da entidade relacionada à mídia.", example = "1001")
        @NotBlank(message = "O id da entity não pode ser vazio")
        @Min(value = 1, message = "O campo entityId deve ser maior que 0")
        Long entityId,

        @Schema(description = "Arquivo a ser enviado.")
        @NotBlank(message = "A file não pode ser vazia")
        @NotNull(message = "O id do professor é obrigatório")
        MultipartFile file
) {}
