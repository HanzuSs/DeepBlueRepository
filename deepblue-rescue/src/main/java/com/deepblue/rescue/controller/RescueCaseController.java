package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.service.RescueCaseService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rescue-cases")
@Tag(name = "Casos de rescate", description = "Consulta y actualización del estado de casos de rescate")
public class RescueCaseController {

    private final RescueCaseService service;

    public RescueCaseController(RescueCaseService service) {
        this.service = service;
    }

    // GET /api/rescue-cases/RES-2026-001
    @GetMapping("/{caseCode}")
    @Operation(summary = "Consultar un caso de rescate", description = "Obtiene los detalles de un caso mediante su código.")
    @ApiResponse(responseCode = "200", description = "Caso de rescate encontrado")
    public ResponseEntity<RescueCaseResponse> findByCode(
            @Parameter(description = "Código único del caso de rescate", example = "RES-2026-001")
            @PathVariable String caseCode) {

        return ResponseEntity.ok(
                service.findByCode(caseCode)
        );
    }

    // GET /api/rescue-cases?status=IN_REHABILITATION
    @GetMapping
    @Operation(summary = "Listar casos por estado", description = "Filtra los casos de rescate por un estado del catálogo RescueStatus.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de casos que coinciden con el estado"),
            @ApiResponse(responseCode = "400", description = "El estado no es un valor válido de RescueStatus")
    })
    public ResponseEntity<List<RescueCaseResponse>> findByStatus(
            @Parameter(description = "Estado por el que se filtran los casos", example = "IN_REHABILITATION")
            @RequestParam RescueStatus status) {

        return ResponseEntity.ok(
                service.findByStatus(status)
        );
    }

    // PATCH /api/rescue-cases/RES-2026-001/status
    @PatchMapping("/{caseCode}/status")
    @Operation(summary = "Cambiar el estado de un caso", description = "Actualiza el estado del caso indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado; devuelve el caso actualizado"),
            @ApiResponse(responseCode = "400", description = "La solicitud no cumple el formato o validación esperados")
    })
    public ResponseEntity<RescueCaseResponse> changeStatus(
            @Parameter(description = "Código único del caso de rescate", example = "RES-2026-001")
            @PathVariable String caseCode,
            @Valid @RequestBody ChangeRescueStatusRequest request) {

        return ResponseEntity.ok(
                service.changeStatus(caseCode, request)
        );
    }
}
