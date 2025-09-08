package co.com.auth.usecase.login;

import co.com.auth.model.role.Role;
import co.com.auth.model.role.gateways.RoleRepository;
import co.com.auth.model.security.AuthToken;
import co.com.auth.model.security.gateways.PasswordEncoderGateway;
import co.com.auth.model.security.gateways.TokenProvider;
import co.com.auth.model.user.User;
import co.com.auth.model.user.gateways.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class LoginUseCaseTest {

    @Mock private UserRepository users;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoderGateway passwords;
    @Mock private TokenProvider tokens;

    @InjectMocks
    private LoginUseCase useCase;

    private final String EMAIL = "user@test.com";
    private final String RAW_PASSWORD = "123456";
    private final String STORED_PASSWORD = "{bcrypt}hash";

    private User baseUserWithRole(Long roleId) {
        return User.builder()
                .userId("u1")
                .email(EMAIL)
                .password(STORED_PASSWORD)
                .rol(Role.builder().idRol(roleId).name("BASIC").build())
                .build();
    }

    @Nested
    @DisplayName("login() - casos felices y de error")
    class LoginCases {

        @Test
        @DisplayName("Éxito: retorna token cuando credenciales y rol son válidos")
        void login_success_returnsToken() {
            // Arrange
            var user = baseUserWithRole(10L);
            var fullRole = Role.builder().idRol(10L).name("FULL").description("Full role").build();
            var authToken = mock(AuthToken.class);

            when(users.findByEmail(EMAIL)).thenReturn(Mono.just(user));
            when(passwords.matches(RAW_PASSWORD, STORED_PASSWORD)).thenReturn(Mono.just(true));
            when(roleRepository.findById(10L)).thenReturn(Mono.just(fullRole));
            when(tokens.createAccessToken(any(User.class))).thenReturn(Mono.just(authToken));

            // Act
            var result = useCase.login(EMAIL, RAW_PASSWORD);

            // Assert
            StepVerifier.create(result)
                    .expectNext(authToken)
                    .verifyComplete();

            // Capturamos el User con el que se generó el token para asegurar que trae el rol enriquecido
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(tokens).createAccessToken(userCaptor.capture());
            User userPassed = userCaptor.getValue();
            assertNotNull(userPassed.getRol());
            assertEquals(fullRole.getIdRol(), userPassed.getRol().getIdRol());
            assertEquals(fullRole.getName(), userPassed.getRol().getName());

            verify(users).findByEmail(EMAIL);
            verify(passwords).matches(RAW_PASSWORD, STORED_PASSWORD);
            verify(roleRepository).findById(10L);
            verifyNoMoreInteractions(users, passwords, roleRepository, tokens);
        }

        @Test
        @DisplayName("Error: usuario no existe → IllegalArgumentException (credenciales inválidas)")
        void login_userNotFound_emitsBadCredentials() {
            // Arrange
            when(users.findByEmail(EMAIL)).thenReturn(Mono.empty());

            // Act
            var result = useCase.login(EMAIL, RAW_PASSWORD);

            // Assert
            StepVerifier.create(result)
                    .expectErrorSatisfies(ex -> {
                        assertTrue(ex instanceof IllegalArgumentException);
                        assertEquals("Usuario o contraseña incorrecta", ex.getMessage());
                    })
                    .verify();

            verify(users).findByEmail(EMAIL);
            verifyNoMoreInteractions(users, passwords, roleRepository, tokens);
        }

        @Test
        @DisplayName("Error: contraseña incorrecta → IllegalArgumentException (credenciales inválidas)")
        void login_wrongPassword_emitsBadCredentials() {
            // Arrange
            var user = baseUserWithRole(10L);
            when(users.findByEmail(EMAIL)).thenReturn(Mono.just(user));
            when(passwords.matches(RAW_PASSWORD, STORED_PASSWORD)).thenReturn(Mono.just(false));

            // Act
            var result = useCase.login(EMAIL, RAW_PASSWORD);

            // Assert
            StepVerifier.create(result)
                    .expectErrorSatisfies(ex -> {
                        assertTrue(ex instanceof IllegalArgumentException);
                        assertEquals("Usuario o contraseña incorrecta", ex.getMessage());
                    })
                    .verify();

            verify(users).findByEmail(EMAIL);
            verify(passwords).matches(RAW_PASSWORD, STORED_PASSWORD);
            verifyNoMoreInteractions(users, passwords, roleRepository, tokens);
        }

        @Test
        @DisplayName("Error: usuario sin rol asignado → IllegalStateException")
        void login_userWithoutRole_emitsIllegalState() {
            // Arrange
            var user = User.builder()
                    .userId("u1").email(EMAIL).password(STORED_PASSWORD)
                    .rol(null) // sin rol
                    .build();

            when(users.findByEmail(EMAIL)).thenReturn(Mono.just(user));
            when(passwords.matches(RAW_PASSWORD, STORED_PASSWORD)).thenReturn(Mono.just(true));

            // Act
            var result = useCase.login(EMAIL, RAW_PASSWORD);

            // Assert
            StepVerifier.create(result)
                    .expectErrorSatisfies(ex -> {
                        assertTrue(ex instanceof IllegalStateException);
                        assertEquals("Usuario sin rol asignado", ex.getMessage());
                    })
                    .verify();

            verify(users).findByEmail(EMAIL);
            verify(passwords).matches(RAW_PASSWORD, STORED_PASSWORD);
            verifyNoMoreInteractions(users, passwords, roleRepository, tokens);
        }

        @Test
        @DisplayName("Error: rol no encontrado en repositorio → IllegalStateException")
        void login_roleNotFound_emitsIllegalState() {
            // Arrange
            var user = baseUserWithRole(99L);
            when(users.findByEmail(EMAIL)).thenReturn(Mono.just(user));
            when(passwords.matches(RAW_PASSWORD, STORED_PASSWORD)).thenReturn(Mono.just(true));
            when(roleRepository.findById(99L)).thenReturn(Mono.empty());

            // Act
            var result = useCase.login(EMAIL, RAW_PASSWORD);

            // Assert
            StepVerifier.create(result)
                    .expectErrorSatisfies(ex -> {
                        assertTrue(ex instanceof IllegalStateException);
                        assertEquals("Rol no encontrado", ex.getMessage());
                    })
                    .verify();

            verify(users).findByEmail(EMAIL);
            verify(passwords).matches(RAW_PASSWORD, STORED_PASSWORD);
            verify(roleRepository).findById(99L);
            verifyNoMoreInteractions(users, passwords, roleRepository, tokens);
        }

        @Test
        @DisplayName("Error: falla generando token → propaga excepción")
        void login_tokenProviderFailure_propagates() {
            // Arrange
            var user = baseUserWithRole(10L);
            var fullRole = Role.builder().idRol(10L).name("FULL").build();

            when(users.findByEmail(EMAIL)).thenReturn(Mono.just(user));
            when(passwords.matches(RAW_PASSWORD, STORED_PASSWORD)).thenReturn(Mono.just(true));
            when(roleRepository.findById(10L)).thenReturn(Mono.just(fullRole));
            when(tokens.createAccessToken(any(User.class)))
                    .thenReturn(Mono.error(new RuntimeException("token error")));

            // Act
            var result = useCase.login(EMAIL, RAW_PASSWORD);

            // Assert
            StepVerifier.create(result)
                    .expectErrorSatisfies(ex -> {
                        assertTrue(ex instanceof RuntimeException);
                        assertEquals("token error", ex.getMessage());
                    })
                    .verify();

            verify(users).findByEmail(EMAIL);
            verify(passwords).matches(RAW_PASSWORD, STORED_PASSWORD);
            verify(roleRepository).findById(10L);
            verify(tokens).createAccessToken(any(User.class));
            verifyNoMoreInteractions(users, passwords, roleRepository, tokens);
        }
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(users, roleRepository, passwords, tokens);
    }
}
