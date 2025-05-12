package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Media API", description = "Endpoints para gerenciar arquivos de mídia no MinIO.")
@RestController
@RequestMapping("/api/media")
@SecurityRequirement(name = "API Key")
@Validated
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @Operation(summary = "Upload de um arquivo", description = "Faz o upload de um arquivo para o MinIO e salva as informações no banco de dados.",
            responses = {@ApiResponse(responseCode = "201", description = "Arquivo enviado com sucesso.")})
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> uploadFile(
            @Valid @RequestPart("mediaRequest") MediaRequest mediaRequest,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaService.uploadMedia(mediaRequest, file));
    }

    @GetMapping("{serviceName}/{mediaId}")
    public ResponseEntity<String> getMedia(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("mediaId") Long mediaId) {
        return ResponseEntity.ok(mediaService.getMediaUrl(serviceName, mediaId));
    }

    @Operation(summary = "Listar mídias por entidade e usuário",
            description = "Lista todas as mídias associadas a uma entidade específica e que foram feitas por um usuário específico.")
    @GetMapping("/lists/{serviceName}/{uploadedBy}")
    public ResponseEntity<List<MediaResponse>> listMediaByEntity(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("uploadedBy") Long uploadedBy) {
        return ResponseEntity.ok(mediaService.listMediaByUploadedBy(serviceName, uploadedBy));
    }

    @Operation(summary = "Atualizar mídia", description = "Atualiza um arquivo no MinIO e suas informações no banco de dados.",
            responses = {@ApiResponse(responseCode = "200", description = "Mídia atualizada com sucesso."),
                    @ApiResponse(responseCode = "404", description = "Mídia não encontrada.")})
    @PutMapping("/{entityId}")
    public ResponseEntity<MediaResponse> updateMedia(
            @PathVariable("entityId") Long entityId,
            @Valid @RequestPart("mediaRequest") MediaRequest mediaRequest,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(mediaService.updateMedia(entityId, mediaRequest, file));
    }


    @Operation(summary = "Desativar mídia", description = "Marca a mídia como inativa no banco de dados.",
            responses = {@ApiResponse(responseCode = "204", description = "Mídia desativada com sucesso."),
                    @ApiResponse(responseCode = "404", description = "Mídia não encontrada.")})
    @DeleteMapping("{serviceName}/{mediaId}")
    public ResponseEntity<Void> disableMedia(@PathVariable String serviceName, @PathVariable Long mediaId) {
        mediaService.disableMedia(serviceName, mediaId);
        return ResponseEntity.noContent().build();
    }

}