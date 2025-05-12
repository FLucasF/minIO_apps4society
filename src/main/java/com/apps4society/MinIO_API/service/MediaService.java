package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.model.entity.Media;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaService {

    /**
     * Faz o upload de uma nova mídia para o MinIO e a salva no banco de dados.
     * @param request Objeto contendo os dados do upload.
     * @return MediaResponse com os detalhes da mídia salva.
     */
//    MediaResponse uploadMedia(String serviceName, Long uploadedBy, Long entityId, MultipartFile file);
    MediaResponse uploadMedia(MediaRequest mediaRequest, MultipartFile file);

    String getMediaUrl(String serviceName, Long mediaId);    /**
//     * Lista todas as mídias ativas associadas a uma entidade específica.
//     * @param serviceName Nome do serviço ao qual as mídias pertencem.
//     * @param entityId ID da entidade associada às mídias.
//     * @return Lista de MediaResponse contendo URLs assinadas e informações das mídias.
//     */
    List<MediaResponse> listMediaByUploadedBy(String serviceName, Long uploadedBy);
//
//    /**
//     * Atualiza um arquivo de mídia existente no MinIO e suas informações no banco de dados.
//     * @param mediaId ID da mídia a ser atualizada.
//     * @param request Objeto contendo os novos dados do upload.
//     * @return MediaResponse com os detalhes da mídia atualizada.
//     */
    MediaResponse updateMedia(Long entityId, MediaRequest mediaRequest, MultipartFile file);
    /**
     * Desativa uma mídia, removendo seu acesso ativo e movendo o arquivo para a pasta de desativados no MinIO.
     * @param mediaId ID da mídia a ser desativada.
     */
    void disableMedia(String serviceName, Long mediaId);
}
