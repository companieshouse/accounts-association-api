package uk.gov.companieshouse.accounts.association.mapper;

import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;

import java.util.List;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AssociationMapperTest {

    @Mock
    private UsersService usersService;

    @Mock
    private CompanyService companyService;

    private AssociationMapper associationMapper;

    private Mockers mockers;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    private static final String DEFAULT_DISPLAY_NAME = "Not provided";

    private static final String DEFAULT_KIND = "association";

    @BeforeEach
    void setup() {
        associationMapper = new AssociationMapperImpl();
        associationMapper.usersService = usersService;
        associationMapper.companyService = companyService;
        mockers = new Mockers(null, null, companyService, usersService);
    }

    @Test
    void localDateTimeToOffsetDateTimeWithNullReturnsNull(){
        Assertions.assertNull(associationMapper.localDateTimeToOffsetDateTime(null));
    }

    @Test
    void localDateTimeToOffsetDateTimeReturnsCorrectTimestamp(){
        final var inputDate = LocalDateTime.now();
        final var outputDate = associationMapper.localDateTimeToOffsetDateTime(inputDate);
        Assertions.assertEquals(localDateTimeToNormalisedString(inputDate), reduceTimestampResolution(outputDate.toString()));
    }

    @Test
    void enrichWithUserDetailsUsesDataFromInputObject(){
        final var user = testDataManager.fetchUserDtos("111").getFirst();
        final var associationPreprocessed = testDataManager.fetchAssociationDto("1", user).userEmail(null).displayName(null);

        associationMapper.enrichWithUserDetails(associationPreprocessed, user);

        Assertions.assertEquals("bruce.wayne@gotham.city", associationPreprocessed.getUserEmail());
        Assertions.assertEquals("Batman", associationPreprocessed.getDisplayName());
    }

    @Test
    void enrichWithUserDetailsRetrievesDataIfInputObjectIsNull(){
        final var user = testDataManager.fetchUserDtos("111").getFirst();
        final var associationPreprocessed = testDataManager.fetchAssociationDto("1", user).userEmail(null).displayName(null);

//        mockers.mockUsersServiceFetchUserDetails("111");

        associationMapper.enrichWithUserDetails(associationPreprocessed, null);

        Assertions.assertEquals("bruce.wayne@gotham.city", associationPreprocessed.getUserEmail());
        Assertions.assertEquals("Batman", associationPreprocessed.getDisplayName());
    }

    @Test
    void enrichWithUserDetailsUsesDefaultDataIfInputObjectAndUserIdAreNull(){
        final var user = testDataManager.fetchUserDtos("111").getFirst();
        final var associationPreprocessed = testDataManager.fetchAssociationDto("1", user).userId(null).displayName(null);

        associationMapper.enrichWithUserDetails(associationPreprocessed, null);

        Assertions.assertEquals("bruce.wayne@gotham.city", associationPreprocessed.getUserEmail());
        Assertions.assertEquals(DEFAULT_DISPLAY_NAME, associationPreprocessed.getDisplayName());
    }

    @Test
    void enrichWithCompanyDetailsUsesDataFromInputObject(){
        final var user = testDataManager.fetchUserDtos("111").getFirst();
        final var company = testDataManager.fetchCompanyDetailsDtos("111111").getFirst();
        final var associationPreprocessed = testDataManager.fetchAssociationDto("1", user).companyName(null).companyStatus(null);

        associationMapper.enrichWithCompanyDetails(associationPreprocessed, company);

        Assertions.assertEquals("Wayne Enterprises", associationPreprocessed.getCompanyName());
        Assertions.assertEquals("active", associationPreprocessed.getCompanyStatus());
    }

    @Test
    void enrichWithCompanyDetailsRetrievesDataIfInputObjectIsNull(){
        final var user = testDataManager.fetchUserDtos("111").getFirst();
        final var associationPreprocessed = testDataManager.fetchAssociationDto("1", user).companyName(null).companyStatus(null);

        mockers.mockCompanyServiceFetchCompanyProfile("111111");

        associationMapper.enrichWithCompanyDetails(associationPreprocessed, null);

        Assertions.assertEquals("Wayne Enterprises", associationPreprocessed.getCompanyName());
        Assertions.assertEquals("active", associationPreprocessed.getCompanyStatus());
    }

    @Test
    void enrichAssociationWithLinksAndKindCorrectlyUpdatesAssociation(){
        final var association = new Association().id("1");
        associationMapper.enrichAssociationWithLinksAndKind(association);
        Assertions.assertEquals("/associations/1", association.getLinks().getSelf());
        Assertions.assertEquals(DEFAULT_KIND, association.getKind());
    }

    @Test
    void daoToDtoWithNullAssociationDaoReturnsNull(){
        Assertions.assertNull(associationMapper.daoToDto(null, null, null));
    }

    static Stream<Arguments> daoToDtoTestData(){
        final var associationDao = testDataManager.fetchAssociationDaos("1").getFirst();
        final var userDto = testDataManager.fetchUserDtos("111").getFirst();
        final var companyDto = testDataManager.fetchCompanyDetailsDtos("111111").getFirst();
        final var expectedAssociation = testDataManager.fetchAssociationDto("1", userDto);
        return Stream.of(
                Arguments.of(associationDao, expectedAssociation, null, null),
                Arguments.of(associationDao, expectedAssociation, userDto, null),
                Arguments.of(associationDao, expectedAssociation, null, companyDto),
                Arguments.of(associationDao, expectedAssociation, userDto, companyDto)
      );
    }

    @ParameterizedTest
    @MethodSource("daoToDtoTestData")
    void daoToDtoPerformsMappingRegardlessOfAvailableData(final AssociationDao associationDao, final Association expectedAssociation, final User userDto, final CompanyDetails companyDto){
        if (Objects.isNull(userDto)) {
//            mockers.mockUsersServiceFetchUserDetails("111");
        }

        if (Objects.isNull(companyDto)) {
            mockers.mockCompanyServiceFetchCompanyProfile("111111");
        }

        final var association = associationMapper.daoToDto(associationDao, userDto, companyDto);
        Assertions.assertTrue(comparisonUtils.compare(expectedAssociation, List.of("createdAt", "approvedAt", "removedAt", "etag", "id", "userId", "userEmail", "displayName", "companyNumber", "companyName", "status", "companyStatus", "kind", "approvalRoute", "approvalExpiryAt", "links"), List.of(), Map.of()).matches(association));
    }

}
