package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.RescueCaseService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RescueCaseController.class)
@Import(GlobalExceptionHandler.class)
class RescueCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RescueCaseService service;

    private RescueCaseResponse response(Long id, RescueStatus status) {
        return new RescueCaseResponse(
                id,
                101L,
                LocalDate.of(2026, 8, 20),
                "Bahia Concha",
                status,
                201L,
                301L
        );
    }

    // ---------------------------------------------------------------
    // GET /api/rescue-cases/{caseCode}
    // ---------------------------------------------------------------

    @Test
    void shouldReturnRescueCaseByCode() throws Exception {
        when(service.findByCode("RES-2026-001"))
                .thenReturn(response(1L, RescueStatus.IN_REHABILITATION));

        mockMvc.perform(get("/api/rescue-cases/{code}", "RES-2026-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rescueLocation").value("Bahia Concha"))
                .andExpect(jsonPath("$.status").value("IN_REHABILITATION"));

        verify(service).findByCode("RES-2026-001");
    }

    @Test
    void shouldReturn404WhenCaseDoesNotExist() throws Exception {
        when(service.findByCode("RES-999"))
                .thenThrow(new ResourceNotFoundException(
                        "Rescue case not found: RES-999"));

        mockMvc.perform(get("/api/rescue-cases/{code}", "RES-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Rescue case not found: RES-999"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturn500WhenUnexpectedErrorOccurs() throws Exception {
        when(service.findByCode("RES-500"))
                .thenThrow(new IllegalStateException("Connection to database lost"));

        mockMvc.perform(get("/api/rescue-cases/{code}", "RES-500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                // el detalle interno no se expone al cliente
                .andExpect(jsonPath("$.message")
                        .value("An unexpected error occurred"))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    // ---------------------------------------------------------------
    // GET /api/rescue-cases?status=...
    // ---------------------------------------------------------------

    @Test
    void shouldReturnCasesByStatus() throws Exception {
        when(service.findByStatus(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(
                        response(1L, RescueStatus.IN_REHABILITATION),
                        response(2L, RescueStatus.IN_REHABILITATION)
                ));

        mockMvc.perform(get("/api/rescue-cases")
                        .param("status", "IN_REHABILITATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("IN_REHABILITATION"))
                .andExpect(jsonPath("$[1].status").value("IN_REHABILITATION"));

        verify(service).findByStatus(RescueStatus.IN_REHABILITATION);
    }

    @Test
    void shouldReturn400WhenStatusParamIsInvalid() throws Exception {
        mockMvc.perform(get("/api/rescue-cases")
                        .param("status", "FLYING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.status")
                        .value("Invalid value: FLYING"));

        verify(service, never()).findByStatus(any());
    }

    @Test
    void shouldReturn400WhenStatusParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/rescue-cases"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details").isMap());

        verify(service, never()).findByStatus(any());
    }

    // ---------------------------------------------------------------
    // PATCH /api/rescue-cases/{caseCode}/status
    // ---------------------------------------------------------------

    @Test
    void shouldChangeStatus() throws Exception {
        when(service.changeStatus(
                eq("RES-001"),
                any(ChangeRescueStatusRequest.class)))
                .thenReturn(response(1L, RescueStatus.READY_FOR_RELEASE));

        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "READY_FOR_RELEASE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_RELEASE"));

        ArgumentCaptor<ChangeRescueStatusRequest> captor =
                ArgumentCaptor.forClass(ChangeRescueStatusRequest.class);

        verify(service).changeStatus(eq("RES-001"), captor.capture());

        assertThat(captor.getValue().status())
                .isEqualTo(RescueStatus.READY_FOR_RELEASE);
    }

    @Test
    void shouldReturn400WhenStatusIsMissingInBody() throws Exception {
        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.details.status")
                        .value("Status is required"));

        // la validacion falla antes de llegar al Service
        verify(service, never()).changeStatus(anyString(), any());
    }

    @Test
    void shouldReturn400WhenStatusEnumValueIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "FLYING"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Malformed or invalid JSON request"))
                .andExpect(jsonPath("$.details.body").exists());

        verify(service, never()).changeStatus(anyString(), any());
    }

    @Test
    void shouldReturn409WhenTransitionIsInvalid() throws Exception {
        when(service.changeStatus(eq("RES-001"), any()))
                .thenThrow(new BusinessRuleException(
                        "Invalid status transition for case RES-001: "
                                + "ADMITTED -> RELEASED"));

        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RELEASED"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Invalid status transition for case RES-001: "
                                + "ADMITTED -> RELEASED"))
                .andExpect(jsonPath("$.details").isMap());
    }
}
