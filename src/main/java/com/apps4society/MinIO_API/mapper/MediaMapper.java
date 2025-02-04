package com.apps4society.MinIO_API.mapper;

import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.entity.Media;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface MediaMapper {
    @Mapping(target = "serviceName", source = "serviceName") // Garante o mapeamento explícito
    MediaDTO entityToDto(Media Media);
}
