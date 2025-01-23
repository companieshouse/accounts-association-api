package uk.gov.companieshouse.accounts.association.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonTo;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;

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
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class ReactiveCompanyService {

    private final WebClient companyWebClient;

    private static final Logger LOG = LoggerFactory.getLogger( APPLICATION_NAMESPACE );

    public ReactiveCompanyService( @Qualifier( "companyWebClient" ) final WebClient companyWebClient ) {
        this.companyWebClient = companyWebClient;
    }

    private Mono<CompanyDetails> toFetchCompanyProfileRequest( final String companyNumber, final String xRequestId ) {
        return companyWebClient.get()
                .uri( String.format( "/company/%s/company-detail", companyNumber ) )
                .retrieve()
                .bodyToMono( String.class )
                .map( parseJsonTo( CompanyDetails.class ) )
                .onErrorMap( throwable -> {
                    if ( throwable instanceof WebClientResponseException exception ){
                        if ( NOT_FOUND.equals( exception.getStatusCode() ) ){
                            LOG.errorContext( xRequestId, String.format( "Could not find company profile for company %s", companyNumber ), exception, null );
                            return new NotFoundRuntimeException( APPLICATION_NAMESPACE, "Failed to find company" );
                        }
                    }
                    LOG.errorContext( xRequestId, String.format( "Failed to retrieve company profile for company %s", companyNumber ), (Exception) throwable, null );
                    throw new InternalServerErrorRuntimeException( "Failed to retrieve company profile" );
                } )
                .doOnSubscribe( onSubscribe -> LOG.infoContext( xRequestId, String.format( "Sending request to company-profile-api: GET /company/{company_number}/company-detail. Attempting to retrieve company profile for company: %s" , companyNumber ), null ) )
                .doFinally( signalType -> LOG.infoContext( xRequestId, String.format( "Finished request to company-profile-api for company: %s.", companyNumber ), null ) );
    }

    public CompanyDetails fetchCompanyProfile( final String companyNumber ){
        final var xRequestId = getXRequestId();
        return toFetchCompanyProfileRequest( companyNumber, xRequestId ).block( Duration.ofSeconds( 20L ) );
    }

    public Map<String, CompanyDetails> fetchCompanyProfiles( final Stream<AssociationDao> associations ) {
        final var xRequestId = getXRequestId();
        return Flux.fromStream( associations )
                .map( AssociationDao::getCompanyNumber )
                .distinct()
                .flatMap( companyNumber -> toFetchCompanyProfileRequest( companyNumber, xRequestId ) )
                .collectMap( CompanyDetails::getCompanyNumber )
                .block( Duration.ofSeconds( 20L ) );
    }

}



