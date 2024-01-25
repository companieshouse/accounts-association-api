package uk.gov.companieshouse.accounts.association.configuration;

import java.util.Collections;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.accounts.association.AccountsAssociationServiceApplication;
import uk.gov.companieshouse.accounts.association.interceptor.LoggingInterceptor;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.api.interceptor.UserAuthenticationInterceptor;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {


    private final LoggingInterceptor loggingInterceptor;

    public InterceptorConfig( final LoggingInterceptor loggingInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
    }

    /**
     * Setup the interceptors to run against endpoints when the endpoints are called
     * Interceptors are executed in the order they are added to the registry
     *
     * @param registry The spring interceptor registry
     */
    @Override
    public void addInterceptors(@NonNull final InterceptorRegistry registry) {
        addLoggingInterceptor(registry);
        addEricInterceptors(registry);
    }

    /**
     * Interceptor that logs all calls to endpoints
     *
     * @param registry The spring interceptor registry
     */
    private void addLoggingInterceptor( final InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor);
    }

    private void addEricInterceptors( final InterceptorRegistry registry){
        registry.addInterceptor(new UserAuthenticationInterceptor( Collections.EMPTY_LIST, Collections.singletonList("oauth2"),
                new InternalUserInterceptor(AccountsAssociationServiceApplication.applicationNameSpace) ) );
    }

}
