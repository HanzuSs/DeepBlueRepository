package com.deepblue.rescue.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RescueCaseTest {

    @Test
    void shouldAssignAnimalAndKeepBidirectionalRelationship() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );
        RescueCase rescueCase = new RescueCase(
                "RES-2026-001",
                LocalDate.of(2026, 8, 3),
                "Bahia Concha",
                RescueStatus.ADMITTED
        );
        Animal animal = new Animal(
                "AN-2026-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);

        assertThat(rescueCase.getAnimal()).isSameAs(animal);
        assertThat(animal.getRescueCase()).isSameAs(rescueCase);
        assertThat(rescueCase.getRescueCenter()).isSameAs(center);
    }

    @Test
    void shouldUpdateDomainStateThroughSetters() {
        RescueCase rescueCase = new RescueCase(
                "RES-2026-002",
                LocalDate.of(2026, 8, 4),
                "Taganga",
                RescueStatus.UNDER_EVALUATION
        );

        rescueCase.setCaseCode("RES-2026-099");
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 10));
        rescueCase.setRescueLocation("Rodadero");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);

        assertThat(rescueCase.getCaseCode()).isEqualTo("RES-2026-099");
        assertThat(rescueCase.getRescueDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(rescueCase.getRescueLocation()).isEqualTo("Rodadero");
        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.IN_REHABILITATION);
    }
}
