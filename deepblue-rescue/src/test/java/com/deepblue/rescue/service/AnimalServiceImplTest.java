package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class AnimalServiceImplTest {

    private static final Long ANIMAL_ID = 1L;

    @Mock
    private AnimalRepository repository;

    @Mock
    private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;

    private AnimalResponse response(RescueStatus status) {
        return new AnimalResponse(ANIMAL_ID, ANIMAL_ID, "Green Sea Turtle",
                "Chelonia mydas", AnimalSex.FEMALE, 101L, status);
    }

    private Animal animalWithCaseStatus(RescueStatus status) {
        RescueCase rescueCase = mock(RescueCase.class);
        when(rescueCase.getStatus()).thenReturn(status);

        Animal animal = mock(Animal.class);
        when(animal.getRescueCase()).thenReturn(rescueCase);
        return animal;
    }

    @Test
    void shouldFindAnimalById() {
        Animal animal = mock(Animal.class);
        AnimalResponse expected = response(RescueStatus.IN_REHABILITATION);

        when(repository.findById(ANIMAL_ID))
                .thenReturn(Optional.of(animal));
        when(mapper.toResponse(animal)).thenReturn(expected);

        AnimalResponse result = service.findByCode(ANIMAL_ID);

        assertThat(result).isEqualTo(expected);
        verify(repository).findById(ANIMAL_ID);
    }

    @Test
    void shouldThrowResourceNotFoundWhenAnimalDoesNotExist() {
        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldFindOnlyAnimalsInRehabilitation() {
        Animal first = mock(Animal.class);
        Animal second = mock(Animal.class);
        AnimalResponse firstResponse = response(RescueStatus.IN_REHABILITATION);
        AnimalResponse secondResponse = response(RescueStatus.IN_REHABILITATION);

        when(repository.findByRescueCaseStatus(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(first, second));
        when(mapper.toResponse(first)).thenReturn(firstResponse);
        when(mapper.toResponse(second)).thenReturn(secondResponse);

        List<AnimalResponse> result = service.findAnimalsInRehabilitation();

        assertThat(result).containsExactly(firstResponse, secondResponse);
    }

    @ParameterizedTest(name = "{0} -> puede recibir tratamiento")
    @EnumSource(value = RescueStatus.class,
            names = {"UNDER_EVALUATION", "IN_REHABILITATION"})
    void shouldReturnTrueForEvaluationAndRehabilitation(RescueStatus status) {
        when(repository.findById(ANIMAL_ID))
                .thenReturn(Optional.of(animalWithCaseStatus(status)));

        assertThat(service.canReceiveTreatment(ANIMAL_ID)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> NO puede recibir tratamiento")
    @EnumSource(value = RescueStatus.class,
            names = {"UNDER_EVALUATION", "IN_REHABILITATION"},
            mode = EnumSource.Mode.EXCLUDE)
    void shouldReturnFalseForEveryOtherStatus(RescueStatus status) {
        when(repository.findById(ANIMAL_ID))
                .thenReturn(Optional.of(animalWithCaseStatus(status)));

        assertThat(service.canReceiveTreatment(ANIMAL_ID)).isFalse();
    }

    @Test
    void shouldThrowResourceNotFoundWhenCheckingUnknownAnimal() {
        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.canReceiveTreatment(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
