package com.deepblue.rescue.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RescueCenterTest {

    @Test
    void shouldAddCaseAndEstablishBidirectionalRelationship() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );
        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 1),
                "Bahia Concha",
                RescueStatus.ADMITTED
        );

        center.addCase(rescueCase);

        assertThat(center.getRescueCases()).containsExactly(rescueCase);
        assertThat(rescueCase.getRescueCenter()).isSameAs(center);
    }

    @Test
    void shouldRemoveCaseAndDetachAssociation() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );
        RescueCase rescueCase = new RescueCase(
                "RES-002",
                LocalDate.of(2026, 8, 2),
                "Taganga",
                RescueStatus.IN_REHABILITATION
        );
        center.addCase(rescueCase);

        center.removeCase(rescueCase);

        assertThat(center.getRescueCases()).doesNotContain(rescueCase);
        assertThat(rescueCase.getRescueCenter()).isNull();
    }

    @Test
    void shouldExposeMutatorsForDomainState() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        center.setCode("DB-ATL");
        center.setName("DeepBlue Atlantic Center");
        center.setCity("Cartagena");

        assertThat(center.getCode()).isEqualTo("DB-ATL");
        assertThat(center.getName()).isEqualTo("DeepBlue Atlantic Center");
        assertThat(center.getCity()).isEqualTo("Cartagena");
    }
}
