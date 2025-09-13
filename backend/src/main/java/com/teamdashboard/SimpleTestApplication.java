package com.teamdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Import;
import com.teamdashboard.controller.TestController;

@SpringBootApplication(exclude = {
    HttpEncodingAutoConfiguration.class,
    MultipartAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    DataSourceAutoConfiguration.class
}, scanBasePackages = {})
@Import(TestController.class)
public class SimpleTestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SimpleTestApplication.class, args);
    }
}