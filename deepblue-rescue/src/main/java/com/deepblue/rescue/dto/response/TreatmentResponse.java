package com.deepblue.rescue.dto.response;

import com.deepblue.rescue.domain.TreatmentType;

import java.time.LocalDateTime;

public record TreatmentResponse(

        Long id,

        Long animalId,

        Long specialistId,

        LocalDateTime performedAt,

        TreatmentType type,

        String description

) {
}