package com.deepblue.rescue.mapper;

import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TreatmentMapper {

    @Mapping(target = "animalId", source = "animal.id")
    @Mapping(target = "specialistId", source = "specialist.id")
    TreatmentResponse toResponse(Treatment treatment);
}