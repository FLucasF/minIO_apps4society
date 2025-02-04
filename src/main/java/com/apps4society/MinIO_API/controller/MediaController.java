package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import com.apps4society.MinIO_API.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "Media API", description = "Endpoints para gerenciar arquivos de mídia no MinIO.")
@RestController
@RequestMapping("/api/media")
@SecurityRequirement(name = "API Key")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @Operation(
            summary = "Upload de um arquivo",
            description = "Faz o upload de um arquivo para o MinIO e salva as informações no banco de dados.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Arquivo enviado com sucesso.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = MediaDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos."),
                    @ApiResponse(responseCode = "401", description = "API Key inválida ou ausente."),
                    @ApiResponse(responseCode = "500", description = "Erro interno ao processar o arquivo.")
            }
    )
    @PostMapping("post/{serviceName}")
    public ResponseEntity<MediaDTO> uploadFile(
            @PathVariable("serviceName")
            @Parameter(description = "Nome do serviço ao qual a mídia pertence.", required = true, example = "educAPI")
            String serviceName,

            @RequestPart("file")
            @Parameter(description = "Arquivo a ser enviado.", required = true, content = @Content(mediaType = "multipart/form-data"))
            MultipartFile file,

            @RequestParam("tag")
            @Parameter(description = "Tag de categorização do arquivo.", required = true)
            String tag,

            @RequestParam("entityType")
            @Parameter(description = "Tipo da entidade associada ao arquivo.", required = true)
            EntityType entityType,

            @RequestParam("uploadedBy")
            @Parameter(description = "ID do usuário que fez o upload.", required = true)
            Long uploadedBy) {

        MediaDTO mediaDTO = mediaService.saveMedia(serviceName, file, tag, entityType, uploadedBy);
        return ResponseEntity.ok(mediaDTO);
    }

    @Operation(
            summary = "Buscar URL assinada de uma mídia",
            description = "Gera e retorna uma URL assinada para acessar a mídia armazenada no MinIO.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "URL assinada gerada com sucesso.",
                            content = @Content(mediaType = "application/json",
                                    examples = @ExampleObject(value = """
                                        {
                                            "url": "http://localhost:9000/test/EAD%20prova%201.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20250130%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20250130T155100Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=ba7a50c6ec4895f1a24b9d6603487616ddfd150c8decd4e89b711d139be217a0"
                                        }
                                        """))),
                    @ApiResponse(responseCode = "404", description = "Mídia não encontrada."),
                    @ApiResponse(responseCode = "401", description = "API Key inválida ou ausente."),
                    @ApiResponse(responseCode = "500", description = "Erro interno ao buscar a URL da mídia.")
            }
    )
    @GetMapping("get/{serviceName}/{mediaId}")
    public ResponseEntity<Map<String, String>> getMediaUrl(
            @PathVariable("serviceName")
            @Parameter(description = "Nome do serviço ao qual a mídia pertence.", required = true, example = "educAPI")
            String serviceName,

            @PathVariable("mediaId")
            @Parameter(description = "ID único da mídia.", required = true, example = "1")
            Long mediaId) {

        Map<String, String> response = mediaService.getMediaUrl(serviceName, mediaId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Atualizar mídia",
            description = "Atualiza um arquivo no MinIO e suas informações no banco de dados.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Mídia atualizada com sucesso.",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MediaDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Mídia não encontrada."),
                    @ApiResponse(responseCode = "401", description = "API Key inválida ou ausente."),
                    @ApiResponse(responseCode = "500", description = "Erro interno ao atualizar a mídia.")
            }
    )
    @PutMapping("update/{serviceName}/{mediaId}")
    public ResponseEntity<MediaDTO> updateMedia(
            @PathVariable("serviceName")
            @Parameter(description = "Nome do serviço ao qual a mídia pertence.", required = true, example = "educAPI")
            String serviceName,

            @PathVariable("mediaId")
            @Parameter(description = "ID da mídia.", required = true, example = "1")
            Long mediaId,

            @RequestParam("entityType")
            @Parameter(description = "Tipo da entidade associada ao arquivo.", required = true)
            EntityType entityType,

            @RequestParam("tag")
            @Parameter(description = "Tag de categorização do arquivo.", required = true)
            String tag,

            @RequestParam("mediaType")
            @Parameter(description = "Tipo de mídia (IMAGE, VIDEO, AUDIO).", required = true)
            MediaType mediaType,

            @RequestPart("file")
            @Parameter(description = "Novo arquivo a ser enviado.", required = true, content = @Content(mediaType = "multipart/form-data"))
            MultipartFile file) {

        MediaDTO updatedMedia = mediaService.updateMedia(serviceName, mediaId, entityType, tag, mediaType, file);
        return ResponseEntity.ok(updatedMedia);
    }
}