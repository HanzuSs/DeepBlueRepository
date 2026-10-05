package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    private static final Long ANIMAL_ID = 1L;
    private static final Long SPECIALIST_ID = 2L;
    private static final LocalDate RESCUE_DATE = LocalDate.of(2026, 8, 20);
    private static final LocalDateTime VALID_DATE = LocalDateTime.of(2026, 8, 21, 9, 0);

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    private CreateTreatmentRequest requestAt(LocalDateTime performedAt,
                                             TreatmentType type) {
        return new CreateTreatmentRequest(ANIMAL_ID, SPECIALIST_ID, performedAt,
                type, "Cleaning of left front flipper injury.");
    }

    private Specialist specialist(boolean active) {
        return new Specialist("SPEC-001", "Elena", "Vargas",
                "elena.vargas@deepblue.org", active);
    }

    private Animal animalWithCaseStatus(RescueStatus status) {
        RescueCase rescueCase = mock(RescueCase.class);
        when(rescueCase.getStatus()).thenReturn(status);

        Animal animal = mock(Animal.class);
        when(animal.getRescueCase()).thenReturn(rescueCase);
        return animal;
    }

    @Test
    void shouldRegisterTreatmentWhenAllRulesAreMet() {
        RescueCase rescueCase = mock(RescueCase.class);
        when(rescueCase.getStatus()).thenReturn(RescueStatus.IN_REHABILITATION);
        when(rescueCase.getRescueDate()).thenReturn(RESCUE_DATE);

        Animal animal = mock(Animal.class);
        when(animal.getRescueCase()).thenReturn(rescueCase);

        Specialist specialist = specialist(true);
        TreatmentResponse response = new TreatmentResponse(3L, ANIMAL_ID,
                SPECIALIST_ID, VALID_DATE, TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury.");

        when(animalRepository.findById(ANIMAL_ID))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findById(SPECIALIST_ID))
                .thenReturn(Optional.of(specialist));
        when(treatmentRepository.save(any(Treatment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Treatment.class))).thenReturn(response);

        TreatmentResponse result = service.register(
                requestAt(VALID_DATE, TreatmentType.WOUND_CARE));

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Treatment> captor = ArgumentCaptor.forClass(Treatment.class);
        verify(treatmentRepository).save(captor.capture());
        Treatment saved = captor.getValue();
        assertThat(saved.getAnimal()).isSameAs(animal);
        assertThat(saved.getSpecialist()).isSameAs(specialist);
        assertThat(saved.getPerformedAt()).isEqualTo(VALID_DATE);
        assertThat(saved.getType()).isEqualTo(TreatmentType.WOUND_CARE);
    }

    @Test
    void shouldAllowTreatmentOnTheSameDayOfTheRescue() {
        RescueCase rescueCase = mock(RescueCase.class);
        when(rescueCase.getStatus()).thenReturn(RescueStatus.UNDER_EVALUATION);
        when(rescueCase.getRescueDate()).thenReturn(RESCUE_DATE);

        Animal animal = mock(Animal.class);
        when(animal.getRescueCase()).thenReturn(rescueCase);

        when(animalRepository.findById(ANIMAL_ID))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findById(SPECIALIST_ID))
                .thenReturn(Optional.of(specialist(true)));
        when(treatmentRepository.save(any(Treatment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.register(requestAt(RESCUE_DATE.atTime(6, 30),
                TreatmentType.OBSERVATION));

        verify(treatmentRepository).save(any(Treatment.class));
    }

    @Test
    void shouldThrowResourceNotFoundWhenAnimalDoesNotExist() {
        when(animalRepository.findById(ANIMAL_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(
                requestAt(VALID_DATE, TreatmentType.WOUND_CARE)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(ANIMAL_ID.toString());

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundWhenSpecialistDoesNotExist() {
        when(animalRepository.findById(ANIMAL_ID))
                .thenReturn(Optional.of(mock(Animal.class)));
        when(specialistRepository.findById(SPECIALIST_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(
                requestAt(VALID_DATE, TreatmentType.WOUND_CARE)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(SPECIALIST_ID.toString());

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectInactiveSpecialistAndNeverSave() {
        when(animalRepository.findById(ANIMAL_ID))
                .thenReturn(Optional.of(mock(Animal.class)));
        when(specialistRepository.findById(SPECIALIST_ID))
                .thenReturn(Optional.of(specialist(false)));

        assertThatThrownBy(() -> service.register(
                requestAt(VALID_DATE, TreatmentType.WOUND_CARE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");

        verify(treatmentRepository, never()).save(any());
        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldRejectTreatmentWhenCaseIsReleased() {
        Animal animal = animalWithCaseStatus(RescueStatus.RELEASED);

        when(animalRepository.findById(ANIMAL_ID))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findById(SPECIALIST_ID))
                .thenReturn(Optional.of(specialist(true)));

        assertThatThrownBy(() -> service.register(
                requestAt(VALID_DATE, TreatmentType.OBSERVATION)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RELEASED");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectTreatmentWhenCaseIsClosed() {
        Animal animal = animalWithCaseStatus(RescueStatus.CLOSED);

        when(animalRepository.findById(ANIMAL_ID))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findById(SPECIALIST_ID))
                .thenReturn(Optional.of(specialist(true)));

        assertThatThrownBy(() -> service.register(
                requestAt(VALID_DATE, TreatmentType.OBSERVATION)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CLOSED");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectTreatmentBeforeTheRescueDate() {
        RescueCase rescueCase = mock(RescueCase.class);
        when(rescueCase.getStatus()).thenReturn(RescueStatus.IN_REHABILITATION);
        when(rescueCase.getRescueDate()).thenReturn(RESCUE_DATE);

        Animal animal = mock(Animal.class);
        when(animal.getRescueCase()).thenReturn(rescueCase);

        when(animalRepository.findById(ANIMAL_ID))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findById(SPECIALIST_ID))
                .thenReturn(Optional.of(specialist(true)));

        assertThatThrownBy(() -> service.register(
                requestAt(LocalDateTime.of(2026, 8, 15, 10, 0),
                        TreatmentType.WOUND_CARE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("rescue date");

        verify(treatmentRepository, never()).save(any());
    }

    @ParameterizedTest(name = "caso {0} permite registrar tratamientos")
    @EnumSource(value = RescueStatus.class,
            names = {"RELEASED", "CLOSED"},
            mode = EnumSource.Mode.EXCLUDE)
    void shouldAcceptEveryStatusExceptReleasedAndClosed(RescueStatus status) {
        RescueCase rescueCase = mock(RescueCase.class);
        when(rescueCase.getStatus()).thenReturn(status);
        when(rescueCase.getRescueDate()).thenReturn(RESCUE_DATE);

        Animal animal = mock(Animal.class);
        when(animal.getRescueCase()).thenReturn(rescueCase);

        when(animalRepository.findById(ANIMAL_ID))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findById(SPECIALIST_ID))
                .thenReturn(Optional.of(specialist(true)));
        when(treatmentRepository.save(any(Treatment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.register(requestAt(VALID_DATE, TreatmentType.HYDRATION));

        verify(treatmentRepository).save(any(Treatment.class));
    }

    @Test
    void shouldFindTreatmentsByAnimalId() {
        Treatment first = mock(Treatment.class);
        Treatment second = mock(Treatment.class);
        TreatmentResponse firstResponse = new TreatmentResponse(3L, ANIMAL_ID,
                SPECIALIST_ID, VALID_DATE, TreatmentType.WOUND_CARE, "first");
        TreatmentResponse secondResponse = new TreatmentResponse(4L, ANIMAL_ID,
                SPECIALIST_ID, VALID_DATE.plusDays(1), TreatmentType.MEDICATION, "second");

        when(treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(ANIMAL_ID))
                .thenReturn(List.of(first, second));
        when(mapper.toResponse(first)).thenReturn(firstResponse);
        when(mapper.toResponse(second)).thenReturn(secondResponse);

        List<TreatmentResponse> result = service.findByAnimalId(ANIMAL_ID);

        assertThat(result).containsExactly(firstResponse, secondResponse);
    }
}
