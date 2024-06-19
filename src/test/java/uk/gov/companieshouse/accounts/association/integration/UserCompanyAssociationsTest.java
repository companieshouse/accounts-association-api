package uk.gov.companieshouse.accounts.association.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
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
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthCodeConfirmationEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthorisationRemovedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationAcceptedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InviteEmailData;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.accounts.association.rest.CompanyProfileEndpoint;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut;
import uk.gov.companieshouse.api.accounts.associations.model.ResponseBodyPost;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserUserGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.sdk.ApiClientService;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_ACCEPTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITE_MESSAGE_TYPE;

@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
public class UserCompanyAssociationsTest {

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

    @Autowired
    EmailService emailService;

    @MockBean
    KafkaProducerFactory kafkaProducerFactory;

    @Autowired
    AssociationsRepository associationsRepository;

    @MockBean
    StaticPropertyUtil staticPropertyUtil;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet9999;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet111;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet666;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet5555;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet9191;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet$$$$;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet8888;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet000;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet222;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet333;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet444;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet99999;

    private static final String DEFAULT_KIND = "association";

    @Value( "${invitation.url}")
    private String COMPANY_INVITATIONS_URL;


    private final LocalDateTime now = LocalDateTime.now();

    private ApiResponse<CompanyDetails> toCompanyDetailsApiResponse( final String companyNumber, final String companyName ){
        final var companyDetails = new CompanyDetails();
        companyDetails.setCompanyNumber( companyNumber );
        companyDetails.setCompanyName( companyName );
        return new ApiResponse<>( 200, Map.of(), companyDetails );
    }

    private ApiResponse<User> toGetUserDetailsApiResponse( final String userId, final String email, final String displayName ){
        final var user = new User().userId(userId).email( email ).displayName( displayName );
        return new ApiResponse<>( 200, Map.of(), user );
    }

    private ApiResponse<UsersList> toSearchUserDetailsApiResponse( final String email, final String userId ){
        final var usersList = new UsersList();
        usersList.add( new User().email( email ).userId( userId ) );
        return new ApiResponse<>( 200, Map.of(), usersList );
    }

    CountDownLatch latch;

    @BeforeEach
    public void setup() throws ApiErrorResponseException, URIValidationException {

        final var invitationEighteen = new InvitationDao();
        invitationEighteen.setInvitedBy("666");
        invitationEighteen.setInvitedAt(now.plusDays(4));

        final var associationEighteen = new AssociationDao();
        associationEighteen.setCompanyNumber("333333");
        associationEighteen.setUserId("9999");
        //associationEighteen.setUserEmail("scrooge.mcduck@disney.land");
        associationEighteen.setStatus(StatusEnum.CONFIRMED.getValue());
        associationEighteen.setId("18");
        associationEighteen.setApprovedAt(now.plusDays(1));
        associationEighteen.setRemovedAt(now.plusDays(2));
        associationEighteen.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationEighteen.setApprovalExpiryAt(now.plusDays(3));
        associationEighteen.setInvitations( List.of( invitationEighteen ) );
        associationEighteen.setEtag( "aa" );

        final var invitationNineteen = new InvitationDao();
        invitationNineteen.setInvitedBy("666");
        invitationNineteen.setInvitedAt( now.plusDays(8) );

        final var associationNineteen = new AssociationDao();
        associationNineteen.setCompanyNumber("444444");
        associationNineteen.setUserId("9999");
        associationNineteen.setUserEmail("scrooge.mcduck@disney.land");
        associationNineteen.setStatus(StatusEnum.CONFIRMED.getValue());
        associationNineteen.setId("19");
        associationNineteen.setApprovedAt( now.plusDays(5) );
        associationNineteen.setRemovedAt( now.plusDays(6) );
        associationNineteen.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationNineteen.setApprovalExpiryAt( now.plusDays(7) );
        associationNineteen.setInvitations( List.of( invitationNineteen ) );
        associationNineteen.setEtag("bb");

        final var invitationTwenty = new InvitationDao();
        invitationTwenty.setInvitedBy("666");
        invitationTwenty.setInvitedAt( now.plusDays(12) );

        final var associationTwenty = new AssociationDao();
        associationTwenty.setCompanyNumber("555555");
        associationTwenty.setUserId("9999");
        associationTwenty.setUserEmail("scrooge.mcduck@disney.land");
        associationTwenty.setStatus(StatusEnum.CONFIRMED.getValue());
        associationTwenty.setId("20");
        associationTwenty.setApprovedAt( now.plusDays(9) );
        associationTwenty.setRemovedAt( now.plusDays(10) );
        associationTwenty.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationTwenty.setApprovalExpiryAt( now.plusDays(11) );
        associationTwenty.setInvitations( List.of( invitationTwenty ) );
        associationTwenty.setEtag("cc");

        final var invitationTwentyOne = new InvitationDao();
        invitationTwentyOne.setInvitedBy("666");
        invitationTwentyOne.setInvitedAt( now.plusDays(16) );

        final var associationTwentyOne = new AssociationDao();
        associationTwentyOne.setCompanyNumber("666666");
        associationTwentyOne.setUserId("9999");
        associationTwentyOne.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyOne.setStatus(StatusEnum.CONFIRMED.getValue());
        associationTwentyOne.setId("21");
        associationTwentyOne.setApprovedAt( now.plusDays(13) );
        associationTwentyOne.setRemovedAt( now.plusDays(14) );
        associationTwentyOne.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationTwentyOne.setApprovalExpiryAt( now.plusDays(15) );
        associationTwentyOne.setInvitations( List.of( invitationTwentyOne ) );
        associationTwentyOne.setEtag("dd");

        final var invitationTwentyTwo = new InvitationDao();
        invitationTwentyTwo.setInvitedBy("666");
        invitationTwentyTwo.setInvitedAt( now.plusDays(20) );

        final var associationTwentyTwo = new AssociationDao();
        associationTwentyTwo.setCompanyNumber("777777");
        associationTwentyTwo.setUserId("9999");
        associationTwentyTwo.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyTwo.setStatus(StatusEnum.CONFIRMED.getValue());
        associationTwentyTwo.setId("22");
        associationTwentyTwo.setApprovedAt( now.plusDays(17) );
        associationTwentyTwo.setRemovedAt( now.plusDays(18) );
        associationTwentyTwo.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentyTwo.setApprovalExpiryAt( now.plusDays(19) );
        associationTwentyTwo.setInvitations( List.of( invitationTwentyTwo ) );
        associationTwentyTwo.setEtag("ee");

        final var invitationTwentyThree = new InvitationDao();
        invitationTwentyThree.setInvitedBy("5555");
        invitationTwentyThree.setInvitedAt( now.plusDays(24) );

        final var associationTwentyThree = new AssociationDao();
        associationTwentyThree.setCompanyNumber("888888");
        associationTwentyThree.setUserId("9999");
        associationTwentyThree.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyThree.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentyThree.setId("23");
        associationTwentyThree.setApprovedAt( now.plusDays(21) );
        associationTwentyThree.setRemovedAt( now.plusDays(22) );
        associationTwentyThree.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentyThree.setApprovalExpiryAt( now.plusDays(23) );
        associationTwentyThree.setInvitations( List.of( invitationTwentyThree ) );
        associationTwentyThree.setEtag("ff");

        final var invitationTwentyFour = new InvitationDao();
        invitationTwentyFour.setInvitedBy("5555");
        invitationTwentyFour.setInvitedAt( now.plusDays(28) );

        final var associationTwentyFour = new AssociationDao();
        associationTwentyFour.setCompanyNumber("999999");
        associationTwentyFour.setUserId("9999");
        associationTwentyFour.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyFour.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentyFour.setId("24");
        associationTwentyFour.setApprovedAt( now.plusDays(25) );
        associationTwentyFour.setRemovedAt( now.plusDays(26) );
        associationTwentyFour.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentyFour.setApprovalExpiryAt( now.plusDays(27) );
        associationTwentyFour.setInvitations( List.of( invitationTwentyFour ) );
        associationTwentyFour.setEtag("gg");

        final var invitationTwentyFive = new InvitationDao();
        invitationTwentyFive.setInvitedBy("5555");
        invitationTwentyFive.setInvitedAt( now.plusDays(32) );

        final var associationTwentyFive = new AssociationDao();
        associationTwentyFive.setCompanyNumber("x111111");
        associationTwentyFive.setUserId("9999");
        associationTwentyFive.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyFive.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentyFive.setId("25");
        associationTwentyFive.setApprovedAt( now.plusDays(29) );
        associationTwentyFive.setRemovedAt( now.plusDays(30) );
        associationTwentyFive.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentyFive.setApprovalExpiryAt( now.plusDays(31) );
        associationTwentyFive.setInvitations( List.of( invitationTwentyFive ) );
        associationTwentyFive.setEtag("hh");

        final var invitationTwentySix = new InvitationDao();
        invitationTwentySix.setInvitedBy("5555");
        invitationTwentySix.setInvitedAt( now.plusDays(36) );

        final var associationTwentySix = new AssociationDao();
        associationTwentySix.setCompanyNumber("x222222");
        associationTwentySix.setUserId("9999");
        associationTwentySix.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentySix.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentySix.setId("26");
        associationTwentySix.setApprovedAt( now.plusDays(33) );
        associationTwentySix.setRemovedAt( now.plusDays(34) );
        associationTwentySix.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentySix.setApprovalExpiryAt( now.plusDays(35) );
        associationTwentySix.setInvitations( List.of( invitationTwentySix ) );
        associationTwentySix.setEtag("ii");

        final var invitationTwentySeven = new InvitationDao();
        invitationTwentySeven.setInvitedBy("5555");
        invitationTwentySeven.setInvitedAt( now.plusDays(40) );

        final var associationTwentySeven = new AssociationDao();
        associationTwentySeven.setCompanyNumber("x333333");
        associationTwentySeven.setUserId("9999");
        associationTwentySeven.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentySeven.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentySeven.setId("27");
        associationTwentySeven.setApprovedAt( now.plusDays(37) );
        associationTwentySeven.setRemovedAt( now.plusDays(38) );
        associationTwentySeven.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentySeven.setApprovalExpiryAt( now.plusDays(39) );
        associationTwentySeven.setInvitations( List.of( invitationTwentySeven ) );
        associationTwentySeven.setEtag("jj");

        final var invitationTwentyEight = new InvitationDao();
        invitationTwentyEight.setInvitedBy("5555");
        invitationTwentyEight.setInvitedAt( now.plusDays(44) );

        final var associationTwentyEight = new AssociationDao();
        associationTwentyEight.setCompanyNumber("x444444");
        associationTwentyEight.setUserId("9999");
        associationTwentyEight.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyEight.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentyEight.setId("28");
        associationTwentyEight.setApprovedAt( now.plusDays(41) );
        associationTwentyEight.setRemovedAt( now.plusDays(42) );
        associationTwentyEight.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentyEight.setApprovalExpiryAt( now.plusDays(43) );
        associationTwentyEight.setInvitations( List.of( invitationTwentyEight ) );
        associationTwentyEight.setEtag("kk");

        final var invitationTwentyNine = new InvitationDao();
        invitationTwentyNine.setInvitedBy("5555");
        invitationTwentyNine.setInvitedAt( now.plusDays(48) );

        final var associationTwentyNine = new AssociationDao();
        associationTwentyNine.setCompanyNumber("x555555");
        associationTwentyNine.setUserId("9999");
        associationTwentyNine.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyNine.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentyNine.setId("29");
        associationTwentyNine.setApprovedAt( now.plusDays(45) );
        associationTwentyNine.setRemovedAt( now.plusDays(46) );
        associationTwentyNine.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentyNine.setApprovalExpiryAt( now.plusDays(47) );
        associationTwentyNine.setInvitations( List.of( invitationTwentyNine ) );
        associationTwentyNine.setEtag("ll");

        final var invitationThirty = new InvitationDao();
        invitationThirty.setInvitedBy("5555");
        invitationThirty.setInvitedAt( now.plusDays(52) );

        final var associationThirty = new AssociationDao();
        associationThirty.setCompanyNumber("x666666");
        associationThirty.setUserId("9999");
        associationThirty.setUserEmail("scrooge.mcduck@disney.land");
        associationThirty.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationThirty.setId("30");
        associationThirty.setApprovedAt( now.plusDays(49) );
        associationThirty.setRemovedAt( now.plusDays(50) );
        associationThirty.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationThirty.setApprovalExpiryAt( now.plusDays(51) );
        associationThirty.setInvitations( List.of( invitationThirty ) );
        associationThirty.setEtag("mm");

        final var invitationThirtyOne = new InvitationDao();
        invitationThirtyOne.setInvitedBy("111");
        invitationThirtyOne.setInvitedAt( now.plusDays(56) );

        final var associationThirtyOne = new AssociationDao();
        associationThirtyOne.setCompanyNumber("x777777");
        associationThirtyOne.setUserId("9999");
        associationThirtyOne.setUserEmail("scrooge.mcduck@disney.land");
        associationThirtyOne.setStatus(StatusEnum.REMOVED.getValue());
        associationThirtyOne.setId("31");
        associationThirtyOne.setApprovedAt( now.plusDays(53) );
        associationThirtyOne.setRemovedAt( now.plusDays(54) );
        associationThirtyOne.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationThirtyOne.setApprovalExpiryAt( now.plusDays(55) );
        associationThirtyOne.setInvitations( List.of( invitationThirtyOne ) );
        associationThirtyOne.setEtag("nn");

        final var invitationThirtyTwo = new InvitationDao();
        invitationThirtyTwo.setInvitedBy("111");
        invitationThirtyTwo.setInvitedAt( now.plusDays(60) );

        final var associationThirtyTwo = new AssociationDao();
        associationThirtyTwo.setCompanyNumber("x888888");
        associationThirtyTwo.setUserId("9999");
        associationThirtyTwo.setUserEmail("scrooge.mcduck@disney.land");
        associationThirtyTwo.setStatus(StatusEnum.REMOVED.getValue());
        associationThirtyTwo.setId("32");
        associationThirtyTwo.setApprovedAt( now.plusDays(57) );
        associationThirtyTwo.setRemovedAt( now.plusDays(58) );
        associationThirtyTwo.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationThirtyTwo.setApprovalExpiryAt( now.plusDays(59) );
        associationThirtyTwo.setInvitations( List.of( invitationThirtyTwo ) );
        associationThirtyTwo.setEtag("oo");

        final var invitationThirtyThree = new InvitationDao();
        invitationThirtyThree.setInvitedBy("111");
        invitationThirtyThree.setInvitedAt( now.plusDays(64) );

        final var associationThirtyThree = new AssociationDao();
        associationThirtyThree.setCompanyNumber("x999999");
        associationThirtyThree.setUserId("9999");
        associationThirtyThree.setUserEmail("scrooge.mcduck@disney.land");
        associationThirtyThree.setStatus(StatusEnum.REMOVED.getValue());
        associationThirtyThree.setId("33");
        associationThirtyThree.setApprovedAt( now.plusDays(61) );
        associationThirtyThree.setRemovedAt( now.plusDays(62) );
        associationThirtyThree.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationThirtyThree.setApprovalExpiryAt( now.plusDays(63) );
        associationThirtyThree.setInvitations( List.of( invitationThirtyThree ) );
        associationThirtyThree.setEtag("pp");

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

        final var associationThirtyFour = new AssociationDao();
        associationThirtyFour.setCompanyNumber("333333");
        associationThirtyFour.setUserEmail("russell.howard@comedy.com");
        associationThirtyFour.setId("34");
        associationThirtyFour.setStatus(StatusEnum.REMOVED.getValue());
        associationThirtyFour.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationThirtyFour.setEtag("qq");

        final var associationThirtyFive = new AssociationDao();
        associationThirtyFive.setCompanyNumber("333333");
        associationThirtyFive.setUserId("111");
        associationThirtyFive.setId("35");
        associationThirtyFive.setStatus(StatusEnum.REMOVED.getValue());
        associationThirtyFive.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationThirtyFive.setEtag("rr");

        final var associationThirtySix = new AssociationDao();
        associationThirtySix.setCompanyNumber("111111");
        associationThirtySix.setUserId("111");
        associationThirtySix.setId("36");
        associationThirtySix.setStatus(StatusEnum.CONFIRMED.getValue());
        associationThirtySix.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationThirtySix.setEtag("rs");

        final var invitationTwo = new InvitationDao();
        invitationTwo.setInvitedBy("666");
        invitationTwo.setInvitedAt( now.plusDays(8) );

        final var associationTwo = new AssociationDao();
        associationTwo.setCompanyNumber("x999999");
        associationTwo.setUserId("222");
        associationTwo.setUserEmail("the.joker@gotham.city");
        associationTwo.setStatus(StatusEnum.CONFIRMED.getValue());
        associationTwo.setId("38");
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
        associationThree.setCompanyNumber("x999999");
        associationThree.setUserId("333");
        associationThree.setUserEmail("harley.quinn@gotham.city");
        associationThree.setStatus(StatusEnum.CONFIRMED.getValue());
        associationThree.setId("39");
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
        associationFour.setCompanyNumber("x999999");
        associationFour.setUserId("444");
        associationFour.setUserEmail("robin@gotham.city");
        associationFour.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFour.setId("40");
        associationFour.setApprovedAt( now.plusDays(13) );
        associationFour.setRemovedAt( now.plusDays(14) );
        associationFour.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationFour.setApprovalExpiryAt( now.plusDays(15) );
        associationFour.setInvitations( List.of( invitationFour ) );
        associationFour.setEtag("d");

        final var associationFortyOne = new AssociationDao();
        associationFortyOne.setCompanyNumber("x888888");
        associationFortyOne.setUserId("222");
        associationFortyOne.setUserEmail("the.joker@gotham.city");
        associationFortyOne.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFortyOne.setId("41");
        associationFortyOne.setApprovedAt( now.plusDays(5) );
        associationFortyOne.setRemovedAt( now.plusDays(6) );
        associationFortyOne.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationFortyOne.setApprovalExpiryAt( now.plusDays(7) );
        associationFortyOne.setInvitations( List.of() );
        associationFortyOne.setEtag("b");

        final var invitationFortyTwoA = new InvitationDao();
        invitationFortyTwoA.setInvitedBy("222");
        invitationFortyTwoA.setInvitedAt( now.plusDays(16) );

        final var invitationFortyTwoB = new InvitationDao();
        invitationFortyTwoB.setInvitedBy("444");
        invitationFortyTwoB.setInvitedAt( now.plusDays(14) );

        final var associationFortyTwo = new AssociationDao();
        associationFortyTwo.setCompanyNumber("x888888");
        associationFortyTwo.setUserId("333");
        associationFortyTwo.setUserEmail("harley.quinn@gotham.city");
        associationFortyTwo.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationFortyTwo.setId("42");
        associationFortyTwo.setApprovedAt( now.plusDays(9) );
        associationFortyTwo.setRemovedAt( now.plusDays(10) );
        associationFortyTwo.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFortyTwo.setApprovalExpiryAt( now.plusDays(11) );
        associationFortyTwo.setInvitations( List.of( invitationFortyTwoA, invitationFortyTwoB ) );
        associationFortyTwo.setEtag("c");

        final var associationFortyThree = new AssociationDao();
        associationFortyThree.setCompanyNumber("x888888");
        associationFortyThree.setUserId("444");
        associationFortyThree.setUserEmail("robin@gotham.city");
        associationFortyThree.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFortyThree.setId("43");
        associationFortyThree.setApprovedAt( now.plusDays(13) );
        associationFortyThree.setRemovedAt( now.plusDays(14) );
        associationFortyThree.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationFortyThree.setApprovalExpiryAt( now.plusDays(15) );
        associationFortyThree.setInvitations( List.of() );
        associationFortyThree.setEtag("d");

        final var associationFortyFour = new AssociationDao();
        associationFortyFour.setCompanyNumber("x777777");
        associationFortyFour.setUserId("777");
        associationFortyFour.setUserEmail("homer.simpson@springfield.com");
        associationFortyFour.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFortyFour.setId("44");
        associationFortyFour.setApprovedAt( now.plusDays(5) );
        associationFortyFour.setRemovedAt( now.plusDays(6) );
        associationFortyFour.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationFortyFour.setApprovalExpiryAt( now.plusDays(7) );
        associationFortyFour.setInvitations( List.of() );
        associationFortyFour.setEtag("b");

        final var invitationFortyFiveA = new InvitationDao();
        invitationFortyFiveA.setInvitedBy("777");
        invitationFortyFiveA.setInvitedAt( now.plusDays(16) );

        final var invitationFortyFiveB = new InvitationDao();
        invitationFortyFiveB.setInvitedBy("999");
        invitationFortyFiveB.setInvitedAt( now.plusDays(14) );

        final var associationFortyFive = new AssociationDao();
        associationFortyFive.setCompanyNumber("x777777");
        associationFortyFive.setUserId("888");
        associationFortyFive.setUserEmail("marge.simpson@springfield.com");
        associationFortyFive.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationFortyFive.setId("45");
        associationFortyFive.setApprovedAt( now.plusDays(9) );
        associationFortyFive.setRemovedAt( now.plusDays(10) );
        associationFortyFive.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFortyFive.setApprovalExpiryAt( now.plusDays(11) );
        associationFortyFive.setInvitations( List.of( invitationFortyFiveA, invitationFortyFiveB ) );
        associationFortyFive.setEtag("c");

        final var associationFortySix = new AssociationDao();
        associationFortySix.setCompanyNumber("x777777");
        associationFortySix.setUserId("999");
        associationFortySix.setUserEmail("bart.simpson@springfield.com");
        associationFortySix.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFortySix.setId("46");
        associationFortySix.setApprovedAt( now.plusDays(13) );
        associationFortySix.setRemovedAt( now.plusDays(14) );
        associationFortySix.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationFortySix.setApprovalExpiryAt( now.plusDays(15) );
        associationFortySix.setInvitations( List.of() );
        associationFortySix.setEtag("d");



        final var invitationFortySevenOldest = new InvitationDao();
        invitationFortySevenOldest.setInvitedBy("666");
        invitationFortySevenOldest.setInvitedAt(now.minusDays(9));

        final var invitationFortySevenMedian = new InvitationDao();
        invitationFortySevenMedian.setInvitedBy("333");
        invitationFortySevenMedian.setInvitedAt(now.minusDays(6));

        final var invitationFortySevenNewest = new InvitationDao();
        invitationFortySevenNewest.setInvitedBy("444");
        invitationFortySevenNewest.setInvitedAt(now.minusDays(4));

        final var associationFortySeven = new AssociationDao();
        associationFortySeven.setCompanyNumber("333333P");
        associationFortySeven.setUserId("99999");
        associationFortySeven.setUserEmail("scrooge.mcduck1@disney.land");
        associationFortySeven.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationFortySeven.setId("47");
        associationFortySeven.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFortySeven.setApprovalExpiryAt(now.plusDays(11));
        associationFortySeven.setInvitations( List.of( invitationFortySevenMedian, invitationFortySevenOldest, invitationFortySevenNewest ) );
        associationFortySeven.setEtag( "aa" );

        final var invitationFortyEightOldest = new InvitationDao();
        invitationFortyEightOldest.setInvitedBy("111");
        invitationFortyEightOldest.setInvitedAt( now.minusDays(3) );

        final var invitationFortyEightMedian = new InvitationDao();
        invitationFortyEightMedian.setInvitedBy("222");
        invitationFortyEightMedian.setInvitedAt( now.minusDays(2) );

        final var invitationFortyEightNewest = new InvitationDao();
        invitationFortyEightNewest.setInvitedBy("444");
        invitationFortyEightNewest.setInvitedAt( now.plusDays(8) );

        final var associationFortyEight = new AssociationDao();
        associationFortyEight.setCompanyNumber("444444P");
        associationFortyEight.setUserId("99999");
        associationFortyEight.setUserEmail("scrooge.mcduck1@disney.land");
        associationFortyEight.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationFortyEight.setId("48");
        associationFortyEight.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFortyEight.setApprovalExpiryAt( now.plusDays(8) );
        associationFortyEight.setInvitations( List.of( invitationFortyEightOldest, invitationFortyEightMedian, invitationFortyEightNewest ) );
        associationFortyEight.setEtag("bb");

        associationsRepository.insert( List.of( associationEighteen, associationNineteen, associationTwenty, associationTwentyOne,
                associationTwentyTwo, associationTwentyThree, associationTwentyFour, associationTwentyFive, associationTwentySix,
                associationTwentySeven, associationTwentyEight, associationTwentyNine, associationThirty, associationThirtyOne,
                associationThirtyTwo, associationThirtyThree, associationSeventeen, associationThirtyFour, associationThirtyFive, associationThirtySix,
                associationTwo, associationThree, associationFour, associationFortyOne, associationFortyTwo, associationFortyThree,
                associationFortyFour, associationFortyFive, associationFortySix, associationFortySeven, associationFortyEight ) );
        Mockito.doReturn( toCompanyDetailsApiResponse( "111111", "Sainsbury's" ) ).when( companyProfileEndpoint ).fetchCompanyProfile(  "111111" );

        Mockito.doReturn( toCompanyDetailsApiResponse( "333333", "Tesco" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "333333" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "444444", "Sainsbury's" ) ).when( companyProfileEndpoint ).fetchCompanyProfile(  "444444" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "555555", "Morrison" ) ).when( companyProfileEndpoint ).fetchCompanyProfile(  "555555" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "666666", "Aldi" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "666666" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "777777", "Lidl" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "777777" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "888888", "McDonald's" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "888888" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "999999", "Burger King" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "999999" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "x111111", "Pizza Hut" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "x111111" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "x222222", "Dominos" ) ).when( companyProfileEndpoint ).fetchCompanyProfile(  "x222222" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "x333333", "Pizza Express" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "x333333" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "x444444", "Nandos" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "x444444" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "x555555", "Subway" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "x555555" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "x666666", "Greggs" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "x666666" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "x777777", "Facebook" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "x777777" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "x888888", "Twitter" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "x888888" );
        Mockito.doReturn( toCompanyDetailsApiResponse( "x999999", "Instram" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "x999999" );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "919191" );

        Mockito.doReturn( privateAccountsUserUserGet9999 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "9999" );
        Mockito.doReturn( privateAccountsUserUserGet111 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "111" );
        Mockito.doReturn( privateAccountsUserUserGet666 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "666" );
        Mockito.doReturn( privateAccountsUserUserGet5555 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "5555" );
        Mockito.doReturn( privateAccountsUserUserGet9191 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "9191" );
        Mockito.doReturn( privateAccountsUserUserGet$$$$ ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "$$$$" );
        Mockito.doReturn( privateAccountsUserUserGet000 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "000" );
        Mockito.doReturn( privateAccountsUserUserGet222 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "222" );
        Mockito.doReturn( privateAccountsUserUserGet333 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "333" );
        Mockito.doReturn( privateAccountsUserUserGet444 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "444" );
        Mockito.doReturn( privateAccountsUserUserGet99999 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "99999" );

        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "9999", "scrooge.mcduck@disney.land", "Scrooge McDuck" ) ).when( privateAccountsUserUserGet9999 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "111", "bruce.wayne@gotham.city", "Batman" ) ).when( privateAccountsUserUserGet111 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "666", "homer.simpson@springfield.com", null ) ).when( privateAccountsUserUserGet666 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "5555", "ross@friends.com", null ) ).when( privateAccountsUserUserGet5555 ).execute();
        Mockito.lenient().doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet9191 ).execute();
        Mockito.lenient().doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet$$$$ ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "000", "zero@coke.com", null ) ).when( privateAccountsUserUserGet000 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "222", "the.joker@gotham.city", null ) ).when( privateAccountsUserUserGet222 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "333", "harley.quinn@gotham.city", null ) ).when( privateAccountsUserUserGet333 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "444", "robin@gotham.city", null ) ).when( privateAccountsUserUserGet444 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "99999", "scrooge.mcduck1@disney.land", null ) ).when( privateAccountsUserUserGet99999 ).execute();

        Mockito.doReturn( toCompanyDetailsApiResponse( "000000", "Boston Dynamics" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "000000" );
        Mockito.doReturn( toSearchUserDetailsApiResponse( "bruce.wayne@gotham.city", "111" ) ).when( accountsUserEndpoint ).searchUserDetails( eq( List.of( "bruce.wayne@gotham.city" ) ) );

        Mockito.doReturn( toSearchUserDetailsApiResponse( "robin@gotham.city", "444" ) ).when( accountsUserEndpoint ).searchUserDetails( eq( List.of( "robin@gotham.city" ) ) );

        latch = new CountDownLatch(1);
        doAnswer( invocation -> {
            latch.countDown();
            return null;
        } ).when(emailProducer).sendEmail(any(), any());

    }

    @Test
    @DirtiesContext( methodMode = MethodMode.BEFORE_METHOD )
    void updateAssociationStatusForIdUserAcceptedInvitationNotificationsSendsNotification() throws Exception {
        Mockito.lenient().doReturn( toSearchUserDetailsApiResponse(  "harley.quinn@gotham.city", "333" ) ).when( accountsUserEndpoint ).searchUserDetails( eq( List.of( "harley.quinn@gotham.city" ) ) );


        latch = new CountDownLatch(3);
        doAnswer( invocation -> {
            latch.countDown();
            return null;
        } ).when(emailProducer).sendEmail(any(), any());

        mockMvc.perform( patch( "/associations/{associationId}", "42" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer, times( 3 ) ).sendEmail( argThat( invitationAcceptedEmailDataMatcher( List.of("the.joker@gotham.city", "robin@gotham.city", "harley.quinn@gotham.city" ), "Companies House: harley.quinn@gotham.city is now authorised to file online for Twitter", "harley.quinn@gotham.city", "Twitter", "the.joker@gotham.city" ) ), eq( INVITATION_ACCEPTED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void fetchAssociationsByWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchAssociationsByWithoutEricIdentityReturnsUnauthorised() throws Exception {
        mockMvc.perform( get( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isUnauthorized() );
    }

    @Test
    void fetchAssociationsByWithInvalidPageIndexReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations?page_index=-1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchAssociationsByWithInvalidItemsPerPageReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations?items_per_page=0" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchAssociationsByWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations?company_number=$$$$$$" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchAssociationsByWithNonExistentUserReturnsForbidden() throws Exception {
        mockMvc.perform( get( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isForbidden() );
    }

    @Test
    void fetchAssociationsByWithNonexistentCompanyReturnsNotFound() throws Exception {
        Mockito.doReturn( privateAccountsUserUserGet8888 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "8888" );
        Mockito.doReturn( toGetUserDetailsApiResponse( "8888", "mr.blobby@nightmare.com", "Mr Blobby" ) ).when( privateAccountsUserUserGet8888 ).execute();
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( companyProfileEndpoint ).fetchCompanyProfile( "222222" );

        mockMvc.perform( get( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "8888")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isNotFound() );
    }

    @Test
    void fetchAssociationsByWithInvalidStatusReturnsZeroResults() throws Exception {
        final var response =
                mockMvc.perform( get( "/associations?status=$$$$" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() )
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        final AssociationsList associationsList = objectMapper.readValue( response.getContentAsByteArray(), AssociationsList.class );

        Assertions.assertEquals( 0, associationsList.getTotalResults() );
    }

    @Test
    void fetchAssociationsByUsesDefaultsIfValuesAreNotProvided() throws Exception {
        final var response =
                mockMvc.perform( get( "/associations" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() )
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

        Assertions.assertTrue( items.containsAll( List.of ( "18", "19", "20", "21", "22" ) ) );
        Assertions.assertEquals( "/associations?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 5, associationsList.getTotalResults() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
    }

    @Test
    void fetchAssociationsByWithOneStatusAppliesStatusFilterCorrectly() throws Exception {
        final var response =
                mockMvc.perform( get( "/associations?status=removed" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() )
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

        Assertions.assertTrue( items.containsAll( List.of ( "31", "32", "33" ) ) );
        Assertions.assertEquals( "/associations?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 3, associationsList.getTotalResults() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
    }

    @Test
    void fetchAssociationsByWithMultipleStatusesAppliesStatusFilterCorrectly() throws Exception {
        final var response =
                mockMvc.perform( get( "/associations?status=confirmed&status=removed" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() )
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

        Assertions.assertTrue( items.containsAll( List.of ( "18", "19", "20", "21", "22", "31", "32", "33" ) ) );
        Assertions.assertEquals( "/associations?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 8, associationsList.getTotalResults() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
    }

    @Test
    void fetchAssociationsByImplementsPaginationCorrectly() throws Exception {
        final var response =
                mockMvc.perform( get( "/associations?status=confirmed&status=awaiting-approval&status=removed&page_index=2&items_per_page=3" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() )
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

        Assertions.assertTrue( items.containsAll( List.of ( "24", "25", "26" ) ) );
        Assertions.assertEquals( "/associations?page_index=2&items_per_page=3", links.getSelf() );
        Assertions.assertEquals( "/associations?page_index=3&items_per_page=3", links.getNext() );
        Assertions.assertEquals( 2, associationsList.getPageNumber() );
        Assertions.assertEquals( 3, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 16, associationsList.getTotalResults() );
        Assertions.assertEquals( 6, associationsList.getTotalPages() );
    }

    @Test
    void fetchAssociationsByFiltersBasedOnCompanyNumberCorrectly() throws Exception {
        final var response =
                mockMvc.perform( get( "/associations?company_number=333333" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() )
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

        Assertions.assertTrue( items.contains( "18" ) );
        Assertions.assertEquals( "/associations?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 1, associationsList.getTotalResults() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
    }

    private String reduceTimestampResolution( String timestamp ){
        return timestamp.substring( 0, timestamp.indexOf( ":" ) );
    }

    private String localDateTimeToNormalisedString( LocalDateTime localDateTime ){
        final var timestamp = localDateTime.toString();
        return reduceTimestampResolution( timestamp );
    }

    @Test
    void fetchAssociationsByDoesMappingCorrectly() throws Exception {

        final var response =
                mockMvc.perform( get( "/associations?company_number=333333" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() )
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        final AssociationsList associationsList = objectMapper.readValue( response.getContentAsByteArray(), AssociationsList.class );

        final var associations = associationsList.getItems();
        final var associationOne = associations.getFirst();

        Assertions.assertEquals( "aa", associationOne.getEtag() );
        Assertions.assertEquals( "18", associationOne.getId() );
        Assertions.assertEquals( "9999", associationOne.getUserId() );
        Assertions.assertEquals( "scrooge.mcduck@disney.land", associationOne.getUserEmail() );
        Assertions.assertEquals( "Scrooge McDuck", associationOne.getDisplayName() );
        Assertions.assertEquals( "333333", associationOne.getCompanyNumber() );
        Assertions.assertEquals( "Tesco", associationOne.getCompanyName() );
        Assertions.assertEquals( StatusEnum.CONFIRMED, associationOne.getStatus() );
        Assertions.assertNotNull( associationOne.getCreatedAt() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(1) ), localDateTimeToNormalisedString( associationOne.getApprovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(2) ), localDateTimeToNormalisedString( associationOne.getRemovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( DEFAULT_KIND, associationOne.getKind() );
        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE, associationOne.getApprovalRoute() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(3) ), reduceTimestampResolution( associationOne.getApprovalExpiryAt() ) );
        Assertions.assertEquals( "/associations/18", associationOne.getLinks().getSelf() );
    }

    @Test
    void getAssociationDetailsWithoutPathVariableReturnsNotFound() throws Exception {
        mockMvc.perform( get( "/associations/" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isNotFound() );
    }
    @Test
    void getAssociationsDetailsWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/{id}", "1" )
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect(status().isBadRequest());
    }
    @Test
    void getAssociationDetailsWithMalformedInputReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/{id}", "$" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAssociationUserDetailsWithNonexistentIdReturnsNotFound() throws Exception {
        mockMvc.perform( get( "/associations/{id}", "1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isNotFound() );
    }

    @Test
    void getAssociationDetailsFetchesAssociationDetails() throws Exception {
        final var responseBody =
                mockMvc.perform( get( "/associations/{id}", "18" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() )
                        .andReturn()
                        .getResponse().getContentAsByteArray();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        final var association = objectMapper.readValue(responseBody, Association.class );

        Assertions.assertEquals( "9999", association.getUserId());

    }

    @Test
    void addAssociationWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"000000\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithoutEricIdentityReturnsUnauthorised() throws Exception {
        mockMvc.perform(post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"000000\"}" ) )
                .andExpect( status().isUnauthorized() );
    }

    @Test
    void addAssociationWithoutRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "000")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithoutCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "000")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "000")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"$$$$$$\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithExistingAssociationReturnsBadRequest() throws Exception {
        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithNonexistentCompanyNumberReturnsNotFound() throws Exception {
        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "000")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"919191\"}" ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void addAssociationCreatesNewAssociationCorrectlyAndReturnsAssociationIdWithCreatedHttpStatus() throws Exception {
        final var responseJson =
                mockMvc.perform(post( "/associations" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "000")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*")
                                .contentType( MediaType.APPLICATION_JSON )
                                .content( "{\"company_number\":\"000000\"}" ) )
                        .andExpect( status().isCreated() )
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final var objectMapper = new ObjectMapper();
        final var response = objectMapper.readValue( responseJson, ResponseBodyPost.class );

        final var associationOptional = associationsRepository.findById( response.getAssociationId() );
        Assertions.assertTrue( associationOptional.isPresent() );

        final var association = associationOptional.get();
        Assertions.assertEquals( "000000", association.getCompanyNumber() );
        Assertions.assertEquals( "000", association.getUserId() );
        Assertions.assertEquals( StatusEnum.CONFIRMED.getValue(), association.getStatus() );
        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getEtag() );
    }

    @Test
    void addAssociationWithNonExistentUserReturnsForbidden() throws Exception {
        mockMvc.perform( post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"000000\"}" ) )
                .andExpect( status().isForbidden() );
    }

    ArgumentMatcher<AuthCodeConfirmationEmailData> authCodeConfirmationEmailDataMatcher( String to, String subject, String authorisedPerson, String companyName ){
        return emailData ->
                to.equals( emailData.getTo() ) &&
                        subject.equals( emailData.getSubject() ) &&
                        authorisedPerson.equals( emailData.getAuthorisedPerson() ) &&
                        companyName.equals( emailData.getCompanyName() );
    }

    @Test
    void addAssociationWithUserThatHasDisplayNameUsesDisplayName() throws Exception {
        mockMvc.perform(post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\"}" ) )
                .andExpect( status().isCreated() );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer ).sendEmail( argThat( authCodeConfirmationEmailDataMatcher( "scrooge.mcduck@disney.land", "Companies House: Batman is now authorised to file online for Sainsbury's", "Batman", "Sainsbury's" ) ), eq( AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void addAssociationReturnsCreatedWhenEmailIsNotSentOut() throws Exception {
        Mockito.doThrow( new EmailSendingException("Failed to send email", new Exception()) ).when( emailProducer ).sendEmail( any(), any() );

        mockMvc.perform(post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\"}" ) )
                .andExpect( status().isCreated() );
    }

    @Test
    void updateAssociationStatusForIdWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform( patch( "/associations/{associationId}", "18" )
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithMalformedAssociationIdReturnsBadRequest() throws Exception {
        mockMvc.perform( patch( "/associations/{associationId}", "$$$" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithoutRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform( patch( "/associations/{associationId}", "18" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithoutStatusReturnsBadRequest() throws Exception {
        mockMvc.perform( patch( "/associations/{associationId}", "18" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithMalformedStatusReturnsBadRequest() throws Exception {
        mockMvc.perform( patch( "/associations/{associationId}", "18" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"complicated\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithNonexistentAssociationIdReturnsNotFound() throws Exception {
        mockMvc.perform( patch( "/associations/{associationId}", "9191" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void updateAssociationStatusForIdWithRemovedUpdatesAssociationStatus() throws Exception {
        final var oldAssociationData = associationsRepository.findById("18").get();

        mockMvc.perform( patch( "/associations/{associationId}", "18" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        final var newAssociationData = associationsRepository.findById("18").get();
        Assertions.assertEquals( RequestBodyPut.StatusEnum.REMOVED.getValue(), newAssociationData.getStatus() );
        Assertions.assertEquals( localDateTimeToNormalisedString( oldAssociationData.getApprovedAt() ), localDateTimeToNormalisedString( newAssociationData.getApprovedAt() ) );
        Assertions.assertNotEquals( oldAssociationData.getRemovedAt(), newAssociationData.getRemovedAt() );
        Assertions.assertNotEquals( oldAssociationData.getEtag(), newAssociationData.getEtag() );
        Assertions.assertEquals( oldAssociationData.getUserEmail(), newAssociationData.getUserEmail() );
        Assertions.assertEquals( oldAssociationData.getUserId(), newAssociationData.getUserId() );
    }

    @Test
    void updateAssociationStatusForIdWithConfirmedUpdatesAssociationStatus() throws Exception {
        final var oldAssociationData = associationsRepository.findById("18").get();

        mockMvc.perform( patch( "/associations/{associationId}", "18" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        final var newAssociationData = associationsRepository.findById("18").get();
        Assertions.assertEquals( RequestBodyPut.StatusEnum.CONFIRMED.getValue(), newAssociationData.getStatus() );
        Assertions.assertNotEquals( oldAssociationData.getApprovedAt(), newAssociationData.getApprovedAt() );
        Assertions.assertEquals( localDateTimeToNormalisedString( oldAssociationData.getRemovedAt() ), localDateTimeToNormalisedString( newAssociationData.getRemovedAt() ) );
        Assertions.assertNotEquals( oldAssociationData.getEtag(), newAssociationData.getEtag() );
        Assertions.assertEquals( oldAssociationData.getUserEmail(), newAssociationData.getUserEmail() );
        Assertions.assertEquals( oldAssociationData.getUserId(), newAssociationData.getUserId() );
    }


    @Test
    void updateAssociationStatusForIdWithNullUserIdAndNonexistentUserAndConfirmedReturnsBadRequest() throws Exception {
        Mockito.doReturn( new ApiResponse<>( 204, Map.of(), new UsersList() ) ).when( accountsUserEndpoint ).searchUserDetails( List.of( "light.yagami@death.note" ) );

        final var associationZero = new AssociationDao();
        associationZero.setCompanyNumber("000000");
        associationZero.setUserEmail("light.yagami@death.note");
        associationZero.setStatus(StatusEnum.CONFIRMED.getValue());
        associationZero.setId("0");
        associationZero.setApprovedAt(now.plusDays(1));
        associationZero.setRemovedAt(now.plusDays(2));
        associationZero.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationZero.setApprovalExpiryAt(now.plusDays(3));
        associationZero.setInvitations( List.of() );
        associationZero.setEtag( "aa" );

        associationsRepository.insert( associationZero );

        mockMvc.perform( patch( "/associations/{associationId}", "0" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndExistingUserAndRemovedUpdatesAssociationStatus() throws Exception {
        Mockito.doReturn( toSearchUserDetailsApiResponse( "light.yagami@death.note", "000" ) ).when( accountsUserEndpoint ).searchUserDetails( any() );

        final var association1 = new AssociationDao();
        association1.setCompanyNumber("000000");
        association1.setUserId("000");
        association1.setStatus(StatusEnum.CONFIRMED.getValue());
        association1.setId("0");
        association1.setApprovedAt(now.plusDays(1));
        association1.setRemovedAt(now.plusDays(2));
        association1.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        association1.setApprovalExpiryAt(now.plusDays(3));
        association1.setInvitations( List.of() );
        association1.setEtag( "aa" );

        final var associationZero = new AssociationDao();
        associationZero.setCompanyNumber("000000");
        associationZero.setUserEmail("light.yagami@death.note");
        associationZero.setStatus(StatusEnum.CONFIRMED.getValue());

        associationZero.setApprovedAt(now.plusDays(1));
        associationZero.setRemovedAt(now.plusDays(2));
        associationZero.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationZero.setApprovalExpiryAt(now.plusDays(3));
        associationZero.setInvitations( List.of() );
        associationZero.setEtag( "aa" );

        associationsRepository.insert( associationZero );
        associationsRepository.insert( association1 );

        mockMvc.perform( patch( "/associations/{associationId}", "0" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        final var newAssociationData = associationsRepository.findById("0").get();
        Assertions.assertEquals( RequestBodyPut.StatusEnum.REMOVED.getValue(), newAssociationData.getStatus() );
        Assertions.assertEquals( localDateTimeToNormalisedString( associationZero.getApprovedAt() ), localDateTimeToNormalisedString( newAssociationData.getApprovedAt() ) );
        Assertions.assertNotEquals( localDateTimeToNormalisedString( associationZero.getRemovedAt() ), localDateTimeToNormalisedString( newAssociationData.getRemovedAt() ) );
        Assertions.assertNotEquals( associationZero.getEtag(), newAssociationData.getEtag() );
        Assertions.assertNotEquals( associationZero.getUserEmail(), newAssociationData.getUserEmail() );
        Assertions.assertNotEquals( associationZero.getUserId(), newAssociationData.getUserId() );
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndNonexistentUserAndRemovedUpdatesAssociationStatus() throws Exception {
        Mockito.doReturn( new ApiResponse<>( 204, Map.of(), new UsersList() ) ).when( accountsUserEndpoint ).searchUserDetails( List.of( "light.yagami@death.note" ) );

        final var associationZero = new AssociationDao();
        associationZero.setCompanyNumber("000000");
        associationZero.setUserEmail("light.yagami@death.note");
        associationZero.setUserId("000");
        associationZero.setStatus(StatusEnum.CONFIRMED.getValue());
        associationZero.setId("0");
        associationZero.setApprovedAt(now.plusDays(1));
        associationZero.setRemovedAt(now.plusDays(2));
        associationZero.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationZero.setApprovalExpiryAt(now.plusDays(3));
        associationZero.setInvitations( List.of() );
        associationZero.setEtag( "aa" );

        associationsRepository.insert( associationZero );

        mockMvc.perform( patch( "/associations/{associationId}", "0" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        final var newAssociationData = associationsRepository.findById("0").get();
        Assertions.assertEquals( RequestBodyPut.StatusEnum.REMOVED.getValue(), newAssociationData.getStatus() );
        Assertions.assertEquals( localDateTimeToNormalisedString( associationZero.getApprovedAt() ), localDateTimeToNormalisedString( newAssociationData.getApprovedAt() ) );
        Assertions.assertNotEquals( localDateTimeToNormalisedString( associationZero.getRemovedAt() ), localDateTimeToNormalisedString( newAssociationData.getRemovedAt() ) );
        Assertions.assertNotEquals( associationZero.getEtag(), newAssociationData.getEtag() );
        Assertions.assertEquals( associationZero.getUserEmail(), newAssociationData.getUserEmail() );
        Assertions.assertEquals( associationZero.getUserId(), newAssociationData.getUserId() );
    }

    ArgumentMatcher<AuthorisationRemovedEmailData> authorisationRemovedEmailDataMatcher( final String to, final String subject, final String personWhoWasRemoved, final String companyName, final String personWhoRemovedAuthorisation ){
        return emailData ->
                to.equals( emailData.getTo() ) &&
                subject.equals( emailData.getSubject() ) &&
                personWhoWasRemoved.equals( emailData.getPersonWhoWasRemoved() ) &&
                companyName.equals( emailData.getCompanyName() ) &&
                personWhoRemovedAuthorisation.equals( emailData.getPersonWhoRemovedAuthorisation() );
    }


    @Test
    @DirtiesContext( methodMode = MethodMode.BEFORE_METHOD )
    void updateAssociationStatusForIdNotificationsWhereTargetUserIdExistsSendsNotification() throws Exception {
        mockMvc.perform( patch( "/associations/{associationId}", "40" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );


    }

    @Test
    @DirtiesContext( methodMode = MethodMode.BEFORE_METHOD )
    void updateAssociationStatusForIdNotificationsWhereTargetUserIdDoesNotExistSendsNotification() throws Exception {
        Mockito.doReturn( toSearchUserDetailsApiResponse( "robin@gotham.city", "444" ) ).when( accountsUserEndpoint ).searchUserDetails( eq( List.of( "robin@gotham.city" ) ) );

        final var association = associationsRepository.findById( "40" ).get();
        association.setUserId( null );
        associationsRepository.save( association );

        mockMvc.perform( patch( "/associations/{associationId}", "40" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer ).sendEmail( argThat( authorisationRemovedEmailDataMatcher("the.joker@gotham.city", "Companies House: robin@gotham.city's authorisation removed to file online for Instram", "robin@gotham.city", "Instram", "harley.quinn@gotham.city") ), eq( AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereUserCannotBeFoundSendsNotification() throws Exception {
        final var association = associationsRepository.findById( "40" ).get();
        association.setUserId( null );
        associationsRepository.save( association );

        Mockito.doReturn( new ApiResponse<>( 204, Map.of(), new UsersList() ) ).when( accountsUserEndpoint ).searchUserDetails( List.of( "robin@gotham.city" ) );

        mockMvc.perform( patch( "/associations/{associationId}", "40" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer ).sendEmail( argThat( authorisationRemovedEmailDataMatcher("the.joker@gotham.city", "Companies House: robin@gotham.city's authorisation removed to file online for Instram", "robin@gotham.city", "Instram", "harley.quinn@gotham.city") ), eq( AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    @DirtiesContext( methodMode = MethodMode.BEFORE_METHOD )
    void updateAssociationStatusForIdNotificationsUsesDisplayNamesWhenAvailable() throws Exception {

        Mockito.doReturn( privateAccountsUserUserGet333 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "333" );
        Mockito.doReturn( privateAccountsUserUserGet444 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "444" );
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "333", "harley.quinn@gotham.city", "Harley Quinn" ) ).when( privateAccountsUserUserGet333 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "444", "robin@gotham.city", "Robin" ) ).when( privateAccountsUserUserGet444 ).execute();

        mockMvc.perform( patch( "/associations/{associationId}", "40" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );


    }

    @Test
    void updateAssociationToConfirmedStatusForOtherUserShouldThrow400Error() throws Exception {

        Mockito.doReturn( privateAccountsUserUserGet333 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "333" );
        Mockito.doReturn( privateAccountsUserUserGet444 ).when( accountsUserEndpoint ).createGetUserDetailsRequest( "444" );
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "333", "harley.quinn@gotham.city", "Harley Quinn" ) ).when( privateAccountsUserUserGet333 ).execute();
        Mockito.lenient().doReturn( toGetUserDetailsApiResponse( "444", "robin@gotham.city", "Robin" ) ).when( privateAccountsUserUserGet444 ).execute();

       var response =  mockMvc.perform( patch( "/associations/{associationId}", "40" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isBadRequest() ).andReturn();
      Assertions.assertEquals("{\"errors\":[{\"error\":\"requesting user does not have access to perform the action\",\"type\":\"ch:service\"}]}",response.getResponse().getContentAsString());

    }

    ArgumentMatcher<InvitationAcceptedEmailData> invitationAcceptedEmailDataMatcher( List<String> to, String subject, String authorisedPerson, String companyName, String personWhoCreatedInvite ){
        return emailData -> to.contains( emailData.getTo() ) &&
                            subject.equals( emailData.getSubject() ) &&
                            authorisedPerson.equals( emailData.getAuthorisedPerson() ) &&
                            companyName.equals( emailData.getCompanyName() ) &&
                            personWhoCreatedInvite.equals( emailData.getPersonWhoCreatedInvite() );
    }

    @Test
    void inviteUserWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform( post( "/associations/invitations" )
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWithoutEricIdentityReturnsUnauthorised() throws Exception {
        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isUnauthorized() );
    }

    @Test
    void inviteUserWithMalformedEricIdentityReturnsForbidden() throws Exception {
        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "$$$$")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void inviteUserWithNonexistentEricIdentityReturnsForbidden() throws Exception {
        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void inviteUserWithoutRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWithoutCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"$$$$$$\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWithNonexistentCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"919191\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWithoutInviteeEmailIdReturnsBadRequest() throws Exception {
        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWithMalformedInviteeEmailIdReturnsBadRequest() throws Exception {
        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"$$$\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeEmailAndCompanyNumberExistsAndInviteeUserIsFoundPerformsSwapAndUpdateOperations() throws Exception {
        Mockito.doReturn( toSearchUserDetailsApiResponse( "russell.howard@comedy.com", "8888" ) ).when( accountsUserEndpoint ).searchUserDetails( eq( List.of( "russell.howard@comedy.com" ) ) );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"russell.howard@comedy.com\"}" ) )
                .andExpect( status().isCreated() );

        final var association = associationsRepository.findById( "34" ).get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertNull( association.getUserEmail() );
        Assertions.assertEquals( "8888", association.getUserId() );
        Assertions.assertEquals( "333333", association.getCompanyNumber() );
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus() );
        Assertions.assertEquals( ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getApprovalExpiryAt() );
        Assertions.assertNotNull( association.getEtag() );
        Assertions.assertNotEquals( "qq", association.getEtag() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "9999", invitation.getInvitedBy() );
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeEmailAndCompanyNumberExistsAndInviteeUserIsNotFoundDoesNotPerformSwapButDoesPerformUpdateOperation() throws Exception {
        Mockito.doReturn( new ApiResponse<>( 204, Map.of(), new UsersList() ) ).when( accountsUserEndpoint ).searchUserDetails( List.of( "russell.howard@comedy.com" ) );

        latch = new CountDownLatch(2);
        doAnswer( invocation -> {
            latch.countDown();
            return null;
        } ).when(emailProducer).sendEmail(any(), any());

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"russell.howard@comedy.com\"}" ) )
                .andExpect( status().isCreated() );

        final var association = associationsRepository.findById( "34" ).get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertEquals( "russell.howard@comedy.com", association.getUserEmail() );
        Assertions.assertNull( association.getUserId() );
        Assertions.assertEquals( "333333", association.getCompanyNumber() );
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus() );
        Assertions.assertEquals( ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getApprovalExpiryAt() );
        Assertions.assertNotNull( association.getEtag() );
        Assertions.assertNotEquals( "qq", association.getEtag() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "9999", invitation.getInvitedBy() );

        final var invitationEmailMatches =
                InvitationEmailDataMatcher(
                        "scrooge.mcduck@disney.land",
                        "Companies House: russell.howard@comedy.com invited to be authorised to file online for Tesco",
                        "Scrooge McDuck",
                        "russell.howard@comedy.com",
                        "Tesco" );

        final var inviteEmailMatches =
                InviteEmailDataMatcher( "russell.howard@comedy.com",
                        "Companies House: invitation to be authorised to file online for Tesco",
                        "Scrooge McDuck",
                        "Tesco",
                        COMPANY_INVITATIONS_URL );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer, times( 2 ) ).sendEmail( argThat( emailData -> {
            if ( emailData instanceof InvitationEmailData ) return invitationEmailMatches.test( (InvitationEmailData) emailData );
            if ( emailData instanceof InviteEmailData ) return inviteEmailMatches.test( (InviteEmailData) emailData );
            return false;
        } ), argThat( messageType -> List.of( INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue() ).contains( messageType ) ) );
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberExistsDoesNotPerformSwapButDoesPerformUpdateOperation() throws Exception {
        latch = new CountDownLatch(2);
        doAnswer( invocation -> {
            latch.countDown();
            return null;
        } ).when(emailProducer).sendEmail(any(), any());

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isCreated() );

        final var association = associationsRepository.findById( "35" ).get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertNull( association.getUserEmail() );
        Assertions.assertEquals( "111", association.getUserId() );
        Assertions.assertEquals( "333333", association.getCompanyNumber() );
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus() );
        Assertions.assertEquals( ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getApprovalExpiryAt() );
        Assertions.assertNotNull( association.getEtag() );
        Assertions.assertNotEquals( "rr", association.getEtag() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "9999", invitation.getInvitedBy() );

        final var invitationEmailMatches =
                InvitationEmailDataMatcher(
                        "scrooge.mcduck@disney.land",
                        "Companies House: bruce.wayne@gotham.city invited to be authorised to file online for Tesco",
                        "Scrooge McDuck",
                        "bruce.wayne@gotham.city",
                        "Tesco" );

        final var inviteEmailMatches =
                InviteEmailDataMatcher( "bruce.wayne@gotham.city",
                        "Companies House: invitation to be authorised to file online for Tesco",
                        "Scrooge McDuck",
                        "Tesco",
                        COMPANY_INVITATIONS_URL );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer, times( 2 ) ).sendEmail( argThat( emailData -> {
            if ( emailData instanceof InvitationEmailData ) return invitationEmailMatches.test( (InvitationEmailData) emailData );
            if ( emailData instanceof InviteEmailData ) return inviteEmailMatches.test( (InviteEmailData) emailData );
            return false;
        } ), argThat( messageType -> List.of( INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue() ).contains( messageType ) ) );
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberDoesNotExistCreatesNewAssociation() throws Exception {
        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isCreated() );

        final var association = associationsRepository.fetchAssociationForCompanyNumberAndUserId("444444", "111").get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertNull( association.getUserEmail() );
        Assertions.assertEquals( "111", association.getUserId() );
        Assertions.assertEquals( "444444", association.getCompanyNumber() );
        Assertions.assertEquals( ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getEtag() );
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus() );
        Assertions.assertNotNull( association.getApprovalExpiryAt() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "9999", invitation.getInvitedBy() );
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeUserEmailAndCompanyNumberDoesNotExistAndInviteeUserIsNotFoundCreatesNewAssociation() throws Exception {
        Mockito.doReturn( new ApiResponse<>( 204, Map.of(), new UsersList() ) ).when( accountsUserEndpoint ).searchUserDetails( List.of( "madonna@singer.com" ) );

        latch = new CountDownLatch(2);
        doAnswer( invocation -> {
            latch.countDown();
            return null;
        } ).when(emailProducer).sendEmail(any(), any());

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"madonna@singer.com\"}" ) )
                .andExpect( status().isCreated() );

        final var association = associationsRepository.fetchAssociationForCompanyNumberAndUserEmail("333333", "madonna@singer.com").get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertEquals( "madonna@singer.com", association.getUserEmail() );
        Assertions.assertNull( association.getUserId() );
        Assertions.assertEquals( "333333", association.getCompanyNumber() );
        Assertions.assertEquals( ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getEtag() );
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus() );
        Assertions.assertNotNull( association.getApprovalExpiryAt() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "9999", invitation.getInvitedBy() );

        final var invitationEmailMatches =
                InvitationEmailDataMatcher(
                        "scrooge.mcduck@disney.land",
                        "Companies House: madonna@singer.com invited to be authorised to file online for Tesco",
                        "Scrooge McDuck",
                        "madonna@singer.com",
                        "Tesco" );

        final var inviteEmailMatches =
                InviteEmailDataMatcher( "madonna@singer.com",
                        "Companies House: invitation to be authorised to file online for Tesco",
                        "Scrooge McDuck",
                        "Tesco",
                        COMPANY_INVITATIONS_URL );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer, times( 2 ) ).sendEmail( argThat( emailData -> {
            if ( emailData instanceof InvitationEmailData ) return invitationEmailMatches.test( (InvitationEmailData) emailData );
            if ( emailData instanceof InviteEmailData ) return inviteEmailMatches.test( (InviteEmailData) emailData );
            return false;
        } ), argThat( messageType -> List.of( INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue() ).contains( messageType ) ) );
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberExistsDoesNotPerformSwapButThrowsBadRequest() throws Exception {

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"111111\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isBadRequest() ).andReturn();

    }

    Predicate<InvitationEmailData> InvitationEmailDataMatcher( String to, String subject, String personWhoCreatedInvite, String invitee, String companyName ){
        return emailData ->
                to.equals( emailData.getTo() ) &&
                        subject.equals( emailData.getSubject() ) &&
                        personWhoCreatedInvite.equals( emailData.getPersonWhoCreatedInvite() ) &&
                        invitee.equals(emailData.getInvitee()) &&
                        companyName.equals( emailData.getCompanyName() );
    }

    Predicate<InviteEmailData> InviteEmailDataMatcher( String to, String subject, String inviterDisplayName, String companyName, String invitationLink ){
        return emailData ->
                to.equals(emailData.getTo()) &&
                subject.equals(emailData.getSubject()) &&
                inviterDisplayName.equals(emailData.getInviterDisplayName()) &&
                companyName.equals(emailData.getCompanyName()) &&
                invitationLink.equals(emailData.getInvitationLink())
                ;
    }

    @Test
    void inviteUserWithUserThatHasDisplayNameUsesDisplayName()  throws Exception {
        latch = new CountDownLatch(2);
        doAnswer( invocation -> {
            latch.countDown();
            return null;
        } ).when(emailProducer).sendEmail(any(), any());

        mockMvc.perform(post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isCreated() );

        final var invitationEmailMatches =
        InvitationEmailDataMatcher(
                "scrooge.mcduck@disney.land",
                "Companies House: bruce.wayne@gotham.city invited to be authorised to file online for Sainsbury's",
                "Scrooge McDuck",
                "bruce.wayne@gotham.city",
                "Sainsbury's" );

        final var inviteEmailMatches =
        InviteEmailDataMatcher( "bruce.wayne@gotham.city",
                "Companies House: invitation to be authorised to file online for Sainsbury's",
                "Scrooge McDuck",
                "Sainsbury's",
                COMPANY_INVITATIONS_URL );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer, times( 2 ) ).sendEmail( argThat( emailData -> {
            if ( emailData instanceof InvitationEmailData ) return invitationEmailMatches.test( (InvitationEmailData) emailData );
            if ( emailData instanceof InviteEmailData ) return inviteEmailMatches.test( (InviteEmailData) emailData );
            return false;
        } ), argThat( messageType -> List.of( INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue() ).contains( messageType ) ) );
    }

    @Test
    void fetchActiveInvitationsForUserWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("Eric-identity", "99999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchActiveInvitationsForUserWithoutEricIdentityReturnsUnauthorised() throws Exception {
        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isUnauthorized() );
    }

    @Test
    void fetchActiveInvitationsForUserWithMalformedEricIdentityReturnsForbidden() throws Exception {
        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "$$$$")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isForbidden() );
    }

    @Test
    void fetchActiveInvitationsForUserWithNonexistentEricIdentityReturnsForbidden() throws Exception {
        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isForbidden() );
    }

    @Test
    void fetchActiveInvitationsForUserWithUnacceptablePageIndexReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/invitations?page_index=-1&items_per_page=1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "99999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchActiveInvitationsForUserWithUnacceptableItemsPerPageReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/invitations?page_index=0&items_per_page=-1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "99999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchActiveInvitationsForUserRetrievesActiveInvitationsInCorrectOrderAndPaginatesCorrectly() throws Exception {
        final var response =
                mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "99999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() )
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        final var invitations = objectMapper.readValue( response.getContentAsByteArray(), InvitationsList.class ).getItems();
        final var invitation = invitations.getFirst();

        Assertions.assertEquals( 1, invitations.size() );
        Assertions.assertEquals( "robin@gotham.city", invitation.getInvitedBy() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "48", invitation.getAssociationId() );
        Assertions.assertTrue( invitation.getIsActive() );
    }

    @Test
    void fetchActiveInvitationsForUserWithoutActiveInvitationsReturnsEmptyList() throws Exception {
        final var response =
                mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "111")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() )
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        final var invitations = objectMapper.readValue( response.getContentAsByteArray(), InvitationsList.class );
        Assertions.assertTrue( invitations.getItems().isEmpty() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }

}