package com.example.javawebflux.client;

import com.example.javawebflux.observability.LatencyLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Component
public class RestritivoClient {

    private final WebClient webClient;
    private final String restritivoUrl;

    public RestritivoClient(
            WebClient.Builder webClientBuilder,
            @Value("${integracao.restritivo.url:http://localhost:3001/restritivo}") String restritivoUrl) {
        this.webClient = webClientBuilder.build();
        this.restritivoUrl = restritivoUrl;
    }

    public Mono<Boolean> consultar(String cpf) {
                Mono<Boolean> chamada = webClient.get()
                .uri(UriComponentsBuilder.fromUriString(restritivoUrl)
                        .queryParam("cpf", "{cpf}")
                        .buildAndExpand(cpf)
                        .toUri())
                .retrieve()
                .bodyToMono(RestritivoResponse.class)
                .map(RestritivoResponse::temRestritivo);
        return LatencyLogger.measure("dependency=restritivo", chamada);
    }

    private record RestritivoResponse(Boolean temRestritivo) {
    }
}