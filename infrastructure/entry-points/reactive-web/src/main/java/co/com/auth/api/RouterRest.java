package co.com.auth.api;

import co.com.auth.api.dto.CreateUserDto;
import co.com.auth.api.dto.LoginRequestDto;
import co.com.auth.api.dto.ResponseDTO;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.web.bind.annotation.RequestMethod;
@Configuration
public class RouterRest {
    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/usuarios",
                    method = RequestMethod.POST,
                    beanClass = Handler.class,
                    beanMethod = "listenSaveUser",
                    operation = @Operation(
                            operationId = "crearUsuario",
                            summary = "Registrar usuario",
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = CreateUserDto.class)
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "Usuario creado con exito",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(example = """
                                {
                                  "success": true,
                                  "message": "Usuario creado con exito",
                                  "code": 201,
                                  "response": {
                                    "userId": "5ae98718-9a56-45d5-b12b-9a2b72318264",
                                    "numberDocument": "12345678",
                                    "name": "Juan",
                                    "lastName": "Vela",
                                    "birthDate": "1990-01-01",
                                    "address": "Calle 123 #45-67",
                                    "email": "vela217@email.com",
                                    "phone": "+573001234567",
                                    "roleId": 1,
                                    "baseSalary": 2500000
                                  }
                                }
                            """)
                                            )
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Error en datos de entrada",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(example = """
                                {
                                  "success": false,
                                  "message": "Invalid request payload",
                                  "code": 400,
                                  "response": {
                                    "errors": [
                                      {"message": "name is required", "field": "name"},
                                      {"message": "no debe ser nulo", "field": "name"},
                                      {"message": "numberDocument is required", "field": "numberDocument"},
                                      {"message": "lastName is required", "field": "lastName"}
                                    ]
                                  }
                                }
                            """)
                                            )
                                    ),
                                    @ApiResponse(
                                            responseCode = "409",
                                            description = "Conflicto email en uso",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(example = """
                                {
                                  "success": false,
                                  "message": "El correo ya esta en uso",
                                  "code": 409,
                                  "response": null
                                }
                            """)
                                            )
                                    ),
                                    @ApiResponse(
                                            responseCode = "500",
                                            description = "Error inesperado",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(example = """
                                {
                                  "success": false,
                                  "message": "An unexpected error occurred",
                                  "code": 500,
                                  "response": null
                                }
                            """)

                                            )
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/usuarios/{document}",
                    method = RequestMethod.GET,
                    beanClass = Handler.class,
                    beanMethod = "getByDocument",
                    operation = @Operation(
                            operationId = "consultarUsuarioPorDocumento",
                            summary = "Consultar usuario por documento",
                            parameters = {
                                    @Parameter(
                                            name = "document",
                                            in = ParameterIn.PATH,      // 👈 importante: PATH, no QUERY
                                            required = true,
                                            description = "Número de documento del usuario",
                                            schema = @Schema(type = "string", example = "123456789")
                                    )
                            },
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Usuario encontrado",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(example = """
                                                    {
                                                      "success": true,
                                                      "message": "Consulta exitosa",
                                                      "code": 200,
                                                      "response": {
                                                        "userId": "5ae98718-9a56-45d5-b12b-9a2b72318264",
                                                        "numberDocument": "12345678",
                                                        "name": "Juan",
                                                        "lastName": "Vela"
                                                      }
                                                    }
                                                    """)
                                            )
                                    ),
                                    @ApiResponse(
                                            responseCode = "404",
                                            description = "Usuario no encontrado",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(example = """
                                                    {
                                                      "success": false,
                                                      "message": "Usuario no encontrado",
                                                      "code": 404,
                                                      "response": null
                                                    }
                                                    """)
                                            )
                                    )
                            }
                    )
            ),
            // -------- Login --------
            @RouterOperation(
                    path = "/api/v1/login",
                    method = RequestMethod.POST,
                    beanClass = Handler.class,
                    beanMethod = "login",
                    operation = @Operation(
                            operationId = "login",
                            summary = "Autenticación de usuario (login)",
                            description = "Valida credenciales y retorna un JWT con fecha de expiración.",
                            security = {}, // <- esta operación NO requiere autenticación en la documentación
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = LoginRequestDto.class),
                                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    value = """
                                  { "email": "user@test.com", "password": "123456" }
                                  """
                                            )
                                    )
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Login exitoso",
                                            content = @Content(mediaType = "application/json",
                                                    schema = @Schema(implementation = ResponseDTO.class),
                                                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                            value = """
                                      {
                                        "success": true,
                                        "message": "Usuario logueado exitosamente",
                                        "statusCode": 200,
                                        "data": {
                                          "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
                                          "expiresAtEpochSeconds": 1725000000
                                        }
                                      }
                                      """
                                                    ))),
                                    @ApiResponse(responseCode = "400", description = "Solicitud inválida",
                                            content = @Content(mediaType = "application/json",
                                                    schema = @Schema(implementation = ResponseDTO.class),
                                                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                            value = """
                                                                    {
                                                                         "statusCode": 400,
                                                                         "success": false,
                                                                         "message": "Validation failed",
                                                                         "data": {
                                                                             "path": "/api/v1/login",
                                                                             "timestamp": "2025-09-07T22:45:12.072294700-05:00",
                                                                             "correlationId": "df1e16c2-004f-4cca-93ec-1e4c235b41f4",
                                                                             "details": {
                                                                                 "errors": [
                                                                                     {
                                                                                         "message": "no debe estar vacío",
                                                                                         "field": "email"
                                                                                     }
                                                                                 ]
                                                                             }
                                                                         }
                                                                     }
                                      """
                                                    ))),
                                    @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
                                            content = @Content(mediaType = "application/json",
                                                    schema = @Schema(implementation = ResponseDTO.class),
                                                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                            value = """
                                      {
                                        "success": false,
                                        "message": "Token inválido o no provisto",
                                        "statusCode": 401,
                                        "data": null
                                      }
                                      """
                                                    ))),
                                    @ApiResponse(responseCode = "500", description = "Error inesperado")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(
                POST("/api/v1/usuarios"),handler::listenSaveUser
        ).andRoute(GET("/api/v1/usuarios/{document}"), handler::getByDocument).
                andRoute(POST("/api/v1/login"), handler::login);
    }
}

