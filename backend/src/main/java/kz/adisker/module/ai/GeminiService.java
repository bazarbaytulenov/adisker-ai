package kz.adisker.module.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.adisker.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Клиент Google Gemini API (ТЗ п.9). Ключ хранится только на сервере.
 * Используется для генерации черновиков: циклограммы, рекомендации, тексты.
 * ИИ НЕ утверждает юридически значимые документы (ТЗ п.19).
 */
@Slf4j
@Service
public class GeminiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiService(ObjectMapper objectMapper,
                         @Value("${ai.gemini.api-key:}") String apiKey,
                         @Value("${ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
                         @Value("${ai.gemini.model:gemini-1.5-flash}") String model) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Генерирует текст по промпту. Возвращает черновик для проверки пользователем.
     */
    public String generate(String prompt) {
        if (!isConfigured()) {
            throw new BusinessException("AI-интеграция не настроена (GEMINI_API_KEY не задан)");
        }
        try {
            Map<String, Object> body = Map.of(
                    "contents", new Object[]{
                            Map.of("parts", new Object[]{ Map.of("text", prompt) })
                    });

            String response = restClient.post()
                    .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return extractText(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini generation failed: {}", e.getMessage());
            throw new BusinessException("Ошибка генерации ИИ: " + e.getMessage());
        }
    }

    private String extractText(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode text = root.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text");
        if (text.isMissingNode()) {
            throw new BusinessException("Пустой ответ от ИИ");
        }
        return text.asText();
    }
}
