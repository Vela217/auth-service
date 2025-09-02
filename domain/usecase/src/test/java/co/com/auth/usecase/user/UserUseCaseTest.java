package co.com.auth.usecase.user;

import co.com.auth.model.role.Role;
import co.com.auth.model.role.gateways.RoleRepository;
import co.com.auth.model.user.User;
import co.com.auth.model.user.gateways.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private CreateUseCase useCase;

    @Captor
    private ArgumentCaptor<User> userCaptor;


    private User buildInputUser() {
        return User.builder()
                .userId("U-1")
                .numberDocument("123456789")
                .name("Juan")
                .lastName("Vela")
                .birthDate(LocalDate.of(1995, 1, 1))
                .address("Calle 123")
                .email("vela@email.com")
                .phone("3000000000")
                .baseSalary(new BigDecimal("6000000"))
                .rol(Role.builder().idRol(1L).build()) // solo id para buscar rol real
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
    @DisplayName("Debería registrar usuario cuando rol existe y email no está en uso")
    void shouldRegisterUserWhenRoleExistsAndEmailFree() {
        // Arrange
        User input = buildInputUser();
        Role foundRole = buildRepoRole();

        when(roleRepository.findById(1L)).thenReturn(Mono.just(foundRole));
        when(userRepository.existsByEmail("vela@email.com")).thenReturn(Mono.just(false));
        // Guardamos devolviendo el mismo objeto que entra (común en tests)
        when(userRepository.save(any(User.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert
        StepVerifier.create(useCase.registerUser(input))
                .assertNext(saved -> {
                    assertEquals("vela@email.com", saved.getEmail());
                    assertNotNull(saved.getRol());
                    assertEquals(1L, saved.getRol().getIdRol());
                    assertEquals("ADMIN", saved.getRol().getName());
                })
                .verifyComplete();

        // Verificar interacciones y que el rol usado sea el devuelto por el repo
        verify(roleRepository).findById(1L);
        verify(userRepository).existsByEmail("vela@email.com");
        verify(userRepository).save(userCaptor.capture());

        User toSave = userCaptor.getValue();
        // Como Role no tiene equals hashCode, validamos por identidad de referencia
        assertSame(foundRole, toSave.getRol(), "El rol dentro del User a guardar debe ser el rol resuelto del repositorio");
        verifyNoMoreInteractions(roleRepository, userRepository);
    }

    @Test
    @DisplayName("Debería fallar cuando el rol no existe")
    void shouldFailWhenRoleDoesNotExist() {
        // Arrange
        User input = buildInputUser();
        when(roleRepository.findById(1L)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(useCase.registerUser(input))
                .expectErrorSatisfies(ex -> {
                    assertInstanceOf(IllegalArgumentException.class, ex);
                    assertEquals("El rol no existe", ex.getMessage());
                })
                .verify();

        verify(roleRepository).findById(1L);
        // No debería consultar email ni intentar guardar
        verifyNoInteractions(userRepository);
        verifyNoMoreInteractions(roleRepository);
    }

    @Test
    @DisplayName("Debería fallar cuando el email ya está en uso")
    void shouldFailWhenEmailAlreadyInUse() {
        // Arrange
        User input = buildInputUser();
        Role foundRole = buildRepoRole();

        when(roleRepository.findById(1L)).thenReturn(Mono.just(foundRole));
        when(userRepository.existsByEmail("vela@email.com")).thenReturn(Mono.just(true));

        // Act & Assert
        StepVerifier.create(useCase.registerUser(input))
                .expectErrorSatisfies(ex -> {
                    assertInstanceOf(IllegalStateException.class, ex);
                    assertEquals("El correo ya esta en uso", ex.getMessage());
                })
                .verify();

        verify(roleRepository).findById(1L);
        verify(userRepository).existsByEmail("vela@email.com");
        verify(userRepository, never()).save(any());
        verifyNoMoreInteractions(roleRepository, userRepository);
    }


}