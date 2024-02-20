package uk.gov.companieshouse.accounts.association.controllers.manual;

import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.rest.CompanyProfileEndpoint;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

@RestController
public class CompanyProfileTestController {

    private final CompanyProfileEndpoint companyProfileEndpoint;

    public CompanyProfileTestController(CompanyProfileEndpoint companyProfileEndpoint ) {
        this.companyProfileEndpoint = companyProfileEndpoint;
    }

    @GetMapping( "/associations/company/{company_number}" )
    public ResponseEntity<?> fetchCompanyProfile( @PathVariable( "company_number" ) String companyNumber ) throws IOException, URIValidationException {

        try {
            companyProfileEndpoint.fetchCompanyProfile(null);
            System.err.println( "Test 1/3 failed" );
        } catch ( Exception e ){
            System.err.println( "Test 1/3 passed" );
        }

        try {
            companyProfileEndpoint.fetchCompanyProfile("");
            System.err.println( "Test 2/3 failed" );
        } catch ( Exception e ){
            System.err.println( "Test 2/3 passed" );
        }

        try {
            companyProfileEndpoint.fetchCompanyProfile("abc");
            System.err.println( "Test 3/3 failed" );
        } catch ( Exception e ){
            System.err.println( "Test 3/3 passed" );
        }

        final var companyData = companyProfileEndpoint.fetchCompanyProfile(companyNumber);
        final var companyName = companyData.getData().getCompanyName();
        return ResponseEntity.ok(companyName);
    }

}
