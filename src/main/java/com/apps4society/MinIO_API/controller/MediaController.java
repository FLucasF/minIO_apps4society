package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.enums.EntityType;
import com.apps4society.MinIO_API.model.enums.MediaType;
import com.apps4society.MinIO_API.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

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
                    @ApiResponse(responseCode = "201", description = "Arquivo enviado com sucesso."),
                    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos."),
                    @ApiResponse(responseCode = "401", description = "API Key inválida ou ausente."),
                    @ApiResponse(responseCode = "500", description = "Erro interno ao processar o arquivo.")
            }
    )
    @PostMapping("post")
    public ResponseEntity<MediaDTO> uploadFile(
            @RequestParam("serviceName") String serviceName,
            @RequestPart("file") MultipartFile file,
            @RequestParam("tag") String tag,
            @RequestParam("entityType") EntityType entityType,
            @RequestParam("uploadedBy") Long uploadedBy) {

        MediaDTO mediaDTO = mediaService.saveMedia(serviceName, file, tag, entityType, uploadedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaDTO);
    }

    @Operation(
            summary = "Buscar URL assinada de uma mídia",
            description = "Gera e retorna uma URL assinada para acessar a mídia armazenada no MinIO.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "URL assinada gerada com sucesso."),
                    @ApiResponse(responseCode = "404", description = "Mídia não encontrada."),
                    @ApiResponse(responseCode = "401", description = "API Key inválida ou ausente."),
                    @ApiResponse(responseCode = "500", description = "Erro interno ao buscar a URL da mídia.")
            }
    )
    @GetMapping("get/{serviceName}/{mediaId}")
    public ResponseEntity<Map<String, String>> getMediaUrl(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("mediaId") Long mediaId) {

        return Optional.ofNullable(mediaService.getMediaUrl(serviceName, mediaId))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(
            summary = "Atualizar mídia",
            description = "Atualiza um arquivo no MinIO e suas informações no banco de dados.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Mídia atualizada com sucesso."),
                    @ApiResponse(responseCode = "404", description = "Mídia não encontrada."),
                    @ApiResponse(responseCode = "401", description = "API Key inválida ou ausente."),
                    @ApiResponse(responseCode = "500", description = "Erro interno ao atualizar a mídia.")
            }
    )
    @PutMapping("update/{serviceName}/{mediaId}")
    public ResponseEntity<MediaDTO> updateMedia(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("mediaId") Long mediaId,
            @RequestParam("entityType") EntityType entityType,
            @RequestParam("tag") String tag,
            @RequestParam("mediaType") MediaType mediaType,
            @RequestPart("file") MultipartFile file) {

        return Optional.ofNullable(mediaService.updateMedia(serviceName, mediaId, entityType, tag, mediaType, file))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(
            summary = "Desativar mídia",
            description = "Marca a mídia como inativa no banco de dados.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Mídia desativada com sucesso."),
                    @ApiResponse(responseCode = "404", description = "Mídia não encontrada."),
                    @ApiResponse(responseCode = "401", description = "API Key inválida ou ausente."),
                    @ApiResponse(responseCode = "500", description = "Erro interno ao desativar a mídia.")
            }
    )
    @DeleteMapping("delete/{serviceName}/{mediaId}")
    public ResponseEntity<Void> disableMedia(@PathVariable String serviceName, @PathVariable Long mediaId) {
        boolean deleted = mediaService.disableMedia(serviceName, mediaId) != null;

        if (deleted) {
            return ResponseEntity.noContent().build(); // ✅ 204 No Content para sucesso
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // ✅ 404 se não encontrar
        }
    }
}
