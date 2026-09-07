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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        void flywayExecutesInitialMigrations() {
        List<String> appliedVersions = jdbcTemplate.query(
            """
            select version
            from flyway_schema_history
            where version in ('1', '2')
            order by installed_rank
            """,
            (resultSet, rowNumber) -> resultSet.getString("version")
        );

        assertEquals(List.of("1", "2"), appliedVersions);
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

}
