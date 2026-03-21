package com.zyc.copier_v0;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CopierV0Application {

    public static void main(String[] args) {
        SpringApplication.run(CopierV0Application.class, args);
    }

}
