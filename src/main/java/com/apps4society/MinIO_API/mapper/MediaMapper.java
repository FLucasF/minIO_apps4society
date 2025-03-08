package com.apps4society.MinIO_API.mapper;

import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.model.entity.Media;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface MediaMapper {

    @Mapping(target = "id", ignore = true) // O ID será gerado pelo banco
    @Mapping(target = "active", constant = "true") // Define como ativo por padrão
    Media toEntity(MediaRequest request);

    MediaResponse toResponse(Media media);

}
