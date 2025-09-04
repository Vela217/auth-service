package co.com.auth.api;

import co.com.auth.api.dto.CreateUserDto;
import co.com.auth.api.exception.DtoValidator;
import co.com.auth.api.mapper.UserMapper;
import co.com.auth.model.role.Role;
import co.com.auth.model.user.User;
import co.com.auth.usecase.getuser.GetUserUseCase;
import co.com.auth.usecase.user.CreateUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class HandlerTest {

    @Mock
    private CreateUseCase createUserUseCase;
    @Mock
    GetUserUseCase getUserUseCase;
    @Mock
    UserMapper userMapper;
    @Mock
    DtoValidator dtoValidator;
    @Mock
    TransactionalOperator tx;


    Handler handler;
    WebTestClient client;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @BeforeEach
    void setUp() {
        // Hacemos que la "transacción" no altere el publisher
        when(tx.transactional(org.mockito.ArgumentMatchers.<Mono<?>>any()))
                .thenAnswer(inv -> inv.getArgument(0));


        handler = new Handler(createUserUseCase, getUserUseCase, tx, userMapper, dtoValidator);

        // Router mínimo para testear el Handler (functional style)
        RouterFunction<ServerResponse> router = RouterFunctions.route()
                .POST("/users", handler::listenSaveUser)
                .GET("/users/{document}", handler::getByDocument)
                .build();

        client = WebTestClient.bindToRouterFunction(router).configureClient().build();
    }

    private CreateUserDto sampleDto() {
        return CreateUserDto.builder()
                .name("Juan")
                .lastName("Vela")
                .birthDate(LocalDate.of(1995, 1, 1))
                .address("Calle 123 #45-67")
                .email("nicolas@email.com")
                .roleId(2L)
                .phone("+573001234567")
                .baseSalary(BigDecimal.valueOf(200000000))
                .build();
    }

    private User sampleUser() {
        return User.builder()
                .userId("U-1")
                .name("Juan")
                .lastName("Vela")
                .birthDate(LocalDate.of(1995, 1, 1))
                .address("Calle 123 #45-67")
                .email("nicolas@email.com")
                .rol(Role.builder().idRol(2L).build())
                .phone("+573001234567")
                .baseSalary(BigDecimal.valueOf(200000000))
                .build();
    }


    @Test
    @DisplayName("listenSaveUser -> 201 Created con ResponseDTO esperado")
    void listenSaveUser_created() {
        // Arrange
        CreateUserDto reqDto = sampleDto();
        User toPersist = sampleUser();
        User persisted = toPersist; // suponemos que save devuelve el mismo con id

        // El validador devuelve el mismo dto (pasa validación)
        when(dtoValidator.validate(any(CreateUserDto.class)))
                .thenAnswer(inv -> Mono.just((CreateUserDto) inv.getArgument(0)));
        // Mapper a entidad
        when(userMapper.toEntity(any(CreateUserDto.class))).thenReturn(toPersist);
        // Caso de uso exitoso
        when(createUserUseCase.registerUser(eq(toPersist))).thenReturn(Mono.just(persisted));
        CreateUserDto respDto = sampleDto();
        when(userMapper.toDto(eq(persisted))).thenReturn(respDto);

        // Act & Assert (HTTP)
        client.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(reqDto) // Spring lo serializa con Jackson
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Usuario creado con exito")
                .jsonPath("$.statusCode").isEqualTo(201);

        // Interacciones esenciales
        verify(dtoValidator).validate(any(CreateUserDto.class));
        verify(userMapper).toEntity(any(CreateUserDto.class));
        verify(createUserUseCase).registerUser(userCaptor.capture());
        verify(userMapper).toDto(eq(persisted));
        verify(tx).transactional(any(Mono.class));

        User toSave = userCaptor.getValue();
        verifyNoMoreInteractions(dtoValidator, userMapper, createUserUseCase, getUserUseCase);
    }

    @Test
    @DisplayName("getByDocument -> 200 OK con ResponseDTO esperado")
    void getByDocument_ok() {
        // Arrange
        String doc = "123456789";
        User found = sampleUser();
        when(getUserUseCase.findByNumberDocument(doc)).thenReturn(Mono.just(found));
        when(userMapper.toDto(found)).thenReturn(sampleDto());
        when(tx.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        client.get()
                .uri("/users/{document}", doc)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Consulta exitosa")
                .jsonPath("$.statusCode").isEqualTo(200);

        verify(getUserUseCase).findByNumberDocument(doc);
        verify(userMapper).toDto(found);
        verify(tx).transactional(any(Mono.class));
        verifyNoMoreInteractions(getUserUseCase, userMapper);
    }

}