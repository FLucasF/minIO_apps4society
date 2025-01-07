package com.apps4society.MinIO_API.Mapper;

import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.entity.Media;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MediaMapper {
    MediaDTO entityToDto(Media Media);
}
