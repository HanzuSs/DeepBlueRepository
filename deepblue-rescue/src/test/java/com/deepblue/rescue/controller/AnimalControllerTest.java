package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.AnimalService;
import com.deepblue.rescue.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnimalController.class)
@Import(GlobalExceptionHandler.class)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    @MockitoBean
    private TreatmentService treatmentService;

    private AnimalResponse animal(Long id, RescueStatus status) {
        return new AnimalResponse(
                id,
                id,
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE,
                101L,
                status
        );
    }

    private TreatmentResponse treatment(Long id, TreatmentType type) {
        return new TreatmentResponse(
                id,
                1L,
                2L,
                LocalDateTime.of(2026, 8, 21, 9, 0),
                type,
                "Cleaning and treatment of flipper injury."
        );
    }

    // ---------------------------------------------------------------
    // GET /api/animals/{animalId}
    // ---------------------------------------------------------------

    @Test
    void shouldReturnAnimalByCode() throws Exception {
        when(animalService.findByCode(1L))
                .thenReturn(animal(1L, RescueStatus.IN_REHABILITATION));

        mockMvc.perform(get("/api/animals/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.commonName").value("Green Sea Turtle"))
                .andExpect(jsonPath("$.sex").value("FEMALE"))
                .andExpect(jsonPath("$.rescueStatus").value("IN_REHABILITATION"));

        verify(animalService).findByCode(1L);
    }

    @Test
    void shouldReturn404WhenAnimalDoesNotExist() throws Exception {
        when(animalService.findByCode(999L))
                .thenThrow(new ResourceNotFoundException(
                        "Animal not found: 999"));

        mockMvc.perform(get("/api/animals/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Animal not found: 999"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturn400WhenAnimalIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/animals/{id}", "AN-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.animalId")
                        .value("Invalid value: AN-001"));

        verify(animalService, never()).findByCode(anyLong());
    }

    // ---------------------------------------------------------------
    // GET /api/animals/in-rehabilitation
    // ---------------------------------------------------------------

    @Test
    void shouldReturnAnimalsInRehabilitation() throws Exception {
        when(animalService.findAnimalsInRehabilitation())
                .thenReturn(List.of(
                        animal(1L, RescueStatus.IN_REHABILITATION),
                        animal(2L, RescueStatus.IN_REHABILITATION)
                ));

        mockMvc.perform(get("/api/animals/in-rehabilitation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rescueStatus").value("IN_REHABILITATION"))
                .andExpect(jsonPath("$[1].rescueStatus").value("IN_REHABILITATION"));

        verify(animalService).findAnimalsInRehabilitation();

        // la ruta literal no debe ser capturada por /{animalId}
        verify(animalService, never()).findByCode(anyLong());
    }

    // ---------------------------------------------------------------
    // GET /api/animals/{animalId}/treatments
    // ---------------------------------------------------------------

    @Test
    void shouldReturnAnimalTreatments() throws Exception {
        when(treatmentService.findByAnimalId(1L))
                .thenReturn(List.of(
                        treatment(10L, TreatmentType.WOUND_CARE),
                        treatment(11L, TreatmentType.HYDRATION)
                ));

        mockMvc.perform(get("/api/animals/{id}/treatments", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("WOUND_CARE"))
                .andExpect(jsonPath("$[1].type").value("HYDRATION"));

        verify(treatmentService).findByAnimalId(1L);
    }

    // ---------------------------------------------------------------
    // GET /api/animals/{animalId}/treatment-eligibility
    // ---------------------------------------------------------------

    @Test
    void shouldReturnTreatmentEligibility() throws Exception {
        when(animalService.canReceiveTreatment(1L)).thenReturn(true);

        mockMvc.perform(get("/api/animals/{id}/treatment-eligibility", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalId").value(1))
                .andExpect(jsonPath("$.eligible").value(true));

        verify(animalService).canReceiveTreatment(1L);
    }

    @Test
    void shouldReturnNotEligibleWhenAnimalCannotReceiveTreatment()
            throws Exception {

        when(animalService.canReceiveTreatment(2L)).thenReturn(false);

        mockMvc.perform(get("/api/animals/{id}/treatment-eligibility", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalId").value(2))
                .andExpect(jsonPath("$.eligible").value(false));

        verify(animalService).canReceiveTreatment(2L);
    }

    @Test
    void shouldReturn404WhenCheckingEligibilityOfUnknownAnimal()
            throws Exception {

        when(animalService.canReceiveTreatment(999L))
                .thenThrow(new ResourceNotFoundException(
                        "Animal not found: 999"));

        mockMvc.perform(get("/api/animals/{id}/treatment-eligibility", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Animal not found: 999"))
                .andExpect(jsonPath("$.details").isMap());

        verify(animalService).canReceiveTreatment(999L);
    }
}
