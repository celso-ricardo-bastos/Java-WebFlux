# Spring WebFlux: API Reativa de Demonstração

Projeto de demonstração de programação reativa com Java 21 e Spring Boot WebFlux. Apresenta composição de fluxos assíncronos com `Mono`, chamadas HTTP não bloqueantes via `WebClient`, execução paralela de dependências, tratamento de timeout e observabilidade de latência. A oferta spread é o cenário prático usado para exercitar esses recursos.

## Fluxo

```text
POST /api/oferta-spread
          |
          v
GET /restritivo?cpf=...  (sequencial)
          |
          +-- temRestritivo=true  --> 422 com motivo; encerra
          |
          +-- temRestritivo=false --> GET /conta e GET /oferta em paralelo
                                       |
                                       +-- ambos true --> 200 com a promoção
                                       +-- algum false --> 200 com "OK"
```

O timeout de 1 segundo é total: inclui `/restritivo` e, quando o fluxo continua, as chamadas paralelas a `/conta` e `/oferta`. Se o limite for ultrapassado, o fluxo é cancelado e a API responde `400`.

## Requisitos e execução

Requisitos: JDK 21, Maven e Mockoon.

1. Importe e inicie o ambiente Mockoon de [mockoon/WebFlux.json](mockoon/WebFlux.json). Ele usa a porta `3001`.
2. Na raiz do projeto, inicie a API:

```bash
mvn spring-boot:run
```

A aplicação fica disponível em `http://localhost:8080`.

## API da aplicação

### `POST /api/oferta-spread`

Cabeçalho: `Content-Type: application/json`

Body:

```json
{
  "cpf": "12345678900"
}
```

Exemplo para Windows PowerShell:

```powershell
curl.exe -X POST "http://localhost:8080/api/oferta-spread" `
  -H "Content-Type: application/json" `
  -H "Accept: */*" `
  --data-raw '{"cpf":"12345678900"}'
```

### Respostas

| Condição | HTTP | Corpo |
| --- | --- | --- |
| `temRestritivo` é `true` | `422 Unprocessable Entity` | `{"status":"Possui restrição"}` |
| Restrição é `false`; `possuiConta` e `temOferta` são `true` | `200 OK` | Template promocional abaixo |
| Restrição é `false`; conta ou oferta é `false` | `200 OK` | Texto `OK` |
| O fluxo leva mais de 1 segundo | `400 Bad Request` | `{"latencia":"maior que 1 minuto"}` |

Resposta promocional:

```json
{
  "id": "promo-usd-spread",
  "type": "modal",
  "title": "Promoção: dólar com spread reduzido",
  "message": "Aproveite! Hoje o spread promocional no câmbio USD é de 0,30%.",
  "promoSpreadPercent": 0.30,
  "currency": "USD"
}
```

> **Nota:** o timeout configurado é de 1 segundo, mas o texto atualmente devolvido no campo `latencia` diz `maior que 1 minuto`. A mensagem pode ser alinhada ao limite numa alteração posterior.

## APIs externas Mockoon

As dependências externas deste fluxo foram criadas como mocks no Mockoon. Todas usam `GET`, recebem o CPF pela query string e escutam na porta `3001`.

| Rota | JSON de resposta | Uso no fluxo |
| --- | --- | --- |
| `/restritivo?cpf=12345678900` | `{"temRestritivo": false}` | Primeira consulta; `true` encerra com 422 |
| `/conta?cpf=12345678900` | `{"possuiConta": true}` | Consultada se não houver restrição |
| `/oferta?cpf=12345678900` | `{"temOferta": true}` | Consultada em paralelo com `/conta` |

Os nomes dos campos precisam corresponder ao contrato: `temRestritivo`, `possuiConta` e `temOferta`.

### Importar e configurar o ambiente Mockoon

O arquivo [WebFlux.json](mockoon/WebFlux.json) é um ambiente exportado do Mockoon, pronto para importação:

1. Abra o Mockoon.
2. No menu de ambientes, escolha **Import environments** ou **Import environment** (o nome pode variar conforme a versão).
3. Selecione `mockoon/WebFlux.json` neste projeto.
4. Selecione o ambiente importado chamado **WebFlux** e inicie-o. Confirme que está escutando na porta `3001`.
5. Mantenha o Mockoon rodando enquanto testa a aplicação.

No ambiente incluído, `/restritivo` responde `false`, `/conta` responde `true` e `/oferta` responde `true`. A latência de `/oferta` está configurada em `1000 ms` para demonstrar o timeout. Para testar a promoção, edite a latência da resposta de `/oferta` para `0 ms`. Para testar o ramo de restrição, edite o body de `/restritivo` para:

```json
{"temRestritivo": true}
```

Restaure `false` para testar `/conta` e `/oferta`. Os bodies JSON das três respostas, para colar nos respectivos mocks, são:

```json
{"temRestritivo": false}
```

```json
{"possuiConta": true}
```

```json
{"temOferta": true}
```

## Logs de latência

Os logs são exibidos no terminal de execução/debug da aplicação no VS Code:

- `event=http_latency`: latência total do endpoint, status HTTP e resultado terminal.
- `event=latency`: latência individual da integração `restritivo`, `conta` ou `oferta`.

Exemplos:

```text
event=latency operation=dependency=restritivo outcome=ON_COMPLETE latencyMs=42
event=latency operation=dependency=conta outcome=ON_COMPLETE latencyMs=81
event=http_latency endpoint=POST /api/oferta-spread status=200 outcome=ON_COMPLETE latencyMs=137
```

As linhas não registram CPF nem query string. `CANCEL` em uma integração pode indicar que ela foi cancelada pelo timeout global.

## Configuração das URLs externas

As URLs locais padrão podem ser sobrescritas por propriedades Spring:

| Propriedade | Valor padrão |
| --- | --- |
| `integracao.restritivo.url` | `http://localhost:3001/restritivo` |
| `integracao.conta.url` | `http://localhost:3001/conta` |
| `integracao.oferta.url` | `http://localhost:3001/oferta` |

Variáveis de ambiente equivalentes:

```text
INTEGRACAO_RESTRITIVO_URL=http://localhost:3001/restritivo
INTEGRACAO_CONTA_URL=http://localhost:3001/conta
INTEGRACAO_OFERTA_URL=http://localhost:3001/oferta
```

## Testes

Execute a suíte com:

```bash
mvn test
```

Os testes cobrem a resposta 422 para restrição, a promoção quando as validações permitem, a resposta `OK` quando falta uma condição e o timeout.