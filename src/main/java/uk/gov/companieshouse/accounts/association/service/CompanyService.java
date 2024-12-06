package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.rest.CompanyProfileEndpoint;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class CompanyService {


    private final CompanyProfileEndpoint companyProfileEndpoint;

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    @Autowired
    public CompanyService(CompanyProfileEndpoint companyProfileEndpoint) {
        this.companyProfileEndpoint = companyProfileEndpoint;
    }

    public Supplier<CompanyDetails> createFetchCompanyProfileRequest( final String companyNumber ) {
        final var request = companyProfileEndpoint.createFetchCompanyProfileRequest( companyNumber );
        final var xRequestId = getXRequestId();
        return () -> {
            try {
                LOG.debugContext( xRequestId, String.format( "Sending request to company-profile-api: GET /company/{company_number}/company-detail. Attempting to retrieve company profile for company: %s" , companyNumber ), null );
                return request.execute().getData();
            } catch ( ApiErrorResponseException exception ) {
                if ( exception.getStatusCode() == 404 ) {
                    LOG.errorContext( xRequestId, new Exception( String.format( "Could not find company profile for company %s", companyNumber ) ), null );
                    throw new NotFoundRuntimeException( "accounts-association-api", "Failed to find company" );
                } else {
                    LOG.errorContext( xRequestId, new Exception( String.format( "Failed to retrieve company profile for company %s" , companyNumber ) ), null );
                    throw new InternalServerErrorRuntimeException( "Failed to retrieve company profile" );
                }
            } catch ( URIValidationException exception ) {
                LOG.errorContext( xRequestId, new Exception( String.format( "Failed to fetch company profile for company %s, because uri was incorrectly formatted", companyNumber ) ), null );
                throw new InternalServerErrorRuntimeException( "Invalid uri for company-profile-api service" );
            } catch ( Exception exception ) {
                LOG.errorContext( xRequestId, new Exception( String.format("Failed to retrieve company profile for company %s", companyNumber) ), null );
                throw new InternalServerErrorRuntimeException( "Failed to retrieve company profile" );
            }
        };
    }

    public CompanyDetails fetchCompanyProfile( final String companyNumber ) {
        return createFetchCompanyProfileRequest( companyNumber ).get();
    }

    public Map<String, CompanyDetails> fetchCompanyProfiles( final Stream<AssociationDao> associationDaos ){
        final Map<String, CompanyDetails> companies = new ConcurrentHashMap<>();
        associationDaos.map( AssociationDao::getCompanyNumber )
                .distinct()
                .map( this::createFetchCompanyProfileRequest )
                .parallel()
                .map( Supplier::get )
                .forEach( company -> companies.put( company.getCompanyNumber(), company ) );
        return companies;
    }

}
