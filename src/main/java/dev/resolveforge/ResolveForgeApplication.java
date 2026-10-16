package dev.resolveforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ResolveForgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResolveForgeApplication.class, args);
    }
}
