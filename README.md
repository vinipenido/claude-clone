# Claude Clone — Chat com IA em Java/Spring Boot

> Clone do Claude em Java e Spring Boot: chat com streaming em tempo real (SSE), histórico persistido em PostgreSQL e integração com a API da Anthropic.

Clone simplificado do Claude construído em Java com Spring Boot, com streaming de respostas em tempo real, persistência de histórico de conversas e integração com a API da Anthropic (Claude).

Projeto desenvolvido como parte de um estudo prático de Spring Boot, aplicando conceitos de arquitetura em camadas, processamento assíncrono e comunicação com APIs externas via streaming.

## Funcionalidades

- **Streaming de respostas em tempo real** via Server-Sent Events (SSE) — o texto vai aparecendo pedaço por pedaço, como no ChatGPT
- **Persistência de conversas e mensagens** em PostgreSQL
- **Memória de contexto** — cada nova mensagem é enviada junto com todo o histórico da conversa, permitindo que a IA "lembre" do que já foi dito
- **Consulta de histórico** — endpoints para listar conversas e recuperar o histórico completo de uma conversa específica
- **Tratamento de erros centralizado** — respostas de erro padronizadas (404, 400, 502) tratadas via `@RestControllerAdvice`

## Stack técnica

| Tecnologia | Uso |
|---|---|
| Java 25 | Linguagem |
| Spring Boot 4.1.1 | Framework principal |
| Spring Data JPA / Hibernate | Persistência |
| PostgreSQL | Banco de dados |
| Jackson 3 | Serialização JSON |
| Java HttpClient (nativo) | Cliente HTTP para streaming SSE |
| Docker / Docker Compose | Ambiente do banco de dados |
| Anthropic API (Claude Sonnet) | Modelo de IA |

## Arquitetura

O projeto segue arquitetura em camadas tradicional do Spring Boot:

```
Controller  →  Service  →  Repository (JPA) ←→ PostgreSQL
                   ↓
              AnthropicClient (streaming HTTP)
```

- **Controller**: recebe a requisição HTTP e devolve um `SseEmitter` imediatamente, sem bloquear a thread principal
- **Service**: orquestra a lógica de negócio — busca/cria a conversa, salva a mensagem do usuário, monta o histórico, chama o client de IA e salva a resposta
- **AnthropicClient**: responsável pela comunicação HTTP com a API da Anthropic, lendo a resposta em streaming linha a linha e repassando cada trecho de texto via callback
- **Repository**: acesso a dados via Spring Data JPA, com queries derivadas por nome de método
- **GlobalExceptionHandler**: intercepta exceções de qualquer Controller e converte em respostas HTTP padronizadas

### Decisões técnicas relevantes

- **SSE em vez de WebFlux**: o streaming foi implementado com `SseEmitter` (Spring MVC tradicional) combinado com `@Async`, em vez de WebFlux/programação reativa. Essa escolha evita misturar duas camadas de complexidade (aprender Spring Boot e programação reativa) ao mesmo tempo, mantendo o projeto didático e ainda assim funcional para o caso de uso.
- **`java.net.http.HttpClient` nativo**: para consumir o stream SSE da Anthropic linha por linha, o cliente HTTP nativo do Java (via `BodyHandlers.ofInputStream()`) atendeu bem, sem necessidade de trazer dependências reativas como WebClient.
- **DTOs em vez de expor entidades diretamente**: os endpoints de consulta devolvem `record`s (`ConversationSummaryDto`, `MessageDto`) em vez das entidades JPA, evitando problemas de serialização cíclica no relacionamento `@OneToMany`/`@ManyToOne` e desacoplando o modelo de banco do contrato da API.
- **Jackson 3 (`tools.jackson`)**: a partir do Spring Boot 4, o Jackson deixou de vir embutido por padrão na starter web e migrou para Jackson 3, com mudança de pacote (`com.fasterxml.jackson` → `tools.jackson`). O projeto já nasce alinhado a essa mudança.

## Como rodar localmente

### Pré-requisitos
- Java 25
- Docker e Docker Compose
- Uma chave de API da Anthropic ([console.anthropic.com](https://console.anthropic.com/settings/keys))

### Passos

1. Clone o repositório:
```bash
git clone https://github.com/vinipenido/claude-clone.git
cd claude-clone
```

2. Suba o banco de dados PostgreSQL:
```bash
docker compose up -d
```

3. Configure a variável de ambiente com sua chave da Anthropic:
```bash
export ANTHROPIC_API_KEY=sua-chave-aqui
```

4. Rode a aplicação:
```bash
./mvnw spring-boot:run
```

A aplicação sobe na porta `8080`.

## Endpoints da API

### Enviar mensagem (streaming)
```
POST /api/chat
Content-Type: application/json

{
  "conversationId": null,
  "message": "Olá, tudo bem?"
}
```

`conversationId` é opcional — se omitido ou `null`, uma nova conversa é criada. A resposta é um stream SSE com dois tipos de evento:

```
event:conversation
data:1

event:token
data:Olá

event:token
data:! Tudo bem, e você?
```

Exemplo de teste via `curl`:
```bash
curl -N -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Olá, tudo bem?"}'
```

### Listar conversas
```
GET /api/chat
```

Resposta:
```json
[
  { "id": 1, "title": "Olá, tudo bem?", "createdAt": "2026-08-26T04:03:09" }
]
```

### Histórico de uma conversa
```
GET /api/chat/{conversationId}/messages
```

Resposta:
```json
[
  { "id": 1, "role": "USER", "content": "Olá, tudo bem?", "createdAt": "2026-08-26T04:03:09" },
  { "id": 2, "role": "ASSISTANT", "content": "Olá! Tudo bem, e você?", "createdAt": "2026-08-26T04:03:11" }
]
```

## Tratamento de erros

| Situação | Status HTTP | Exemplo de resposta |
|---|---|---|
| Mensagem vazia | 400 | `{"error": "A mensagem não pode estar vazia"}` |
| Conversa inexistente | 404 | `{"error": "Conversa não encontrada: 999"}` |
| Falha na API da Anthropic | 502 | `{"error": "Erro ao se comunicar com a IA: ..."}` |

## Possíveis melhorias futuras

- Extrair um `ConversationController` dedicado, separando a rota `/api/conversations` de `/api/chat`
- Adicionar testes automatizados (unitários e de integração)
- Migrar `ddl-auto=update` para migrations versionadas com Flyway
- Adicionar autenticação de usuários (hoje o histórico é global, sem isolamento por usuário)
- Interface web simples para consumir a API sem depender de `curl`

## Autor

Vinicius Costa — projeto desenvolvido como parte de estudo prático de Java e Spring Boot, com foco em entender arquitetura de aplicações backend além da sintaxe da linguagem.
