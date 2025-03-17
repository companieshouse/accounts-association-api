package uk.gov.companieshouse.accounts.association.controller;

import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.PreviousStatesDao;
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
import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
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
        mockMvc.perform(patch("/associationsx")
                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "000")
                                .header("ERIC-Identity-Type", "oauth2"))
                .andExpect( status().isForbidden() );
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
    void fetchAssociationsByCanFetchMigratedAssociation() throws Exception {
        final var user = testDataManager.fetchUserDtos( "MKUser001" ).getFirst();
        final var expectedAssociationsList = new AssociationsList()
                .items( List.of( testDataManager.fetchAssociationDto( "MKAssociation001", user  ) ) )
                .links( new Links().self( "/associations?page_index=0&items_per_page=15" ).next( "" ) )
                .itemsPerPage( 15 ).pageNumber( 0 ).totalPages( 1 ).totalResults( 1 );

        mockers.mockUsersServiceFetchUserDetails( "MKUser001" );
        Mockito.doReturn( expectedAssociationsList ).when( associationsService ).fetchAssociationsForUserStatusAndCompany( user, List.of( StatusEnum.MIGRATED.getValue() ), 0, 15, null );

        final var response =
                mockMvc.perform( get( "/associations?status=migrated" )
                                .header( "X-Request-Id", "theId123" )
                                .header( "Eric-identity", "MKUser001" )
                                .header( "ERIC-Identity-Type", "oauth2" ) )
                        .andExpect( status().isOk() );

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var associations = associationsList.getItems();
        final var associationOne = associations.getFirst();

        Assertions.assertEquals( "MKAssociation001", associationOne.getId() );
        Assertions.assertEquals( "migrated", associationOne.getStatus().getValue() );
        Assertions.assertEquals( "migration", associationOne.getApprovalRoute().getValue() );
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
        Mockito.doReturn(page).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(user, List.of(AWAITING_APPROVAL.getValue(), CONFIRMED.getValue(), REMOVED.getValue(),MIGRATED.getValue()),0,15,"111111");

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

        Assertions.assertEquals("/associations/1", responseBodyPost.getAssociationLink());
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
        Mockito.doReturn(page).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(user, List.of(AWAITING_APPROVAL.getValue(), CONFIRMED.getValue(), REMOVED.getValue(), MIGRATED.getValue()),0,15,"111111");
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
        Mockito.doReturn(page).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(user, List.of(AWAITING_APPROVAL.getValue(), CONFIRMED.getValue(), REMOVED.getValue(),MIGRATED.getValue()),0,15,"111111");
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
    void addAssociationCanBeAppliedToMigratedAssociation() throws Exception {
        final var originalAssociationDao = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();
        final var company = testDataManager.fetchCompanyDetailsDtos( "MKCOMP001" ).getFirst();
        final var updatedAssociation = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst()
                .status( "confirmed" )
                .previousStates( new ArrayList<>( List.of( new PreviousStatesDao().status( "migrated" ).changedBy( "MKUser001" ).changedAt( LocalDateTime.now() ) ) ) )
                .approvalRoute( "auth_code" )
                .etag( generateEtag() );

        mockers.mockUsersServiceFetchUserDetails( "MKUser001" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn( new PageImpl<>( new ArrayList<>( List.of( originalAssociationDao ) ) ) ).when( associationsService ).fetchAssociationsDaoForUserStatusAndCompany( any(),any(), any(),any(),anyString() );
        Mockito.doReturn( List.of( "MKUser002" ) ).when( associationsService ).fetchAssociatedUsers( "MKCOMP001" );
        Mockito.doReturn( updatedAssociation ).when( associationsService ).upsertAssociation( any( AssociationDao.class ) );
        Mockito.doReturn( sendEmailMock ).when( emailService ).sendAuthCodeConfirmationEmailToAssociatedUser( eq( "theId123" ), eq( company ), eq( "Mario" ) );

        mockMvc.perform( post( "/associations" )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-Identity", "MKUser001" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"MKCOMP001\"}" ) )
                .andExpect( status().isCreated() );

        Assertions.assertEquals( "confirmed", updatedAssociation.getStatus() );
        Assertions.assertEquals( "auth_code", updatedAssociation.getApprovalRoute() );
        Assertions.assertNotNull( updatedAssociation.getEtag() );
        Assertions.assertEquals( 1, updatedAssociation.getPreviousStates().size() );
        Assertions.assertEquals( "migrated", updatedAssociation.getPreviousStates().getFirst().getStatus() );
        Assertions.assertEquals( "MKUser001", updatedAssociation.getPreviousStates().getFirst().getChangedBy() );
        Assertions.assertNotNull( updatedAssociation.getPreviousStates().getFirst().getChangedAt() );

        Mockito.verify( emailService ).sendAuthCodeConfirmationEmailToAssociatedUser( eq( "theId123" ), argThat( comparisonUtils.compare( company, List.of( "companyNumber", "companyName" ), List.of(), Map.of() ) ), eq( "Mario" ) );
    }

}