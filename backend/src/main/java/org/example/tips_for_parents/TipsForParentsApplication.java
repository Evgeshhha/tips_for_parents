package org.example.tips_for_parents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class TipsForParentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TipsForParentsApplication.class, args);
    }
}
