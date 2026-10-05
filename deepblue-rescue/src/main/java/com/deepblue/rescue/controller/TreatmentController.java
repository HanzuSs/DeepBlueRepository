package com.deepblue.rescue.controller;

import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.service.TreatmentService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/treatments")
@Tag(name = "Tratamientos", description = "Registro de tratamientos veterinarios")
public class TreatmentController {

    private final TreatmentService service;

    public TreatmentController(TreatmentService service) {
        this.service = service;
    }

    // POST /api/treatments -> 201 Created (se creo un nuevo Treatment)
    @PostMapping
    @Operation(summary = "Registrar un tratamiento", description = "Crea un tratamiento a partir de los datos enviados.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tratamiento creado"),
            @ApiResponse(responseCode = "400", description = "La solicitud no cumple el formato o validación esperados")
    })
    public ResponseEntity<TreatmentResponse> register(
            @Valid @RequestBody CreateTreatmentRequest request) {

        TreatmentResponse response = service.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
