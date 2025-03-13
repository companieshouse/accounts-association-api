package uk.gov.companieshouse.accounts.association.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.configuration.WebSecurityConfig;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.Links;

@WebMvcTest( UserCompanyAssociation.class )
@Import( WebSecurityConfig.class )
@Tag( "unit-test" )
class UserCompanyAssociationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private StaticPropertyUtil staticPropertyUtil;

    @MockBean
    private UsersService usersService;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private AssociationsService associationsService;

    @MockBean
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
        mockMvc.perform(get("/associations/")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationDetailsWithMalformedInputReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations/$")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationUserDetailsWithNonexistentIdReturnsNotFound() throws Exception {
        Mockito.doReturn(Optional.empty()).when(associationsService).findAssociationById("11");
        mockMvc.perform(get("/associations/11")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationDetailsFetchesAssociationDetails() throws Exception {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var association = testDataManager.fetchAssociationDto( "18", user );

        Mockito.doReturn(Optional.of(association)).when(associationsService).findAssociationById("18");

        final var response = mockMvc.perform(get("/associations/18")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk());
        final var result = parseResponseTo( response, Association.class );

        Assertions.assertEquals("18", result.getId());
    }

    @Test
    void getAssociationForIdCanFetchMigratedAssociation() throws Exception {
        final var user = testDataManager.fetchUserDtos( "MKUser001" ).getFirst();
        final var association = testDataManager.fetchAssociationDto( "MKAssociation001", user );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).findAssociationById( "MKAssociation001" );

        final var response =
                mockMvc.perform( get( "/associations/MKAssociation001" )
                                .header( "X-Request-Id", "theId123" )
                                .header( "Eric-identity", "MKUser001" )
                                .header( "ERIC-Identity-Type", "oauth2" ) )
                        .andExpect( status().isOk() );

        final var responseAssociation = parseResponseTo( response, Association.class );

        Assertions.assertEquals( "MKAssociation001", responseAssociation.getId() );
        Assertions.assertEquals( "migrated", responseAssociation.getStatus().getValue() );
        Assertions.assertEquals( "migration", responseAssociation.getApprovalRoute().getValue() );
    }

    @Test
    void getInvitationsForAssociationWithUnacceptablePageIndexReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations/1/invitations?page_index=-1&items_per_page=1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInvitationsForAssociationWithUnacceptableItemsPerPageReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations/1/invitations?page_index=0&items_per_page=-1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInvitationsForAssociationWithMalformedInputReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations/$/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInvitationsForAssociationWithNonexistentIdReturnsNotFound() throws Exception {
        Mockito.doReturn(Optional.empty()).when(associationsService).findAssociationDaoById("11");

        mockMvc.perform(get("/associations/11/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInvitationsForAssociationFetchesInvitations() throws Exception {
        final var associationDao = testDataManager.fetchAssociationDaos( "37" ).getFirst();
        final var invitations = testDataManager.fetchInvitations( "37" );

        Mockito.doReturn(Optional.of(associationDao)).when(associationsService).findAssociationDaoById("37");
        Mockito.doReturn( new InvitationsList().items( invitations ) ).when(associationsService).fetchInvitations(associationDao, 0, 15);

        final var response = mockMvc.perform(get("/associations/37/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
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
                .self("/associations/37/invitations?page_index=1&items_per_page=1")
                .next("/associations/37/invitations?page_index=2&items_per_page=1");

        final var mockInvitationsList = new InvitationsList()
                .items(invitations).links(mockLinks)
                .itemsPerPage(1).pageNumber(1).totalResults(2).totalPages(2);

        when(associationsService.findAssociationDaoById("37")).thenReturn(Optional.of(association));
        when(associationsService.fetchInvitations(association, 1, 1)).thenReturn(mockInvitationsList);

        final var response = mockMvc.perform(get("/associations/37/invitations?page_index=1&items_per_page=1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk());
        final var invitationsList = parseResponseTo( response, InvitationsList.class );

        Mockito.verify(associationsService).findAssociationDaoById("37");
        Mockito.verify(associationsService).fetchInvitations(association, 1, 1);

        assertNotNull(invitationsList);
        assertEquals(1, invitationsList.getItemsPerPage().intValue());
        assertEquals(1, invitationsList.getPageNumber().intValue());
        assertEquals(2, invitationsList.getTotalResults().intValue());
        assertEquals(2, invitationsList.getTotalPages().intValue());
        assertNotNull(invitationsList.getLinks());
        assertEquals("/associations/37/invitations?page_index=1&items_per_page=1", invitationsList.getLinks().getSelf());
        assertEquals("/associations/37/invitations?page_index=2&items_per_page=1", invitationsList.getLinks().getNext());
    }

    @Test
    void getInvitationsForAssociationWithMigratedAssociationReturnsEmpty() throws Exception {
        final var associationDao = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();

        Mockito.doReturn( Optional.of( associationDao ) ).when( associationsService ).findAssociationDaoById( "MKAssociation001" );
        Mockito.doReturn( new InvitationsList().items( List.of() ) ).when( associationsService ).fetchInvitations( associationDao, 0, 15 );

        final var response = mockMvc.perform( get( "/associations/MKAssociation001/invitations" )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "MKUser001" )
                        .header( "ERIC-Identity-Type", "oauth2" ) )
                .andExpect( status().isOk() );

        final var resultInvitationsList = parseResponseTo( response, InvitationsList.class );
        final var resultInvitations = resultInvitationsList.getItems();

        Assertions.assertTrue( resultInvitations.isEmpty() );
    }

    @Test
    void updateAssociationStatusForIdWithMalformedAssociationIdReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/associations/$$$")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"removed\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithoutRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/associations/18")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithoutStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/associations/18")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithMalformedStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/associations/18")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"complicated\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithNonexistentAssociationIdReturnsNotFound() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        Mockito.doReturn(Optional.empty()).when(associationsService).findAssociationById("9191");

        mockMvc.perform(patch("/associations/9191")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"confirmed\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAssociationStatusForIdWithRemovedUpdatesAssociationStatus() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "35" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "000" );
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).findAssociationDaoById( "35" );
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn( Mono.empty() ).when( emailService ).sendAuthorisationRemovedEmailToRemovedUser( eq( "theId123" ), eq( "333333" ), any( Mono.class ), eq( "Scrooge McDuck" ), eq( "light.yagami@death.note" ) );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendAuthorisationRemovedEmailToAssociatedUser( eq( "theId123" ), eq( "333333" ), any( Mono.class ), eq( "Scrooge McDuck" ), eq( "light.yagami@death.note" ) );

        mockMvc.perform(patch("/associations/35")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"removed\"}"))
                .andExpect(status().isOk());

        Mockito.verify(associationsService).updateAssociation(anyString(), any(Update.class));
    }

    @Test
    void updateAssociationStatusForIdWithConfirmedUpdatesWithNoUserFoundShouldThrow404AssociationStatus() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "36" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).findAssociationDaoById( "36" );
        Mockito.doReturn( null ).when(usersService).searchUserDetails(List.of("light.yagami@death.note"));

        mockMvc.perform(patch("/associations/36")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"confirmed\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndExistingUserAndConfirmedUpdatesAssociationStatus() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "36" ).getFirst();

        mockers.mockUsersServiceSearchUserDetails( "000" );
        mockers.mockUsersServiceFetchUserDetails( "000" );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).findAssociationDaoById( "36" );

        mockMvc.perform(patch("/associations/36")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"confirmed\"}"))
                .andExpect(status().isOk());

        Mockito.verify(associationsService, new Times(1)).updateAssociation(anyString(), any(Update.class));
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndNonexistentUserAndConfirmedReturnsBadRequest() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "36" ).getFirst();

        mockers.mockUsersServiceSearchUserDetailsEmptyList( "light.yagami@death.note" );
        mockers.mockUsersServiceFetchUserDetails( "000" );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).findAssociationDaoById( "36" );

        mockMvc.perform(patch("/associations/36")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"confirmed\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndExistingUserAndRemovedUpdatesAssociationStatus() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "34" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "000" );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).findAssociationDaoById( "34" );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendInvitationRejectedEmailToAssociatedUser( eq( "theId123" ), eq( "111111" ), any( Mono.class ), eq( "light.yagami@death.note" ) );

        mockMvc.perform(patch("/associations/34")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"removed\"}"))
                .andExpect(status().isOk());

        Mockito.verify(associationsService, new Times(1)).updateAssociation(anyString(), any(Update.class));
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndNonexistentUserAndRemovedUpdatesAssociationStatus() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "34" ).getFirst();

        mockers.mockUsersServiceSearchUserDetailsEmptyList( "light.yagami@death.note" );
        mockers.mockUsersServiceFetchUserDetails( "000" );
        Mockito.doReturn( Optional.of( association ) ).when( associationsService ).findAssociationDaoById( "34" );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendInvitationRejectedEmailToAssociatedUser( eq( "theId123" ), eq( "111111" ), any( Mono.class ), eq( "light.yagami@death.note" ) );

        mockMvc.perform(patch("/associations/34")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"removed\"}"))
                .andExpect(status().isOk());

        Mockito.verify(associationsService, new Times(1)).updateAssociation(anyString(), any(Update.class));
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereTargetUserIdExistsSendsNotification() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "35" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999", "000" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).findAssociationDaoById("35");
        Mockito.doReturn( List.of( "9999" ) ).when( associationsService ).fetchAssociatedUsers( "333333" );
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn( Mono.empty() ).when( emailService ).sendAuthorisationRemovedEmailToRemovedUser( eq( "theId123" ), eq( "333333" ), any( Mono.class ), eq( "Scrooge McDuck" ), eq( "light.yagami@death.note" ) );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendAuthorisationRemovedEmailToAssociatedUser( eq( "theId123" ), eq( "333333" ), any( Mono.class ), eq( "Scrooge McDuck" ), eq( "light.yagami@death.note" ) );

        mockMvc.perform( patch( "/associations/35" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendAuthorisationRemovedEmailToAssociatedUser( eq( "theId123" ), eq( "333333" ), any( Mono.class ), eq( "Scrooge McDuck" ), eq( "light.yagami@death.note" ) );
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereTargetUserIdDoesNotExistSendsNotification() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "34" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "111", "000" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).findAssociationDaoById("34");
        Mockito.doReturn( List.of( "111" ) ).when( associationsService ).fetchAssociatedUsers( "111111" );
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn( Mono.empty() ).when( emailService ).sendInviteCancelledEmail( eq( "theId123" ), eq( "111111" ), any( Mono.class ), eq( "Batman" ), eq( association ) );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendInvitationCancelledEmailToAssociatedUser( eq( "theId123" ), eq( "111111" ), any( Mono.class ), eq( "Batman" ), eq( "light.yagami@death.note" ) );

        mockMvc.perform( patch( "/associations/34" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendInvitationCancelledEmailToAssociatedUser( eq( "theId123" ), eq( "111111" ), any( Mono.class ), eq( "Batman" ), eq( "light.yagami@death.note" ) );
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereUserCannotBeFoundSendsNotification() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "34" ).getFirst();

        mockers.mockUsersServiceSearchUserDetailsEmptyList( "light.yagami@death.note" );
        mockers.mockUsersServiceFetchUserDetails( "111" );
        mockers.mockCompanyServiceFetchCompanyProfile("111111");
        Mockito.doReturn(Optional.of(association)).when(associationsService).findAssociationDaoById("34");
        Mockito.doReturn( List.of("000" ) ).when( associationsService ).fetchAssociatedUsers( "111111" );
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn( Mono.empty() ).when( emailService ).sendInviteCancelledEmail( eq( "theId123" ), eq( "111111" ), any( Mono.class ), eq( "Batman" ), eq( association ) );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendInvitationCancelledEmailToAssociatedUser( eq( "theId123" ), eq( "111111" ), any( Mono.class ), eq( "Batman" ), eq( "light.yagami@death.note" ) );

        mockMvc.perform( patch( "/associations/34" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendInvitationCancelledEmailToAssociatedUser( eq( "theId123" ), eq( "111111" ), any( Mono.class ), eq( "Batman" ), eq( "light.yagami@death.note" ) );
    }

    @Test
    void updateAssociationStatusForIdNotificationsUsesDisplayNamesWhenAvailable() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "4" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        mockers.mockUsersServiceSearchUserDetails( "444" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).findAssociationDaoById("4");
        Mockito.doReturn( List.of( "333" ) ).when( associationsService ).fetchAssociatedUsers( "111111" );
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn( Mono.empty() ).when( emailService ).sendAuthorisationRemovedEmailToRemovedUser( eq( "theId123" ), eq( "111111" ), any( Mono.class ), eq( "Harleen Quinzel" ), eq( "Boy Wonder" ) );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendAuthorisationRemovedEmailToAssociatedUser( eq( "theId123" ), eq( "111111" ), any( Mono.class ), eq( "Harleen Quinzel" ), eq( "Boy Wonder" ) );

        mockMvc.perform( patch( "/associations/4" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendAuthorisationRemovedEmailToRemovedUser( eq( "theId123" ), eq( "111111" ), any( Mono.class ), eq( "Harleen Quinzel" ), eq( "444" ) );
        Mockito.verify( emailService ).sendAuthorisationRemovedEmailToAssociatedUser( eq( "theId123" ), eq( "111111" ), any( Mono.class ), eq( "Harleen Quinzel" ), eq("Boy Wonder") );
    }

    @Test
    void updateAssociationStatusForIdUserAcceptedInvitationNotificationsSendsNotification() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "6" ).getFirst();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "666", "222" );
        mockers.mockUsersServiceSearchUserDetails( "666" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).findAssociationDaoById("6");
        Mockito.doReturn( List.of( "222", "444" ) ).when( associationsService ).fetchAssociatedUsers( "111111" );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendInvitationAcceptedEmailToAssociatedUser( eq( "theId123" ), eq( "111111" ), any( Mono.class ), any( Mono.class ), eq( "homer.simpson@springfield.com" ) );

        mockMvc.perform( patch( "/associations/6" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "666")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendInvitationAcceptedEmailToAssociatedUser( eq( "theId123" ), eq( "111111" ), any( Mono.class ), any( Mono.class ), eq( "homer.simpson@springfield.com" ) );
    }

    @Test
    void confirmUserStatusForExistingAssociationWithoutOneLoginUserShouldThrow400BadRequest() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "6" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "666", "222" );
        mockers.mockUsersServiceSearchUserDetailsEmptyList( "homer.simpson@springfield.com" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).findAssociationDaoById("6");
        Mockito.doReturn( List.of( "222", "444" ) ).when( associationsService ).fetchAssociatedUsers( "111111" );

        mockMvc.perform( patch( "/associations/6" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "666")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void confirmUserStatusRequestForExistingAssociationWithoutOneLoginUserAndDifferentRequestingUserShouldThrow400BadRequest() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "6" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "666", "222" );
        mockers.mockUsersServiceSearchUserDetailsEmptyList( "homer.simpson@springfield.com" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).findAssociationDaoById("6");
        Mockito.doReturn( List.of( "222", "444" ) ).when( associationsService ).fetchAssociatedUsers( "111111" );

        mockMvc.perform( patch( "/associations/6" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "222")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdUserAcceptedInvitationNotificationsUsesDisplayNamesWhenAvailable() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "38" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999", "444" );
        mockers.mockUsersServiceSearchUserDetailsEmptyList( "scrooge.mcduck@disney.land" );
        mockers.mockCompanyServiceFetchCompanyProfile( "x222222" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).findAssociationDaoById("38");
        Mockito.doReturn( List.of( "111", "222", "444" ) ).when( associationsService ).fetchAssociatedUsers( "x222222" );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendInvitationAcceptedEmailToAssociatedUser( eq( "theId123" ), eq( "x222222" ), any( Mono.class ), any( Mono.class ), eq( "Scrooge McDuck" ) );

        mockMvc.perform( patch( "/associations/38" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendInvitationAcceptedEmailToAssociatedUser( eq( "theId123" ), eq( "x222222" ), any( Mono.class ), any(), eq( "Scrooge McDuck" ) );
    }

    @Test
    void updateAssociationStatusForIdUserCancelledInvitationNotificationsSendsNotification() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "38" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999", "222" );
        mockers.mockUsersServiceSearchUserDetailsEmptyList( "scrooge.mcduck@disney.land" );
        mockers.mockCompanyServiceFetchCompanyProfile( "x222222" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).findAssociationDaoById("38");
        Mockito.doReturn( List.of( "111", "222", "444" ) ).when( associationsService ).fetchAssociatedUsers( "x222222" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "x222222", "222" );
        Mockito.doReturn( Mono.empty() ).when( emailService ).sendInviteCancelledEmail( eq( "theId123" ), eq( "x222222" ), any( Mono.class ), eq( "the.joker@gotham.city" ), eq( association ) );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendInvitationCancelledEmailToAssociatedUser( eq( "theId123" ), eq( "x222222" ), any( Mono.class ), eq( "the.joker@gotham.city" ), eq( "Scrooge McDuck" ) );

        mockMvc.perform( patch( "/associations/38" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "222")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendInvitationCancelledEmailToAssociatedUser( eq( "theId123" ), eq( "x222222" ), any( Mono.class ), eq( "the.joker@gotham.city" ), eq( "Scrooge McDuck" ) );
    }

    @Test
    void updateAssociationStatusForIdUserRejectedInvitationNotificationsSendsNotification() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "38" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockUsersServiceSearchUserDetailsEmptyList( "scrooge.mcduck@disney.land" );
        mockers.mockCompanyServiceFetchCompanyProfile( "x222222" );
        Mockito.doReturn(Optional.of(association)).when(associationsService).findAssociationDaoById("38");
        Mockito.doReturn( List.of( "111", "222", "444" ) ).when( associationsService ).fetchAssociatedUsers( "x222222" );

        Mockito.doReturn( sendEmailMock ).when( emailService ).sendInvitationRejectedEmailToAssociatedUser( eq( "theId123" ), eq( "x222222" ), any( Mono.class ), eq( "Scrooge McDuck" ) );

        mockMvc.perform( patch( "/associations/38" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendInvitationRejectedEmailToAssociatedUser( eq( "theId123" ), eq( "x222222" ), any( Mono.class ), eq( "Scrooge McDuck" ) );
    }

}
