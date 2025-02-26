package guru.springframework.mercedesmanual;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MercedesManualApplication {
    public static void main(String[] args) {
        SpringApplication.run(MercedesManualApplication.class, args);
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> webServerFactoryCustomizer() {
        return factory -> {
            String port = System.getenv("PORT");
            factory.setPort(port != null ? Integer.parseInt(port) : 8080);
        };
    }
}