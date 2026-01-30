package uk.gov.companieshouse.accounts.association.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonTo;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.model.company.RegisteredEmailAddressJson;

@Service
public class CompanyService {

    private final WebClient companyWebClient;
    private final WebClient oracleQueryWebClient;

    public CompanyService( @Qualifier( "companyWebClient" ) final WebClient companyWebClient, @Qualifier( "oracleQueryWebClient" ) final WebClient oracleQueryWebClient ) {
        this.companyWebClient = companyWebClient;
        this.oracleQueryWebClient = oracleQueryWebClient;
    }

    private Mono<CompanyDetails> toFetchCompanyProfileRequest( final String companyNumber, final String xRequestId ) {
        return companyWebClient.get()
                .uri( String.format( "/company/%s/company-detail", companyNumber ) )
                .header( "X-Request-Id", xRequestId )
                .retrieve()
                .bodyToMono( String.class )
                .map( parseJsonTo( CompanyDetails.class ) )
                .onErrorMap( throwable -> {
                    if ( throwable instanceof WebClientResponseException exception && NOT_FOUND.equals( exception.getStatusCode() ) ){
                        return new NotFoundRuntimeException( xRequestId, "Failed to find company", exception );
                    }
                    throw new InternalServerErrorRuntimeException( xRequestId, "Failed to retrieve company profile", (Exception) throwable );
                } )
                .doOnSubscribe( onSubscribe -> LOGGER.infoContext( xRequestId, String.format( "Sending request to company-profile-api: GET /company/{company_number}/company-detail. Attempting to retrieve company profile for company: %s" , companyNumber ), null ) )
                .doFinally( signalType -> LOGGER.infoContext( xRequestId, String.format( "Finished request to company-profile-api for company: %s.", companyNumber ), null ) );
    }

    public CompanyDetails fetchCompanyProfile( final String companyNumber ){
        return toFetchCompanyProfileRequest( companyNumber, getXRequestId() ).block( Duration.ofSeconds( 20L ) );
    }

    public Map<String, CompanyDetails> fetchCompanyProfiles( final Stream<AssociationDao> associations ) {
        final var xRequestId = getXRequestId();
        return Flux.fromStream( associations )
                .map( AssociationDao::getCompanyNumber )
                .flatMap( companyNumber -> toFetchCompanyProfileRequest( companyNumber, xRequestId ) )
                .collectMap( CompanyDetails::getCompanyNumber )
                .block( Duration.ofSeconds( 20L ) );
    }

    private Mono<RegisteredEmailAddressJson> toFetchRegisteredEmailAddressRequest(final String companyNumber,final String xRequestId ) {
        return oracleQueryWebClient.get()
                .uri( String.format( "/company/%s/registered-email-address", companyNumber ) )
                .header( "X-Request-Id", xRequestId )
                .retrieve()
                .bodyToMono( String.class )
                .map( parseJsonTo( RegisteredEmailAddressJson.class ) )
                .onErrorMap( throwable -> {
                    if ( throwable instanceof WebClientResponseException exception && NOT_FOUND.equals( exception.getStatusCode() ) ){
                        return new NotFoundRuntimeException( xRequestId, "Failed to find registered email address for company", exception );
                    }
                    throw new InternalServerErrorRuntimeException( xRequestId, "Failed to retrieve registered email address", (Exception) throwable );
                } )
                .doOnSubscribe( onSubscribe -> LOGGER.infoContext( xRequestId, String.format( "Sending request to oracle-query-api: GET /company/%s/registered-email-address. Attempting to retrieve registered email address.", companyNumber ), null ) )
                .doFinally( signalType -> LOGGER.infoContext( xRequestId, String.format( "Finished request to oracle-query-api for company: %s.", companyNumber ), null ) );
    }

    public String fetchRegisteredEmailAddress( final String companyNumber ){
        final var xRequestId = getXRequestId();
        final var response = toFetchRegisteredEmailAddressRequest( companyNumber, xRequestId )
                .block( Duration.ofSeconds( 20L ) );
        final var registeredEmailAddress = response == null ? null : response.getRegisteredEmailAddress();
        if ( registeredEmailAddress == null || registeredEmailAddress.isBlank() ) {
            LOGGER.infoContext( xRequestId, String.format( "Registered email address not found or blank for company: %s", companyNumber ), null );
        } else {
            LOGGER.infoContext( xRequestId, String.format( "Retrieved registered email address for company: %s", companyNumber), null );
        }
        return registeredEmailAddress;
    }
}



