package uk.gov.companieshouse.accounts.association.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.rest.CompanyProfileMockEndpoint;
import uk.gov.companieshouse.api.accounts.associations.model.CompanyInfo;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class CompaniesServiceTest {

    @InjectMocks
    CompaniesService companiesService;

    @Mock
    CompanyProfileMockEndpoint companyProfileMockEndpoint;

    @Test
    void fetchCompaniesWithMalformedInputOrEmptyListOrListOfNonexistentCompanyNumbersReturnsEmptyList(){
        Mockito.doReturn( List.of() ).when( companyProfileMockEndpoint ).fetchCompanies( any() );

        Assertions.assertEquals( List.of(), companiesService.fetchCompanies( null ) );
        Assertions.assertEquals( List.of(), companiesService.fetchCompanies( List.of() ) );
        Assertions.assertEquals( List.of(), companiesService.fetchCompanies( List.of( "" ) ) );
        Assertions.assertEquals( List.of(), companiesService.fetchCompanies( List.of( "abc" ) ) );
        Assertions.assertEquals( List.of(), companiesService.fetchCompanies( List.of( "999999" ) ) );
    }

    @Test
    void fetchCompaniesShouldReturnSpecifiedCompanies(){

        Mockito.doReturn( List.of( new CompanyInfo().companyNumber( "111111" ).companyName( "Wayne Enterprises" ) ) ).when( companyProfileMockEndpoint ).fetchCompanies( eq( List.of( "111111" ) ) );
        Mockito.doReturn( List.of( new CompanyInfo().companyNumber( "111111" ).companyName( "Wayne Enterprises" ), new CompanyInfo().companyNumber( "333333" ).companyName("Queen Victoria Pub") ) ).when( companyProfileMockEndpoint ).fetchCompanies( eq( List.of( "111111", "333333" ) ) );

        final var company = companiesService.fetchCompanies( List.of("111111") );
        Assertions.assertEquals( 1, company.size() );
        Assertions.assertEquals( "Wayne Enterprises", company.get(0).getCompanyName() );

        final var companies = companiesService.fetchCompanies( List.of("111111", "333333") );
        Assertions.assertEquals( 2, companies.size() );
        Assertions.assertTrue( companies.stream().map(CompanyInfo::getCompanyName).toList().containsAll(List.of( "Wayne Enterprises", "Queen Victoria Pub") ) );
    }

}
