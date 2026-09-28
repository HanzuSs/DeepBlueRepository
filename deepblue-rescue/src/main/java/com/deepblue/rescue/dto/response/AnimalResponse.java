package com.deepblue.rescue.dto.response;

import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueStatus;

public record AnimalResponse(

        Long id,

        Long animalId,

        String commonName,

        String scientificName,

        AnimalSex sex,

        Long caseId,

        RescueStatus rescueStatus

) {
}