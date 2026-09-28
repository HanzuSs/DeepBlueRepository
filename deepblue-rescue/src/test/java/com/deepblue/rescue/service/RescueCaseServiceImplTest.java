package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
class RescueCaseServiceImplTest {

    @Mock
    private RescueCaseRepository repository;

    @Mock
    private RescueCaseMapper mapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    private RescueCaseResponse responseWith(RescueStatus status) {
        return new RescueCaseResponse(1L, 101L, LocalDate.of(2026, 8, 20),
                "Santa Marta Bay", status, 201L, 301L);
    }

    private RescueCase caseWithStatus(RescueStatus status) {
        RescueCase rescueCase = mock(RescueCase.class);
        when(rescueCase.getStatus()).thenReturn(status);
        return rescueCase;
    }

    @Test
    void shouldFindRescueCaseByCode() {
        RescueCase rescueCase = mock(RescueCase.class);
        RescueCaseResponse response = responseWith(RescueStatus.ADMITTED);

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        RescueCaseResponse result = service.findByCode("RES-001");

        assertThat(result).isEqualTo(response);
        verify(repository).findByCaseCode("RES-001");
        verify(mapper).toResponse(rescueCase);
    }

    @Test
    void shouldThrowResourceNotFoundWhenCaseDoesNotExist() {
        when(repository.findByCaseCode("RES-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("RES-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RES-999");

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldFindRescueCasesByStatus() {
        RescueCase first = mock(RescueCase.class);
        RescueCase second = mock(RescueCase.class);
        RescueCaseResponse firstResponse = responseWith(RescueStatus.IN_REHABILITATION);
        RescueCaseResponse secondResponse = responseWith(RescueStatus.IN_REHABILITATION);

        when(repository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(first, second));
        when(mapper.toResponse(first)).thenReturn(firstResponse);
        when(mapper.toResponse(second)).thenReturn(secondResponse);

        List<RescueCaseResponse> result =
                service.findByStatus(RescueStatus.IN_REHABILITATION);

        assertThat(result).containsExactly(firstResponse, secondResponse);
    }

    @Test
    void shouldReturnEmptyListWhenNoCaseMatchesStatus() {
        when(repository.findByStatusOrderByRescueDateAsc(RescueStatus.CLOSED))
                .thenReturn(List.of());

        assertThat(service.findByStatus(RescueStatus.CLOSED)).isEmpty();

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldChangeStatusWhenTransitionIsValid() {
        RescueCase rescueCase = caseWithStatus(RescueStatus.ADMITTED);
        RescueCaseResponse response = responseWith(RescueStatus.UNDER_EVALUATION);

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));
        when(repository.save(rescueCase)).thenReturn(rescueCase);
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        RescueCaseResponse result = service.changeStatus("RES-001",
                new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION));

        assertThat(result).isEqualTo(response);
        verify(rescueCase).setStatus(RescueStatus.UNDER_EVALUATION);
        verify(repository).save(rescueCase);
    }

    @Test
    void shouldRejectInvalidTransitionAndNeverSave() {
        RescueCase rescueCase = caseWithStatus(RescueStatus.ADMITTED);

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));

        assertThatThrownBy(() -> service.changeStatus("RES-001",
                new ChangeRescueStatusRequest(RescueStatus.READY_FOR_RELEASE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ADMITTED")
                .hasMessageContaining("READY_FOR_RELEASE");

        verify(rescueCase, never()).setStatus(any());
        verify(repository, never()).save(any());
        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldThrowResourceNotFoundWhenChangingStatusOfMissingCase() {
        when(repository.findByCaseCode("RES-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus("RES-999",
                new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @ParameterizedTest(name = "{0} -> {1} es una transicion valida")
    @CsvSource({
            "ADMITTED,          UNDER_EVALUATION",
            "UNDER_EVALUATION,  IN_REHABILITATION",
            "IN_REHABILITATION, READY_FOR_RELEASE",
            "READY_FOR_RELEASE, RELEASED"
    })
    void shouldAllowEveryStepOfTheRescueFlow(RescueStatus current,
                                             RescueStatus next) {
        RescueCase rescueCase = caseWithStatus(current);

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));
        when(repository.save(rescueCase)).thenReturn(rescueCase);
        when(mapper.toResponse(rescueCase)).thenReturn(responseWith(next));

        RescueCaseResponse result = service.changeStatus("RES-001",
                new ChangeRescueStatusRequest(next));

        assertThat(result.status()).isEqualTo(next);
        verify(repository).save(rescueCase);
    }

    @ParameterizedTest(name = "{0} -> {1} esta prohibida")
    @CsvSource({
            "ADMITTED,          RELEASED",
            "ADMITTED,          IN_REHABILITATION",
            "UNDER_EVALUATION,  ADMITTED",
            "IN_REHABILITATION, UNDER_EVALUATION",
            "RELEASED,           IN_REHABILITATION",
            "RELEASED,           CLOSED",
            "CLOSED,             ADMITTED"
    })
    void shouldRejectEveryForbiddenTransition(RescueStatus current,
                                              RescueStatus next) {
        RescueCase rescueCase = caseWithStatus(current);

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));

        assertThatThrownBy(() -> service.changeStatus("RES-001",
                new ChangeRescueStatusRequest(next)))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).save(any());
    }
}
