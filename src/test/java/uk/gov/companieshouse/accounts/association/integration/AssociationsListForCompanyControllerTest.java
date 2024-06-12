package uk.gov.companieshouse.accounts.association.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.accounts.association.configuration.InterceptorConfig;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.accounts.association.rest.CompanyProfileEndpoint;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserUserGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.sdk.ApiClientService;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class AssociationsListForCompanyControllerTest {

    @Value("${internal.api.url}")
    private String internalApiUrl;

    @Container
    @ServiceConnection
    static MongoDBContainer container = new MongoDBContainer("mongo:5");

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    public MockMvc mockMvc;

    @MockBean
    ApiClientService apiClientService;

    @MockBean
    InternalApiClient internalApiClient;

    @MockBean
    CompanyProfileEndpoint companyProfileEndpoint;

    @MockBean
    AccountsUserEndpoint accountsUserEndpoint;

    @MockBean
    EmailProducer emailProducer;

    @MockBean
    KafkaProducerFactory kafkaProducerFactory;

    @Autowired
    AssociationsRepository associationsRepository;

    @MockBean
    InterceptorConfig interceptorConfig;

    @MockBean
    StaticPropertyUtil staticPropertyUtil;


    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet111;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet222;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet333;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet444;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet555;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet666;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet777;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet888;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet999;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet1111;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet2222;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet3333;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet4444;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet5555;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet6666;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet7777;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet9191;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet8888;

    private static final String DEFAULT_KIND = "association";
    private static final String DEFAULT_DISPLAY_NAME = "Not provided";

    private final LocalDateTime now = LocalDateTime.now();

    private ApiResponse<CompanyDetails> toCompanyDetailsApiResponse( final String companyNumber, final String companyName ){
        final var companyDetails = new CompanyDetails();
        companyDetails.setCompanyNumber( companyNumber );
        companyDetails.setCompanyName( companyName );
        return new ApiResponse<>( 200, Map.of(), companyDetails );
    }

    private ApiResponse<User> toGetUserDetailsApiResponse( final String email, final String displayName ){
        final var user = new User().email( email ).displayName( displayName );
        return new ApiResponse<>( 200, Map.of(), user );
    }

    @BeforeEach
    public void setup() throws ApiErrorResponseException, URIValidationException {

        final var invitationOne = new InvitationDao();
        invitationOne.setInvitedBy("666");
        invitationOne.setInvitedAt(now.plusDays(4));

        final var associationOne = new AssociationDao();
        associationOne.setCompanyNumber("111111");
        associationOne.setUserId("111");
        associationOne.setUserEmail("bruce.wayne@gotham.city");
        associationOne.setStatus(StatusEnum.CONFIRMED.getValue());
        associationOne.setId("1");
        associationOne.setApprovedAt(now.plusDays(1));
        associationOne.setRemovedAt(now.plusDays(2));
        associationOne.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationOne.setApprovalExpiryAt(now.plusDays(3));
        associationOne.setInvitations( List.of( invitationOne ) );
        associationOne.setEtag( "a" );

        final var invitationTwo = new InvitationDao();
        invitationTwo.setInvitedBy("666");
        invitationTwo.setInvitedAt( now.plusDays(8) );

        final var associationTwo = new AssociationDao();
        associationTwo.setCompanyNumber("111111");
        associationTwo.setUserId("222");
        associationTwo.setUserEmail("the.joker@gotham.city");
        associationTwo.setStatus(StatusEnum.CONFIRMED.getValue());
        associationTwo.setId("2");
        associationTwo.setApprovedAt( now.plusDays(5) );
        associationTwo.setRemovedAt( now.plusDays(6) );
        associationTwo.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationTwo.setApprovalExpiryAt( now.plusDays(7) );
        associationTwo.setInvitations( List.of( invitationTwo ) );
        associationTwo.setEtag("b");

        final var invitationThree = new InvitationDao();
        invitationThree.setInvitedBy("666");
        invitationThree.setInvitedAt( now.plusDays(12) );

        final var associationThree = new AssociationDao();
        associationThree.setCompanyNumber("111111");
        associationThree.setUserId("333");
        associationThree.setUserEmail("harley.quinn@gotham.city");
        associationThree.setStatus(StatusEnum.CONFIRMED.getValue());
        associationThree.setId("3");
        associationThree.setApprovedAt( now.plusDays(9) );
        associationThree.setRemovedAt( now.plusDays(10) );
        associationThree.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationThree.setApprovalExpiryAt( now.plusDays(11) );
        associationThree.setInvitations( List.of( invitationThree ) );
        associationThree.setEtag("c");

        final var invitationFour = new InvitationDao();
        invitationFour.setInvitedBy("666");
        invitationFour.setInvitedAt( now.plusDays(16) );

        final var associationFour = new AssociationDao();
        associationFour.setCompanyNumber("111111");
        associationFour.setUserId("444");
        associationFour.setUserEmail("robin@gotham.city");
        associationFour.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFour.setId("4");
        associationFour.setApprovedAt( now.plusDays(13) );
        associationFour.setRemovedAt( now.plusDays(14) );
        associationFour.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationFour.setApprovalExpiryAt( now.plusDays(15) );
        associationFour.setInvitations( List.of( invitationFour ) );
        associationFour.setEtag("d");

        final var invitationFive = new InvitationDao();
        invitationFive.setInvitedBy("666");
        invitationFive.setInvitedAt( now.plusDays(20) );

        final var associationFive = new AssociationDao();
        associationFive.setCompanyNumber("111111");
        associationFive.setUserId("555");
        associationFive.setUserEmail("barbara.gordon@gotham.city");
        associationFive.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFive.setId("5");
        associationFive.setApprovedAt( now.plusDays(17) );
        associationFive.setRemovedAt( now.plusDays(18) );
        associationFive.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFive.setApprovalExpiryAt( now.plusDays(19) );
        associationFive.setInvitations( List.of( invitationFive ) );
        associationFive.setEtag("e");

        final var invitationSix = new InvitationDao();
        invitationSix.setInvitedBy("5555");
        invitationSix.setInvitedAt( now.plusDays(24) );

        final var associationSix = new AssociationDao();
        associationSix.setCompanyNumber("111111");
        associationSix.setUserId("666");
        associationSix.setUserEmail("homer.simpson@springfield.com");
        associationSix.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationSix.setId("6");
        associationSix.setApprovedAt( now.plusDays(21) );
        associationSix.setRemovedAt( now.plusDays(22) );
        associationSix.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationSix.setApprovalExpiryAt( now.plusDays(23) );
        associationSix.setInvitations( List.of( invitationSix ) );
        associationSix.setEtag("f");

        final var invitationSeven = new InvitationDao();
        invitationSeven.setInvitedBy("5555");
        invitationSeven.setInvitedAt( now.plusDays(28) );

        final var associationSeven = new AssociationDao();
        associationSeven.setCompanyNumber("111111");
        associationSeven.setUserId("777");
        associationSeven.setUserEmail("marge.simpson@springfield.com");
        associationSeven.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationSeven.setId("7");
        associationSeven.setApprovedAt( now.plusDays(25) );
        associationSeven.setRemovedAt( now.plusDays(26) );
        associationSeven.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationSeven.setApprovalExpiryAt( now.plusDays(27) );
        associationSeven.setInvitations( List.of( invitationSeven ) );
        associationSeven.setEtag("g");

        final var invitationEight = new InvitationDao();
        invitationEight.setInvitedBy("5555");
        invitationEight.setInvitedAt( now.plusDays(32) );

        final var associationEight = new AssociationDao();
        associationEight.setCompanyNumber("111111");
        associationEight.setUserId("888");
        associationEight.setUserEmail("bart.simpson@springfield.com");
        associationEight.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationEight.setId("8");
        associationEight.setApprovedAt( now.plusDays(29) );
        associationEight.setRemovedAt( now.plusDays(30) );
        associationEight.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationEight.setApprovalExpiryAt( now.plusDays(31) );
        associationEight.setInvitations( List.of( invitationEight ) );
        associationEight.setEtag("h");

        final var invitationNine = new InvitationDao();
        invitationNine.setInvitedBy("5555");
        invitationNine.setInvitedAt( now.plusDays(36) );

        final var associationNine = new AssociationDao();
        associationNine.setCompanyNumber("111111");
        associationNine.setUserId("999");
        associationNine.setUserEmail("lisa.simpson@springfield.com");
        associationNine.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationNine.setId("9");
        associationNine.setApprovedAt( now.plusDays(33) );
        associationNine.setRemovedAt( now.plusDays(34) );
        associationNine.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationNine.setApprovalExpiryAt( now.plusDays(35) );
        associationNine.setInvitations( List.of( invitationNine ) );
        associationNine.setEtag("i");

        final var invitationTen = new InvitationDao();
        invitationTen.setInvitedBy("5555");
        invitationTen.setInvitedAt( now.plusDays(40) );

        final var associationTen = new AssociationDao();
        associationTen.setCompanyNumber("111111");
        associationTen.setUserId("1111");
        associationTen.setUserEmail("maggie.simpson@springfield.com");
        associationTen.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTen.setId("10");
        associationTen.setApprovedAt( now.plusDays(37) );
        associationTen.setRemovedAt( now.plusDays(38) );
        associationTen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTen.setApprovalExpiryAt( now.plusDays(39) );
        associationTen.setInvitations( List.of( invitationTen ) );
        associationTen.setEtag("j");

        final var invitationEleven = new InvitationDao();
        invitationEleven.setInvitedBy("5555");
        invitationEleven.setInvitedAt( now.plusDays(44) );

        final var associationEleven = new AssociationDao();
        associationEleven.setCompanyNumber("111111");
        associationEleven.setUserId("2222");
        associationEleven.setUserEmail("crusty.the.clown@springfield.com");
        associationEleven.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationEleven.setId("11");
        associationEleven.setApprovedAt( now.plusDays(41) );
        associationEleven.setRemovedAt( now.plusDays(42) );
        associationEleven.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationEleven.setApprovalExpiryAt( now.plusDays(43) );
        associationEleven.setInvitations( List.of( invitationEleven ) );
        associationEleven.setEtag("k");

        final var invitationTwelve = new InvitationDao();
        invitationTwelve.setInvitedBy("5555");
        invitationTwelve.setInvitedAt( now.plusDays(48) );

        final var associationTwelve = new AssociationDao();
        associationTwelve.setCompanyNumber("111111");
        associationTwelve.setUserId("3333");
        associationTwelve.setUserEmail("itchy@springfield.com");
        associationTwelve.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwelve.setId("12");
        associationTwelve.setApprovedAt( now.plusDays(45) );
        associationTwelve.setRemovedAt( now.plusDays(46) );
        associationTwelve.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwelve.setApprovalExpiryAt( now.plusDays(47) );
        associationTwelve.setInvitations( List.of( invitationTwelve ) );
        associationTwelve.setEtag("l");

        final var invitationThirteen = new InvitationDao();
        invitationThirteen.setInvitedBy("5555");
        invitationThirteen.setInvitedAt( now.plusDays(52) );

        final var associationThirteen = new AssociationDao();
        associationThirteen.setCompanyNumber("111111");
        associationThirteen.setUserId("4444");
        associationThirteen.setUserEmail("scratchy@springfield.com");
        associationThirteen.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationThirteen.setId("13");
        associationThirteen.setApprovedAt( now.plusDays(49) );
        associationThirteen.setRemovedAt( now.plusDays(50) );
        associationThirteen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationThirteen.setApprovalExpiryAt( now.plusDays(51) );
        associationThirteen.setInvitations( List.of( invitationThirteen ) );
        associationThirteen.setEtag("m");

        final var invitationFourteen = new InvitationDao();
        invitationFourteen.setInvitedBy("111");
        invitationFourteen.setInvitedAt( now.plusDays(56) );

        final var associationFourteen = new AssociationDao();
        associationFourteen.setCompanyNumber("111111");
        associationFourteen.setUserId("5555");
        associationFourteen.setUserEmail("ross@friends.com");
        associationFourteen.setStatus(StatusEnum.REMOVED.getValue());
        associationFourteen.setId("14");
        associationFourteen.setApprovedAt( now.plusDays(53) );
        associationFourteen.setRemovedAt( now.plusDays(54) );
        associationFourteen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFourteen.setApprovalExpiryAt( now.plusDays(55) );
        associationFourteen.setInvitations( List.of( invitationFourteen ) );
        associationFourteen.setEtag("n");

        final var invitationFifteen = new InvitationDao();
        invitationFifteen.setInvitedBy("111");
        invitationFifteen.setInvitedAt( now.plusDays(60) );

        final var associationFifteen = new AssociationDao();
        associationFifteen.setCompanyNumber("111111");
        associationFifteen.setUserId("6666");
        associationFifteen.setUserEmail("rachel@friends.com");
        associationFifteen.setStatus(StatusEnum.REMOVED.getValue());
        associationFifteen.setId("15");
        associationFifteen.setApprovedAt( now.plusDays(57) );
        associationFifteen.setRemovedAt( now.plusDays(58) );
        associationFifteen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFifteen.setApprovalExpiryAt( now.plusDays(59) );
        associationFifteen.setInvitations( List.of( invitationFifteen ) );
        associationFifteen.setEtag("o");

        final var invitationSixteen = new InvitationDao();
        invitationSixteen.setInvitedBy("111");
        invitationSixteen.setInvitedAt( now.plusDays(64) );

        final var associationSixteen = new AssociationDao();
        associationSixteen.setCompanyNumber("111111");
        associationSixteen.setUserId("7777");
        associationSixteen.setUserEmail("chandler@friends.com");
        associationSixteen.setStatus(StatusEnum.REMOVED.getValue());
        associationSixteen.setId("16");
        associationSixteen.setApprovedAt( now.plusDays(61) );
        associationSixteen.setRemovedAt( now.plusDays(62) );
        associationSixteen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationSixteen.setApprovalExpiryAt( now.plusDays(63) );
        associationSixteen.setInvitations( List.of( invitationSixteen ) );
        associationSixteen.setEtag("p");

        final var invitationSeventeen = new InvitationDao();
        invitationSeventeen.setInvitedBy("111");
        invitationSeventeen.setInvitedAt( now.plusDays(68) );

        final var associationSeventeen = new AssociationDao();
        associationSeventeen.setCompanyNumber("222222");
        associationSeventeen.setUserId("8888");
        associationSeventeen.setUserEmail("mr.blobby@nightmare.com");
        associationSeventeen.setStatus(StatusEnum.CONFIRMED.getValue());
        associationSeventeen.setId("17");
        associationSeventeen.setApprovedAt( now.plusDays(65) );
        associationSeventeen.setRemovedAt( now.plusDays(66) );
        associationSeventeen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationSeventeen.setApprovalExpiryAt( now.plusDays(67) );
        associationSeventeen.setInvitations( List.of( invitationSeventeen ) );
        associationSeventeen.setEtag("q");

        associationsRepository.insert( List.of( associationOne, associationTwo, associationThree, associationFour,
                associationFive, associationSix, associationSeven, associationEight, associationNine, associationTen,
                associationEleven, associationTwelve, associationThirteen, associationFourteen, associationFifteen,
                associationSixteen, associationSeventeen) );

        Mockito.doReturn( toCompanyDetailsApiResponse( "111111", "Wayne Enterprises" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( eq( "111111" ) );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( companyProfileEndpoint ).fetchCompanyProfile( eq( "919191" ) );

        Mockito.doReturn( privateAccountsUserUserGet111 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "111" );
        Mockito.doReturn( privateAccountsUserUserGet222 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "222" );
        Mockito.doReturn( privateAccountsUserUserGet333 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "333" );
        Mockito.doReturn( privateAccountsUserUserGet444 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "444" );
        Mockito.doReturn( privateAccountsUserUserGet555 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "555" );
        Mockito.doReturn( privateAccountsUserUserGet666 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "666" );
        Mockito.doReturn( privateAccountsUserUserGet777 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "777" );
        Mockito.doReturn( privateAccountsUserUserGet888 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "888" );
        Mockito.doReturn( privateAccountsUserUserGet999 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "999" );
        Mockito.doReturn( privateAccountsUserUserGet1111 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "1111" );
        Mockito.doReturn( privateAccountsUserUserGet2222 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "2222" );
        Mockito.doReturn( privateAccountsUserUserGet3333 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "3333" );
        Mockito.doReturn( privateAccountsUserUserGet4444 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "4444" );
        Mockito.doReturn( privateAccountsUserUserGet5555 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "5555" );
        Mockito.doReturn( privateAccountsUserUserGet6666 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "6666" );
        Mockito.doReturn( privateAccountsUserUserGet7777 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "7777" );
        Mockito.doReturn( privateAccountsUserUserGet9191 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "9191" );

        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "bruce.wayne@gotham.city", "Batman" ) ).when( privateAccountsUserUserGet111 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "the.joker@gotham.city", null ) ).when( privateAccountsUserUserGet222 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "harley.quinn@gotham.city", "Harleen Quinzel" ) ).when( privateAccountsUserUserGet333 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "robin@gotham.city", "Boy Wonder" ) ).when( privateAccountsUserUserGet444 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "barbara.gordon@gotham.city", "Batwoman" ) ).when( privateAccountsUserUserGet555 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "homer.simpson@springfield.com", null ) ).when( privateAccountsUserUserGet666 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "marge.simpson@springfield.com", null ) ).when( privateAccountsUserUserGet777 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "bart.simpson@springfield.com", null ) ).when( privateAccountsUserUserGet888 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "lisa.simpson@springfield.com", null ) ).when( privateAccountsUserUserGet999 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "maggie.simpson@springfield.com", null ) ).when( privateAccountsUserUserGet1111 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "crusty.the.clown@springfield.com", null ) ).when( privateAccountsUserUserGet2222 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "itchy@springfield.com", null ) ).when( privateAccountsUserUserGet3333 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "scratchy@springfield.com", null ) ).when( privateAccountsUserUserGet4444 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "ross@friends.com", null ) ).when( privateAccountsUserUserGet5555 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "rachel@friends.com", null ) ).when( privateAccountsUserUserGet6666 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "chandler@friends.com", null ) ).when( privateAccountsUserUserGet7777 ).execute();
        Mockito.lenient().doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet9191 ).execute();

        Mockito.doNothing().when(interceptorConfig).addInterceptors( any() );
    }

    @Test
    void getAssociationsForCompanyWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/companies/{company_number}", "$$$$$$" ).header("X-Request-Id", "theId123") )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyWithNonexistentCompanyReturnsNotFound() throws Exception {
        mockMvc.perform( get( "/associations/companies/{company_number}", "919191" ).header("X-Request-Id", "theId123") )
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse();
    }

    @Test
    void getAssociationsForCompanyWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/companies/{company_number}", "111111" ) )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyWithoutQueryParamsUsesDefaults() throws Exception {
        final var response =
                mockMvc.perform( get( "/associations/companies/{company_number}", "111111" ).header("X-Request-Id", "theId123") )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        final AssociationsList associationsList = objectMapper.readValue( response.getContentAsByteArray(), AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" ) ) );
        Assertions.assertEquals( "/associations/companies/111111?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 13, associationsList.getTotalResults() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedFalseAppliesFilter() throws Exception {
        final var response =
                mockMvc.perform( get( "/associations/companies/{company_number}?include_removed=false", "111111" ).header("X-Request-Id", "theId123") )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        final AssociationsList associationsList = objectMapper.readValue( response.getContentAsByteArray(), AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" ) ) );
        Assertions.assertEquals( "/associations/companies/111111?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 13, associationsList.getTotalResults() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedTrueDoesNotApplyFilter() throws Exception {
        final var response =
                mockMvc.perform( get( "/associations/companies/{company_number}?include_removed=true", "111111" ).header("X-Request-Id", "theId123") )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        final AssociationsList associationsList = objectMapper.readValue( response.getContentAsByteArray(), AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15" ) ) );
        Assertions.assertEquals( "/associations/companies/111111?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "/associations/companies/111111?page_index=1&items_per_page=15", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 16, associationsList.getTotalResults() );
        Assertions.assertEquals( 2, associationsList.getTotalPages() );
    }

    @Test
    void getAssociationsForCompanyPaginatesCorrectly() throws Exception {
        final var response =
                mockMvc.perform( get( "/associations/companies/{company_number}?include_removed=true&items_per_page=3&page_index=2", "111111" ).header("X-Request-Id", "theId123") )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        final AssociationsList associationsList = objectMapper.readValue( response.getContentAsByteArray(), AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "7", "8", "9" ) ) );
        Assertions.assertEquals( "/associations/companies/111111?page_index=2&items_per_page=3", links.getSelf() );
        Assertions.assertEquals( "/associations/companies/111111?page_index=3&items_per_page=3", links.getNext() );
        Assertions.assertEquals( 2, associationsList.getPageNumber() );
        Assertions.assertEquals( 3, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 16, associationsList.getTotalResults() );
        Assertions.assertEquals( 6, associationsList.getTotalPages() );
    }

    @Test
    void getAssociationsForCompanyWhereAccountsUserEndpointCannotFindUserReturnsNotFound() throws Exception {
        Mockito.doReturn( privateAccountsUserUserGet8888 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "8888" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "222222", "Nightmare" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( eq( "222222" ) );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet8888 ).execute();

        mockMvc.perform( get( "/associations/companies/{company_number}", "222222" ).header("X-Request-Id", "theId123") )
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationsForCompanyWhereCompanyProfileEndpointCannotFindCompanyReturnsNotFound() throws Exception {
        Mockito.doReturn( toGetUserDetailsApiResponse( "mr.blobby@nightmare.com", "Mr Blobby" ) ).when( accountsUserEndpoint ).getUserDetails( eq( "8888" ) );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( companyProfileEndpoint ).fetchCompanyProfile( eq( "222222" ) );

        mockMvc.perform( get( "/associations/companies/{company_number}", "222222" ).header("X-Request-Id", "theId123") )
                .andExpect(status().isNotFound());
    }

    private String reduceTimestampResolution( String timestamp ){
        return timestamp.substring( 0, timestamp.indexOf( ":" ) );
    }

    private String localDateTimeToNormalisedString( LocalDateTime localDateTime ){
        final var timestamp = localDateTime.toString();
        return reduceTimestampResolution( timestamp );
    }

    @Test
    void getAssociationsForCompanyDoesMappingCorrectly() throws Exception {

        final var response =
                mockMvc.perform( get( "/associations/companies/{company_number}?include_removed=true&items_per_page=2&page_index=0", "111111" ).header("X-Request-Id", "theId123") )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        final AssociationsList associationsList = objectMapper.readValue( response.getContentAsByteArray(), AssociationsList.class );

        final var associations = associationsList.getItems();
        final var associationOne = associations.getFirst();

        Assertions.assertEquals( "a", associationOne.getEtag() );
        Assertions.assertEquals( "1", associationOne.getId() );
        Assertions.assertEquals( "111", associationOne.getUserId() );
        Assertions.assertEquals( "bruce.wayne@gotham.city", associationOne.getUserEmail() );
        Assertions.assertEquals( "Batman", associationOne.getDisplayName() );
        Assertions.assertEquals( "111111", associationOne.getCompanyNumber() );
        Assertions.assertEquals( "Wayne Enterprises", associationOne.getCompanyName() );
        Assertions.assertEquals( StatusEnum.CONFIRMED, associationOne.getStatus() );
        Assertions.assertNotNull( associationOne.getCreatedAt() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(1) ), localDateTimeToNormalisedString( associationOne.getApprovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(2) ), localDateTimeToNormalisedString( associationOne.getRemovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( DEFAULT_KIND, associationOne.getKind() );
        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE, associationOne.getApprovalRoute() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(3) ), reduceTimestampResolution( associationOne.getApprovalExpiryAt() ) );
        Assertions.assertEquals( "/associations/1", associationOne.getLinks().getSelf() );

        final var associationTwo = associations.get( 1 );
        Assertions.assertEquals( "222", associationTwo.getUserId() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, associationTwo.getDisplayName() );
        Assertions.assertEquals( "Wayne Enterprises", associationTwo.getCompanyName() );
    }

    @Test
    void getAssociationsForCompanyWithUnacceptablePaginationParametersShouldReturnBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/companies/{company_number}?include_removed=true&items_per_page=1&page_index=-1", "111111" ).header("X-Request-Id", "theId123") )
                .andExpect(status().isBadRequest());

        mockMvc.perform( get( "/associations/companies/{company_number}?include_removed=true&items_per_page=0&page_index=0", "111111" ).header("X-Request-Id", "theId123") )
                .andExpect(status().isBadRequest());
    }


    @Test
    void getAssociationsForCompanyFetchesAssociation() throws Exception {
       final var response =
        mockMvc.perform( get( "/associations/companies/{company_number}", "111111" ).header("X-Request-Id", "theId123") )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        final var associationsList = objectMapper.readValue( response, AssociationsList.class );

        Assertions.assertEquals( 13, associationsList.getTotalResults() );
    }


    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }

}