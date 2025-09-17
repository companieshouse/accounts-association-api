package uk.gov.companieshouse.accounts.association.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_ADMIN_READ_PERMISSION;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_IDENTITY;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_IDENTITY_TYPE_API_KEY;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_IDENTITY_TYPE_OAUTH;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.X_REQUEST_ID;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_READ_PERMISSION;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.fetchAllStatusesWithout;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.REMOVED;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.configuration.WebSecurityConfig;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsTransactionService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.Links;

@WebMvcTest(AssociationsListForCompanyController.class)
@ExtendWith(MockitoExtension.class)
@Import(WebSecurityConfig.class)
@Tag("unit-test")
class AssociationsListForCompanyControllerTest {

    @Value("${internal.api.url}")
    private String internalApiUrl;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AssociationsTransactionService associationsTransactionService;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private UsersService usersService;

    @MockitoBean
    private StaticPropertyUtil staticPropertyUtil;

    private static final String DEFAULT_KIND = "association";
    private static final String DEFAULT_DISPLAY_NAME = "Not provided";
    private static final String ASSOCIATIONS_COMPANIES_URL = "/associations/companies/";
    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        ReflectionTestUtils.setField(staticPropertyUtil, "APPLICATION_NAMESPACE", "acsp-manage-users-api");
    }

    @Test
    void getAssociationsForCompanyWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + "$$$$$$")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "111")
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyWithNonexistentCompanyReturnsNotFound() throws Exception {
        final var requestingUserId = "111";
        final var companyNumber = "919191";

        when(companyService.fetchCompanyProfile(companyNumber)).thenThrow(new NotFoundRuntimeException("Not found"));
        Mockito.doReturn(true).when(associationsTransactionService).confirmedAssociationExists(companyNumber, requestingUserId);

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationsForCompanyWithoutQueryParamsUsesDefaults() throws Exception {
        final var requestingUserId = "111";
        final var requestingUser = testDataManager.fetchUserDtos(requestingUserId).getFirst();
        final var requestingUserAssociation = testDataManager.fetchAssociationDto("1", requestingUser);
        final var companyNumber = "111111";
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst();
        final var associationsList = new AssociationsList()
                .totalResults(1).totalPages(1).pageNumber(0).itemsPerPage(15)
                .links(new Links().self(String.format("%s/associations", internalApiUrl)).next(""))
                .items(List.of(requestingUserAssociation));


        when(companyService.fetchCompanyProfile(companyNumber)).thenReturn(companyDetails);
        Mockito.doReturn(true).when(associationsTransactionService).confirmedAssociationExists(companyNumber, "111");
        Mockito.doReturn(associationsList).when(associationsTransactionService).fetchUnexpiredAssociationsForCompanyAndStatuses(any(), eq(fetchAllStatusesWithout(Set.of(StatusEnum.REMOVED))),  isNull(), isNull(), eq(0), eq(15));

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isOk());

        Mockito.verify(associationsTransactionService).fetchUnexpiredAssociationsForCompanyAndStatuses(eq(companyDetails) ,  eq(fetchAllStatusesWithout(Set.of(StatusEnum.REMOVED))),  isNull(), isNull(), eq(0), eq(15));
    }

    @Test
    void getAssociationsForCompanySupportsRequestsFromAdminUsers() throws Exception {
        final var requestingUserId = "111";
        final var marioUser = testDataManager.fetchUserDtos("MKUser001").getFirst();
        final var marioAssociation = testDataManager.fetchAssociationDto("MKAssociation001", marioUser);
        final var associationsList = new AssociationsList()
                .totalResults(1).totalPages(1).pageNumber(0).itemsPerPage(15)
                .links(new Links().self(String.format("%s/associations", internalApiUrl)).next(""))
                .items(List.of(marioAssociation));
        final var companyNumber = "MKCOMP001";
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst();

        when(companyService.fetchCompanyProfile(companyNumber)).thenReturn(companyDetails);
        Mockito.doReturn(true).when(associationsTransactionService).confirmedAssociationExists(companyNumber, requestingUserId);
        Mockito.doReturn(associationsList).when(associationsTransactionService).fetchUnexpiredAssociationsForCompanyAndStatuses(any(), eq(fetchAllStatusesWithout(Set.of(StatusEnum.REMOVED))),  isNull(), isNull(), eq(0), eq(15));

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_ADMIN_READ_PERMISSION.key, ERIC_ADMIN_READ_PERMISSION.value))
                .andExpect(status().isOk());
    }

    @Test
    void getAssociationsForCompanySupportsRequestsFromAPIKey() throws Exception {
        final var requestingUserId = "111";
        final var marioUser = testDataManager.fetchUserDtos("MKUser001").getFirst();
        final var marioAssociation = testDataManager.fetchAssociationDto("MKAssociation001", marioUser);
        final var associationsList = new AssociationsList()
                .totalResults(1).totalPages(1).pageNumber(0).itemsPerPage(15)
                .links(new Links().self(String.format("%s/associations", internalApiUrl)).next(""))
                .items(List.of(marioAssociation));
        final var companyNumber = "MKCOMP001";
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst();

        when(companyService.fetchCompanyProfile(companyNumber)).thenReturn(companyDetails);
        Mockito.doReturn(true).when(associationsTransactionService).confirmedAssociationExists(companyNumber, requestingUserId);
        Mockito.doReturn(associationsList).when(associationsTransactionService).fetchUnexpiredAssociationsForCompanyAndStatuses(any(), eq(fetchAllStatusesWithout(Set.of(StatusEnum.REMOVED))),  isNull(), isNull(), eq(0), eq(15));

        final var response = mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                .andExpect(status().isOk());

        final var associations = parseResponseTo(response, AssociationsList.class);

        final var items =
                associations.getItems()
                        .stream()
                        .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                        .toList();

        Assertions.assertTrue(items.contains("MKAssociation001"));
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedFalseAppliesFilter() throws Exception {
        final var requestingUserId = "111";
        final var batmanUser = testDataManager.fetchUserDtos("111").getFirst();
        final var batmanAssociation = testDataManager.fetchAssociationDto("1", batmanUser);
        final var companyNumber = "111111";
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst();
        final var expectedAssociationsList = new AssociationsList()
                .totalResults(1).totalPages(1).pageNumber(0).itemsPerPage(15)
                .links(new Links().self(String.format("%s/associations", internalApiUrl)).next(""))
                .items(List.of(batmanAssociation));

        when(companyService.fetchCompanyProfile(companyNumber)).thenReturn(companyDetails);
        Mockito.doReturn(true).when(associationsTransactionService).confirmedAssociationExists(companyNumber, requestingUserId);
        Mockito.doReturn(expectedAssociationsList).when(associationsTransactionService).fetchUnexpiredAssociationsForCompanyAndStatuses(any(), eq(fetchAllStatusesWithout(Set.of(StatusEnum.REMOVED))), isNull(), isNull(), eq(0), eq(15));

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber + "?include_removed=false")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isOk());

        Mockito.verify(associationsTransactionService).fetchUnexpiredAssociationsForCompanyAndStatuses(eq(companyDetails) , eq(fetchAllStatusesWithout(Set.of(StatusEnum.REMOVED))),  isNull(), isNull(), eq(0), eq(15));
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedTrueDoesNotApplyFilter() throws Exception {
        final var requestingUserId = "111";
        final var requestingUser = testDataManager.fetchUserDtos(requestingUserId).getFirst();
        final var association = testDataManager.fetchAssociationDto("1", requestingUser);
        final var expectedUser = testDataManager.fetchUserDtos("222").getFirst();
        final var expectedAssociation = testDataManager.fetchAssociationDto("2", expectedUser);
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos("111111").getFirst();
        final var companyNumber = "111111";
        final var expectedAssociationsList = new AssociationsList()
                .totalResults(2).totalPages(1).pageNumber(0).itemsPerPage(15)
                .links(new Links().self(String.format("%s/associations", internalApiUrl)).next(""))
                .items(List.of(association, expectedAssociation));

        when(companyService.fetchCompanyProfile(companyNumber)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());
        Mockito.doReturn(true).when(associationsTransactionService).confirmedAssociationExists(companyNumber, requestingUserId);
        Mockito.doReturn(expectedAssociationsList).when(associationsTransactionService).fetchUnexpiredAssociationsForCompanyAndStatuses(any(), eq(fetchAllStatusesWithout(Set.of())),  isNull(), isNull(), eq(0), eq(15));

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber + "?include_removed=true")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isOk());

        Mockito.verify(associationsTransactionService).fetchUnexpiredAssociationsForCompanyAndStatuses(eq(companyDetails), eq(fetchAllStatusesWithout(Set.of())),  isNull(), isNull(), eq (0), eq(15));
    }

    @Test
    void getAssociationsForCompanyPaginatesCorrectly() throws Exception {
        final var requestingUserId = "111";
        final var companyNumber = "111111";
        final var targetUser = testDataManager.fetchUserDtos("222").getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDto("2", targetUser);
        final var expectedAssociationsList = new AssociationsList()
                .totalResults(2).totalPages(2).pageNumber(1).itemsPerPage(1)
                .links(new Links().self(String.format("%s/associations", internalApiUrl)).next(""))
                .items(List.of(targetAssociation));

        Mockito.doReturn(true).when(associationsTransactionService).confirmedAssociationExists(companyNumber, requestingUserId);
        Mockito.doReturn(expectedAssociationsList).when(associationsTransactionService).fetchUnexpiredAssociationsForCompanyAndStatuses(any(), eq(fetchAllStatusesWithout(Set.of())),  isNull(), isNull(), eq(1), eq(1));

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber + "?include_removed=true&items_per_page=1&page_index=1")
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, requestingUserId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map(Association::getId)
                        .toList();

        Assertions.assertTrue(items.contains("2"));
        Assertions.assertEquals(String.format("%s/associations", internalApiUrl), links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(1, associationsList.getPageNumber());
        Assertions.assertEquals(1, associationsList.getItemsPerPage());
        Assertions.assertEquals(2, associationsList.getTotalResults());
        Assertions.assertEquals(2, associationsList.getTotalPages());
    }

    @Test
    void getAssociationsForCompanyDoesMappingCorrectly() throws Exception {
        final var requestingUserId = "111";
        final var requestingUser = testDataManager.fetchUserDtos("111").getFirst();
        final var requestingAssociation = testDataManager.fetchAssociationDto("1", requestingUser);
        final var targetUser = testDataManager.fetchUserDtos("222").getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDto("2", targetUser);
        final var companyNumber = "111111";
        final var expectedAssociationsList = new AssociationsList()
                .totalResults(2).totalPages(1).pageNumber(0).itemsPerPage(2)
                .links(new Links().self(String.format("%s/associations", internalApiUrl)).next(""))
                .items(List.of(requestingAssociation, targetAssociation));

        Mockito.doReturn(true).when(associationsTransactionService).confirmedAssociationExists(companyNumber, requestingUserId);
        Mockito.doReturn(expectedAssociationsList).when(associationsTransactionService).fetchUnexpiredAssociationsForCompanyAndStatuses(any(), eq(fetchAllStatusesWithout(Set.of())),  isNull(), isNull(), eq(0), eq(2));

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber + "?include_removed=true&items_per_page=2&page_index=0")
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, requestingUserId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                        .andExpect(status().isOk());
        final var associationsList = parseResponseTo(response, AssociationsList.class);

        final var associations = associationsList.getItems();
        final var firstAssociation = associations.getFirst();

        Assertions.assertEquals("a", firstAssociation.getEtag());
        Assertions.assertEquals("1", firstAssociation.getId());
        Assertions.assertEquals("111", firstAssociation.getUserId());
        Assertions.assertEquals("bruce.wayne@gotham.city", firstAssociation.getUserEmail());
        Assertions.assertEquals("Batman", firstAssociation.getDisplayName());
        Assertions.assertEquals("111111", firstAssociation.getCompanyNumber());
        Assertions.assertEquals("Wayne Enterprises", firstAssociation.getCompanyName());
        Assertions.assertEquals(CONFIRMED, firstAssociation.getStatus());
        Assertions.assertNull(firstAssociation.getCreatedAt());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.plusDays(1)), localDateTimeToNormalisedString(firstAssociation.getApprovedAt().toLocalDateTime()));
        Assertions.assertEquals(localDateTimeToNormalisedString(now.plusDays(2)), localDateTimeToNormalisedString(firstAssociation.getRemovedAt().toLocalDateTime()));
        Assertions.assertEquals(DEFAULT_KIND, firstAssociation.getKind());
        Assertions.assertEquals(ApprovalRouteEnum.AUTH_CODE, firstAssociation.getApprovalRoute());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.plusDays(3)), reduceTimestampResolution(firstAssociation.getApprovalExpiryAt()));
        Assertions.assertEquals("/associations/1", firstAssociation.getLinks().getSelf());

        final var secondAssociation = associations.get(1);
        Assertions.assertEquals("222", secondAssociation.getUserId());
        Assertions.assertEquals(DEFAULT_DISPLAY_NAME, secondAssociation.getDisplayName());
        Assertions.assertEquals("Wayne Enterprises", secondAssociation.getCompanyName());
    }

    @Test
    void getAssociationsForCompanyWithUnacceptablePaginationParametersShouldReturnBadRequest() throws Exception {
        final var requestingUser = "111";
        final var companyNumber = "111111";

        Mockito.doReturn(true).when(associationsTransactionService).confirmedAssociationExists(companyNumber, "111");

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber + "?include_removed=true&items_per_page=1&page_index=-1")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUser)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber +"?include_removed=true&items_per_page=0&page_index=0")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUser)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyFetchesAssociation() throws Exception {
        final var requestingUserId = "111";
        final var requestingUser = testDataManager.fetchUserDtos("111").getFirst();
        final var requestingAssociation = testDataManager.fetchAssociationDto("1", requestingUser);
        final var companyNumber = "111111";
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst();
        final var expectedAssociationsList = new AssociationsList().totalResults(1).items(List.of(requestingAssociation));

        when(companyService.fetchCompanyProfile(companyNumber)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());
        Mockito.doReturn(true).when(associationsTransactionService).confirmedAssociationExists(companyNumber, requestingUserId);
        Mockito.doReturn(expectedAssociationsList).when(associationsTransactionService).fetchUnexpiredAssociationsForCompanyAndStatuses(eq(companyDetails), eq(fetchAllStatusesWithout(Set.of(StatusEnum.REMOVED))),  isNull(), isNull(), eq(0), eq(15));

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber + "?user_email=bruce.wayne@gotham.city")
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, requestingUserId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);

        Assertions.assertEquals(1, associationsList.getTotalResults());
        Assertions.assertEquals("1", associationsList.getItems().getFirst().getId());
    }

    @Test
    void getAssociationsForCompanyWithNonexistentUserEmailFetchesEmptyList() throws Exception {
        final var requestingUserId = "111";
        final var companyNumber = "111111";
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst();
        final var expectedAssociationsList = new AssociationsList().totalResults(0).items(List.of());

        when(companyService.fetchCompanyProfile(companyNumber)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());
        Mockito.doReturn(true).when(associationsTransactionService).confirmedAssociationExists(companyNumber, requestingUserId);
        Mockito.doReturn(expectedAssociationsList).when(associationsTransactionService).fetchUnexpiredAssociationsForCompanyAndStatuses(eq(companyDetails), eq(fetchAllStatusesWithout (Set.of(StatusEnum.REMOVED))),  isNull(), isNull(), eq(0) , eq(15));

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber + "?user_email=the.void@space.com")
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, requestingUserId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);

        Assertions.assertEquals(0, associationsList.getTotalResults());
    }

    @Test
    void getAssociationsForCompanyCanRetrieveMigratedAssociations() throws Exception {
        final var requestingUserId = "MKUser002";
        final var marioUser = testDataManager.fetchUserDtos("MKUser001").getFirst();
        final var luigiUser = testDataManager.fetchUserDtos("MKUser002").getFirst();
        final var peachUser = testDataManager.fetchUserDtos("MKUser003").getFirst();
        final var marioAssociation = testDataManager.fetchAssociationDto("MKAssociation001", marioUser);
        final var luigiAssociation = testDataManager.fetchAssociationDto("MKAssociation002", luigiUser);
        final var peachAssociation = testDataManager.fetchAssociationDto("MKAssociation003", peachUser);
        final var companyNumber = "MKCOMP001";
        final var mushroomKingdomCompany = testDataManager.fetchCompanyDetailsDtos("MKCOMP001").getFirst();
        final var expectedAssociationsList = new AssociationsList().totalResults(3).items(List.of(marioAssociation, luigiAssociation, peachAssociation));

        when(companyService.fetchCompanyProfile(companyNumber)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());
        Mockito.doReturn(true).when(associationsTransactionService).confirmedAssociationExists(companyNumber, requestingUserId);
        Mockito.doReturn(expectedAssociationsList).when(associationsTransactionService).fetchUnexpiredAssociationsForCompanyAndStatuses(eq(mushroomKingdomCompany), eq(fetchAllStatusesWithout(Set.of())),  isNull(), isNull(), eq(0), eq(15));

        final var response = mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber + "?include_removed=true")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);
        final var associations = associationsList.getItems();

        Assertions.assertEquals(3, associationsList.getTotalResults());

        for (final Association association: associations){
            final var expectedStatus = switch (association.getId()){
                case "MKAssociation001" -> "migrated";
                case "MKAssociation002" -> "confirmed";
                case "MKAssociation003" -> "removed";
                default -> "unknown";
            };

            final var expectedApprovalRoute = switch (association.getId()){
                case "MKAssociation001", "MKAssociation003" -> "migration";
                case "MKAssociation002" -> "auth_code";
                default -> "unknown";
            };

            Assertions.assertEquals(expectedStatus, association.getStatus().getValue());
            Assertions.assertEquals(expectedApprovalRoute, association.getApprovalRoute().getValue());
        }
    }

    @Test
    void getAssociationsForCompanyReturnsForbiddenWhenCalledByAUserThatIsNotAMemberOfCompanyOrAdmin() throws Exception {
        final var requestingUserId = "MKUser001";
        final var companyNumber = "111111";

        when(usersService.fetchUserDetails(requestingUserId, "theId123")).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(associationsTransactionService.confirmedAssociationExists("111111", "MKUser001")).thenReturn(false);
        when(companyService.fetchCompanyProfile(companyNumber)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isForbidden());
    }


    @Test
    void getAssociationsForCompanyUserAndStatusWithUserIdFetchesAssociations() throws Exception {
        final var user = testDataManager.fetchUserDtos("MKUser002").getFirst();
        final var companyNumber = "MKCOMP001";
        final var company = testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst();

        when(companyService.fetchCompanyProfile(companyNumber)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());
        Mockito.doReturn(testDataManager.fetchUserDtos("MKUser002").getFirst()).when(usersService).retrieveUserDetails("theId123", "MKUser002", null);
        Mockito.doReturn(Optional.of(testDataManager.fetchAssociationDto("MKAssociation002" , user))).when(
                associationsTransactionService).fetchUnexpiredAssociationsForCompanyUserAndStatuses(company, Set.of(CONFIRMED, REMOVED), user, "luigi@mushroom.kingdom");

        final var response = mockMvc.perform(post(ASSOCIATIONS_COMPANIES_URL + companyNumber + "/search")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "test")
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} "))
                .andExpect(status().isOk());

        final var association = parseResponseTo(response, Association.class);
        Assertions.assertEquals("MKAssociation002", association.getId());
    }

    static Stream<Arguments> getAssociationsForCompanyUserAndStatusHappyCaseScenarios() {
        return Stream.of(
                Arguments.of(", \"status\":[\"confirmed\", \"removed\"]"),
                Arguments.of(", \"status\":[]"),
                Arguments.of("")

      );
    }

    @ParameterizedTest
    @MethodSource("getAssociationsForCompanyUserAndStatusHappyCaseScenarios")
    void getAssociationsForCompanyUserAndStatusWithUserEmailFetchesAssociations(final String status) throws Exception {
        final var user = testDataManager.fetchUserDtos("MKUser002").getFirst();
        final var companyNumber = "MKCOMP001";
        final var company = testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst();

        when(companyService.fetchCompanyProfile(companyNumber)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());
        Mockito.doReturn(testDataManager.fetchUserDtos("MKUser002").getFirst()).when(usersService).retrieveUserDetails("theId123", null, "luigi@mushroom.kingdom");
        Mockito.doReturn(Optional.of(testDataManager.fetchAssociationDto("MKAssociation002" , user))).when(
                associationsTransactionService).fetchUnexpiredAssociationsForCompanyUserAndStatuses(eq(company), any(), eq(user), eq("luigi@mushroom.kingdom"));

        final var response = mockMvc.perform(post(ASSOCIATIONS_COMPANIES_URL + companyNumber + "/search")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "test")
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"user_email\":\"luigi@mushroom.kingdom\" %s} ", status)))
                .andExpect(status().isOk());

        final var association = parseResponseTo(response, Association.class);
        Assertions.assertEquals("MKAssociation002", association.getId());
    }

    @Test
    void getAssociationsForCompanyUserWithNonExistentUserEmailFetchesAssociations() throws Exception {
        final var user = testDataManager.fetchUserDtos("MKUser001").getFirst();
        final var companyNumber = "MKCOMP001";
        final var company = testDataManager.fetchCompanyDetailsDtos("MKCOMP001").getFirst();

        when(companyService.fetchCompanyProfile(companyNumber)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());
        Mockito.doReturn(null).when(usersService).retrieveUserDetails("theId123", null, "mario@mushroom.kingdom");
        Mockito.doReturn(Optional.of(testDataManager.fetchAssociationDto("MKAssociation001" , user))).when(
                associationsTransactionService).fetchUnexpiredAssociationsForCompanyUserAndStatuses(company, Set.of(MIGRATED), null , "mario@mushroom.kingdom");

        final var response = mockMvc.perform(post(ASSOCIATIONS_COMPANIES_URL + companyNumber + "/search")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "test")
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_email\":\"mario@mushroom.kingdom\" , \"status\":[\"migrated\"] }"))
                .andExpect(status().isOk());

        final var association = parseResponseTo(response, Association.class);
        Assertions.assertEquals("MKAssociation001", association.getId());
    }

    static Stream<Arguments> getAssociationsForCompanyUserMalformedScenarios() {
        return Stream.of(
                Arguments.of("$$$$$" ,"MKCOMP001", ", \"status\":[\"confirmed\", \"removed\"]"),
                Arguments.of("MKUser002" ,"$$$$$", ", \"status\":[\"confirmed\", \"removed\"]"),
                Arguments.of("MKUser002" ,"MKCOMP001", ", \"status\":[\"$$$$$$\", \"removed\"]")

      );
    }

    @ParameterizedTest
    @MethodSource("getAssociationsForCompanyUserMalformedScenarios")
    void getAssociationsForCompanyUserAndStatusMalformedReturnsBadRequests(final String userId, final String companyNumber, final String status) throws Exception {
        mockMvc.perform(post(String.format(ASSOCIATIONS_COMPANIES_URL + "%s/search", companyNumber))
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "test")
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"user_id\":\"%s\" %s }", userId , status)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyUserAndStatusWithMalformedEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ASSOCIATIONS_COMPANIES_URL + "MKCOMP001/search")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "test")
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_email\":\"$\\() {}$$$$@mushroomkingdom\" "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyUserAndStatusWithoutUserReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ASSOCIATIONS_COMPANIES_URL + "MKCOMP001/search")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "test")
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\":[\"confirmed\", \"removed\"]} "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyUserAndStatusWithMalformedUserEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ASSOCIATIONS_COMPANIES_URL + "MKCOMP001/search")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "test")
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"user_email\":\"$$$$$@mushroom.kingdom\" ,\"status\":[\"confirmed\", \"removed\"]} "))
                .andExpect(status().isBadRequest());
    }

    static Stream<Arguments> getAssociationsForCompanyMalformedBodyScenarios() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("{ \"user_email\":\"111@mushroom.kingdom\", \"user_id\":\"111\" }")

      );
    }

    @ParameterizedTest
    @MethodSource("getAssociationsForCompanyMalformedBodyScenarios")
    void getAssociationsForCompanyMalformedBodyScenarios(final String body) throws Exception{
        mockMvc.perform(post(ASSOCIATIONS_COMPANIES_URL + "MKCOMP001/search")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "test")
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyWithNonExistentCompanyNumberReturnsNotFound() throws Exception {
        final var user = testDataManager.fetchUserDtos("MKUser002").getFirst();
        final var companyNumber = "MKCOMP001";
        final var company = testDataManager.fetchCompanyDetailsDtos("MKCOMP001").getFirst();
        when(companyService.fetchCompanyProfile(companyNumber)).thenThrow(new NotFoundRuntimeException("Company not found"));
        Mockito.doReturn(testDataManager.fetchUserDtos("MKUser002").getFirst()).when(usersService).retrieveUserDetails("theId123", "MKUser002", null);
        Mockito.doReturn(Optional.of(testDataManager.fetchAssociationDto("MKAssociation002" , user))).when(
                associationsTransactionService).fetchUnexpiredAssociationsForCompanyUserAndStatuses(company, Set.of(CONFIRMED, REMOVED), user, "luigi@mushroom.kingdom");

        mockMvc.perform(post(ASSOCIATIONS_COMPANIES_URL + companyNumber + "/search")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "test")
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} "))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationsForCompanyWithNonExistentUserIdReturnsNotFound() throws Exception {
        final var user = testDataManager.fetchUserDtos("MKUser002").getFirst();
        final var companyNumber = "MKCOMP001";
        final var company = testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst();
        when(companyService.fetchCompanyProfile(companyNumber)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());
        Mockito.doThrow(new NotFoundRuntimeException("Test")).when(usersService).retrieveUserDetails("theId123", "MKUser002", null);
        Mockito.doReturn(Optional.of(testDataManager.fetchAssociationDto("MKAssociation002" , user))).when(
                associationsTransactionService).fetchUnexpiredAssociationsForCompanyUserAndStatuses(company, Set.of(CONFIRMED, REMOVED), user, "luigi@mushroom.kingdom");

        mockMvc.perform(post(ASSOCIATIONS_COMPANIES_URL + companyNumber + "/search")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "test")
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} "))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationsForCompanyNonExistentAssociationsReturnsNotFound() throws Exception {
        final var user = testDataManager.fetchUserDtos("MKUser002").getFirst();
        final var company = testDataManager.fetchCompanyDetailsDtos("MKCOMP001").getFirst();
        final var companyNumber = "MKCOMP001";

        when(companyService.fetchCompanyProfile(companyNumber)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());
        Mockito.doReturn(testDataManager.fetchUserDtos("MKUser002").getFirst()).when(usersService).retrieveUserDetails("theId123", "MKUser002", null);
        Mockito.doReturn(Optional.empty()).when(associationsTransactionService).fetchUnexpiredAssociationsForCompanyUserAndStatuses(company, Set.of(CONFIRMED, REMOVED), user, "luigi@mushroom.kingdom");

        mockMvc.perform(post(ASSOCIATIONS_COMPANIES_URL + companyNumber + "/search")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "test")
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} "))
                .andExpect(status().isNotFound());
    }
}