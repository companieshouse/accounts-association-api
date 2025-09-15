package uk.gov.companieshouse.accounts.association.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.service.client.CompanyClient;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.company.CompanyDetails;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class CompanyServiceTest {

    @Mock
    private CompanyClient companyClient;

    @InjectMocks
    private CompanyService companyService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup() {
    }

    @Test
    void fetchCompanyProfileForNullOrMalformedOrNonexistentCompanyReturnsNotFoundRuntimeException() {
        when(companyClient.requestCompanyProfile(any(), any())).thenThrow(NotFoundRuntimeException.class);
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile(null));
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile("!@£"));
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile("404COMP"));
    }

    @Test
    void fetchCompanyProfileWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        when(companyClient.requestCompanyProfile(any(), any())).thenThrow(InternalServerErrorRuntimeException.class);
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfile("111111"));
    }

    @Test
    void fetchCompanyProfileReturnsSpecifiedCompany() {
        when(companyClient.requestCompanyProfile(any(), any())).thenReturn(testDataManager.fetchCompanyDetailsDtos("111111").getFirst());
        Assertions.assertEquals("Wayne Enterprises", companyService.fetchCompanyProfile("111111").getCompanyName());
    }

    @Test
    void fetchCompanyProfilesWithNullStreamThrowsNullPointerException(){
        Assertions.assertThrows(NullPointerException.class, () -> companyService.fetchCompanyProfiles(null));
    }

    @Test
    void fetchCompanyProfilesWithEmptyStreamReturnsEmptyMap() {
        Assertions.assertEquals(0, companyService.fetchCompanyProfiles(Stream.of()).size());
    }

    @Test
    void fetchCompanyProfilesWithStreamThatHasNonExistentCompanyReturnsNotFoundRuntimeException(){
        when(companyClient.requestCompanyProfile(eq("404COMP"), any())).thenThrow(NotFoundRuntimeException.class);
        final var associationDao = new AssociationDao();
        associationDao.setCompanyNumber("404COMP");
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfiles(Stream.of(associationDao)));
    }

    // TODO: ParsingUtil unit test, NOT a company service unit test
//    @Test
//    void fetchCompanyProfilesWithStreamThatHasMalformedCompanyNumberReturnsInternalServerErrorRuntimeException(){
//        final var associationDao = new AssociationDao();
//        associationDao.setCompanyNumber("£$@123");
////        mockers.mockRestClientForFetchCompanyProfileErrorResponse("£$@123", 400);
//        try (MockedStatic<ParsingUtil> mockedStatic = Mockito.mockStatic(ParsingUtil.class)) {
//            mockedStatic.when(() -> ParsingUtil.parseJsonTo(any(), any())).thenCallRealMethod();
//            Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfiles(Stream.of(associationDao)));
//        }
//    }

    @Test
    void fetchCompanyProfilesWithStreamWithArbitraryErrorReturnsInternalServerErrorRuntimeException(){
        final var associationDao = testDataManager.fetchAssociationDaos("1").getFirst();
        when(companyClient.requestCompanyProfile(any(), any())).thenThrow(InternalServerErrorRuntimeException.class);
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfiles(Stream.of(associationDao)));
    }

    @Test
    void fetchCompanyProfilesWithStreamReturnsMap() {
        final var associationDao = testDataManager.fetchAssociationDaos("1").getFirst();
        final var companyNumber = "111111";
        when(companyClient.requestCompanyProfile(any(), any())).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        Map<String, CompanyDetails> companies;
        companies = companyService.fetchCompanyProfiles(Stream.of(associationDao, associationDao));

        Assertions.assertEquals(1, companies.size());
        Assertions.assertTrue(companies.containsKey(companyNumber));
        Assertions.assertTrue(companies.values().stream().map(CompanyDetails::getCompanyNumber).toList().contains(companyNumber));
    }
}
