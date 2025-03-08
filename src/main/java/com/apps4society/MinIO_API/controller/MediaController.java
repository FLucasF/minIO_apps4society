package com.apps4society.MinIO_API.controller;

import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Media API", description = "Endpoints para gerenciar arquivos de mídia no MinIO.")
@RestController
@RequestMapping("/api/media")
@SecurityRequirement(name = "API Key")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @Operation(summary = "Upload de um arquivo", description = "Faz o upload de um arquivo para o MinIO e salva as informações no banco de dados.",
            responses = {@ApiResponse(responseCode = "201", description = "Arquivo enviado com sucesso.")})
    @PostMapping()
    public ResponseEntity<MediaResponse> uploadFile(@RequestParam("serviceName") String serviceName,
                                                    @RequestParam("uploadedBy") Long uploadedBy,
                                                    @RequestParam("entityId") Long entityId,
                                                    @RequestPart("file") MultipartFile file) {
        MediaRequest request = new MediaRequest(serviceName, uploadedBy, entityId, file);
        MediaResponse response = mediaService.uploadMedia(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("{serviceName}/{mediaId}")
    public ResponseEntity<String> getMedia(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("mediaId") Long mediaId) {

        String mediaUrl = mediaService.getMediaUrl(serviceName, mediaId);
        return ResponseEntity.ok(mediaUrl);
    }


    @Operation(summary = "Listar mídias por entidade",
            description = "Lista todas as mídias associadas a uma entidade específica.")
    @GetMapping("/lists/{serviceName}/{entityId}")  // Verifique se a URL está correta
    public ResponseEntity<List<MediaResponse>> listMediaByEntity(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("entityId") Long entityId) {
        return ResponseEntity.ok(mediaService.listMediaByEntity(serviceName, entityId));
    }

    @Operation(summary = "Atualizar mídia", description = "Atualiza um arquivo no MinIO e suas informações no banco de dados.",
            responses = {@ApiResponse(responseCode = "200", description = "Mídia atualizada com sucesso."),
                    @ApiResponse(responseCode = "404", description = "Mídia não encontrada.")})
    @PutMapping("{serviceName}/{mediaId}")
    public ResponseEntity<MediaResponse> updateMedia(@PathVariable("serviceName") String serviceName,
                                                     @PathVariable("mediaId") Long mediaId,
                                                     @RequestParam("uploadedBy") Long uploadedBy,
                                                     @RequestParam("entityId") Long entityId,
                                                     @RequestPart("file") MultipartFile file) {
        MediaRequest request = new MediaRequest(serviceName, uploadedBy, entityId, file);
        return ResponseEntity.ok(mediaService.updateMedia(serviceName, mediaId, request));
    }


    @Operation(summary = "Desativar mídia", description = "Marca a mídia como inativa no banco de dados.",
            responses = {@ApiResponse(responseCode = "204", description = "Mídia desativada com sucesso."),
                    @ApiResponse(responseCode = "404", description = "Mídia não encontrada.")})
    @DeleteMapping("{serviceName}/{mediaId}")
    public ResponseEntity<Void> disableMedia(@PathVariable String serviceName, @PathVariable Long mediaId) {
        mediaService.disableMedia(serviceName, mediaId);
        return ResponseEntity.noContent().build(); // ✅ 204 No Content para sucesso
    }

}