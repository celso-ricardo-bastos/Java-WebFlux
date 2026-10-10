package com.example.javawebflux.api;

import com.example.javawebflux.JavaWebfluxApplication;
import com.example.javawebflux.client.ContaClient;
import com.example.javawebflux.client.OfertaClient;
import com.example.javawebflux.client.RestritivoClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = JavaWebfluxApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class OfertaSpreadControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RestritivoClient restritivoClient;

    @MockitoBean
    private ContaClient contaClient;

        @MockitoBean
        private OfertaClient ofertaClient;

    @Test
        void shouldReturn422AndSkipOtherChecksWhenRestrictionIsTrue() {
                when(restritivoClient.consultar(anyString())).thenReturn(Mono.just(true));

        webTestClient.post()
                .uri("/api/oferta-spread")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"cpf":"12345678900"}
                        """)
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("Possui restrição");

        verifyNoInteractions(contaClient, ofertaClient);
    }

    @Test
    void shouldReturnPromotionWhenRestrictionIsFalseAndOtherChecksAreTrue() {
        when(restritivoClient.consultar(anyString())).thenReturn(Mono.just(false));
        when(contaClient.consultar(anyString())).thenReturn(Mono.just(true));
        when(ofertaClient.consultar(anyString())).thenReturn(Mono.just(true));

        webTestClient.post()
                .uri("/api/oferta-spread")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"cpf":"12345678900"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo("promo-usd-spread")
                .jsonPath("$.type").isEqualTo("modal")
                .jsonPath("$.title").isEqualTo("Promoção: dólar com spread reduzido")
                .jsonPath("$.message").isEqualTo("Aproveite! Hoje o spread promocional no câmbio USD é de 0,30%.")
                .jsonPath("$.promoSpreadPercent").isEqualTo(0.30)
                .jsonPath("$.currency").isEqualTo("USD");
    }

    @Test
        void shouldReturnOkWhenEitherAccountOrOfferIsFalse() {
                when(restritivoClient.consultar(anyString())).thenReturn(Mono.just(false));
                when(contaClient.consultar(anyString())).thenReturn(Mono.just(false));
                when(ofertaClient.consultar(anyString())).thenReturn(Mono.just(true));

        webTestClient.post()
                .uri("/api/oferta-spread")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"cpf":"12345678900"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("OK");
    }

    @Test
    void shouldReturnBadRequestWhenAnExternalCheckTakesMoreThanOneSecond() {
        when(restritivoClient.consultar(anyString())).thenReturn(Mono.just(false));
        when(contaClient.consultar(anyString()))
                .thenReturn(Mono.delay(Duration.ofSeconds(2)).map(ignored -> true));
        when(ofertaClient.consultar(anyString())).thenReturn(Mono.just(true));

        webTestClient.post()
                .uri("/api/oferta-spread")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"cpf":"12345678900"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.latencia").isEqualTo("maior que 1 minuto");
    }
}