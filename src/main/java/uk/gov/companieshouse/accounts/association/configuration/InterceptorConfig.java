package uk.gov.companieshouse.accounts.association.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.accounts.association.interceptor.RequestLifecycleInterceptor;
import uk.gov.companieshouse.accounts.association.service.UsersService;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private final UsersService usersService;

    public InterceptorConfig(final UsersService usersService) {
        this.usersService = usersService;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(new RequestLifecycleInterceptor(usersService));
    }

}