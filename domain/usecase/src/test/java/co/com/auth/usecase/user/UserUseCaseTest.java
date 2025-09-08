package co.com.auth.usecase.user;

import co.com.auth.model.role.Role;
import co.com.auth.model.role.gateways.RoleRepository;
import co.com.auth.model.security.gateways.PasswordEncoderGateway;
import co.com.auth.model.user.User;
import co.com.auth.model.user.gateways.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoderGateway passwordEncoder;

    @InjectMocks
    private CreateUseCase useCase;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private static final String EMAIL = "vela@email.com";
    private static final String RAW_PASSWORD = "Secreta#123";
    private static final String ENCODED_PASSWORD = "{bcrypt}HASH";

    private User buildInputUser() {
        return User.builder()
                .userId("U-1")
                .numberDocument("123456789")
                .name("Juan")
                .lastName("Vela")
                .birthDate(LocalDate.of(1995, 1, 1))
                .address("Calle 123")
                .email(EMAIL)
                .phone("3000000000")
                .baseSalary(new BigDecimal("6000000"))
                .password(RAW_PASSWORD)                    // password cruda
                .rol(Role.builder().idRol(1L).build())     // solo id para resolver rol real
                .build();
    }

    private Role buildRepoRole() {
        return Role.builder()
                .idRol(1L)
                .name("ADMIN")
                .description("Administrador")
                .build();
    }

    @Test
    @DisplayName("OK: registra usuario cuando rol existe y email no está en uso (password codificada)")
    void shouldRegisterUserWhenRoleExistsAndEmailFree_withPasswordEncoded() {
        // Arrange
        User input = buildInputUser();
        Role foundRole = buildRepoRole();

        when(roleRepository.findById(1L)).thenReturn(Mono.just(foundRole));
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(Mono.just(ENCODED_PASSWORD));
        when(userRepository.existsByEmail(EMAIL)).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act
        var result = useCase.registerUser(input);

        // Assert
        StepVerifier.create(result)
                .assertNext(saved -> {
                    assertEquals(EMAIL, saved.getEmail());
                    assertEquals(ENCODED_PASSWORD, saved.getPassword(), "Debe guardarse la contraseña codificada");
                    assertNotNull(saved.getRol());
                    assertEquals(1L, saved.getRol().getIdRol());
                    assertEquals("ADMIN", saved.getRol().getName());
                })
                .verifyComplete();

        // Verificar orden lógico de interacciones
        InOrder inOrder = inOrder(roleRepository, passwordEncoder, userRepository);
        inOrder.verify(roleRepository).findById(1L);
        inOrder.verify(passwordEncoder).encode(RAW_PASSWORD);
        inOrder.verify(userRepository).existsByEmail(EMAIL);
        inOrder.verify(userRepository).save(userCaptor.capture());

        User toSave = userCaptor.getValue();
        assertSame(foundRole, toSave.getRol(), "El rol en el User a guardar debe ser el resuelto del repositorio");
        assertEquals(ENCODED_PASSWORD, toSave.getPassword());

        verifyNoMoreInteractions(roleRepository, passwordEncoder, userRepository);
    }

    @Test
    @DisplayName("Error: el rol no existe (no codifica ni consulta email/guarda)")
    void shouldFailWhenRoleDoesNotExist() {
        // Arrange
        User input = buildInputUser();
        when(roleRepository.findById(1L)).thenReturn(Mono.empty());

        // Act
        var result = useCase.registerUser(input);

        // Assert
        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> {
                    assertInstanceOf(IllegalArgumentException.class, ex);
                    assertEquals("El rol no existe", ex.getMessage());
                })
                .verify();

        verify(roleRepository).findById(1L);
        verifyNoInteractions(passwordEncoder, userRepository);
        verifyNoMoreInteractions(roleRepository);
    }

    @Test
    @DisplayName("Error: email ya está en uso (se codifica pero NO se guarda)")
    void shouldFailWhenEmailAlreadyInUse_passwordEncodedButNotSaved() {
        // Arrange
        User input = buildInputUser();
        Role foundRole = buildRepoRole();

        when(roleRepository.findById(1L)).thenReturn(Mono.just(foundRole));
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(Mono.just(ENCODED_PASSWORD));
        when(userRepository.existsByEmail(EMAIL)).thenReturn(Mono.just(true));

        // Act
        var result = useCase.registerUser(input);

        // Assert
        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> {
                    assertInstanceOf(IllegalStateException.class, ex);
                    assertEquals("El correo ya está en uso", ex.getMessage()); // ¡ojo al acento!
                })
                .verify();

        InOrder inOrder = inOrder(roleRepository, passwordEncoder, userRepository);
        inOrder.verify(roleRepository).findById(1L);
        inOrder.verify(passwordEncoder).encode(RAW_PASSWORD);     // en tu flujo actual se codifica antes de validar email
        inOrder.verify(userRepository).existsByEmail(EMAIL);
        verify(userRepository, never()).save(any());

        verifyNoMoreInteractions(roleRepository, passwordEncoder, userRepository);
    }

}
