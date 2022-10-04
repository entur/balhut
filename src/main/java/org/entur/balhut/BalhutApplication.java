package org.entur.balhut;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableTask
public class BalhutApplication {

    public static void main(String[] args) {
        SpringApplication.run(BalhutApplication.class, args);
    }

    @Bean
    public BalhutTaskListener taskExecutionListener() {
        return new BalhutTaskListener();
    }
}
