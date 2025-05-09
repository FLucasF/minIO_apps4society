package com.apps4society.MinIO_API.service;

import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.model.DTO.MediaResponse;

import java.util.List;

public interface MediaService {

    /**
     * Faz o upload de uma nova mídia para o MinIO e a salva no banco de dados.
     * @param request Objeto contendo os dados do upload.
     * @return MediaResponse com os detalhes da mídia salva.
     */
    MediaResponse uploadMedia(MediaRequest request);

    /**
     * Retorna a URL de acesso a uma mídia específica.
     * @param serviceName Nome do serviço ao qual a mídia pertence.
     * @param mediaId ID da mídia a ser acessada.
     * @return URL de acesso à mídia.
     */
    String getMediaUrl(String serviceName, Long mediaId);    /**

     * Lista todas as mídias ativas associadas a uma entidade específica.
     * @param serviceName Nome do serviço ao qual as mídias pertencem.
     * @param entityId ID da entidade associada às mídias.
     * @return Lista de MediaResponse contendo URLs assinadas e informações das mídias.
     */
    List<MediaResponse> listMediaByEntity(String serviceName, Long entityId);

    /**
     * Atualiza um arquivo de mídia existente no MinIO e suas informações no banco de dados.
     * @param mediaId ID da mídia a ser atualizada.
     * @param request Objeto contendo os novos dados do upload.
     * @return MediaResponse com os detalhes da mídia atualizada.
     */
    MediaResponse updateMedia(String serviceName, Long mediaId, MediaRequest request);

    /**
     * Desativa uma mídia, removendo seu acesso ativo e movendo o arquivo para a pasta de desativados no MinIO.
     * @param mediaId ID da mídia a ser desativada.
     */
    void disableMedia(String serviceName, Long mediaId);
}
