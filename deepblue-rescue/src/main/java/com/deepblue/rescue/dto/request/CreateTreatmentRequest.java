package com.deepblue.rescue.dto.request;

import com.deepblue.rescue.domain.TreatmentType;

import java.time.LocalDateTime;

public record CreateTreatmentRequest(

        Long animalId,

        Long specialistId,

        LocalDateTime performedAt,

        TreatmentType type,

        String description

) {
}