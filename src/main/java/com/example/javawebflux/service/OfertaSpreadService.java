package com.example.javawebflux.service;

import com.example.javawebflux.client.ContaClient;
import com.example.javawebflux.client.OfertaClient;
import com.example.javawebflux.client.RestritivoClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Service
public class OfertaSpreadService {

    private final RestritivoClient restritivoClient;
    private final ContaClient contaClient;
    private final OfertaClient ofertaClient;

    public OfertaSpreadService(
            RestritivoClient restritivoClient,
            ContaClient contaClient,
            OfertaClient ofertaClient) {
        this.restritivoClient = restritivoClient;
        this.contaClient = contaClient;
        this.ofertaClient = ofertaClient;
    }

    public Mono<ResponseEntity<?>> consultar(String cpf) {
        return restritivoClient.consultar(cpf)
                .flatMap(temRestritivo -> {
                    if (temRestritivo) {
                        return Mono.just(ResponseEntity.unprocessableEntity()
                                .body(new RestricaoResponse("Possui restrição")));
                    }

                    return Mono.zip(
                        contaClient.consultar(cpf), 
                        ofertaClient.consultar(cpf)
                    )
                    .map(resultados -> {
                        if (resultados.getT1() && resultados.getT2()) {
                            return ResponseEntity.ok(createPromocaoResponse());
                        }
                        return ResponseEntity.ok("OK");
                    });
                })
                .timeout(Duration.ofSeconds(1))
                .onErrorResume(TimeoutException.class, exception ->
                        Mono.just(ResponseEntity.badRequest()
                            .body(new LatenciaResponse("maior que 1 segundo"))));
    }

    public record PromocaoResponse(
            String id,
            String type,
            String title,
            String message,
            BigDecimal promoSpreadPercent,
            String currency) {
    }

    public record LatenciaResponse(String latencia) {
    }

    public record RestricaoResponse(String status) {
    }

    private PromocaoResponse createPromocaoResponse() {
        return new PromocaoResponse(
                "promo-usd-spread",
                "modal",
                "Promoção: dólar com spread reduzido",
                "Aproveite! Hoje o spread promocional no câmbio USD é de 0,30%.",
                new BigDecimal("0.30"),
                "USD");
    }
}