package uk.gov.companieshouse.accounts.association.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.configuration.WebSecurityConfig;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.Links;
import uk.gov.companieshouse.api.accounts.associations.model.PreviousStatesList;
import uk.gov.companieshouse.api.accounts.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_READ_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_UPDATE_PERMISSION;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ASSOCIATIONS;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_IDENTITY;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_ID_VALUE;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.KEY_ROLES_VALUE;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.MK_USER_001;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.OAUTH_2;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.X_REQUEST_ID;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.X_REQUEST_ID_VALUE;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.UNAUTHORISED;

@WebMvcTest( UserCompanyAssociation.class )
@Import( WebSecurityConfig.class )
@Tag( "unit-test" )
class UserCompanyAssociationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private StaticPropertyUtil staticPropertyUtil;

    @MockitoBean
    private UsersService usersService;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private AssociationsService associationsService;

    @MockitoBean
    private EmailService emailService;

    final Function<String, Mono<Void>> sendEmailMock = userId -> Mono.empty();

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static Mockers mockers;

    @BeforeEach
    public void setup() {
        mockers = new Mockers( null, null, companyService, usersService );
        mockMvc = MockMvcBuilders.webAppContextSetup( context )
                .apply( SecurityMockMvcConfigurers.springSecurity() )
                .build();
    }

    @Test
    void getAssociationDetailsWithoutPathVariableReturnsNotFound() throws Exception {
        mockMvc.perform(get(ASSOCIATIONS + "/")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "000")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationDetailsWithMalformedInputReturnsBadRequest() throws Exception {
        mockMvc.perform(get(ASSOCIATIONS + "/$")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "000")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationUserDetailsWithNonexistentIdReturnsNotFound() throws Exception {
        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationDto("11");
        mockMvc.perform(get(ASSOCIATIONS + "/11")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "000")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationDetailsFetchesAssociationDetails() throws Exception {
        final var user = testDataManager.fetchUserDtos(ERIC_ID_VALUE).getFirst();
        final var association = testDataManager.fetchAssociationDto( "18", user );

        Mockito.doReturn(Optional.of(association)).when(associationsService).fetchAssociationDto("18");

        final var response = mockMvc.perform(get(ASSOCIATIONS + "/18")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_ID_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect(status().isOk());
        final var result = parseResponseTo( response, Association.class );

        Assertions.assertEquals("18", result.getId());
    }

    @Test
    void getAssociationForIdCanFetchMigratedAssociation() throws Exception {
        final var user = testDataManager.fetchUserDtos(MK_USER_001).getFirst();
        final var association = testDataManager.fetchAssociationDto( "MKAssociation001", user );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).fetchAssociationDto( "MKAssociation001" );

        final var response =
                mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation001")
                                .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, MK_USER_001)
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                        .andExpect( status().isOk() );

        final var responseAssociation = parseResponseTo( response, Association.class );

        Assertions.assertEquals( "MKAssociation001", responseAssociation.getId() );
        Assertions.assertEquals( "migrated", responseAssociation.getStatus().getValue() );
        Assertions.assertEquals( "migration", responseAssociation.getApprovalRoute().getValue() );
    }
    @Test
    void getAssociationForIdWithAPIKeyRequest() throws Exception {
        final var user = testDataManager.fetchUserDtos(MK_USER_001).getFirst();
        final var association = testDataManager.fetchAssociationDto( "MKAssociation001", user );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).fetchAssociationDto( "MKAssociation001" );

        final var response = mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect( status().isOk() );

        final var responseAssociation = parseResponseTo( response, Association.class );

        Assertions.assertEquals( "MKAssociation001", responseAssociation.getId() );
    }

    @Test
    void getAssociationForIdCanBeCalledByAdmin() throws Exception {
        final var user = testDataManager.fetchUserDtos(MK_USER_001).getFirst();
        final var association = testDataManager.fetchAssociationDto( "MKAssociation001", user );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).fetchAssociationDto( "MKAssociation001" );

        mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header( "Eric-Authorised-Roles", ADMIN_READ_PERMISSION ) )
                .andExpect( status().isOk() );
    }

    @Test
    void getAssociationForIdCanRetrieveUnauthorisedAssociation() throws Exception {
        final var user = testDataManager.fetchUserDtos( "MKUser004" ).getFirst();
        final var proposedAssociation = testDataManager.fetchAssociationDto( "MKAssociation004", user );

        mockers.mockUsersServiceFetchUserDetails( "111" );
        Mockito.doReturn( Optional.of( proposedAssociation ) ).when( associationsService ).fetchAssociationDto( "MKAssociation004" );

        final var response =
                mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation004")
                                .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, "111")
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                                .header( "Eric-Authorised-Roles", ADMIN_READ_PERMISSION ) )
                        .andExpect( status().isOk() );

        final var association = parseResponseTo( response, Association.class );
        Assertions.assertEquals( "MKAssociation004", association.getId() );
        Assertions.assertEquals( StatusEnum.UNAUTHORISED, association.getStatus() );
        Assertions.assertNotNull( association.getUnauthorisedAt() );
    }

    @Test
    void getInvitationsForAssociationWithUnacceptablePageIndexReturnsBadRequest() throws Exception {
        mockMvc.perform(get(ASSOCIATIONS + "/1/invitations?page_index=-1&items_per_page=1")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInvitationsForAssociationWithUnacceptableItemsPerPageReturnsBadRequest() throws Exception {
        mockMvc.perform(get(ASSOCIATIONS + "/1/invitations?page_index=0&items_per_page=-1")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInvitationsForAssociationWithMalformedInputReturnsBadRequest() throws Exception {
        mockMvc.perform(get(ASSOCIATIONS + "/$/invitations")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "000")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInvitationsForAssociationWithNonexistentIdReturnsNotFound() throws Exception {
        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationDao("11");

        mockMvc.perform(get(ASSOCIATIONS + "/11/invitations")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "000")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInvitationsForAssociationFetchesInvitations() throws Exception {
        final var invitations = testDataManager.fetchInvitations( "37" );

        Mockito.doReturn( Optional.of( new InvitationsList().items( invitations ) ) ).when(associationsService).fetchInvitations("37", 0, 15);

        final var response = mockMvc.perform(get(ASSOCIATIONS + "/37/invitations")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "000")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect(status().isOk());
        final var resultInvitationsList = parseResponseTo( response, InvitationsList.class );
        final var resultInvitations = resultInvitationsList.getItems();

        Assertions.assertEquals( 3, resultInvitations.size() );
        Assertions.assertEquals( resultInvitations.getFirst(), invitations.getFirst() );
        Assertions.assertEquals( resultInvitations.get(1), invitations.get(1) );
        Assertions.assertEquals( resultInvitations.getLast(), invitations.getLast() );
    }


    @Test
    void getInvitationsForAssociationWithPaginationAndVerifyResponse() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "37" ).getFirst();
        final var invitations = testDataManager.fetchInvitations( "37" );

        final var mockLinks = new Links()
                .self(ASSOCIATIONS + "/37/invitations?page_index=1&items_per_page=1")
                .next(ASSOCIATIONS + "/37/invitations?page_index=2&items_per_page=1");

        final var mockInvitationsList = new InvitationsList()
                .items(invitations).links(mockLinks)
                .itemsPerPage(1).pageNumber(1).totalResults(2).totalPages(2);

        when(associationsService.fetchInvitations("37", 1, 1)).thenReturn(Optional.of(mockInvitationsList));

        final var response = mockMvc.perform(get(ASSOCIATIONS + "/37/invitations?page_index=1&items_per_page=1")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_ID_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect(status().isOk());
        final var invitationsList = parseResponseTo( response, InvitationsList.class );

        Mockito.verify(associationsService).fetchInvitations("37", 1, 1);

        assertNotNull(invitationsList);
        assertEquals(1, invitationsList.getItemsPerPage().intValue());
        assertEquals(1, invitationsList.getPageNumber().intValue());
        assertEquals(2, invitationsList.getTotalResults().intValue());
        assertEquals(2, invitationsList.getTotalPages().intValue());
        assertNotNull(invitationsList.getLinks());
        assertEquals(ASSOCIATIONS + "/37/invitations?page_index=1&items_per_page=1", invitationsList.getLinks().getSelf());
        assertEquals(ASSOCIATIONS + "/37/invitations?page_index=2&items_per_page=1", invitationsList.getLinks().getNext());
    }

    @Test
    void getInvitationsForAssociationWithMigratedAssociationReturnsEmpty() throws Exception {
        Mockito.doReturn( Optional.of( new InvitationsList().items( List.of() ) ) ).when( associationsService ).fetchInvitations( "MKAssociation001", 0, 15 );

        final var response = mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation001/invitations")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, MK_USER_001)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect( status().isOk() );

        final var resultInvitationsList = parseResponseTo( response, InvitationsList.class );
        final var resultInvitations = resultInvitationsList.getItems();

        Assertions.assertTrue( resultInvitations.isEmpty() );
    }

    @Test
    void updateAssociationStatusForIdWithMalformedAssociationIdReturnsBadRequest() throws Exception {
        mockMvc.perform(patch(ASSOCIATIONS + "/$$$")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "000")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"removed\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithoutRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(patch(ASSOCIATIONS + "/18")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "000")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithoutStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(patch(ASSOCIATIONS + "/18")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "000")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithMalformedStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(patch(ASSOCIATIONS + "/18")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "000")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"complicated\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithNonexistentAssociationIdReturnsNotFound() throws Exception {
        mockers.mockUsersServiceFetchUserDetails(ERIC_ID_VALUE);
        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationDto("9191");

        mockMvc.perform(patch(ASSOCIATIONS + "/9191")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_ID_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"confirmed\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAssociationStatusForIdWithRemovedUpdatesAssociationStatus() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "35" ).getFirst();

        Mockito.doReturn( testDataManager.fetchUserDtos( "000" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        mockers.mockUsersServiceFetchUserDetails(ERIC_ID_VALUE);
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).fetchAssociationDao( "35" );
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn(Mono.empty()).when(emailService).sendAuthorisationRemovedEmailToRemovedUser(eq(X_REQUEST_ID_VALUE), eq("333333"), any(Mono.class), eq("Scrooge McDuck"), eq("light.yagami@death.note"));
        Mockito.doReturn(sendEmailMock).when(emailService).sendAuthorisationRemovedEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("333333"), any(Mono.class), eq("Scrooge McDuck"), eq("light.yagami@death.note"));

        mockMvc.perform(patch(ASSOCIATIONS + "/35")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_ID_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"removed\"}"))
                .andExpect(status().isOk());

        Mockito.verify(associationsService).updateAssociation(anyString(), any(Update.class));
    }

    @Test
    void updateAssociationStatusForIdWithConfirmedUpdatesWithNoUserFoundShouldThrow404AssociationStatus() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "36" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails(ERIC_ID_VALUE);
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).fetchAssociationDao( "36" );
        Mockito.doReturn( null ).when(usersService).searchUserDetails(List.of("light.yagami@death.note"));

        mockMvc.perform(patch(ASSOCIATIONS + "/36")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_ID_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"confirmed\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndExistingUserAndConfirmedUpdatesAssociationStatus() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "36" ).getFirst();

        Mockito.doReturn( testDataManager.fetchUserDtos( "000" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        mockers.mockUsersServiceFetchUserDetails( "000" );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).fetchAssociationDao( "36" );

        mockMvc.perform(patch(ASSOCIATIONS + "/36")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "000")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"confirmed\"}"))
                .andExpect(status().isOk());

        Mockito.verify(associationsService, new Times(1)).updateAssociation(anyString(), any(Update.class));
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndExistingUserAndRemovedUpdatesAssociationStatus() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "34" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "000" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "000" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).fetchAssociationDao( "34" );
        Mockito.doReturn(sendEmailMock).when(emailService).sendInvitationRejectedEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("111111"), any(Mono.class), eq("light.yagami@death.note"));

        mockMvc.perform(patch(ASSOCIATIONS + "/34")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "000")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"removed\"}"))
                .andExpect(status().isOk());

        Mockito.verify(associationsService, new Times(1)).updateAssociation(anyString(), any(Update.class));
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndNonexistentUserAndRemovedUpdatesAssociationStatus() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "34" ).getFirst();

        Mockito.doReturn( null ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        mockers.mockUsersServiceFetchUserDetails( "000" );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).fetchAssociationDao( "34" );
        Mockito.doReturn(sendEmailMock).when(emailService).sendInvitationRejectedEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("111111"), any(Mono.class), eq("light.yagami@death.note"));

        mockMvc.perform(patch(ASSOCIATIONS + "/34")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "000")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"removed\"}"))
                .andExpect(status().isOk());

        Mockito.verify(associationsService, new Times(1)).updateAssociation(anyString(), any(Update.class));
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereTargetUserIdExistsSendsNotification() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "35" ).getFirst();
        final var user = testDataManager.fetchUserDtos( "000" ).getFirst();

        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        mockers.mockUsersServiceFetchUserDetails(ERIC_ID_VALUE);
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).fetchAssociationDao("35");
        Mockito.doReturn(Flux.just(ERIC_ID_VALUE)).when(associationsService).fetchConfirmedUserIds("333333");
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn(Mono.empty()).when(emailService).sendAuthorisationRemovedEmailToRemovedUser(eq(X_REQUEST_ID_VALUE), eq("333333"), any(Mono.class), eq("Scrooge McDuck"), eq("light.yagami@death.note"));
        Mockito.doReturn(sendEmailMock).when(emailService).sendAuthorisationRemovedEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("333333"), any(Mono.class), eq("Scrooge McDuck"), eq("light.yagami@death.note"));

        mockMvc.perform(patch(ASSOCIATIONS + "/35")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_ID_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendStatusUpdateEmails( eq( association ), eq( user ), eq( StatusEnum.REMOVED ), any( RequestContextData.class ) );
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereTargetUserIdDoesNotExistSendsNotification() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "34" ).getFirst();
        final var user = testDataManager.fetchUserDtos( "000" ).getFirst();

        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        mockers.mockUsersServiceFetchUserDetails( "111" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).fetchAssociationDao("34");
        Mockito.doReturn( Flux.just( "111" ) ).when( associationsService ).fetchConfirmedUserIds( "111111" );
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn(Mono.empty()).when(emailService).sendInviteCancelledEmail(eq(X_REQUEST_ID_VALUE), eq("111111"), any(Mono.class), eq("Batman"), eq(association));
        Mockito.doReturn(sendEmailMock).when(emailService).sendInvitationCancelledEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("111111"), any(Mono.class), eq("Batman"), eq("light.yagami@death.note"));

        mockMvc.perform(patch(ASSOCIATIONS + "/34")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendStatusUpdateEmails( eq( association ), eq( user ), eq( StatusEnum.REMOVED ), any( RequestContextData.class ) );
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereUserCannotBeFoundSendsNotification() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "34" ).getFirst();
        final User user = null;

        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        mockers.mockUsersServiceFetchUserDetails( "111" );
        mockers.mockCompanyServiceFetchCompanyProfile("111111");
        Mockito.doReturn(Optional.of(association)).when(associationsService).fetchAssociationDao("34");
        Mockito.doReturn( Flux.just("000" ) ).when( associationsService ).fetchConfirmedUserIds( "111111" );
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn(Mono.empty()).when(emailService).sendInviteCancelledEmail(eq(X_REQUEST_ID_VALUE), eq("111111"), any(Mono.class), eq("Batman"), eq(association));
        Mockito.doReturn(sendEmailMock).when(emailService).sendInvitationCancelledEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("111111"), any(Mono.class), eq("Batman"), eq("light.yagami@death.note"));

        mockMvc.perform(patch(ASSOCIATIONS + "/34")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendStatusUpdateEmails( eq( association ), eq( user ), eq( StatusEnum.REMOVED ), any( RequestContextData.class ) );
    }

    @Test
    void updateAssociationStatusForIdNotificationsUsesDisplayNamesWhenAvailable() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "4" ).getFirst();
        final var user = testDataManager.fetchUserDtos( "444" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        Mockito.doReturn(Optional.of(association)).when(associationsService).fetchAssociationDao("4");
        Mockito.doReturn( Flux.just( "333" ) ).when( associationsService ).fetchConfirmedUserIds( "111111" );
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn(Mono.empty()).when(emailService).sendAuthorisationRemovedEmailToRemovedUser(eq(X_REQUEST_ID_VALUE), eq("111111"), any(Mono.class), eq("Harleen Quinzel"), eq("Boy Wonder"));
        Mockito.doReturn(sendEmailMock).when(emailService).sendAuthorisationRemovedEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("111111"), any(Mono.class), eq("Harleen Quinzel"), eq("Boy Wonder"));

        mockMvc.perform(patch(ASSOCIATIONS + "/4")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "333")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendStatusUpdateEmails( eq( association ), eq( user ), eq( StatusEnum.REMOVED ), any( RequestContextData.class ) );
    }

    @Test
    void updateAssociationStatusForIdUserAcceptedInvitationNotificationsSendsNotification() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "6" ).getFirst();
        final var user = testDataManager.fetchUserDtos( "666" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "666", "222" );
        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).fetchAssociationDao("6");
        Mockito.doReturn( Flux.just( "222", "444" ) ).when( associationsService ).fetchConfirmedUserIds( "111111" );
        Mockito.doReturn(sendEmailMock).when(emailService).sendInvitationAcceptedEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("111111"), any(Mono.class), any(Mono.class), eq("homer.simpson@springfield.com"));

        mockMvc.perform(patch(ASSOCIATIONS + "/6")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "666")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendStatusUpdateEmails( eq( association ), eq( user ), eq( StatusEnum.CONFIRMED ), any( RequestContextData.class ) );
    }

    @Test
    void confirmUserStatusRequestForExistingAssociationWithoutOneLoginUserAndDifferentRequestingUserShouldThrow400BadRequest() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "6" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "666", "222" );
        mockers.mockUsersServiceSearchUserDetailsEmptyList( "homer.simpson@springfield.com" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).fetchAssociationDao("6");
        Mockito.doReturn( Flux.just( "222", "444" ) ).when( associationsService ).fetchConfirmedUserIds( "111111" );

        mockMvc.perform(patch(ASSOCIATIONS + "/6")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "222")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdUserAcceptedInvitationNotificationsUsesDisplayNamesWhenAvailable() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "38" ).getFirst();
        final User user = null;

        mockers.mockUsersServiceFetchUserDetails( "9999", "444" );
        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "x222222" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).fetchAssociationDao("38");
        Mockito.doReturn( Flux.just( "111", "222", "444" ) ).when( associationsService ).fetchConfirmedUserIds( "x222222" );
        Mockito.doReturn(sendEmailMock).when(emailService).sendInvitationAcceptedEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("x222222"), any(Mono.class), any(Mono.class), eq("Scrooge McDuck"));

        mockMvc.perform(patch(ASSOCIATIONS + "/38")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendStatusUpdateEmails( eq( association ), eq( user ), eq( StatusEnum.CONFIRMED ), any( RequestContextData.class ));
    }

    @Test
    void updateAssociationStatusForIdUserCancelledInvitationNotificationsSendsNotification() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "38" ).getFirst();
        final var user = testDataManager.fetchUserDtos("9999").getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999", "222" );
        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "x222222" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).fetchAssociationDao("38");
        Mockito.doReturn( Flux.just( "111", "222", "444" ) ).when( associationsService ).fetchConfirmedUserIds( "x222222" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "x222222", "222" );
        Mockito.doReturn(Mono.empty()).when(emailService).sendInviteCancelledEmail(eq(X_REQUEST_ID_VALUE), eq("x222222"), any(Mono.class), eq("the.joker@gotham.city"), eq(association));
        Mockito.doReturn(sendEmailMock).when(emailService).sendInvitationCancelledEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("x222222"), any(Mono.class), eq("the.joker@gotham.city"), eq("Scrooge McDuck"));

        mockMvc.perform(patch(ASSOCIATIONS + "/38")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "222")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendStatusUpdateEmails( eq( association ), eq( user ), eq( StatusEnum.REMOVED ), any( RequestContextData.class ) );
    }

    @Test
    void updateAssociationStatusForIdUserRejectedInvitationNotificationsSendsNotification() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "38" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        Mockito.doReturn( null ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "x222222" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).fetchAssociationDao("38");
        Mockito.doReturn( Flux.just( "111", "222", "444" ) ).when( associationsService ).fetchConfirmedUserIds( "x222222" );

        Mockito.doReturn(sendEmailMock).when(emailService).sendInvitationRejectedEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("x222222"), any(Mono.class), eq("Scrooge McDuck"));

        mockMvc.perform(patch(ASSOCIATIONS + "/38")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendStatusUpdateEmails( eq(association), eq(null), eq(StatusEnum.REMOVED), any(RequestContextData.class) );
    }

    public static Stream<Arguments> updateAssociationStatusForIdMigratedScenarios(){
        return Stream.of(
                Arguments.of(MK_USER_001, "confirmed", true, status().isBadRequest()),
                Arguments.of(MK_USER_001, "removed", true, status().isOk()),
                Arguments.of( "MKUser002", "confirmed", true, status().isOk() ),
                Arguments.of( "MKUser002", "removed", true, status().isOk() ),
                Arguments.of( "MKUser002", "confirmed", false, status().isOk() ),
                Arguments.of( "MKUser002", "removed", false, status().isOk() )

        );
    }

    @ParameterizedTest
    @MethodSource( "updateAssociationStatusForIdMigratedScenarios" )
    void updateAssociationStatusForIdSupportsMigratedAssociation( final String requestingUserId, final String newStatus, final boolean targetUserExists, final ResultMatcher expectedOutcome ) throws Exception {
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( requestingUserId );

        if ( targetUserExists ) {
            Mockito.doReturn(testDataManager.fetchUserDtos(MK_USER_001).getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        } else {
            Mockito.doReturn( null ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        }

        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn( Optional.of( targetAssociation ) ).when( associationsService ).fetchAssociationDao( "MKAssociation001" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( any(), any() );

        Mockito.lenient().doReturn( sendEmailMock ).when( emailService ).sendInvitationEmailToAssociatedUser( any(), any(), any(), any(), any() );
        Mockito.lenient().doReturn( sendEmailMock ).when( emailService ).sendDelegatedRemovalOfMigratedBatchEmail( any(), any(), any(), any(), any() );

        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( String.format( "{\"status\":\"%s\"}", newStatus ) ) )
                .andExpect( expectedOutcome );
    }

    @Test
    void updateAssociationStatusForIdAllowsAdminUserToRemoveAuthorisation() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        final var user = testDataManager.fetchUserDtos( "111" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).fetchAssociationDao( "1" );
        Mockito.doReturn( Flux.just( "222" ) ).when( associationsService ).fetchConfirmedUserIds( "111111" );

        Mockito.doReturn(Mono.empty()).when(emailService).sendAuthorisationRemovedEmailToRemovedUser(eq(X_REQUEST_ID_VALUE), eq("111111"), any(Mono.class), eq("Companies House"), eq("111"));
        Mockito.doReturn(sendEmailMock).when(emailService).sendAuthorisationRemovedEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("111111"), any(Mono.class), eq("Companies House"), eq("Batman"));

        mockMvc.perform(patch(ASSOCIATIONS + "/1")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header( "Eric-Authorised-Roles", ADMIN_UPDATE_PERMISSION )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendStatusUpdateEmails( eq( association ), eq( user ), eq( StatusEnum.REMOVED ), any( RequestContextData.class ) );
    }

    @Test
    void updateAssociationStatusForIdAllowsAdminUserToCancelInvitation() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "6" ).getFirst();
        final var user = testDataManager.fetchUserDtos( "666" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).fetchAssociationDao( "6" );
        Mockito.doReturn( Flux.just( "222" ) ).when( associationsService ).fetchConfirmedUserIds( "111111" );

        Mockito.doReturn(Mono.empty()).when(emailService).sendInviteCancelledEmail(eq(X_REQUEST_ID_VALUE), eq("111111"), any(Mono.class), eq("Companies House"), any(AssociationDao.class));
        Mockito.doReturn(sendEmailMock).when(emailService).sendInvitationCancelledEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("111111"), any(Mono.class), eq("Companies House"), eq("homer.simpson@springfield.com"));

        mockMvc.perform(patch(ASSOCIATIONS + "/6")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header( "Eric-Authorised-Roles", ADMIN_UPDATE_PERMISSION )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );


        Mockito.verify( emailService ).sendStatusUpdateEmails( eq( association ), eq( user ), eq( StatusEnum.REMOVED ), any( RequestContextData.class ) );
    }

    @Test
    void updateAssociationStatusForIdSendsEmailWhenOneUserRemovesAnotherUsersMigratedAssociation() throws Exception {
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos(MK_USER_001).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "MKCOMP001", "MKUser002" );
        Mockito.doReturn( targetUser ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        Mockito.doReturn( Optional.of( targetAssociation ) ).when( associationsService ).fetchAssociationDao( "MKAssociation001" );
        Mockito.doReturn( Flux.just( "MKUser002" ) ).when( associationsService ).fetchConfirmedUserIds( "MKCOMP001" );
        Mockito.doReturn(Mono.empty()).when(emailService).sendDelegatedRemovalOfMigratedEmail(eq(X_REQUEST_ID_VALUE), eq("MKCOMP001"), any(Mono.class), eq("Luigi"), eq("mario@mushroom.kingdom"));
        Mockito.doReturn(sendEmailMock).when(emailService).sendDelegatedRemovalOfMigratedBatchEmail(eq(X_REQUEST_ID_VALUE), eq("MKCOMP001"), any(Mono.class), eq("Luigi"), eq("Mario"));

        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendStatusUpdateEmails( eq( targetAssociation ), eq( targetUser ), eq( StatusEnum.REMOVED ), any( RequestContextData.class ) );
    }

    @Test
    void updateAssociationStatusForIdSendsEmailWhenOneUserRemovesTheirOwnMigratedAssociation() throws Exception {
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos(MK_USER_001).getFirst();

        mockers.mockUsersServiceFetchUserDetails(MK_USER_001, "MKUser002");
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "MKCOMP001", "MKUser002" );
        Mockito.doReturn( targetUser ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        Mockito.doReturn( Optional.of( targetAssociation ) ).when( associationsService ).fetchAssociationDao( "MKAssociation001" );
        Mockito.doReturn( Flux.just( "MKUser002" ) ).when( associationsService ).fetchConfirmedUserIds( "MKCOMP001" );
        Mockito.doReturn(Mono.empty()).when(emailService).sendRemoveOfOwnMigratedEmail(eq(X_REQUEST_ID_VALUE), eq("MKCOMP001"), any(Mono.class), eq(MK_USER_001));
        Mockito.doReturn(sendEmailMock).when(emailService).sendDelegatedRemovalOfMigratedBatchEmail(eq(X_REQUEST_ID_VALUE), eq("MKCOMP001"), any(Mono.class), eq("Mario"), eq("Mario"));

        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, MK_USER_001)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendStatusUpdateEmails( eq( targetAssociation ), eq( targetUser ), eq( StatusEnum.REMOVED ), any( RequestContextData.class ) );
    }

    @Test
    void updateAssociationStatusForIdSendsEmailWhenOneUserConfirmsAnotherUsersMigratedAssociation() throws Exception {
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos(MK_USER_001).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "MKCOMP001", "MKUser002" );
        Mockito.doReturn( targetUser ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        Mockito.doReturn( Optional.of( targetAssociation ) ).when( associationsService ).fetchAssociationDao( "MKAssociation001" );
        Mockito.doReturn( Flux.just( "MKUser002" ) ).when( associationsService ).fetchConfirmedUserIds( "MKCOMP001" );
        Mockito.doReturn(Mono.empty()).when(emailService).sendInviteEmail(eq(X_REQUEST_ID_VALUE), eq("MKCOMP001"), any(Mono.class), eq("Luigi"), anyString(), eq("luigi@mushroom.kingdom"));
        Mockito.doReturn(sendEmailMock).when(emailService).sendInvitationEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("MKCOMP001"), any(Mono.class), eq("Luigi"), eq("Mario"));

        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendStatusUpdateEmails( eq( targetAssociation ), eq( targetUser ), eq( StatusEnum.CONFIRMED ), any( RequestContextData.class ) );
    }

    @Test
    void updateAssociationStatusForIdSendsEmailWhenAPIKeyConfirmsAnUnauthorisedAssociation() throws Exception {
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation004" ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos( "MKUser004" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "MKCOMP001", "MKUser002" );
        Mockito.doReturn( targetUser ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        Mockito.doReturn( Optional.of( targetAssociation ) ).when( associationsService ).fetchAssociationDao( "MKAssociation004" );
        Mockito.doReturn( Flux.just( "MKUser002" ) ).when( associationsService ).fetchConfirmedUserIds( "MKCOMP001" );
        Mockito.doReturn(Mono.empty()).when(emailService).sendInviteEmail(eq(X_REQUEST_ID_VALUE), eq("MKCOMP001"), any(Mono.class), eq("Luigi"), anyString(), eq("bowser@mushroom.kingdom"));
        Mockito.doReturn(sendEmailMock).when(emailService).sendInvitationEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq("MKCOMP001"), any(Mono.class), eq("Luigi"), eq("Bowser"));

        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation004")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendStatusUpdateEmails( eq( targetAssociation ), eq( targetUser ), eq( StatusEnum.CONFIRMED ), any( RequestContextData.class ) );
    }

    @Test
    void getPreviousStatesForAssociationRetrievesData() throws Exception {
        final var previousStatesList = new PreviousStatesList()
                .items( List.of( testDataManager.fetchPreviousStates( "MKAssociation003" ).get( 2 ) ) )
                .links(new Links().self(ASSOCIATIONS + "/MKAssociation003/previous-states?page_index=1&items_per_page=1").next(ASSOCIATIONS + "/MKAssociation003/previous-states?page_index=2&items_per_page=1"))
                .pageNumber( 1 )
                .itemsPerPage( 1 )
                .totalResults( 4 )
                .totalPages( 4 );

        final var now = LocalDateTime.now();

        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn( Optional.of( previousStatesList ) ).when( associationsService ).fetchPreviousStates( "MKAssociation003", 1, 1 );

        final var response = mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation003/previous-states?page_index=1&items_per_page=1")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect( status().isOk() );

        final var result = parseResponseTo( response, PreviousStatesList.class );
        final var links = result.getLinks();
        final var items = result.getItems();

        Assertions.assertEquals( 1, result.getItemsPerPage() );
        Assertions.assertEquals( 1, result.getPageNumber() );
        Assertions.assertEquals( 4, result.getTotalResults() );
        Assertions.assertEquals( 4, result.getTotalPages() );
        Assertions.assertEquals(ASSOCIATIONS + "/MKAssociation003/previous-states?page_index=1&items_per_page=1", links.getSelf());
        Assertions.assertEquals(ASSOCIATIONS + "/MKAssociation003/previous-states?page_index=2&items_per_page=1", links.getNext());
        Assertions.assertEquals( 1, items.size() );
        Assertions.assertEquals( AWAITING_APPROVAL, items.getFirst().getStatus() );
        Assertions.assertEquals( "MKUser003", items.getFirst().getChangedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.minusDays( 7L ) ), reduceTimestampResolution( items.getFirst().getChangedAt() ) );
    }

    @Test
    void getPreviousStatesForAssociationSupportsUnauthorisedAssociation() throws Exception {
        final var proposedPreviousStates = testDataManager.fetchPreviousStates( "MKAssociation005" );

        final var proposedPreviousStatesList = new PreviousStatesList()
                .items( List.of( proposedPreviousStates.getLast(), proposedPreviousStates.get( 3 ) ) )
                .links(new Links().self(ASSOCIATIONS + "/MKAssociation005/previous-states?page_index=0&items_per_page=2").next(ASSOCIATIONS + "/MKAssociation005/previous-states?page_index=1&items_per_page=2"))
                .pageNumber( 0 )
                .itemsPerPage( 2 )
                .totalResults( 5 )
                .totalPages( 3 );

        final var now = LocalDateTime.now();

        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn( Optional.of( proposedPreviousStatesList ) ).when( associationsService ).fetchPreviousStates( "MKAssociation005", 0, 2 );

        final var response = mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation005/previous-states?page_index=0&items_per_page=2")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect( status().isOk() );

        final var previousStatesList = parseResponseTo( response, PreviousStatesList.class );
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();

        Assertions.assertEquals( 2, previousStatesList.getItemsPerPage() );
        Assertions.assertEquals( 0, previousStatesList.getPageNumber() );
        Assertions.assertEquals( 5, previousStatesList.getTotalResults() );
        Assertions.assertEquals( 3, previousStatesList.getTotalPages() );
        Assertions.assertEquals(ASSOCIATIONS + "/MKAssociation005/previous-states?page_index=0&items_per_page=2", links.getSelf());
        Assertions.assertEquals(ASSOCIATIONS + "/MKAssociation005/previous-states?page_index=1&items_per_page=2", links.getNext());
        Assertions.assertEquals( 2, items.size() );

        Assertions.assertEquals( UNAUTHORISED, items.getFirst().getStatus() );
        Assertions.assertEquals( "MKUser005", items.getFirst().getChangedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.minusDays( 3L ) ), reduceTimestampResolution( items.getFirst().getChangedAt() ) );

        Assertions.assertEquals( CONFIRMED, items.getLast().getStatus() );
        Assertions.assertEquals( "Companies House", items.getLast().getChangedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.minusDays( 6L ) ), reduceTimestampResolution( items.getLast().getChangedAt() ) );
    }

    @Test
    void updateAssociationStatusForIdWithAPIKeyCanSetStatusToUnauthorised() throws Exception {
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation002" ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos( "MKUser002" ).getFirst();

        Mockito.doReturn( Optional.of( targetAssociation ) ).when( associationsService ).fetchAssociationDao( "MKAssociation002" );
        Mockito.doReturn( targetUser ).when( usersService ).fetchUserDetails( targetAssociation );

        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation002")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"unauthorised\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( associationsService ).updateAssociation( eq( targetAssociation.getId() ), any( Update.class ) );
    }



    private static Stream<Arguments> updateAssociationStatusForIdWithAPIKeyBadRequestScenarios(){
        return Stream.of(
                Arguments.of( "confirmed" ),
                Arguments.of( "removed" )
        );
    }

    @ParameterizedTest
    @MethodSource( "updateAssociationStatusForIdWithAPIKeyBadRequestScenarios" )
    void updateAssociationStatusForIdWithAPIKeyReturnsBadRequestWhenBodyContainsConfirmedOrRemoved( final String status ) throws Exception {
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation002" ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos( "MKUser002" ).getFirst();

        Mockito.doReturn( Optional.of( targetAssociation ) ).when( associationsService ).fetchAssociationDao( "MKAssociation002" );
        Mockito.doReturn( targetUser ).when( usersService ).fetchUserDetails( targetAssociation );

        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation002")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( String.format( "{\"status\":\"%s\"}", status ) ) )
                .andExpect( status().isBadRequest() );
    }

    private static Stream<Arguments> updateAssociationStatusForIdAPIRequestsThatChangeUnauthorisedOrMigratedAssociationsToConfirmedScenarios(){
        return Stream.of(
                Arguments.of( "MKAssociation004", "MKUser004" ),
                Arguments.of("MKAssociation001", MK_USER_001)
        );
    }

    @ParameterizedTest
    @MethodSource( "updateAssociationStatusForIdAPIRequestsThatChangeUnauthorisedOrMigratedAssociationsToConfirmedScenarios" )
    void updateAssociationStatusForIdSupportsAPIRequestsThatChangeUnauthorisedOrMigratedAssociationsToConfirmed( final String targetAssociationId, final String targetUserId ) throws Exception {
        final var associations = testDataManager.fetchAssociationDaos( targetAssociationId ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos( targetUserId ).getFirst();

        Mockito.doReturn( Optional.of( associations ) ).when( associationsService ).fetchAssociationDao( targetAssociationId );
        Mockito.doReturn( targetUser ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        mockers.mockCompanyServiceFetchCompanyProfile( associations.getCompanyNumber() );
        mockers.mockUsersServiceFetchUserDetails( targetUserId );
        Mockito.doReturn(sendEmailMock).when(emailService).sendAuthCodeConfirmationEmailToAssociatedUser(eq(X_REQUEST_ID_VALUE), eq(associations.getCompanyNumber()), any(Mono.class), eq(targetUser.getDisplayName()));

        mockMvc.perform(patch(String.format(ASSOCIATIONS + "/%s", targetAssociationId))
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( associationsService ).updateAssociation( eq( associations.getId() ), any( Update.class ) );
        Mockito.verify( emailService ).sendStatusUpdateEmails( eq(associations ), eq( targetUser ), eq( StatusEnum.CONFIRMED ), any( RequestContextData.class ) );
    }

    private static Stream<Arguments> updateAssociationStatusForIdSameUserUnauthorisedBadRequestScenarios(){
        return Stream.of(
                Arguments.of( "confirmed" ),
                Arguments.of( "unauthorised" )
        );
    }

    @ParameterizedTest
    @MethodSource( "updateAssociationStatusForIdSameUserUnauthorisedBadRequestScenarios" )
    void updateAssociationStatusForIdWhereUserTriesToConfirmOwnUnauthorisedAssociationOrSetStatusToUnauthorisedReturnsBadRequest( final String status ) throws Exception {
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation004" ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos( "MKUser004" ).getFirst();

        Mockito.doReturn( Optional.of( targetAssociation ) ).when( associationsService ).fetchAssociationDao( "MKAssociation004" );
        Mockito.doReturn( targetUser ).when( usersService ).fetchUserDetails( targetAssociation );

        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation004")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser004")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( String.format( "{\"status\":\"%s\"}", status ) ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWhereDifferentUserAttemptsToConfirmAnotherUsersUnauthorisedAssociationSendsInvitation() throws Exception {
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation004" ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos( "MKUser004" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn( Optional.of( targetAssociation ) ).when( associationsService ).fetchAssociationDao( "MKAssociation004" );
        Mockito.doReturn( targetUser ).when( usersService ).fetchUserDetails( targetAssociation );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "MKCOMP001", "MKUser002" );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendInvitationEmailToAssociatedUser( any(), any(), any(), any(), any() );

        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation004")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( associationsService ).updateAssociation( eq( targetAssociation.getId() ), any( Update.class ) );
    }

    @Test
    void updateAssociationStatusForIdWhereDifferentUserAttemptsToSetAnotherUsersAssociationToUnauthorisedReturnsBadRequest() throws Exception {
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation004" ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos( "MKUser004" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn( Optional.of( targetAssociation ) ).when( associationsService ).fetchAssociationDao( "MKAssociation004" );
        Mockito.doReturn( targetUser ).when( usersService ).fetchUserDetails( targetAssociation );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "MKCOMP001", "MKUser002" );

        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation004")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"unauthorised\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getPreviousStatesForAssociationUsesDefaults() throws Exception {
        final var previousStatesList = new PreviousStatesList()
                .items( List.of() )
                .links(new Links().self(ASSOCIATIONS + "/MKAssociation001/previous-states?page_index=0&items_per_page=15").next(""))
                .pageNumber( 0 )
                .itemsPerPage( 15 )
                .totalResults( 0 )
                .totalPages( 0 );

        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn( Optional.of( previousStatesList ) ).when( associationsService ).fetchPreviousStates( "MKAssociation001", 0, 15 );

        final var response = mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation001/previous-states")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect( status().isOk() );

        final var result = parseResponseTo( response, PreviousStatesList.class );
        final var links = result.getLinks();
        final var items = result.getItems();

        Assertions.assertEquals( 15, result.getItemsPerPage() );
        Assertions.assertEquals( 0, result.getPageNumber() );
        Assertions.assertEquals( 0, result.getTotalResults() );
        Assertions.assertEquals( 0, result.getTotalPages() );
        Assertions.assertEquals(ASSOCIATIONS + "/MKAssociation001/previous-states?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertTrue( items.isEmpty() );
    }

    private static Stream<Arguments> getPreviousStatesForAssociationMalformedScenarios(){
        return Stream.of(
                Arguments.of(ASSOCIATIONS + "/$$$/previous-states"),
                Arguments.of(ASSOCIATIONS + "/MKAssociation003/previous-states?page_index=-1"),
                Arguments.of(ASSOCIATIONS + "/MKAssociation003/previous-states?items_per_page=-1")
        );
    }

    @ParameterizedTest
    @MethodSource( "getPreviousStatesForAssociationMalformedScenarios" )
    void getPreviousStatesForAssociationWithMalformedAssociationIdReturnsBadRequest( final String uri ) throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        mockMvc.perform( get( uri )
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getPreviousStatesForAssociationWithNonexistentAssociationReturnsNotFound() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn( Optional.empty() ).when( associationsService ).fetchPreviousStates( "404MKAssociation", 0, 15 );

        mockMvc.perform(get(ASSOCIATIONS + "/404MKAssociation/previous-states")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect( status().isNotFound() );
    }

    @Test
    void getPreviousStatesForAssociationCanBeCalledByAdmin() throws Exception {
        final var previousStatesList = new PreviousStatesList()
                .items( List.of() )
                .links(new Links().self(ASSOCIATIONS + "/MKAssociation001/previous-states?page_index=0&items_per_page=15").next(""))
                .pageNumber( 0 )
                .itemsPerPage( 15 )
                .totalResults( 0 )
                .totalPages( 0 );

        mockers.mockUsersServiceFetchUserDetails( "111" );
        Mockito.doReturn( Optional.of( previousStatesList ) ).when( associationsService ).fetchPreviousStates( "MKAssociation001", 0, 15 );

        mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation001/previous-states")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header( "Eric-Authorised-Roles", ADMIN_READ_PERMISSION ) )
                .andExpect( status().isOk() );
    }

}
