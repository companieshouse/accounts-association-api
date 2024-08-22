package uk.gov.companieshouse.accounts.association.mapper;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.Preprocessors.ReduceTimeStampResolutionPreprocessor;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.MapperUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationLinks;

import java.util.List;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AssociationMapperTest {

    @Mock
    private CompanyService companyService;
    @Mock
    private UsersService usersService;

    @InjectMocks
    private AssociationMapper associationUserDaoToDtoMapper;

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    private static final String DEFAULT_DISPLAY_NAME = "Not provided";
    private static final String DEFAULT_KIND = "association";

    private static final TestDataManager testDataManager = TestDataManager.getInstance();
    private Mockers mockers;

    @BeforeEach
    public void setup(){
        associationUserDaoToDtoMapper = new AssociationMapper(new MapperUtil(usersService,companyService),new BaseMapperImpl());
        mockers = new Mockers( null, null, null, companyService, usersService );
    }

    @Test
    void enrichAssociationWithUserDetailsWithNullInputThrowsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> associationUserDaoToDtoMapper.enrichAssociation(null));
    }

    @Test
    void enrichAssociationWithUserDetailsWithoutDisplayNameSetsDefaultDisplayName() {
        final var user = testDataManager.fetchUserDtos( "222" ).getFirst();
        final var association = testDataManager.fetchAssociationDto( "2", user )
                        .userEmail( null ).displayName( null ).companyName( null );

        mockers.mockUsersServiceFetchUserDetails( "222" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );

        associationUserDaoToDtoMapper.enrichAssociation(association);

        Assertions.assertEquals("the.joker@gotham.city", association.getUserEmail());
        Assertions.assertEquals(DEFAULT_DISPLAY_NAME, association.getDisplayName());
    }

    @Test
    void enrichAssociationWithUserDetailsSetsDisplayName() {
        final var user = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var association = testDataManager.fetchAssociationDto( "1", user )
                .userEmail( null ).displayName( null ).companyName( null );

        mockers.mockUsersServiceFetchUserDetails( "111" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );

        associationUserDaoToDtoMapper.enrichAssociation(association);

        Assertions.assertEquals("bruce.wayne@gotham.city", association.getUserEmail());
        Assertions.assertEquals("Batman", association.getDisplayName());
    }

    @Test
    void daoToDtoWithNullInputReturnsNull() {
        Assertions.assertNull(associationUserDaoToDtoMapper.daoToDto(null));
    }

    @Test
    void daoToDtoWithOnlyMandatoryFieldsSuccessfullyPerformsMapping() {
        final var associationDao = new AssociationDao();
        associationDao.setId("1");
        associationDao.setUserId("111");
        associationDao.setCompanyNumber("111111");
        associationDao.setStatus(StatusEnum.CONFIRMED.getValue());
        associationDao.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());

        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        mockers.mockUsersServiceFetchUserDetails( "111" );

        final var dto = associationUserDaoToDtoMapper.daoToDto(associationDao);
        final var links = dto.getLinks();

        Assertions.assertEquals("1", dto.getId());
        Assertions.assertEquals("111", dto.getUserId());
        Assertions.assertEquals("111111", dto.getCompanyNumber());
        Assertions.assertEquals("bruce.wayne@gotham.city", dto.getUserEmail());
        Assertions.assertEquals("Batman", dto.getDisplayName());
        Assertions.assertEquals("/associations/1", links.getSelf());
        Assertions.assertEquals(DEFAULT_KIND, dto.getKind());
    }

    @Test
    void daoToDtoWithAllFieldsSuccessfullyPerformsMapping() {
        final var user = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        final var expectedAssociationDto = testDataManager.fetchAssociationDto( "1", user )
                .links( new AssociationLinks().self( "/associations/1" ) );

        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        mockers.mockUsersServiceFetchUserDetails( "111" );

        final var associationDto = associationUserDaoToDtoMapper.daoToDto(associationDao);

        Assertions.assertTrue( comparisonUtils.compare( expectedAssociationDto, List.of( "etag", "id", "userId", "userEmail", "displayName", "companyNumber", "companyName", "status", "createdAt", "approvedAt", "removedAt", "kind", "approvalRoute", "approvalExpiryAt", "links" ), List.of(), Map.of( "createdAt", new ReduceTimeStampResolutionPreprocessor(),"approvedAt", new ReduceTimeStampResolutionPreprocessor(), "removedAt", new ReduceTimeStampResolutionPreprocessor(), "approvalExpiryAt", new ReduceTimeStampResolutionPreprocessor() ) ).matches( associationDto ) );
    }
}
