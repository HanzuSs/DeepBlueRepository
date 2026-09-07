package com.deepblue.rescue;

import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.MedicalRecordRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.dao.DataIntegrityViolationException;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("deepblue_test")
            .withUsername("deepblue")
            .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        assertNotNull(jdbcTemplate);
        }

        @Test
        void flywayExecutesMigrations() {
        List<String> appliedVersions = jdbcTemplate.query(
            """
            select version
            from flyway_schema_history
            where version in ('1', '2', '3')
            order by installed_rank
            """,
            (resultSet, rowNumber) -> resultSet.getString("version")
        );

        assertEquals(List.of("1", "2", "3"), appliedVersions);
        }

        @Test
        void inheritedRepositoryMethodsPersistAndFindCenter() {
        RescueCenter center = rescueCenterRepository.save(
            new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta")
        );

        assertNotNull(center.getId());
        assertEquals(center, rescueCenterRepository.findById(center.getId()).orElseThrow());
        assertTrue(rescueCenterRepository.existsById(center.getId()));
        assertEquals(1, rescueCenterRepository.count());
        }

        @Test
        void rescueCenterOwnsTwoRescueCases() {
        RescueCenter center = new RescueCenter(
            "DB-CAR",
            "DeepBlue Caribbean Center",
            "Santa Marta"
        );
        RescueCase firstCase = new RescueCase(
            "RES-001",
            LocalDate.of(2026, 8, 1),
            "Bahia Concha",
            RescueStatus.ADMITTED
        );
        RescueCase secondCase = new RescueCase(
            "RES-002",
            LocalDate.of(2026, 8, 2),
            "Taganga",
            RescueStatus.IN_REHABILITATION
        );

        center.addCase(firstCase);
        center.addCase(secondCase);
        rescueCenterRepository.saveAndFlush(center);

        List<RescueCase> cases = rescueCaseRepository.findByRescueCenterCode("DB-CAR");

        assertEquals(2, cases.size());
        assertTrue(cases.stream().allMatch(rescueCase -> rescueCase.getRescueCenter().equals(center)));
    }

        @Test
        void rescueCaseHasOneAnimal() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        RescueCase rescueCase = new RescueCase(
            "RES-2026-001",
            LocalDate.of(2026, 8, 1),
            "Bahia Concha",
            RescueStatus.ADMITTED
        );
        Animal animal = new Animal(
            "AN-2026-001",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.UNKNOWN
        );

        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);
        rescueCenterRepository.saveAndFlush(center);

        assertNotNull(rescueCase.getId());
        assertNotNull(animal.getId());
        assertEquals(animal, rescueCase.getAnimal());
        assertEquals(rescueCase, animal.getRescueCase());
        }

        @Test
        void animalPersistsMedicalRecordWithCascade() {
        Animal animal = new Animal(
            "AN-2026-002",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.UNKNOWN
        );
        MedicalRecord medicalRecord = new MedicalRecord(
            new BigDecimal("28.40"),
            "STABLE",
            "Left front flipper injury",
            null
        );

        animal.assignMedicalRecord(medicalRecord);
        animalRepository.saveAndFlush(animal);

        assertNotNull(animal.getId());
        assertNotNull(medicalRecord.getId());
        assertEquals(animal, medicalRecord.getAnimal());
        assertEquals(medicalRecord, animal.getMedicalRecord());
        }

        @Test
        void trackingDeviceCodeIsOptionalAndUnique() {
        RescueCenter center = new RescueCenter("DB-GPS", "GPS Center", "Santa Marta");
        RescueCase firstCase = new RescueCase("RES-GPS-1", LocalDate.now(), "Bahia Concha", RescueStatus.ADMITTED);
        RescueCase secondCase = new RescueCase("RES-GPS-2", LocalDate.now(), "Taganga", RescueStatus.ADMITTED);
        RescueCase thirdCase = new RescueCase("RES-GPS-3", LocalDate.now(), "Rodadero", RescueStatus.ADMITTED);
        center.addCase(firstCase);
        center.addCase(secondCase);
        center.addCase(thirdCase);
        rescueCenterRepository.saveAndFlush(center);

        Animal withoutDevice = new Animal("AN-GPS-1", "Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        withoutDevice.setRescueCase(firstCase);
        animalRepository.saveAndFlush(withoutDevice);

        Animal withDevice = new Animal("AN-GPS-2", "Seal", "Zalophus wollebaeki", AnimalSex.UNKNOWN);
        withDevice.setRescueCase(secondCase);
        withDevice.setTrackingDeviceCode("GPS-001");
        animalRepository.saveAndFlush(withDevice);

        Animal duplicateDevice = new Animal("AN-GPS-3", "Bird", "Sula nebouxii", AnimalSex.UNKNOWN);
        duplicateDevice.setRescueCase(thirdCase);
        duplicateDevice.setTrackingDeviceCode("GPS-001");

        assertThrows(DataIntegrityViolationException.class,
            () -> animalRepository.saveAndFlush(duplicateDevice));
        }

        @Test
        void specialistPersistsTwoExpertiseAreas() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Specialist specialist = new Specialist(
            "SPEC-001",
            "Elena",
            "Vargas",
            "elena@deepblue.org",
            true
        );

        specialist.addExpertise(trauma);
        specialist.addExpertise(rehabilitation);
        specialistRepository.saveAndFlush(specialist);

        assertNotNull(specialist.getId());
        assertEquals(2, specialist.getExpertiseAreas().size());
        assertTrue(trauma.getSpecialists().contains(specialist));
        assertTrue(rehabilitation.getSpecialists().contains(specialist));
        }

        @Test
        void findsCasesByStatus() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        center.addCase(new RescueCase(
            "RES-001",
            LocalDate.of(2026, 8, 1),
            "Bahia Concha",
            RescueStatus.IN_REHABILITATION
        ));
        center.addCase(new RescueCase(
            "RES-002",
            LocalDate.of(2026, 8, 2),
            "Taganga",
            RescueStatus.READY_FOR_RELEASE
        ));
        center.addCase(new RescueCase(
            "RES-003",
            LocalDate.of(2026, 8, 3),
            "Rodadero",
            RescueStatus.IN_REHABILITATION
        ));

        rescueCenterRepository.saveAndFlush(center);

        List<RescueCase> cases = rescueCaseRepository.findByStatusOrderByRescueDateAsc(
            RescueStatus.IN_REHABILITATION
        );

        assertEquals(2, cases.size());
        assertEquals("RES-001", cases.get(0).getCaseCode());
        assertEquals("RES-003", cases.get(1).getCaseCode());
        }

        @Test
        void findsAnimalsByRescueCenterCode() {
        RescueCenter caribbean = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        RescueCase caribbeanCase = new RescueCase(
            "RES-CAR-001",
            LocalDate.of(2026, 8, 1),
            "Bahia Concha",
            RescueStatus.IN_REHABILITATION
        );
        Animal caribbeanAnimal = new Animal(
            "AN-CAR-001",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.UNKNOWN
        );
        caribbean.addCase(caribbeanCase);
        caribbeanCase.assignAnimal(caribbeanAnimal);

        RescueCenter pacific = new RescueCenter("DB-PAC", "DeepBlue Pacific Center", "Buenaventura");
        RescueCase pacificCase = new RescueCase(
            "RES-PAC-001",
            LocalDate.of(2026, 8, 2),
            "Juanchaco",
            RescueStatus.IN_REHABILITATION
        );
        Animal pacificAnimal = new Animal(
            "AN-PAC-001",
            "Sea Lion",
            "Zalophus wollebaeki",
            AnimalSex.UNKNOWN
        );
        pacific.addCase(pacificCase);
        pacificCase.assignAnimal(pacificAnimal);

        rescueCenterRepository.saveAndFlush(caribbean);
        rescueCenterRepository.saveAndFlush(pacific);

        List<Animal> animals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");

        assertEquals(1, animals.size());
        assertEquals("AN-CAR-001", animals.get(0).getAnimalCode());
        }

        @Test
        void findsActiveSpecialistsByExpertise() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Expertise marineMammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();
        Expertise marineBirds = expertiseRepository.findByNameIgnoreCase("Marine Birds").orElseThrow();

        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist mateo = new Specialist("SPEC-002", "Mateo", "Rojas", "mateo@deepblue.org", true);
        mateo.addExpertise(marineMammals);
        mateo.addExpertise(rehabilitation);

        Specialist sofia = new Specialist("SPEC-003", "Sofia", "Marin", "sofia@deepblue.org", true);
        sofia.addExpertise(marineBirds);
        sofia.addExpertise(trauma);

        specialistRepository.saveAll(List.of(elena, mateo, sofia));
        specialistRepository.flush();

        List<Specialist> specialists = specialistRepository.findActiveByExpertise("trauma");

        assertEquals(2, specialists.size());
        assertTrue(specialists.stream().map(Specialist::getFirstName).toList().containsAll(List.of("Elena", "Sofia")));
        }

        @Test
        void persistsAndQueriesIntegratedRescueScenario() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
        RescueCase rescueCase = new RescueCase(
            "RES-2026-100",
            LocalDate.of(2026, 8, 18),
            "Bahia Concha",
            RescueStatus.IN_REHABILITATION
        );
        Animal animal = new Animal(
            "AN-2026-100",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.FEMALE
        );
        animal.setTrackingDeviceCode("GPS-2026-100");
        animal.assignMedicalRecord(new MedicalRecord(
            new BigDecimal("27.80"),
            "STABLE",
            "Injury caused by fishing net",
            "Possible plastic ingestion"
        ));
        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);
        rescueCenterRepository.saveAndFlush(center);

        Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Specialist elena = new Specialist(
            "SPEC-001",
            "Elena",
            "Vargas",
            "elena@deepblue.org",
            true
        );
        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.saveAndFlush(elena);

        treatmentRepository.saveAll(List.of(
            new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 18, 10, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper"
            ),
            new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 19, 10, 0),
                TreatmentType.HYDRATION,
                "Subcutaneous fluid therapy"
            )
        ));
        treatmentRepository.flush();

        assertEquals(animal, rescueCaseRepository.findByCaseCode("RES-2026-100").orElseThrow().getAnimal());
        assertEquals(1, rescueCaseRepository.findByStatusOrderByRescueDateAsc(
            RescueStatus.IN_REHABILITATION
        ).size());
        assertEquals(1, animalRepository.findByRescueCaseRescueCenterCode("DB-CAR").size());
        assertEquals(1, animalRepository.findByCommonNameContainingIgnoreCase("turtle").size());
        assertEquals(1, specialistRepository.findActiveByExpertise("Trauma").size());
        assertEquals(2, treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId()).size());
        assertEquals(2, treatmentRepository.findBySpecialistExpertise("Rehabilitation").size());
        assertEquals(List.of(animal), animalRepository.findInStatusTreatedByExpertise(
            RescueStatus.IN_REHABILITATION,
            "trauma"
        ));
        assertEquals(1, treatmentRepository.findPerformedBetween(
            LocalDateTime.of(2026, 8, 18, 0, 0),
            LocalDateTime.of(2026, 8, 18, 23, 59)
        ).size());
        }

        @Test
        void findsAnimalTreatmentsInChronologicalOrder() {
        TreatmentScenario scenario = persistTreatmentScenario();

        List<Treatment> treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(
            scenario.animal().getId()
        );

        assertEquals(3, treatments.size());
        assertEquals(TreatmentType.WOUND_CARE, treatments.get(0).getType());
        assertEquals(TreatmentType.HYDRATION, treatments.get(1).getType());
        assertEquals(TreatmentType.OBSERVATION, treatments.get(2).getType());
        }

        @Test
        void findsTreatmentsWithinDateInterval() {
        Animal animal = persistAnimalForTreatment("AN-INTERVAL");
        Specialist specialist = specialistRepository.saveAndFlush(
            new Specialist("SPEC-INTERVAL", "Elena", "Vargas", "interval@deepblue.org", true)
        );
        treatmentRepository.saveAll(List.of(
            new Treatment(animal, specialist, LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "Before"),
            new Treatment(animal, specialist, LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION, "Inside"),
            new Treatment(animal, specialist, LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "After")
        ));
        treatmentRepository.flush();

        List<Treatment> treatments = treatmentRepository.findPerformedBetween(
            LocalDateTime.of(2026, 8, 5, 0, 0),
            LocalDateTime.of(2026, 8, 15, 23, 59)
        );

        assertEquals(1, treatments.size());
        assertEquals(LocalDateTime.of(2026, 8, 10, 10, 0), treatments.get(0).getPerformedAt());
        }

        @Test
        void rejectsDuplicateAnimalCode() {
        RescueCenter center = new RescueCenter("DB-UNIQUE", "Unique Center", "Santa Marta");
        RescueCase firstCase = new RescueCase("RES-UNIQUE-1", LocalDate.now(), "Bahia Concha", RescueStatus.ADMITTED);
        RescueCase secondCase = new RescueCase("RES-UNIQUE-2", LocalDate.now(), "Taganga", RescueStatus.ADMITTED);
        center.addCase(firstCase);
        center.addCase(secondCase);
        rescueCenterRepository.saveAndFlush(center);

        Animal firstAnimal = new Animal("AN-100", "Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        firstAnimal.setRescueCase(firstCase);
        animalRepository.saveAndFlush(firstAnimal);
        Animal duplicate = new Animal("AN-100", "Seal", "Zalophus wollebaeki", AnimalSex.UNKNOWN);
        duplicate.setRescueCase(secondCase);

        assertThrows(DataIntegrityViolationException.class, () -> animalRepository.saveAndFlush(duplicate));
        }

        @Test
        void rejectsInvalidRescueStatusCheckConstraint() {
        RescueCenter center = rescueCenterRepository.saveAndFlush(
            new RescueCenter("DB-CHECK", "Check Center", "Santa Marta")
        );

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
            """
            insert into rescue_cases
                (case_code, rescue_date, rescue_location, status, rescue_center_id)
            values (?, ?, ?, ?, ?)
            """,
            "RES-CHECK",
            LocalDate.of(2026, 8, 1),
            "Bahia Concha",
            "INVALID_STATUS",
            center.getId()
        ));
        }

        @Test
        void rejectsTreatmentWithUnknownForeignKeys() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
            """
            insert into treatments
                (animal_id, specialist_id, performed_at, type, description)
            values (?, ?, ?, ?, ?)
            """,
            999_999L,
            999_999L,
            LocalDateTime.of(2026, 8, 1, 10, 0),
            "WOUND_CARE",
            "Invalid foreign keys"
        ));
        }

        private TreatmentScenario persistTreatmentScenario() {
        Animal animal = persistAnimalForTreatment("AN-TREATMENT");
        Specialist elena = specialistRepository.saveAndFlush(
            new Specialist("SPEC-ELENA", "Elena", "Vargas", "elena.treatment@deepblue.org", true)
        );
        Specialist mateo = specialistRepository.saveAndFlush(
            new Specialist("SPEC-MATEO", "Mateo", "Rojas", "mateo.treatment@deepblue.org", true)
        );

        treatmentRepository.saveAll(List.of(
            new Treatment(animal, elena, LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "Cleaning"),
            new Treatment(animal, elena, LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION, "Fluid therapy"),
            new Treatment(animal, mateo, LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "Observation")
        ));
        treatmentRepository.flush();

        return new TreatmentScenario(animal, elena, mateo);
        }

        private Animal persistAnimalForTreatment(String animalCode) {
        RescueCenter center = new RescueCenter(
            "DB-" + animalCode,
            "Treatment Center",
            "Santa Marta"
        );
        RescueCase rescueCase = new RescueCase(
            "RES-" + animalCode,
            LocalDate.of(2026, 7, 1),
            "Bahia Concha",
            RescueStatus.IN_REHABILITATION
        );
        Animal animal = new Animal(animalCode, "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);
        rescueCenterRepository.saveAndFlush(center);
        return animal;
        }

        private record TreatmentScenario(Animal animal, Specialist elena, Specialist mateo) {
        }

}
