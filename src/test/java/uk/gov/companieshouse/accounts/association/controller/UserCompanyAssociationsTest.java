package uk.gov.companieshouse.accounts.association.controller;

import java.util.Map;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.configuration.WebSecurityConfig;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.*;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.REMOVED;

@WebMvcTest(UserCompanyAssociations.class)
@Import(WebSecurityConfig.class)
@Tag("unit-test")
class UserCompanyAssociationsTest {

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

    private static final String DEFAULT_KIND = "association";

    private final LocalDateTime now = LocalDateTime.now();

    final Function<String, Mono<Void>> sendEmailMock = userId -> Mono.empty();

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static Mockers mockers;

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    @BeforeEach
    public void setup() {
        mockers = new Mockers( null, null, companyService, usersService );
        mockMvc = MockMvcBuilders.webAppContextSetup( context )
                .apply( SecurityMockMvcConfigurers.springSecurity() )
                .build();
    }

    @Test
    void fetchAssociationsByTestShouldThrow403ErrorRequestWhenEricIdNotProvided() throws Exception {
        mockMvc.perform(get("/associations")
                .header("X-Request-Id", "theId123"))
                .andExpect( status().isForbidden() );
    }

    @Test
    void fetchAssociationsByTestShouldThrow403ErrorRequestWhenPatchApplied() throws Exception {
        mockMvc.perform(patch("/associations")
                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "000")
                                .header("ERIC-Identity-Type", "oauth2"))
                .andExpect( status().isForbidden() );
    }

    @Test
    void fetchAssociationsByTestShouldThrow400ErrorWhenRequestIdNotProvided() throws Exception {
        mockMvc.perform(get("/associations")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fetchAssociationsByTestShouldReturnEmptyDataWhenNoAssociationsFoundForEricIdentity() throws Exception {
        final var user = testDataManager.fetchUserDtos( "000" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "000" );
        Mockito.doReturn( new AssociationsList().items( List.of()) ).when( associationsService ).fetchAssociationsForUserStatusAndCompany( eq( user ), eq( List.of( StatusEnum.CONFIRMED.getValue() ) ), eq( 0 ), eq( 15 ), isNull() );

        final var response = mockMvc.perform(get("/associations")
                .header("Eric-identity", "000")
                .header("X-Request-Id", "theId123")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk());
        final var associationsList = parseResponseTo( response, AssociationsList.class );

        assertEquals(0, associationsList.getItems().size() );
    }

    @Test
    void fetchAssociationsByTestShouldReturnDataWhenAssociationsFoundForEricIdentity() throws Exception {
        final var user = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var associationsList = new AssociationsList().itemsPerPage(15).pageNumber(0).totalPages(1).totalResults(1).items(List.of());

        mockers.mockUsersServiceFetchUserDetails( "111" );
        when(associationsService.fetchAssociationsForUserStatusAndCompany(user, List.of( StatusEnum.CONFIRMED.getValue() ), 0, 15, "111111")).thenReturn(associationsList);

        final var response = mockMvc.perform(get("/associations?page_index=0&items_per_page=15&company_number=111111")
                .header("Eric-identity", "111")
                .header("X-Request-Id", "theId")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*"));
        final var result = parseResponseTo( response, AssociationsList.class );

        assertEquals( 15, result.getItemsPerPage() );
        assertEquals( 1, result.getTotalResults() );
    }

    @Test
    void fetchAssociationsByTestShouldThrowErrorWhenCompanyNumberIsOfWrongFormat() throws Exception {
        mockMvc.perform(get("/associations?page_index=0&items_per_page=15&company_number=$$$")
                .header("Eric-identity", "000")
                .header("X-Request-Id", "theId")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fetchAssociationsByTestShouldThrow500WhenInternalServerError() throws Exception {
        when(usersService.fetchUserDetails("000")).thenThrow(new InternalServerErrorRuntimeException("test"));
        mockMvc.perform(get("/associations?page_index=0&items_per_page=15&company_number=111111")
                .header("Eric-identity", "000")
                .header("X-Request-Id", "theId")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void fetchAssociationsByWithInvalidPageIndexReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations?page_index=-1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fetchAssociationsByWithInvalidItemsPerPageReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations?items_per_page=0")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
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
    void fetchAssociationsByWithNonexistentCompanyReturnsNotFound() throws Exception {
        final var user = testDataManager.fetchUserDtos( "111" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "111" );
        Mockito.doThrow(new NotFoundRuntimeException("accounts-association-api", "Not found")).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of(StatusEnum.CONFIRMED.getValue()), 0, 15, null);

        mockMvc.perform(get("/associations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isNotFound());
    }

    @Test
    void fetchAssociationsByWithInvalidStatusReturnsZeroResults() throws Exception {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        Mockito.doReturn(new AssociationsList().totalResults(0).items(List.of())).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of("$$$$"), 0, 15, null);

        final var response = mockMvc.perform(get("/associations?status=$$$$")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk());
        final var associationsList = parseResponseTo( response, AssociationsList.class );

        Assertions.assertEquals(0, associationsList.getTotalResults());
    }

    @Test
    void fetchAssociationsByUsesDefaultsIfValuesAreNotProvided() throws Exception {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var expectedAssociationsList = new AssociationsList()
                .items( List.of( testDataManager.fetchAssociationDto( "18", user ) ) )
                .links( new Links().self( "/associations?page_index=0&items_per_page=15" ).next( "" ) )
                .itemsPerPage(15).pageNumber(0).totalPages(1).totalResults(1);

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        Mockito.doReturn( expectedAssociationsList ).when( associationsService ).fetchAssociationsForUserStatusAndCompany(user, List.of(StatusEnum.CONFIRMED.getValue()), 0, 15, null);

        final var response = mockMvc.perform(get("/associations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk());
        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();
        final var item = associationsList.getItems().getFirst().getId();

        Assertions.assertEquals( "18", item );
        Assertions.assertEquals("/associations?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(15, associationsList.getItemsPerPage());
        Assertions.assertEquals(1, associationsList.getTotalResults());
        Assertions.assertEquals(1, associationsList.getTotalPages());
    }

    @Test
    void fetchAssociationsByWithOneStatusAppliesStatusFilterCorrectly() throws Exception {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var expectedAssociationsList = new AssociationsList()
                .items(List.of(testDataManager.fetchAssociationDto( "19", user )))
                .links(new Links().self("/associations?page_index=0&items_per_page=15").next(""))
                .itemsPerPage(15).pageNumber(0).totalPages(1).totalResults(1);

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of(StatusEnum.REMOVED.getValue()), 0, 15, null);

        final var response = mockMvc.perform(get("/associations?status=removed")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk());
        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var item = associationsList.getItems().getFirst().getId();
        final var links = associationsList.getLinks();

        Assertions.assertEquals( "19", item );
        Assertions.assertEquals("/associations?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(15, associationsList.getItemsPerPage());
        Assertions.assertEquals(1, associationsList.getTotalResults());
        Assertions.assertEquals(1, associationsList.getTotalPages());
    }

    @Test
    void fetchAssociationsByWithMultipleStatusesAppliesStatusFilterCorrectly() throws Exception {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var expectedAssociationsList = new AssociationsList()
                .items(List.of(testDataManager.fetchAssociationDto( "18", user ), testDataManager.fetchAssociationDto( "19", user )))
                .links(new Links().self("/associations?page_index=0&items_per_page=15").next(""))
                .itemsPerPage(15).pageNumber(0).totalPages(1).totalResults(2);

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of(StatusEnum.CONFIRMED.getValue(), StatusEnum.REMOVED.getValue()), 0, 15, null);

        final var response = mockMvc.perform(get("/associations?status=confirmed&status=removed")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk());
        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var items = associationsList.getItems().stream().map(Association::getId).toList();
        final var links = associationsList.getLinks();

        Assertions.assertTrue(items.containsAll(List.of("18", "19")));
        Assertions.assertEquals("/associations?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(15, associationsList.getItemsPerPage());
        Assertions.assertEquals(2, associationsList.getTotalResults());
        Assertions.assertEquals(1, associationsList.getTotalPages());
    }

    @Test
    void fetchAssociationsByImplementsPaginationCorrectly() throws Exception {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var expectedAssociationsList = new AssociationsList()
                .items(List.of(testDataManager.fetchAssociationDto( "19", user )))
                .links(new Links().self("/associations?page_index=1&items_per_page=1").next(""))
                .itemsPerPage(1).pageNumber(1).totalPages(2).totalResults(2);

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of(StatusEnum.CONFIRMED.getValue(), StatusEnum.REMOVED.getValue()), 1, 1, null);

        final var response = mockMvc.perform(get("/associations?status=confirmed&status=removed&page_index=1&items_per_page=1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk());
        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var item = associationsList.getItems().getFirst().getId();
        final var links = associationsList.getLinks();

        Assertions.assertEquals( "19", item );
        Assertions.assertEquals("/associations?page_index=1&items_per_page=1", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(1, associationsList.getPageNumber());
        Assertions.assertEquals(1, associationsList.getItemsPerPage());
        Assertions.assertEquals(2, associationsList.getTotalResults());
        Assertions.assertEquals(2, associationsList.getTotalPages());
    }

    @Test
    void fetchAssociationsByFiltersBasedOnCompanyNumberCorrectly() throws Exception {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var expectedAssociationsList = new AssociationsList()
                .items(List.of(testDataManager.fetchAssociationDto( "19", user )))
                .links(new Links().self("/associations?page_index=0&items_per_page=15").next(""))
                .itemsPerPage(15).pageNumber(0).totalPages(1).totalResults(1);

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of(StatusEnum.CONFIRMED.getValue()), 0, 15, "444444");

        final var response = mockMvc.perform(get("/associations?company_number=444444")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk());
        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var item = associationsList.getItems().getFirst().getId();
        final var links = associationsList.getLinks();

        Assertions.assertEquals( "19", item );
        Assertions.assertEquals("/associations?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(15, associationsList.getItemsPerPage());
        Assertions.assertEquals(1, associationsList.getTotalResults());
        Assertions.assertEquals(1, associationsList.getTotalPages());
    }

    @Test
    void fetchAssociationsByDoesMappingCorrectly() throws Exception {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var expectedAssociationsList = new AssociationsList()
                .items(List.of(testDataManager.fetchAssociationDto( "18", user )))
                .links(new Links().self("/associations?page_index=0&items_per_page=15").next(""))
                .itemsPerPage(15).pageNumber(0).totalPages(1).totalResults(1);

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of(StatusEnum.CONFIRMED.getValue()), 0, 15, null);

        final var response = mockMvc.perform(get("/associations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk());
        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var firstAssociation = associationsList.getItems().getFirst();

        Assertions.assertEquals("aa", firstAssociation.getEtag());
        Assertions.assertEquals("18", firstAssociation.getId());
        Assertions.assertEquals("9999", firstAssociation.getUserId());
        Assertions.assertEquals("scrooge.mcduck@disney.land", firstAssociation.getUserEmail());
        Assertions.assertEquals("Scrooge McDuck", firstAssociation.getDisplayName());
        Assertions.assertEquals("333333", firstAssociation.getCompanyNumber());
        Assertions.assertEquals("Tesco", firstAssociation.getCompanyName());
        Assertions.assertEquals(StatusEnum.CONFIRMED, firstAssociation.getStatus());
        Assertions.assertNull(firstAssociation.getCreatedAt());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.plusDays(1)), localDateTimeToNormalisedString(firstAssociation.getApprovedAt().toLocalDateTime()));
        Assertions.assertEquals(localDateTimeToNormalisedString(now.plusDays(2)), localDateTimeToNormalisedString(firstAssociation.getRemovedAt().toLocalDateTime()));
        Assertions.assertEquals(DEFAULT_KIND, firstAssociation.getKind());
        Assertions.assertEquals(ApprovalRouteEnum.AUTH_CODE, firstAssociation.getApprovalRoute());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.plusDays(3)), reduceTimestampResolution(firstAssociation.getApprovalExpiryAt()));
        Assertions.assertEquals("/associations/18", firstAssociation.getLinks().getSelf());
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
    void getAssociationsDetailsWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations/1")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
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
    void addAssociationWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/associations")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"111111\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addAssociationWithoutRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/associations")
                        .header("Eric-identity", "000")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addAssociationWithoutCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/associations")
                        .header("Eric-identity", "000")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addAssociationWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/associations")
                        .header("Eric-identity", "000")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"$$$$$$\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addAssociationWithExistingAssociationReturnsBadRequest() throws Exception {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var association = testDataManager.fetchAssociationDaos( "2" ).getFirst();
        final var page = new PageImpl<>(List.of(association), PageRequest.of(1,15),15);

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        Mockito.doReturn(page).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(user, List.of(AWAITING_APPROVAL.getValue(), CONFIRMED.getValue(), REMOVED.getValue()),0,15,"111111");

        mockMvc.perform(post("/associations")
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"111111\"}"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void addAssociationWithNonexistentCompanyNumberReturnsNotFound() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "000" );
        mockers.mockCompanyServiceFetchCompanyProfileNotFound("919191");

        mockMvc.perform(post("/associations")
                        .header("Eric-identity", "000")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"919191\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addAssociationCreatesNewAssociationCorrectlyAndReturnsAssociationIdWithCreatedHttpStatus() throws Exception {
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        final var company = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "111" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(associationDao).when(associationsService).createAssociation("111111", "111", null, ApprovalRouteEnum.AUTH_CODE, null);
        Mockito.doReturn(new PageImpl<AssociationDao>(new ArrayList<>())).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(any(),any(), any(),any(),anyString());
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendAuthCodeConfirmationEmailToAssociatedUser( eq( "theId123" ), eq( company ), eq( "Batman" ) );

        final var response = mockMvc.perform(post("/associations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"111111\"}"))
                .andExpect(status().isCreated());
        final var responseBodyPost = parseResponseTo( response, ResponseBodyPost.class );

        Assertions.assertEquals("1", responseBodyPost.getAssociationId());
    }

    @Test
    void addAssociationWithNonExistentUserReturnsForbidden() throws Exception {
        mockers.mockUsersServiceFetchUserDetailsNotFound( "9191" );

        mockMvc.perform(post("/associations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"111111\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addAssociationWithUserThatHasNoDisplayNameSetsDisplayNameToEmailAddress() throws Exception {
        final var associationDao = testDataManager.fetchAssociationDaos( "6" ).getFirst();
        final var company = testDataManager.fetchCompanyDetailsDtos( "333333" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "666" );
        mockers.mockCompanyServiceFetchCompanyProfile("333333" );
        Mockito.doReturn(associationDao).when(associationsService).createAssociation("333333", "666", null, ApprovalRouteEnum.AUTH_CODE, null);
        Mockito.doReturn(new PageImpl<AssociationDao>(new ArrayList<>())).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(any(),any(), any(),any(),anyString());
        Mockito.doReturn( List.of( "666" ) ).when( associationsService ).fetchAssociatedUsers( "333333" );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendAuthCodeConfirmationEmailToAssociatedUser( eq( "theId123" ), eq( company ), eq( "homer.simpson@springfield.com" ) );

        mockMvc.perform(post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "666")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\"}" ) )
                .andExpect( status().isCreated() );

        Mockito.verify( emailService ).sendAuthCodeConfirmationEmailToAssociatedUser( eq( "theId123" ), argThat( comparisonUtils.compare( company, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq( "homer.simpson@springfield.com" ) );
    }

    @Test
    void existingAssociationWithStatusAwaitingApprovalWhenPostedShouldUpdateAssociationWithStatusConfirmed() throws Exception {
        final var associationDao = testDataManager.fetchAssociationDaos( "6" ).getFirst();
        final var user = testDataManager.fetchUserDtos( "666" ).getFirst();
        final var company = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var page = new PageImpl<>(List.of(associationDao), PageRequest.of(1,15),15);

        mockers.mockUsersServiceFetchUserDetails( "666" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(page).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(user, List.of(AWAITING_APPROVAL.getValue(), CONFIRMED.getValue(), REMOVED.getValue()),0,15,"111111");
        Mockito.doReturn(associationDao).when(associationsService).upsertAssociation(any(AssociationDao.class));
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendAuthCodeConfirmationEmailToAssociatedUser( eq( "theId123" ), eq( company ), eq( "homer.simpson@springfield.com" ) );

        mockMvc.perform(post("/associations")
                        .header("Eric-identity", "666")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"111111\"}"))
                .andExpect(status().isCreated());

        Mockito.verify( emailService ).sendAuthCodeConfirmationEmailToAssociatedUser( eq( "theId123" ), argThat( comparisonUtils.compare( company, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq( "homer.simpson@springfield.com" ) );
    }

    @Test
    void existingAssociationWithStatusRemovedWhenPostedShouldUpdateAssociationWithStatusConfirmed() throws Exception {
        final var associationDao = testDataManager.fetchAssociationDaos( "14" ).getFirst();
        final var user = testDataManager.fetchUserDtos( "5555" ).getFirst();
        final var company = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var page = new PageImpl<>(List.of(associationDao), PageRequest.of(1,15),15);

        mockers.mockUsersServiceFetchUserDetails( "5555" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(page).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(user, List.of(AWAITING_APPROVAL.getValue(), CONFIRMED.getValue(), REMOVED.getValue()),0,15,"111111");
        Mockito.doReturn(associationDao).when(associationsService).upsertAssociation(any(AssociationDao.class));
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendAuthCodeConfirmationEmailToAssociatedUser( eq( "theId123" ), eq( company ), eq( "ross@friends.com" ) );

        mockMvc.perform(post("/associations")
                        .header("Eric-identity", "5555")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"111111\"}"))
                .andExpect(status().isCreated());

        Mockito.verify( emailService ).sendAuthCodeConfirmationEmailToAssociatedUser( eq( "theId123" ), argThat( comparisonUtils.compare( company, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq( "ross@friends.com" ) );
    }

    @Test
    void addAssociationWithUserThatHasDisplayNameUsesDisplayName() throws Exception {
        final var associationDao = testDataManager.fetchAssociationDaos( "18" ).getFirst();
        final var company = testDataManager.fetchCompanyDetailsDtos( "333333" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );
        Mockito.doReturn(new PageImpl<AssociationDao>(new ArrayList<>())).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(any(),any(), any(),any(),anyString());
        Mockito.doReturn( List.of( "000" ) ).when( associationsService ).fetchAssociatedUsers( "333333" );
        Mockito.doReturn(associationDao).when(associationsService).createAssociation("333333", "9999", null, ApprovalRouteEnum.AUTH_CODE, null);
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendAuthCodeConfirmationEmailToAssociatedUser( eq( "theId123" ), eq( company ), eq( "Scrooge McDuck" ) );

        mockMvc.perform(post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\"}" ) )
                .andExpect( status().isCreated() );

        Mockito.verify( emailService ).sendAuthCodeConfirmationEmailToAssociatedUser( eq( "theId123" ), argThat( comparisonUtils.compare( company, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq( "Scrooge McDuck" ) );
    }

    @Test
    void updateAssociationStatusForIdWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/associations/18")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"removed\"}"))
                .andExpect(status().isBadRequest());
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

    @Test
    void inviteUserWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/associations/invitations")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithMalformedEricIdentityReturnsBadRequest() throws Exception {
        mockers.mockUsersServiceFetchUserDetailsNotFound( "$$$$" );

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
        mockers.mockUsersServiceFetchUserDetailsNotFound( "9191" );

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
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfileNotFound( "919191" );

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
        final var associationDao = testDataManager.fetchAssociationDaos( "36" ).getFirst();
        final var associationDaoForComparison = testDataManager.fetchAssociationDaos( "36" ).getFirst();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "444444" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "444444" );
        mockers.mockUsersServiceSearchUserDetails( "000" );
        Mockito.doReturn(Optional.of(associationDao)).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("444444", "light.yagami@death.note");
        Mockito.doReturn(associationDao).when(associationsService).sendNewInvitation(eq("9999"), any());
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn( Mono.empty() ).when( emailService ).sendInviteEmail( eq( "theId123" ), eq( companyDetails ), eq( "Scrooge McDuck" ), any( String.class ), eq( "light.yagami@death.note" ) );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendInvitationEmailToAssociatedUser( eq( "theId123" ), eq( companyDetails ), eq( "Scrooge McDuck" ), eq( "light.yagami@death.note" ) );

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        Mockito.verify(associationsService).sendNewInvitation(eq("9999"), argThat( comparisonUtils.compare( associationDaoForComparison, List.of( "id", "companyNumber", "createdAt", "approvedAt", "removedAt", "approvalRoute", "approvalExpiryAt", "invitations", "etag", "version" ), List.of( "userId", "userEmail" ), Map.of() ) ));
        Mockito.verify( emailService ).sendInviteEmail( eq( "theId123" ), argThat( comparisonUtils.compare( companyDetails, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq("Scrooge McDuck"), anyString(), eq( "light.yagami@death.note" ) );
        Mockito.verify( emailService ).sendInvitationEmailToAssociatedUser( eq( "theId123" ), argThat( comparisonUtils.compare( companyDetails, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq("Scrooge McDuck"), eq("light.yagami@death.note") );
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeEmailAndCompanyNumberExistsAndInviteeUserIsNotFoundDoesNotPerformSwapButDoesPerformUpdateOperation() throws Exception {
        final var associationDao = testDataManager.fetchAssociationDaos( "36" ).getFirst();
        final var associationDaoForComparison = testDataManager.fetchAssociationDaos( "36" ).getFirst();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "444444" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "444444" );
        mockers.mockUsersServiceSearchUserDetailsEmptyList( "000" );
        Mockito.doReturn(Optional.of(associationDao)).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("444444", "light.yagami@death.note");
        Mockito.doReturn(associationDao).when(associationsService).sendNewInvitation(eq("9999"), any());
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn( Mono.empty() ).when( emailService ).sendInviteEmail( eq( "theId123" ), eq( companyDetails ), eq( "Scrooge McDuck" ), any( String.class ), eq( "light.yagami@death.note" ) );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendInvitationEmailToAssociatedUser( eq( "theId123" ), eq( companyDetails ), eq( "Scrooge McDuck" ), eq( "light.yagami@death.note" ) );

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        Mockito.verify(associationsService).sendNewInvitation(eq("9999"), argThat(comparisonUtils.compare( associationDaoForComparison, List.of( "id", "companyNumber", "createdAt", "approvedAt", "removedAt", "approvalRoute", "approvalExpiryAt", "invitations", "etag", "version", "userId", "userEmail" ), List.of(), Map.of() )));
        Mockito.verify( emailService ).sendInviteEmail( eq( "theId123" ), argThat( comparisonUtils.compare( companyDetails, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq("Scrooge McDuck"), anyString(), eq( "light.yagami@death.note" ) );
        Mockito.verify( emailService ).sendInvitationEmailToAssociatedUser(  eq("theId123"), argThat( comparisonUtils.compare( companyDetails, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq("Scrooge McDuck"), eq("light.yagami@death.note") );
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberExistsDoesNotPerformSwapButDoesPerformUpdateOperation() throws Exception {
        final var requestingUserAssociation = testDataManager.fetchAssociationDaos( "19" ).getFirst();
        final var targetUserAssociation = testDataManager.fetchAssociationDaos( "36" ).getFirst();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "444444" ).getFirst();

        mockers.mockUsersServiceSearchUserDetails( "000" );
        mockers.mockCompanyServiceFetchCompanyProfile( "444444" );
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("444444", "light.yagami@death.note");
        Mockito.doReturn(Optional.of(targetUserAssociation)).when(associationsService).fetchAssociationForCompanyNumberAndUserId("444444", "000");
        Mockito.doReturn(requestingUserAssociation).when(associationsService).sendNewInvitation(eq("9999"), any());
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn( Mono.empty() ).when( emailService ).sendInviteEmail( eq( "theId123" ), eq( companyDetails ), eq( "Scrooge McDuck" ), any( String.class ), eq( "light.yagami@death.note" ) );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendInvitationEmailToAssociatedUser( eq( "theId123" ), eq( companyDetails ), eq( "Scrooge McDuck" ), eq( "light.yagami@death.note" ) );

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        Mockito.verify(associationsService).sendNewInvitation(eq("9999"), any() );
        Mockito.verify( emailService ).sendInviteEmail( eq( "theId123" ), argThat( comparisonUtils.compare( companyDetails, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq("Scrooge McDuck"), anyString(), eq( "light.yagami@death.note" ) );
        Mockito.verify( emailService ).sendInvitationEmailToAssociatedUser( eq("theId123"), argThat( comparisonUtils.compare( companyDetails, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq("Scrooge McDuck"), eq("light.yagami@death.note") );
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberDoesNotExistCreatesNewAssociation() throws Exception {
        final var association = testDataManager.fetchAssociationDaos( "19" ).getFirst();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "444444" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "444444" );
        mockers.mockUsersServiceSearchUserDetails( "000" );
        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("444444", "light.yagami@death.note");
        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserId("444444", "000");
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn( association).when( associationsService ).createAssociation( "444444", "000", null, ApprovalRouteEnum.INVITATION, "9999" );
        Mockito.doReturn( Mono.empty() ).when( emailService ).sendInviteEmail( eq( "theId123" ), eq( companyDetails ), eq( "Scrooge McDuck" ), any( String.class ), eq( "light.yagami@death.note" ) );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendInvitationEmailToAssociatedUser( eq( "theId123" ), eq( companyDetails ), eq( "Scrooge McDuck" ), eq( "light.yagami@death.note" ) );

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        Mockito.verify( emailService ).sendInviteEmail( eq( "theId123" ), argThat( comparisonUtils.compare( companyDetails, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq("Scrooge McDuck"), anyString(), eq( "light.yagami@death.note" ) );
        Mockito.verify( emailService ).sendInvitationEmailToAssociatedUser( eq("theId123"), argThat( comparisonUtils.compare( companyDetails, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq("Scrooge McDuck"), eq("light.yagami@death.note") );
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeUserEmailAndCompanyNumberDoesNotExistAndInviteeUserIsNotFoundCreatesNewAssociation() throws Exception {
        final var newAssociation = testDataManager.fetchAssociationDaos( "36" ).getFirst();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "444444" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "444444" );
        mockers.mockUsersServiceSearchUserDetailsEmptyList( "light.yagami@death.note" );
        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("444444", "light.yagami@death.note");
        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserId("444444", "000");
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doReturn( newAssociation).when( associationsService ).createAssociation( "444444", null, "light.yagami@death.note", ApprovalRouteEnum.INVITATION, "9999" );
        Mockito.doReturn( Mono.empty() ).when( emailService ).sendInviteEmail( eq( "theId123" ), eq( companyDetails ), eq( "Scrooge McDuck" ), any( String.class ), eq( "light.yagami@death.note" ) );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendInvitationEmailToAssociatedUser( eq( "theId123" ), eq( companyDetails ), eq( "Scrooge McDuck" ), eq( "light.yagami@death.note" ) );

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        Mockito.verify( emailService ).sendInviteEmail( eq( "theId123" ), argThat( comparisonUtils.compare( companyDetails, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq("Scrooge McDuck"), anyString(), eq( "light.yagami@death.note" ) );
        Mockito.verify( emailService ).sendInvitationEmailToAssociatedUser( eq("theId123"), argThat( comparisonUtils.compare( companyDetails, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq("Scrooge McDuck"), eq("light.yagami@death.note") );
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeUserEmailAndCompanyNumberExistAndInviteeUserIsFoundThrowsBadRequest() throws Exception {
        final var associationDao = testDataManager.fetchAssociationDaos( "35" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );
        mockers.mockUsersServiceSearchUserDetails( "000" );
        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("333333", "light.yagami@death.note");
        Mockito.doReturn(Optional.of(associationDao)).when(associationsService).fetchAssociationForCompanyNumberAndUserId("333333", "000");
        Mockito.doThrow(new BadRequestRuntimeException("There is an existing association with Confirmed status for the user")).when(associationsService).fetchAssociationForCompanyNumberAndUserId("333333", "000");
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);

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
    void getInvitationsForAssociationWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations/1/invitations")
                        .header("Eric-identity", "000")
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
    void fetchActiveInvitationsForUserWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchActiveInvitationsForUserWithoutEricIdentityReturnsForbidden() throws Exception {
        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isForbidden() );
    }

    @Test
    void fetchActiveInvitationsForUserWithMalformedEricIdentityReturnsForbidden() throws Exception {
        mockers.mockUsersServiceFetchUserDetailsNotFound( "$$$$" );

        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "$$$$")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isForbidden() );
    }

    @Test
    void fetchActiveInvitationsForUserWithNonexistentEricIdentityReturnsForbidden() throws Exception {
        mockers.mockUsersServiceFetchUserDetailsNotFound( "9191" );

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
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchActiveInvitationsForUserWithUnacceptableItemsPerPageReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/invitations?page_index=0&items_per_page=-1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchActiveInvitationsForUserRetrievesActiveInvitationsInCorrectOrderAndPaginatesCorrectly() throws Exception {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999" );

        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "9999")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*") )
                    .andExpect( status().isOk() );

        Mockito.verify( associationsService ).fetchActiveInvitations( argThat( comparisonUtils.compare( user, List.of( "userId" ), List.of(), Map.of() ) ), eq( 1 ), eq( 1 ) );
    }

    @Test
    void fetchActiveInvitationsForUserWithoutPageIndexAndItemsPerPageUsesDefaults() throws Exception {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();

        mockers.mockUsersServiceFetchUserDetails( "9999" );

        mockMvc.perform( get( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isOk() );

        Mockito.verify( associationsService ).fetchActiveInvitations( argThat( comparisonUtils.compare( user, List.of( "userId" ), List.of(), Map.of() ) ), eq( 0 ), eq( 15 ) );
    }

    @Test
    void fetchActiveInvitationsForUserWithPaginationAndVerifyResponse() throws Exception {
        final var user = testDataManager.fetchUserDtos( "000" ).getFirst();
        final var invitations = testDataManager.fetchInvitations( "37" );

        final var mockLinks = new Links()
                .self("/associations/invitations?page_index=1&items_per_page=1")
                .next("/associations/invitations?page_index=2&items_per_page=1");

        final var mockInvitationsList = new InvitationsList()
                .items(invitations).links(mockLinks)
                .itemsPerPage(1).pageNumber(1).totalResults(2).totalPages(2);

        mockers.mockUsersServiceFetchUserDetails( "000" );
        when(associationsService.fetchActiveInvitations(user, 1,1)).thenReturn(mockInvitationsList);

        final var response = mockMvc.perform(get("/associations/invitations?page_index=1&items_per_page=1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk());

        Mockito.verify(associationsService).fetchActiveInvitations( user,1,1);

        final var invitationsList = parseResponseTo( response, InvitationsList.class );

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
    void whenConfirmedAssociationDoesNotExist_thenThrowsBadRequestException() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        when(associationsService.confirmedAssociationExists(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"russell.howard@comedy.com\"}"))
                .andExpect(status().isBadRequest());
    }

}