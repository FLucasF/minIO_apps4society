package com.apps4society.MinIO_API.mapper;

import com.apps4society.MinIO_API.model.DTO.MediaRequest;
import com.apps4society.MinIO_API.model.DTO.MediaResponse;
import com.apps4society.MinIO_API.model.entity.Media;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface MediaMapper {

    @Mapping(target = "entityId", source = "entityId")
    @Mapping(target = "serviceName", source = "serviceName")
    @Mapping(target = "fileName", source = "fileName")
    MediaResponse toResponse(Media media);

}
