package com.deepblue.rescue.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Contrato unico para todos los errores de la API.
 *
 * @param timestamp momento en que ocurrio el error
 * @param status    codigo HTTP numerico (400, 404, 409, 500)
 * @param error     descripcion estandar del codigo (Bad Request, Not Found, ...)
 * @param message   descripcion especifica del error
 * @param details   informacion adicional (por ejemplo, error por campo); vacio si no aplica
 */
public record ErrorResponse(

        LocalDateTime timestamp,

        int status,

        String error,

        String message,

        Map<String, String> details

) {
}
