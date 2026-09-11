package kz.adisker.module.ai;

import kz.adisker.module.audit.AuditService;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Прикладные AI-сценарии для дошкольной организации (ТЗ 5.12, п.9).
 * Все результаты — черновики, требующие проверки пользователем.
 */
@Service
@RequiredArgsConstructor
public class AiService {

    private final GeminiService gemini;
    private final AuditService auditService;

    public boolean available() {
        return gemini.isConfigured();
    }

    /** Генерация циклограммы по теме недели и возрасту группы. */
    public String generateCyclogram(String ageGroup, String theme, String language, UserPrincipal principal) {
        String lang = "kk".equals(language) ? "казахском" : "русском";
        String prompt = """
                Ты — методист детского сада в Казахстане. Составь циклограмму
                (недельное распределение видов деятельности по дням) на %s языке
                для группы возраста «%s» по теме недели «%s».
                Формат: по дням недели, с указанием режимных моментов и видов
                организованной деятельности. Это черновик для проверки методистом.
                """.formatted(lang, ageGroup, theme);
        String result = gemini.generate(prompt);
        auditService.record("CREATE", "ai_generation", null, null, null, "AI: циклограмма");
        return result;
    }

    /** Рекомендации для родителей по индивидуальной карте развития. */
    public String generateRecommendations(String childAge, String summary, String language,
                                          UserPrincipal principal) {
        String lang = "kk".equals(language) ? "казахском" : "русском";
        String prompt = """
                Ты — педагог-психолог детского сада. На %s языке составь мягкие,
                поддерживающие рекомендации для родителей ребёнка возраста «%s»
                на основе результатов наблюдения: %s.
                Без служебных терминов и уровней. Это черновик для проверки.
                """.formatted(lang, childAge, summary);
        String result = gemini.generate(prompt);
        auditService.record("CREATE", "ai_generation", null, null, null, "AI: рекомендации");
        return result;
    }

    /** Произвольная генерация текста по промпту. */
    public String generateText(String prompt, UserPrincipal principal) {
        String result = gemini.generate(prompt);
        auditService.record("CREATE", "ai_generation", null, null, null, "AI: текст");
        return result;
    }
}
