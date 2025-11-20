package uk.gov.companieshouse.accounts.association.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.company.CompanyDetails;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class CompanyServiceTest {

    @Mock
    private WebClient companyWebClient;

    @InjectMocks
    private CompanyService companyService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private Mockers mockers;

    @BeforeEach
    void setup() {
        mockers = new Mockers( companyWebClient, null, null, null );
    }

    @Test
    void fetchCompanyProfileForNullOrMalformedOrNonexistentCompanyReturnsNotFoundRuntimeException() {
        mockers.mockWebClientForFetchCompanyProfileErrorResponse( null, 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile( null ) );

        mockers.mockWebClientForFetchCompanyProfileErrorResponse( "!@£", 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile( "!@£" ) );

        mockers.mockWebClientForFetchCompanyProfileErrorResponse( "404COMP", 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile( "404COMP" ) );
    }

    @Test
    void fetchCompanyProfileWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        mockers.mockWebClientForFetchCompanyProfileJsonParsingError( "111111", true );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfile( "111111" ) );
    }

    @Test
    void fetchCompanyProfileReturnsSpecifiedCompany() throws JsonProcessingException {
        mockers.mockWebClientForFetchCompanyProfile( true, "111111" );
        Assertions.assertEquals( "Wayne Enterprises", companyService.fetchCompanyProfile( "111111" ).getCompanyName() );
    }

    @Test
    void fetchCompanyProfilesWithNullStreamThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> companyService.fetchCompanyProfiles( null ) );
    }

    @Test
    void fetchCompanyProfilesWithEmptyStreamReturnsEmptyMap() {
        Assertions.assertEquals( 0, companyService.fetchCompanyProfiles( Stream.of() ).size() );
    }

    @Test
    void fetchCompanyProfilesWithStreamThatHasNonExistentCompanyReturnsNotFoundRuntimeException(){
        final var associationDao = new AssociationDao();
        associationDao.setCompanyNumber( "404COMP" );
        mockers.mockWebClientForFetchCompanyProfileErrorResponse( "404COMP", 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfiles( Stream.of( associationDao ) ) );
    }

    @Test
    void fetchCompanyProfilesWithStreamThatHasMalformedCompanyNumberReturnsInternalServerErrorRuntimeException(){
        final var associationDao = new AssociationDao();
        associationDao.setCompanyNumber( "£$@123" );
        mockers.mockWebClientForFetchCompanyProfileErrorResponse( "£$@123", 400 );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfiles( Stream.of( associationDao ) ) );
    }

    @Test
    void fetchCompanyProfilesWithStreamWithArbitraryErrorReturnsInternalServerErrorRuntimeException(){
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        mockers.mockWebClientForFetchCompanyProfileJsonParsingError( "111111", true );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfiles( Stream.of( associationDao ) ) );
    }

    @Test
    void fetchCompanyProfilesWithStreamReturnsMap() throws JsonProcessingException {
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        mockers.mockWebClientForFetchCompanyProfile( true, "111111" );
        final var companies = companyService.fetchCompanyProfiles( Stream.of( associationDao, associationDao ) );

        Assertions.assertEquals( 1, companies.size() );
        Assertions.assertTrue( companies.containsKey( "111111" ) );
        Assertions.assertTrue( companies.values().stream().map( CompanyDetails::getCompanyNumber ).toList().contains( "111111" ) );
    }

    @Test
    void fetchRegisteredEmailAddressReturnsEmail() throws JsonProcessingException {
        mockers.mockWebClientForFetchRegisteredEmailAddress(true, "111111", "rea@example.com");
        Assertions.assertEquals("rea@example.com", companyService.fetchRegisteredEmailAddress("111111"));
    }

    @Test
    void fetchRegisteredEmailAddressNotFoundThrowsNotFoundRuntimeException() {
        mockers.mockWebClientForFetchRegisteredEmailAddressErrorResponse("404COMP", 404);
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> companyService.fetchRegisteredEmailAddress("404COMP"));
    }

    @Test
    void fetchRegisteredEmailAddressParsingErrorThrowsInternalServerErrorRuntimeException() {
        mockers.mockWebClientForFetchRegisteredEmailAddressJsonParsingError("111111", true);
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> companyService.fetchRegisteredEmailAddress("111111"));
    }

    @Test
    void fetchRegisteredEmailAddressBlankReturnsBlankAndDoesNotThrow() throws JsonProcessingException {
        mockers.mockWebClientForFetchRegisteredEmailAddress(true, "222222", "");
        Assertions.assertEquals("", companyService.fetchRegisteredEmailAddress("222222"));
    }

}
