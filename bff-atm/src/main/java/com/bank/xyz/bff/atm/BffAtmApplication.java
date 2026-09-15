package com.bank.xyz.bff.atm;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Las dos cabeceras se declaran como esquemas de seguridad para que Swagger UI
// muestre el boton Authorize. La autenticacion la resuelve FiltroTerminalAtm,
// que es anterior al controlador, asi que sin esta declaracion la interfaz no
// tendria donde pedir la credencial y todo se probaria con 401.
@SpringBootApplication
@SecuritySchemes({
        @SecurityScheme(name = "terminal", type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER, paramName = "X-ATM-Terminal",
                description = "Identificador del cajero, por ejemplo ATM-001"),
        @SecurityScheme(name = "clave", type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER, paramName = "X-ATM-Key",
                description = "Clave del terminal")
})
@OpenAPIDefinition(info = @Info(
        title = "Banco XYZ - BFF Cajeros",
        version = "1.0",
        description = "Backend dedicado a los cajeros automaticos. Superficie minima, "
                + "autenticacion por terminal, validaciones de denominacion y auditoria. "
                + "Usa el boton Authorize para cargar las cabeceras X-ATM-Terminal y X-ATM-Key."))
public class BffAtmApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffAtmApplication.class, args);
    }
}
