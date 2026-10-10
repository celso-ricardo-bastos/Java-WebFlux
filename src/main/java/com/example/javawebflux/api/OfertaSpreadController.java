package com.example.javawebflux.api;

import com.example.javawebflux.observability.LatencyLogger;
import com.example.javawebflux.service.OfertaSpreadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/oferta-spread")
public class OfertaSpreadController {

    private final OfertaSpreadService ofertaSpreadService;

    public OfertaSpreadController(OfertaSpreadService ofertaSpreadService) {
        this.ofertaSpreadService = ofertaSpreadService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> consultar(@RequestBody OfertaSpreadRequest request) {
        return LatencyLogger.measureHttpEndpoint(
                "POST /api/oferta-spread",
                ofertaSpreadService.consultar(request.cpf()));
    }

    public record OfertaSpreadRequest(String cpf) {
    }
}