package com.deepblue.rescue.dto.response;

import com.deepblue.rescue.domain.RescueStatus;

import java.time.LocalDate;

public record RescueCaseResponse(

        Long id,

        Long caseId,

        LocalDate rescueDate,

        String rescueLocation,

        RescueStatus status,

        Long centerId,

        Long animalId

) {
}