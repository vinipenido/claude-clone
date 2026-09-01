package com.vinicius.claude_clone.client;

import com.vinicius.claude_clone.exception.AnthropicApiException;
import com.vinicius.claude_clone.model.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

@Component
public class AnthropicClient {

    @Value("${anthropic.api.key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-5";

    public void streamMessage(List<Message> historico, Consumer<String> onToken) throws IOException, InterruptedException {

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", MODEL);
        requestBody.put("max_tokens", 1024);
        requestBody.put("stream", true);

        ArrayNode messagesArray = requestBody.putArray("messages");
        for (Message msg : historico) {
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", msg.getRole() == Message.Role.USER ? "user" : "assistant");
            messageNode.put("content", msg.getContent());
            messagesArray.add(messageNode);
        }

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANTHROPIC_URL))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<java.io.InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            throw new AnthropicApiException("Falha ao conectar com a API da Anthropic", e);
        }

        if (response.statusCode() != 200) {
            throw new AnthropicApiException("Anthropic API retornou status " + response.statusCode(), null);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String json = line.substring(6);
                    JsonNode node = objectMapper.readTree(json);

                    if (node.has("delta") && node.get("delta").has("text")) {
                        String text = node.get("delta").get("text").asText();
                        onToken.accept(text);
                    }
                }
            }
        }
    }
}