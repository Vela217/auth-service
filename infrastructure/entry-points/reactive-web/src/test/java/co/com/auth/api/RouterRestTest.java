package co.com.auth.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@ExtendWith(MockitoExtension.class)
class RouterRestTest {

    private Handler handler;                        // mock (dep. del router)
    private RouterFunction<ServerResponse> router;  // SUT
    private WebTestClient client;                   // cliente HTTP embebido

    @BeforeEach
    void setUp() {
        // AAA — Arrange (común a todas): router + client, sin stubs todavía
        handler = mock(Handler.class);
        router  = new RouterRest().routerFunction(handler);
        client  = WebTestClient.bindToRouterFunction(router)
                .configureClient()
                .build();
    }


    @Test
    @DisplayName("POST /api/v1/usuarios (Accept: application/json) enruta a handler.listenSaveUser -> 201")
    void postUsuarios_routesToListenSaveUser_201() {
        when(handler.listenSaveUser(any()))
                .thenReturn(ServerResponse.status(HttpStatus.CREATED).build());
        client.post()
                .uri("/api/v1/usuarios")
                .accept(MediaType.APPLICATION_JSON)  // predicado exigido por el router
                .bodyValue("{}")
                .exchange()
                .expectStatus().isCreated();
        verify(handler).listenSaveUser(any());
        verifyNoMoreInteractions(handler);
    }


    @Test
    @DisplayName("GET /api/v1/usuarios/{document} enruta a handler.getByDocument -> 200")
    void getUsuariosByDocument_routesToGetByDocument_200() {
        when(handler.getByDocument(any()))
                .thenReturn(ok().build());
        client.get()
                .uri("/api/v1/usuarios/12345678")
                .exchange()
                .expectStatus().isOk();
        verify(handler).getByDocument(any());
        verifyNoMoreInteractions(handler);
    }
}