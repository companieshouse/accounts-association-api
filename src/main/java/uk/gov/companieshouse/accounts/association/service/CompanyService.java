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

@Service
public class CompanyService {

    private final WebClient companyWebClient;

    public CompanyService( @Qualifier( "companyWebClient" ) final WebClient companyWebClient ) {
        this.companyWebClient = companyWebClient;
    }

    private Mono<CompanyDetails> toFetchCompanyProfileRequest( final String companyNumber, final String xRequestId ) {
        return companyWebClient.get()
                .uri( String.format( "/company/%s/company-detail", companyNumber ) )
                .retrieve()
                .bodyToMono( String.class )
                .map( parseJsonTo( CompanyDetails.class ) )
                .onErrorMap( throwable -> {
                    if ( throwable instanceof WebClientResponseException exception && NOT_FOUND.equals( exception.getStatusCode() ) ){
                        return new NotFoundRuntimeException( "Failed to find company", exception );
                    }
                    throw new InternalServerErrorRuntimeException( "Failed to retrieve company profile", (Exception) throwable );
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

}



