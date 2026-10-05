package com.deepblue.rescue.controller;

import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.TreatmentEligibilityResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.service.AnimalService;
import com.deepblue.rescue.service.TreatmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/animals")
@Tag(name = "Animales", description = "Consultas de animales y tratamientos asociados")
public class AnimalController {

    private final AnimalService animalService;

    private final TreatmentService treatmentService;

    public AnimalController(AnimalService animalService,
                            TreatmentService treatmentService) {
        this.animalService = animalService;
        this.treatmentService = treatmentService;
    }

    // GET /api/animals/1
    @GetMapping("/{animalId}")
    @Operation(summary = "Consultar un animal", description = "Obtiene la información de un animal por su identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Animal encontrado"),
            @ApiResponse(responseCode = "400", description = "El identificador no tiene un formato válido")
    })
    public ResponseEntity<AnimalResponse> findByCode(
            @Parameter(description = "Identificador numérico del animal", example = "1")
            @PathVariable Long animalId) {

        return ResponseEntity.ok(
                animalService.findByCode(animalId)
        );
    }

    // GET /api/animals/in-rehabilitation
    // (la ruta literal tiene prioridad sobre /{animalId})
    @GetMapping("/in-rehabilitation")
    @Operation(summary = "Listar animales en rehabilitación")
    @ApiResponse(responseCode = "200", description = "Lista de animales que están en rehabilitación")
    public ResponseEntity<List<AnimalResponse>> findAnimalsInRehabilitation() {

        return ResponseEntity.ok(
                animalService.findAnimalsInRehabilitation()
        );
    }

    // GET /api/animals/1/treatments
    @GetMapping("/{animalId}/treatments")
    @Operation(summary = "Listar tratamientos de un animal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de tratamientos del animal"),
            @ApiResponse(responseCode = "400", description = "El identificador no tiene un formato válido")
    })
    public ResponseEntity<List<TreatmentResponse>> findTreatments(
            @Parameter(description = "Identificador numérico del animal", example = "1")
            @PathVariable Long animalId) {

        return ResponseEntity.ok(
                treatmentService.findByAnimalId(animalId)
        );
    }

    // GET /api/animals/1/treatment-eligibility
    @GetMapping("/{animalId}/treatment-eligibility")
    @Operation(summary = "Consultar elegibilidad para tratamiento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado de la evaluación de elegibilidad"),
            @ApiResponse(responseCode = "400", description = "El identificador no tiene un formato válido")
    })
    public ResponseEntity<TreatmentEligibilityResponse> canReceiveTreatment(
            @Parameter(description = "Identificador numérico del animal", example = "1")
            @PathVariable Long animalId) {

        boolean eligible = animalService.canReceiveTreatment(animalId);

        return ResponseEntity.ok(
                new TreatmentEligibilityResponse(animalId, eligible)
        );
    }
}
