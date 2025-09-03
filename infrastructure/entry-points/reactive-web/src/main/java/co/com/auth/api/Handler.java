package co.com.auth.api;

import co.com.auth.api.dto.CreateUserDto;
import co.com.auth.api.dto.ResponseDTO;
import co.com.auth.api.exception.DtoValidator;
import co.com.auth.api.mapper.UserMapper;
import co.com.auth.usecase.getuser.GetUserUseCase;
import co.com.auth.usecase.user.CreateUseCase;
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
        private final TransactionalOperator tx;
        private final UserMapper userMapper;
        private final DtoValidator dtoValidator;

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

}
