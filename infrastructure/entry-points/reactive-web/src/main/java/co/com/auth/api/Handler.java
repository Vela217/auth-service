package co.com.auth.api;

import co.com.auth.api.dto.CreateUserDto;
import co.com.auth.api.dto.LoginRequestDto;
import co.com.auth.api.dto.LoginResponseDto;
import co.com.auth.api.dto.ResponseDTO;
import co.com.auth.api.exception.DtoValidator;
import co.com.auth.api.mapper.UserMapper;
import co.com.auth.usecase.getuser.GetUserUseCase;
import co.com.auth.usecase.login.LoginUseCase;
import co.com.auth.usecase.user.CreateUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Usuarios", description = "Gestión de usuarios")
public class Handler {

        private final CreateUseCase createUserUseCase;
        private final GetUserUseCase getUserUseCase;
        private final LoginUseCase loginUseCase;
        private final TransactionalOperator tx;
        private final UserMapper userMapper;
        private final DtoValidator dtoValidator;

        @Operation(summary = "Registrar usuario", description = "Crea un usuario validando campos requeridos y unicidad de email.", requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateUserDto.class))), responses = {
                @ApiResponse(responseCode = "201", description = "Usuario creado con exito", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
                @ApiResponse(responseCode = "400", description = "Error en los datos de entrada", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                    {"success":false,"message":"Validation failed","code":400,
                     "errors":[{"field":"name","message":"name is required"}]}"""))),
                @ApiResponse(responseCode = "409", description = "Conflicto email en uso", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                    {"success":false,"message":"Email en uso en ","code":409}"""))),
                @ApiResponse(responseCode = "500", description = "Ocurrioun error inesperado")
        })
        public Mono<ServerResponse> listenSaveUser(ServerRequest req) {
                return req.bodyToMono(CreateUserDto.class)
                                .doOnNext(dto -> log.info("CreateUserDto recibido: {}", dto))
                                .flatMap(dtoValidator::validate)
                                .map(userMapper::toEntity)
                                .flatMap(u -> createUserUseCase.registerUser(u).as(tx::transactional))
                                .doOnNext(saved -> log.info("Usuario persistido ={}",
                                                saved.toString()))
                                .map(userMapper::toDto)
                                .flatMap(userResp -> ServerResponse.status(HttpStatus.CREATED)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(ResponseDTO.builder()
                                                                .success(true)
                                                                .message("Usuario creado con exito")
                                                                .statusCode(HttpStatus.CREATED.value())
                                                                .data(userResp).build()));
        }

        public Mono<ServerResponse> getByDocument(ServerRequest req) {
                final String doc = req.pathVariable("document");

                return getUserUseCase.findByNumberDocument(doc).as(tx::transactional)
                                .doOnSubscribe(s -> log.info("Buscando usuario por documento={}", doc))
                                .map(userMapper::toDto)
                                .doOnNext(dto -> log.info("Usuario encontrado para documento={} -> {}", doc, dto))
                                .flatMap(dto -> ServerResponse.ok()
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(ResponseDTO.builder()
                                                                .success(true)
                                                                .message("Consulta exitosa")
                                                                .statusCode(HttpStatus.OK.value())
                                                                .data(dto).build()));
        }

        public Mono<ServerResponse> login(ServerRequest req){

                return req.bodyToMono(LoginRequestDto.class)
                        .flatMap(d -> {
                                log.info("email entrante: {}", d.email());
                                return dtoValidator.validate(d);
                        })
                        .flatMap(d-> loginUseCase.login(d.email(),d.password()).as(tx::transactional))
                        .map(t-> new LoginResponseDto(t.getToken(), t.getExpiresAt().getEpochSecond()))
                        .doOnSuccess(t -> log.info("token={} expEpoch={}",
                                t.accessToken(),  t.expiresAtEpochSeconds()))
                        .flatMap(dto -> ServerResponse.status(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ResponseDTO.builder()
                                        .success(true)
                                        .message("Usuario logueado exitosamente")
                                        .statusCode(HttpStatus.OK.value())
                                        .data(dto).build()));
        }

}
