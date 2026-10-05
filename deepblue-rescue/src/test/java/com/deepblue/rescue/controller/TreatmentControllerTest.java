package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TreatmentController.class)
@Import(GlobalExceptionHandler.class)
class TreatmentControllerTest {

    private static final String VALID_BODY = """
            {
              "animalId": 1,
              "specialistId": 2,
              "performedAt": "2026-08-21T09:00:00",
              "type": "WOUND_CARE",
              "description": "Cleaning and treatment of flipper injury."
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TreatmentService service;

    // ---------------------------------------------------------------
    // POST /api/treatments
    // ---------------------------------------------------------------

    @Test
    void shouldCreateTreatment() throws Exception {
        TreatmentResponse response = new TreatmentResponse(
                100L,
                1L,
                2L,
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning and treatment of flipper injury."
        );

        when(service.register(any(CreateTreatmentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.animalId").value(1))
                .andExpect(jsonPath("$.specialistId").value(2))
                .andExpect(jsonPath("$.type").value("WOUND_CARE"));

        // el JSON se convirtio correctamente en el DTO que recibe el Service
        ArgumentCaptor<CreateTreatmentRequest> captor =
                ArgumentCaptor.forClass(CreateTreatmentRequest.class);

        verify(service).register(captor.capture());

        CreateTreatmentRequest sent = captor.getValue();
        assertThat(sent.animalId()).isEqualTo(1L);
        assertThat(sent.specialistId()).isEqualTo(2L);
        assertThat(sent.performedAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 21, 9, 0));
        assertThat(sent.type()).isEqualTo(TreatmentType.WOUND_CARE);
    }

    @Test
    void shouldReturn400WhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "animalId": null,
                                  "specialistId": null,
                                  "type": null,
                                  "description": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.details.animalId")
                        .value("Animal id is required"))
                .andExpect(jsonPath("$.details.specialistId")
                        .value("Specialist id is required"))
                .andExpect(jsonPath("$.details.performedAt")
                        .value("Treatment date is required"))
                .andExpect(jsonPath("$.details.type")
                        .value("Treatment type is required"))
                .andExpect(jsonPath("$.details.description")
                        .value("Description is required"));

        verify(service, never()).register(any());
    }

    @Test
    void shouldReturn400WhenDescriptionIsTooShort() throws Exception {
        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "animalId": 1,
                                  "specialistId": 2,
                                  "performedAt": "2026-08-21T09:00:00",
                                  "type": "WOUND_CARE",
                                  "description": "Short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.details.description").value(
                        "Description must contain between 10 and 500 characters"));

        verify(service, never()).register(any());
    }

    @Test
    void shouldReturn400WhenTreatmentDateIsInTheFuture() throws Exception {
        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "animalId": 1,
                                  "specialistId": 2,
                                  "performedAt": "2999-01-01T09:00:00",
                                  "type": "WOUND_CARE",
                                  "description": "Cleaning and treatment of flipper injury."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.details.performedAt")
                        .value("Treatment date cannot be in the future"));

        verify(service, never()).register(any());
    }

    @Test
    void shouldReturn400WhenTreatmentTypeIsNotAValidEnumValue()
            throws Exception {

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "animalId": 1,
                                  "specialistId": 2,
                                  "performedAt": "2026-08-21T09:00:00",
                                  "type": "FLYING",
                                  "description": "Cleaning and treatment of flipper injury."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Malformed or invalid JSON request"))
                .andExpect(jsonPath("$.details.body").exists());

        verify(service, never()).register(any());
    }

    @Test
    void shouldReturn404WhenAnimalDoesNotExist() throws Exception {
        when(service.register(any(CreateTreatmentRequest.class)))
                .thenThrow(new ResourceNotFoundException(
                        "Animal not found: 999"));

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "animalId": 999,
                                  "specialistId": 2,
                                  "performedAt": "2026-08-21T09:00:00",
                                  "type": "WOUND_CARE",
                                  "description": "Cleaning and treatment of flipper injury."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Animal not found: 999"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturn409WhenBusinessRuleIsViolated() throws Exception {
        when(service.register(any(CreateTreatmentRequest.class)))
                .thenThrow(new BusinessRuleException(
                        "Cannot register treatment because the rescue case is RELEASED."));

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Cannot register treatment because the rescue case is RELEASED."))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturn405WhenHttpMethodIsNotSupported() throws Exception {
        // /api/treatments solo expone POST: no debe terminar en un 500
        mockMvc.perform(get("/api/treatments"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.details").isMap());
    }
}
