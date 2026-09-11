package kz.adisker.config;

import kz.adisker.security.TenantGuardInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantGuardInterceptor tenantGuardInterceptor;

    /**
     * Гарантируем UTF-8 в ответах (ТЗ п.8, кириллица/казахские символы).
     * Без явного charset клиенты могут неверно декодировать кириллицу → "?".
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        MappingJackson2HttpMessageConverter json = new MappingJackson2HttpMessageConverter();
        json.setDefaultCharset(StandardCharsets.UTF_8);
        json.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                new MediaType("application", "json", StandardCharsets.UTF_8)));
        converters.add(json);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantGuardInterceptor)
                // публичные и служебные пути исключаем
                .excludePathPatterns(
                        "/auth/**",
                        "/actuator/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/ws/**");
    }
}
