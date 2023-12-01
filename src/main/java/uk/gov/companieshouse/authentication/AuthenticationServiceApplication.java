package uk.gov.companieshouse.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class AuthenticationServiceApplication implements WebMvcConfigurer {

    public static final String APPLICATION_NAME_SPACE = "authentication-service";

    @Autowired
    public AuthenticationServiceApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(AuthenticationServiceApplication.class, args);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
    }
}