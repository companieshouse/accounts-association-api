package uk.gov.companieshouse.accounts.association.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.configuration.WebSecurityConfig;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsTransactionService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.Links;

@WebMvcTest(UserCompanyInvitationsController.class)
@Import(WebSecurityConfig.class)
@Tag("unit-test")
class UserCompanyInvitationsControllerTest {

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
    private AssociationsTransactionService associationsTransactionService;

    @MockitoBean
    private EmailService emailService;

    final Function<String, Mono<Void>> sendEmailMock = userId -> Mono.empty();

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static Mockers mockers;

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    @BeforeEach
    public void setup() {
        mockers = new Mockers(null, null, companyService, usersService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void fetchActiveInvitationsForUserWithoutEricIdentityReturnsForbidden() throws Exception {
        mockMvc.perform(get("/associations/invitations?page_index=1&items_per_page=1")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isForbidden());
    }

    @Test
    void fetchActiveInvitationsForUserWithMalformedEricIdentityReturnsForbidden() throws Exception {
        mockers.mockUsersServiceFetchUserDetailsNotFound("$$$$");

        mockMvc.perform(get("/associations/invitations?page_index=1&items_per_page=1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "$$$$")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isForbidden());
    }

    @Test
    void fetchActiveInvitationsForUserWithNonexistentEricIdentityReturnsForbidden() throws Exception {
        mockers.mockUsersServiceFetchUserDetailsNotFound("9191");

        mockMvc.perform(get("/associations/invitations?page_index=1&items_per_page=1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isForbidden());
    }

    @Test
    void fetchActiveInvitationsForUserWithUnacceptablePageIndexReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations/invitations?page_index=-1&items_per_page=1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fetchActiveInvitationsForUserWithUnacceptableItemsPerPageReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations/invitations?page_index=0&items_per_page=-1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fetchActiveInvitationsForUserRetrievesActiveInvitationsInCorrectOrderAndPaginatesCorrectly() throws Exception {
        final var user = testDataManager.fetchUserDtos("9999").getFirst();

        mockers.mockUsersServiceFetchUserDetails("9999");

        mockMvc.perform(get("/associations/invitations?page_index=1&items_per_page=1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk());

        Mockito.verify(associationsTransactionService).fetchActiveInvitations(argThat(comparisonUtils.compare(user, List.of("userId"), List.of(), Map.of())), eq(1), eq(1));
    }

    @Test
    void fetchActiveInvitationsForUserWithoutPageIndexAndItemsPerPageUsesDefaults() throws Exception {
        final var user = testDataManager.fetchUserDtos("9999").getFirst();

        mockers.mockUsersServiceFetchUserDetails("9999");

        mockMvc.perform(get("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk());

        Mockito.verify(associationsTransactionService).fetchActiveInvitations(argThat(comparisonUtils.compare(user, List.of("userId"), List.of(), Map.of())), eq(0), eq(15));
    }

    @Test
    void fetchActiveInvitationsForUserWithPaginationAndVerifyResponse() throws Exception {
        final var user = testDataManager.fetchUserDtos("000").getFirst();
        final var invitations = testDataManager.fetchInvitations("37");

        final var mockLinks = new Links()
                .self("/associations/invitations?page_index=1&items_per_page=1")
                .next("/associations/invitations?page_index=2&items_per_page=1");

        final var mockInvitationsList = new InvitationsList()
                .items(invitations).links(mockLinks)
                .itemsPerPage(1).pageNumber(1).totalResults(2).totalPages(2);

        mockers.mockUsersServiceFetchUserDetails("000");
        when(associationsTransactionService.fetchActiveInvitations(user, 1,1)).thenReturn(mockInvitationsList);

        final var response = mockMvc.perform(get("/associations/invitations?page_index=1&items_per_page=1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk());

        Mockito.verify(associationsTransactionService).fetchActiveInvitations(user,1,1);

        final var invitationsList = parseResponseTo(response, InvitationsList.class);

        assertNotNull(invitationsList);
        assertEquals(1, invitationsList.getItemsPerPage().intValue());
        assertEquals(1, invitationsList.getPageNumber().intValue());
        assertEquals(2, invitationsList.getTotalResults().intValue());
        assertEquals(2, invitationsList.getTotalPages().intValue());
        assertNotNull(invitationsList.getLinks());
        assertEquals("/associations/invitations?page_index=1&items_per_page=1", invitationsList.getLinks().getSelf());
        assertEquals("/associations/invitations?page_index=2&items_per_page=1", invitationsList.getLinks().getNext());
    }

    @Test
    void inviteUserWithMalformedEricIdentityReturnsBadRequest() throws Exception {
        mockers.mockUsersServiceFetchUserDetailsNotFound("$$$$");

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "$$$$")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void inviteUserWithNonexistentEricIdentityReturnsForbidden() throws Exception {
        mockers.mockUsersServiceFetchUserDetailsNotFound("9191");

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void inviteUserWithoutRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithoutCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"$$$$$$\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithNonexistentCompanyNumberReturnsBadRequest() throws Exception {
        mockers.mockUsersServiceFetchUserDetails("9999");
        mockers.mockCompanyServiceFetchCompanyProfileNotFound("919191");

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"919191\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithoutInviteeEmailIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithMalformedInviteeEmailIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"$$$\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeEmailAndCompanyNumberExistsAndInviteeUserIsFoundPerformsSwapAndUpdateOperations() throws Exception {
        final var associationDao = testDataManager.fetchAssociationDaos("36").getFirst();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos("444444").getFirst();

        mockers.mockUsersServiceFetchUserDetails("9999");
        mockers.mockCompanyServiceFetchCompanyProfile("444444");
        mockers.mockUsersServiceSearchUserDetails("000");
        Mockito.doReturn(Optional.of(associationDao)).when(associationsTransactionService).fetchAssociationDao("444444", "000", "light.yagami@death.note");
        Mockito.doReturn(Stream.of("000")).when(associationsTransactionService).fetchConfirmedUserIds("444444");
        Mockito.when(
                associationsTransactionService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        Mockito.verify(emailService).sendInviteEmail(eq("theId123"), eq("444444"), any(), eq("Scrooge McDuck"), anyString(), eq("light.yagami@death.note"));
        Mockito.verify(emailService).sendInvitationEmailToAssociatedUser(eq("theId123"), eq("444444"), any(), eq("Scrooge McDuck"), eq("light.yagami@death.note"), any());
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeEmailAndCompanyNumberExistsAndInviteeUserIsNotFoundDoesNotPerformSwapButDoesPerformUpdateOperation() throws Exception {
        final var associationDao = testDataManager.fetchAssociationDaos("36").getFirst();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos("444444").getFirst();

        mockers.mockUsersServiceFetchUserDetails("9999");
        mockers.mockCompanyServiceFetchCompanyProfile("444444");
        mockers.mockUsersServiceSearchUserDetailsEmptyList("000");
        Mockito.doReturn(Optional.of(associationDao)).when(associationsTransactionService).fetchAssociationDao("444444", null, "light.yagami@death.note");
        Mockito.doReturn(Stream.of("9999")).when(associationsTransactionService).fetchConfirmedUserIds("444444");
        Mockito.when(
                associationsTransactionService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        Mockito.verify(emailService).sendInviteEmail(eq("theId123"), eq("444444"), any(), eq("Scrooge McDuck"), anyString(), eq("light.yagami@death.note"));
        Mockito.verify(emailService).sendInvitationEmailToAssociatedUser(eq("theId123"), eq("444444"), any(), eq("Scrooge McDuck"), eq("light.yagami@death.note"), any());
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberExistsDoesNotPerformSwapButDoesPerformUpdateOperation() throws Exception {
        final var targetUserAssociation = testDataManager.fetchAssociationDaos("36").getFirst();

        mockers.mockUsersServiceSearchUserDetails("000");
        mockers.mockCompanyServiceFetchCompanyProfile("444444");
        mockers.mockUsersServiceFetchUserDetails("9999");
        Mockito.doReturn(Optional.of(targetUserAssociation)).when(associationsTransactionService).fetchAssociationDao("444444", "000", "light.yagami@death.note");
        Mockito.doReturn(Stream.of("000")).when(associationsTransactionService).fetchConfirmedUserIds("444444");
        Mockito.when(
                associationsTransactionService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        Mockito.verify(emailService).sendInviteEmail(eq("theId123"), eq("444444"), any(), eq("Scrooge McDuck"), anyString(), eq("light.yagami@death.note"));
        Mockito.verify(emailService).sendInvitationEmailToAssociatedUser(eq("theId123"), eq("444444"), any(), eq("Scrooge McDuck"), eq("light.yagami@death.note"), any());
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberDoesNotExistCreatesNewAssociation() throws Exception {
        final var association = testDataManager.fetchAssociationDaos("19").getFirst();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos("444444").getFirst();

        mockers.mockUsersServiceFetchUserDetails("9999");
        mockers.mockCompanyServiceFetchCompanyProfile("444444");
        mockers.mockUsersServiceSearchUserDetails("000");
        Mockito.doReturn(Optional.empty()).when(associationsTransactionService).fetchAssociationDao("444444", null, "light.yagami@death.note");
        Mockito.doReturn(Optional.empty()).when(associationsTransactionService).fetchAssociationDao("444444", "000", null);
        Mockito.doReturn(Stream.of("000")).when(associationsTransactionService).fetchConfirmedUserIds("444444");
        Mockito.when(
                associationsTransactionService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn(association).when(associationsTransactionService).createAssociationWithInvitationApprovalRoute("444444", "000", null, "9999");

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        Mockito.verify(emailService).sendInviteEmail(eq("theId123"), eq("444444"), any(), eq("Scrooge McDuck"), anyString(), eq("light.yagami@death.note"));
        Mockito.verify(emailService).sendInvitationEmailToAssociatedUser(eq("theId123"), eq("444444"), any(), eq("Scrooge McDuck"), eq("light.yagami@death.note"), any());
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeUserEmailAndCompanyNumberDoesNotExistAndInviteeUserIsNotFoundCreatesNewAssociation() throws Exception {
        final var newAssociation = testDataManager.fetchAssociationDaos("36").getFirst();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos("444444").getFirst();

        mockers.mockUsersServiceFetchUserDetails("9999");
        mockers.mockCompanyServiceFetchCompanyProfile("444444");
        mockers.mockUsersServiceSearchUserDetailsEmptyList("light.yagami@death.note");
        Mockito.doReturn(Optional.empty()).when(associationsTransactionService).fetchAssociationDao("444444", null, "light.yagami@death.note");
        Mockito.doReturn(Optional.empty()).when(associationsTransactionService).fetchAssociationDao("444444", "000", null);
        Mockito.doReturn(Stream.of("000")).when(associationsTransactionService).fetchConfirmedUserIds("444444");
        Mockito.when(
                associationsTransactionService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn(newAssociation).when(associationsTransactionService).createAssociationWithInvitationApprovalRoute("444444", null, "light.yagami@death.note", "9999");


        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        Mockito.verify(emailService).sendInviteEmail(eq("theId123"), eq("444444"), any(), eq("Scrooge McDuck"), anyString(), eq("light.yagami@death.note"));
        Mockito.verify(emailService).sendInvitationEmailToAssociatedUser(eq("theId123"), eq("444444"), any(), eq("Scrooge McDuck"), eq("light.yagami@death.note"), any());
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeUserEmailAndCompanyNumberExistAndInviteeUserIsFoundThrowsBadRequest() throws Exception {
        final var associationDao = testDataManager.fetchAssociationDaos("35").getFirst();

        mockers.mockUsersServiceFetchUserDetails("9999");
        mockers.mockCompanyServiceFetchCompanyProfile("333333");
        mockers.mockUsersServiceSearchUserDetails("000");
        Mockito.doReturn(Optional.of(associationDao)).when(associationsTransactionService).fetchAssociationDao("333333", "000", "light.yagami@death.note");
        Mockito.doThrow(new BadRequestRuntimeException("There is an existing association with Confirmed status for the user", new Exception("There is an existing association with Confirmed status for the user"))).when(
                associationsTransactionService).fetchAssociationDao("333333", "000", null);
        Mockito.when(
                associationsTransactionService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenConfirmedAssociationDoesNotExist_thenThrowsBadRequestException() throws Exception {
        mockers.mockUsersServiceFetchUserDetails("9999");
        when(associationsTransactionService.confirmedAssociationExists(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"russell.howard@comedy.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserCanBeAppliedToMigratedAssociationsWithUserId() throws Exception {
        final var targetCompany = testDataManager.fetchCompanyDetailsDtos("MKCOMP001").getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos("MKAssociation001").getFirst();

        mockers.mockUsersServiceFetchUserDetails("MKUser002");
        mockers.mockCompanyServiceFetchCompanyProfile("MKCOMP001");
        mockers.mockUsersServiceSearchUserDetails("MKUser001");
        Mockito.doReturn(true).when(associationsTransactionService).confirmedAssociationExists("MKCOMP001", "MKUser002");
        Mockito.doReturn(Optional.of(targetAssociation)).when(associationsTransactionService).fetchAssociationDao("MKCOMP001", "MKUser001", "mario@mushroom.kingdom");

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "MKUser002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"MKCOMP001\",\"invitee_email_id\":\"mario@mushroom.kingdom\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void inviteUserCanBeAppliedToMigratedAssociationsWithUserEmail() throws Exception {
        final var targetCompany = testDataManager.fetchCompanyDetailsDtos("MKCOMP001").getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos("MKAssociation001").getFirst().userId(null).userEmail("mario@mushroom.kingdom");

        mockers.mockUsersServiceFetchUserDetails("MKUser002");
        mockers.mockCompanyServiceFetchCompanyProfile("MKCOMP001");
        mockers.mockUsersServiceSearchUserDetails("MKUser001");
        Mockito.doReturn(true).when(associationsTransactionService).confirmedAssociationExists("MKCOMP001", "MKUser002");
        Mockito.doReturn(Optional.of(targetAssociation)).when(associationsTransactionService).fetchAssociationDao("MKCOMP001", "MKUser001", "mario@mushroom.kingdom");

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "MKUser002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"MKCOMP001\",\"invitee_email_id\":\"mario@mushroom.kingdom\"}"))
                .andExpect(status().isCreated());
    }

}
