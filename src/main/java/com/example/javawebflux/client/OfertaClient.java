package com.example.javawebflux.client;

import com.example.javawebflux.observability.LatencyLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Component
public class OfertaClient {

    private final WebClient webClient;
    private final String ofertaUrl;

    public OfertaClient(
            WebClient.Builder webClientBuilder,
            @Value("${integracao.oferta.url:http://localhost:3001/oferta}") String ofertaUrl) {
        this.webClient = webClientBuilder.build();
        this.ofertaUrl = ofertaUrl;
    }

    public Mono<Boolean> consultar(String cpf) {
        Mono<Boolean> chamada = webClient.get()
                .uri(UriComponentsBuilder.fromUriString(ofertaUrl)
                        .queryParam("cpf", "{cpf}")
                        .buildAndExpand(cpf)
                        .toUri())
                .retrieve()
                .bodyToMono(OfertaResponse.class)
                .map(OfertaResponse::temOferta);
            return LatencyLogger.measure("dependency=oferta", chamada);
    }

    private record OfertaResponse(Boolean temOferta) {
    }
}