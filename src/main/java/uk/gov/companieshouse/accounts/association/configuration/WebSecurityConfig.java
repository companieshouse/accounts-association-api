package uk.gov.companieshouse.accounts.association.configuration;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static uk.gov.companieshouse.accounts.association.models.SpringRole.ADMIN_READ_ROLE;
import static uk.gov.companieshouse.accounts.association.models.SpringRole.ADMIN_UPDATE_ROLE;
import static uk.gov.companieshouse.accounts.association.models.SpringRole.BASIC_OAUTH_ROLE;
import static uk.gov.companieshouse.accounts.association.models.SpringRole.KEY_ROLE;
import static uk.gov.companieshouse.accounts.association.models.SpringRole.getValues;

import java.util.List;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import uk.gov.companieshouse.accounts.association.filter.UserAuthenticationFilter;
import uk.gov.companieshouse.api.filter.CustomCorsFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private static final Supplier<List<String>> externalMethods = () -> List.of( GET.name() );

    @Bean
    public SecurityFilterChain filterChain( final HttpSecurity http ) throws Exception {
        http.cors( AbstractHttpConfigurer::disable )
                .sessionManagement( s -> s.sessionCreationPolicy( SessionCreationPolicy.STATELESS ) )
                .csrf( AbstractHttpConfigurer::disable )
                .addFilterBefore( new CustomCorsFilter( externalMethods.get() ), CsrfFilter.class )
                .addFilterAfter( new UserAuthenticationFilter(), CsrfFilter.class )
                .authorizeHttpRequests( request -> request
                        .requestMatchers( GET, "/associations-api/healthcheck" ).permitAll()
                        .requestMatchers( GET,"/associations" ).hasAnyRole( getValues( BASIC_OAUTH_ROLE ) )
                        .requestMatchers( POST,"/associations" ).hasAnyRole( getValues( KEY_ROLE ) )
                        .requestMatchers("/associations/invitations" ).hasAnyRole( getValues( BASIC_OAUTH_ROLE ) )
                        .requestMatchers( GET,"/associations/*/invitations" ).hasAnyRole( getValues( BASIC_OAUTH_ROLE ) )
                        .requestMatchers( GET,"/associations/*" ).hasAnyRole( getValues( BASIC_OAUTH_ROLE, ADMIN_READ_ROLE ) )
                        .requestMatchers( GET,"/associations/companies/*" ).hasAnyRole( getValues( BASIC_OAUTH_ROLE ) )
                        .requestMatchers( POST,"/associations/companies/*/search" ).hasAnyRole( getValues( KEY_ROLE ) )
                        .requestMatchers( GET,"/associations/*/previous-states" ).hasAnyRole( getValues( BASIC_OAUTH_ROLE, ADMIN_READ_ROLE ) )
                        .requestMatchers( PATCH,"/associations/*" ).hasAnyRole( getValues( BASIC_OAUTH_ROLE, ADMIN_UPDATE_ROLE, KEY_ROLE ) )
                        .anyRequest().denyAll()
                );
        return http.build();

    }

}