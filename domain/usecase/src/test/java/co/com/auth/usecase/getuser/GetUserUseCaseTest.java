package co.com.auth.usecase.getuser;

import co.com.auth.model.user.User;
import co.com.auth.model.user.gateways.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetUserUseCase getUserUseCase;


    private User sampleUser() {
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
                .rol(null) // solo id para buscar rol real
                .build();
    }

    @Test
    @DisplayName("OK: retorna el usuario cuando existe por número de documento")
    void findByNumberDocument() {
        User expected = sampleUser();
        String number = "123456789";

        when(userRepository.findByNumberDocument(number)).
                thenReturn(Mono.just(expected));

        StepVerifier.create(getUserUseCase.findByNumberDocument(number))
                .assertNext(found -> {
                    assertEquals(expected.getUserId(), found.getUserId());
                    assertEquals(number, found.getNumberDocument());
                    assertEquals(expected.getEmail(), found.getEmail());
                    assertEquals(expected.getPhone(),found.getPhone());
                })
                .verifyComplete();

        verify(userRepository).findByNumberDocument(number);

    }

    @Test
    @DisplayName("Debería fallar cuando no hay un usuario registrado con ese número de documento")
    void shouldErrorWhenUserNotFound() {
    // Arrange
        String number = "987654321";
        when(userRepository.findByNumberDocument(number))
                .thenReturn(Mono.empty());


        // Act & Assert
        StepVerifier.create(getUserUseCase.findByNumberDocument(number))
                .expectErrorSatisfies(ex -> {
                    assertEquals(IllegalArgumentException.class, ex.getClass());
                    assertEquals("Usuario no encontrado", ex.getMessage());
                })
                .verify();

        verify(userRepository).findByNumberDocument(number);
        verifyNoMoreInteractions(userRepository);
    }
}