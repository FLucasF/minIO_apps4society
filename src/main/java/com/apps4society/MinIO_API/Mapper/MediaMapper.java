package com.apps4society.MinIO_API.Mapper;

import com.apps4society.MinIO_API.model.DTO.MediaDTO;
import com.apps4society.MinIO_API.model.entity.Media;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface MediaMapper {
    MediaMapper INSTANCE = Mappers.getMapper(MediaMapper.class);

    MediaDTO entityToDto(Media Media);
    Media dtoToEntity(MediaDTO mediaDTO);

}
