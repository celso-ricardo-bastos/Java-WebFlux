package com.example.javawebflux.client;

import com.example.javawebflux.observability.LatencyLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Component
public class ContaClient {

    private final WebClient webClient;
    private final String contaUrl;

    public ContaClient(
            WebClient.Builder webClientBuilder,
            @Value("${integracao.conta.url:http://localhost:3001/conta}") String contaUrl) {
        this.webClient = webClientBuilder.build();
        this.contaUrl = contaUrl;
    }

    public Mono<Boolean> consultar(String cpf) {
        Mono<Boolean> chamada = webClient.get()
                .uri(UriComponentsBuilder.fromUriString(contaUrl)
                        .queryParam("cpf", "{cpf}")
                        .buildAndExpand(cpf)
                        .toUri())
                .retrieve()
                .bodyToMono(ContaResponse.class)
                .map(ContaResponse::possuiConta);
            return LatencyLogger.measure("dependency=conta", chamada);
    }

    private record ContaResponse(Boolean possuiConta) {
    }
}