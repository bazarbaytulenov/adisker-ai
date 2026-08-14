package kz.adisker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class AdiskerApplication {
    public static void main(String[] args) {
        String dbUrl = System.getenv("SPRING_DATASOURCE_URL");
        String dbPassword = System.getenv("POSTGRES_PASSWORD");
        String dbHost = System.getenv("DB_HOST");
        System.out.println("=== DB DEBUG ===");
        System.out.println("SPRING_DATASOURCE_URL = " + dbUrl);
        System.out.println("POSTGRES_PASSWORD length = " + (dbPassword != null ? dbPassword.length() : "NULL"));
        System.out.println("DB_HOST = " + dbHost);
        System.out.println("================");
        SpringApplication.run(AdiskerApplication.class, args);
    }
}
