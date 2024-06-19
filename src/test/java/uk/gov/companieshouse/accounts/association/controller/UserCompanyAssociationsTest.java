package uk.gov.companieshouse.accounts.association.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.*;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.company.CompanyDetails;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.REMOVED;

@WebMvcTest(UserCompanyAssociations.class)
@Tag("unit-test")
class UserCompanyAssociationsTest {
    private static final String DEFAULT_KIND = "association";
    private final LocalDateTime now = LocalDateTime.now();
    @Autowired
    public MockMvc mockMvc;
    @MockBean
    StaticPropertyUtil staticPropertyUtil;
    @MockBean
    private UsersService usersService;
    @MockBean
    private CompanyService companyService;
    @MockBean
    private AssociationsService associationsService;
    @MockBean
    private AssociationsRepository associationsRepository;
    @MockBean
    private EmailService emailService;
    private Association associationOne;
    private Association associationTwo;
    private AssociationDao associationDaoZero;
    private AssociationDao associationDaoOne;
    private AssociationDao associationDaoTwo;

    private User scrooge;

    @BeforeEach
    public void setup() {
        Mockito.when(associationsService.confirmedAssociationExists(Mockito.any(), Mockito.any())).thenReturn(true);

        final var kira = new User()
                .userId("000")
                .email("light.yagami@death.note")
                .displayName("Kira");
        Mockito.doReturn( kira ).when( usersService ).fetchUserDetails( "000" );

        final var usersList = new UsersList();
        usersList.add(kira);
        Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of("light.yagami@death.note"));

        final var harley = new User()
                .userId( "333" )
                .email( "harley.quinn@gotham.city" );
        Mockito.doReturn( harley ).when( usersService ).fetchUserDetails( "333" );

        scrooge = new User()
                .userId("9999")
                .email("scrooge.mcduck@disney.land")
                .displayName("Scrooge McDuck");
        Mockito.doReturn( scrooge ).when( usersService ).fetchUserDetails( "9999" );

        final var invitationOne =
                new Invitation().invitedBy("homer.simpson@springfield.com")
                        .invitedAt(now.plusDays(4).toString());

        final var associationZero = new Association()
                .id("0")
                .userEmail("light.yagami@death.note");

        associationOne =
                new Association().etag("aa")
                        .id("18")
                        .userId("9999")
                        .userEmail("scrooge.mcduck@disney.land")
                        .displayName("Scrooge McDuck")
                        .companyNumber("333333")
                        .companyName("Tesco")
                        .status(StatusEnum.CONFIRMED)
                        .createdAt(LocalDateTime.now().atOffset(ZoneOffset.UTC))
                        .approvedAt(now.plusDays(1).atOffset(ZoneOffset.UTC))
                        .removedAt(now.plusDays(2).atOffset(ZoneOffset.UTC))
                        .kind(DEFAULT_KIND)
                        .approvalRoute(ApprovalRouteEnum.AUTH_CODE)
                        .approvalExpiryAt(now.plusDays(3).toString())
                        .links(new AssociationLinks().self("/18"));

        final var invitationTwo =
                new Invitation().invitedBy("homer.simpson@springfield.com")
                        .invitedAt(now.plusDays(8).toString());

        associationTwo =
                new Association().etag("bb")
                        .id("19")
                        .userId("9999")
                        .userEmail("scrooge.mcduck@disney.land")
                        .displayName("Scrooge McDuck")
                        .companyNumber("444444")
                        .companyName("Sainsbury's")
                        .status(StatusEnum.REMOVED)
                        .createdAt(LocalDateTime.now().atOffset(ZoneOffset.UTC))
                        .approvedAt(now.plusDays(5).atOffset(ZoneOffset.UTC))
                        .removedAt(now.plusDays(6).atOffset(ZoneOffset.UTC))
                        .kind(DEFAULT_KIND)
                        .approvalRoute(ApprovalRouteEnum.AUTH_CODE)
                        .approvalExpiryAt(now.plusDays(7).toString())
                        .links(new AssociationLinks().self("/19"));


        final var invitationDaoZero = new InvitationDao();
        invitationDaoZero.setInvitedBy( "333" );
        invitationDaoZero.setInvitedAt( now.plusDays(4) );

        associationDaoZero = new AssociationDao();
        associationDaoZero.setId("0");
        associationDaoZero.setUserEmail("light.yagami@death.note");
        associationDaoZero.setStatus("awaiting-approval");
        associationDaoZero.setInvitations( List.of( invitationDaoZero ) );

        associationDaoOne = new AssociationDao();
        associationDaoOne.setCompanyNumber("333333");
        associationDaoOne.setUserEmail("russell.howard@comedy.com");
        associationDaoOne.setId("34");
        associationDaoOne.setStatus(StatusEnum.REMOVED.getValue());
        associationDaoOne.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationDaoOne.setEtag("qq");

        associationDaoTwo = new AssociationDao();
        associationDaoTwo.setCompanyNumber("333333");
        associationDaoTwo.setUserId("111");
        associationDaoTwo.setId("35");
        associationDaoTwo.setStatus(StatusEnum.REMOVED.getValue());
        associationDaoTwo.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationDaoTwo.setEtag("rr");

        Mockito.doReturn(Optional.of(associationDaoZero)).when(associationsService).findAssociationDaoById("0");
        Mockito.doReturn(Optional.of(associationZero)).when(associationsService).findAssociationById("0");

    }

    @Test
    void fetchAssociationsByTestShouldThrow401ErrorRequestWhenEricIdNotProvided() throws Exception {
        var response = mockMvc.perform(get("/associations").header("X-Request-Id", "theId")).andReturn();
        assertEquals(401, response.getResponse().getStatus());
    }

    @Test
    void fetchAssociationsByTestShouldThrow405ErrorRequestWhenPatchApplied() throws Exception {
        var response = mockMvc.perform(patch("/associations").header("X-Request-Id", "theId")).andReturn();
        assertEquals(405, response.getResponse().getStatus());
    }

    @Test
    void fetchAssociationsByTestShouldThrow400ErrorWhenRequestIdNotProvided() throws Exception {
        var response = mockMvc.perform(get("/associations")
                .header("Eric-identity", "000")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*")).andExpect(status().isBadRequest()).andReturn();
        assertEquals("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Required header 'X-Request-Id' is not present.\",\"instance\":\"/associations\"}",
                response.getResponse().getContentAsString());
    }


    @Test
    void fetchAssociationsByTestShouldReturnEmptyDataWhenNoAssociationsFoundForEricIdentity() throws Exception {
        var response = mockMvc.perform(get("/associations")
                .header("Eric-identity", "000")
                .header("X-Request-Id", "theId")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*")).andExpect(status().is2xxSuccessful()).andReturn();
        assertEquals(0, response.getResponse().getContentLength());
    }

    @Test
    void fetchAssociationsByTestShouldReturnDataWhenAssociationsFoundForEricIdentity() throws Exception {
        User user = new User("abc","abc", "abc@abc.com").userId("abcd12345");
        AssociationsList associationsList = new AssociationsList().itemsPerPage(15).pageNumber(0).totalPages(1).totalResults(1);
        when(usersService.fetchUserDetails("abcd12345")).thenReturn(user);
        when(associationsService
                .fetchAssociationsForUserStatusAndCompany(user, Collections.singletonList("confirmed"), 0, 15, "123"))
                .thenReturn(associationsList);
        var response = mockMvc.perform(get("/associations?page_index=0&items_per_page=15&company_number=123")
                .header("Eric-identity", "abcd12345")
                .header("X-Request-Id", "theId")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*")).andReturn();

        String list = response.getResponse().getContentAsString();
        assertTrue(list.contains("\"items_per_page\":15"));
        assertTrue(list.contains("\"total_results\":1"));

    }

    @Test
    void fetchAssociationsByTestShouldTrowErrorWhenCompanyNumberIsOfWrongFormat() throws Exception {
        var response = mockMvc.perform(get("/associations?page_index=0&items_per_page=15&company_number=abc")
                .header("Eric-identity", "000")
                .header("X-Request-Id", "theId")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*")).andExpect(status().isBadRequest()).andReturn();

        String error = "{\"errors\":[{\"error\":\"abc must match \\\"^[0-9A-Z]{1,10}$\\\"\",\"location\":\"accounts_association_api\",\"location_type\":\"request-body\",\"type\":\"ch:validation\"}]}";
        assertEquals(error, response.getResponse().getContentAsString());
    }

    @Test
    void fetchAssociationsByTestShouldTrow500WhenInternalServerError() throws Exception {
        when(usersService.fetchUserDetails("abcd12345")).thenThrow(new InternalServerErrorRuntimeException("test"));
        mockMvc.perform(get("/associations?page_index=0&items_per_page=15&company_number=123")
                .header("Eric-identity", "abcd12345")
                .header("X-Request-Id", "theId")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*")).andExpect(status().is5xxServerError());

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
        mockMvc.perform(get("/associations/12345/invitations?page_index=-1&items_per_page=1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "99999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInvitationsForAssociationWithUnacceptableItemsPerPageReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations/12345/invitations?page_index=0&items_per_page=-1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "99999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fetchAssociationsByWithNonexistentCompanyReturnsNotFound() throws Exception {
        final var user = new User().userId("8888").email("mr.blobby@nightmare.com").displayName("Mr Blobby");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("8888");

        Mockito.doThrow(new NotFoundRuntimeException("accounts-association-api", "Not found")).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of(StatusEnum.CONFIRMED.getValue()), 0, 15, null);

        mockMvc.perform(get("/associations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "8888")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isNotFound());
    }

    @Test
    void fetchAssociationsByWithInvalidStatusReturnsZeroResults() throws Exception {
        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setTotalResults(0);
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(scrooge, List.of("$$$$"), 0, 15, null);

        final var response =
                mockMvc.perform(get("/associations?status=$$$$")
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        final AssociationsList associationsList = objectMapper.readValue(response.getContentAsByteArray(), AssociationsList.class);

        Assertions.assertEquals(0, associationsList.getTotalResults());
    }

    @Test
    void fetchAssociationsByUsesDefaultsIfValuesAreNotProvided() throws Exception {

        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setItems(List.of(associationOne));
        expectedAssociationsList.setLinks(new Links().self("/associations?page_index=0&items_per_page=15").next(""));
        expectedAssociationsList.setItemsPerPage(15);
        expectedAssociationsList.setPageNumber(0);
        expectedAssociationsList.setTotalPages(1);
        expectedAssociationsList.setTotalResults(1);
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(scrooge, List.of(StatusEnum.CONFIRMED.getValue()), 0, 15, null);

        final var response =
                mockMvc.perform(get("/associations")
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        final AssociationsList associationsList = objectMapper.readValue(response.getContentAsByteArray(), AssociationsList.class);
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                        .toList();

        Assertions.assertTrue(items.contains("18"));
        Assertions.assertEquals("/associations?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(15, associationsList.getItemsPerPage());
        Assertions.assertEquals(1, associationsList.getTotalResults());
        Assertions.assertEquals(1, associationsList.getTotalPages());
    }

    @Test
    void fetchAssociationsByWithOneStatusAppliesStatusFilterCorrectly() throws Exception {
        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setItems(List.of(associationTwo));
        expectedAssociationsList.setLinks(new Links().self("/associations?page_index=0&items_per_page=15").next(""));
        expectedAssociationsList.setItemsPerPage(15);
        expectedAssociationsList.setPageNumber(0);
        expectedAssociationsList.setTotalPages(1);
        expectedAssociationsList.setTotalResults(1);
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(scrooge, List.of(StatusEnum.REMOVED.getValue()), 0, 15, null);

        final var response =
                mockMvc.perform(get("/associations?status=removed")
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        final AssociationsList associationsList = objectMapper.readValue(response.getContentAsByteArray(), AssociationsList.class);
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                        .toList();

        Assertions.assertTrue(items.containsAll(List.of("19")));
        Assertions.assertEquals("/associations?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(15, associationsList.getItemsPerPage());
        Assertions.assertEquals(1, associationsList.getTotalResults());
        Assertions.assertEquals(1, associationsList.getTotalPages());
    }

    @Test
    void fetchAssociationsByWithMultipleStatusesAppliesStatusFilterCorrectly() throws Exception {
        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setItems(List.of(associationOne, associationTwo));
        expectedAssociationsList.setLinks(new Links().self("/associations?page_index=0&items_per_page=15").next(""));
        expectedAssociationsList.setItemsPerPage(15);
        expectedAssociationsList.setPageNumber(0);
        expectedAssociationsList.setTotalPages(1);
        expectedAssociationsList.setTotalResults(2);
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(scrooge, List.of(StatusEnum.CONFIRMED.getValue(), StatusEnum.REMOVED.getValue()), 0, 15, null);

        final var response =
                mockMvc.perform(get("/associations?status=confirmed&status=removed")
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        final AssociationsList associationsList = objectMapper.readValue(response.getContentAsByteArray(), AssociationsList.class);
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                        .toList();

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
        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setItems(List.of(associationTwo));
        expectedAssociationsList.setLinks(new Links().self("/associations?page_index=1&items_per_page=1").next(""));
        expectedAssociationsList.setItemsPerPage(1);
        expectedAssociationsList.setPageNumber(1);
        expectedAssociationsList.setTotalPages(2);
        expectedAssociationsList.setTotalResults(2);
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(scrooge, List.of(StatusEnum.CONFIRMED.getValue(), StatusEnum.REMOVED.getValue()), 1, 1, null);

        final var response =
                mockMvc.perform(get("/associations?status=confirmed&status=removed&page_index=1&items_per_page=1")
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        final AssociationsList associationsList = objectMapper.readValue(response.getContentAsByteArray(), AssociationsList.class);
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                        .toList();

        Assertions.assertTrue(items.contains("19"));
        Assertions.assertEquals("/associations?page_index=1&items_per_page=1", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(1, associationsList.getPageNumber());
        Assertions.assertEquals(1, associationsList.getItemsPerPage());
        Assertions.assertEquals(2, associationsList.getTotalResults());
        Assertions.assertEquals(2, associationsList.getTotalPages());
    }

    @Test
    void fetchAssociationsByFiltersBasedOnCompanyNumberCorrectly() throws Exception {
        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setItems(List.of(associationTwo));
        expectedAssociationsList.setLinks(new Links().self("/associations?page_index=0&items_per_page=15").next(""));
        expectedAssociationsList.setItemsPerPage(15);
        expectedAssociationsList.setPageNumber(0);
        expectedAssociationsList.setTotalPages(1);
        expectedAssociationsList.setTotalResults(1);
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(scrooge, List.of(StatusEnum.CONFIRMED.getValue()), 0, 15, "444444");

        final var response =
                mockMvc.perform(get("/associations?company_number=444444")
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        final AssociationsList associationsList = objectMapper.readValue(response.getContentAsByteArray(), AssociationsList.class);
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                        .toList();

        Assertions.assertTrue(items.contains("19"));
        Assertions.assertEquals("/associations?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(15, associationsList.getItemsPerPage());
        Assertions.assertEquals(1, associationsList.getTotalResults());
        Assertions.assertEquals(1, associationsList.getTotalPages());
    }

    private String reduceTimestampResolution(String timestamp) {
        return timestamp.substring(0, timestamp.indexOf(":"));
    }

    private String localDateTimeToNormalisedString(LocalDateTime localDateTime) {
        final var timestamp = localDateTime.toString();
        return reduceTimestampResolution(timestamp);
    }

    @Test
    void fetchAssociationsByDoesMappingCorrectly() throws Exception {
        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setItems(List.of(associationOne));
        expectedAssociationsList.setLinks(new Links().self("/associations?page_index=0&items_per_page=15").next(""));
        expectedAssociationsList.setItemsPerPage(15);
        expectedAssociationsList.setPageNumber(0);
        expectedAssociationsList.setTotalPages(1);
        expectedAssociationsList.setTotalResults(1);
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(scrooge, List.of(StatusEnum.CONFIRMED.getValue()), 0, 15, null);

        final var response =
                mockMvc.perform(get("/associations")
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        final AssociationsList associationsList = objectMapper.readValue(response.getContentAsByteArray(), AssociationsList.class);

        final var associations = associationsList.getItems();
        final var associationOne = associations.getFirst();

        Assertions.assertEquals("aa", associationOne.getEtag());
        Assertions.assertEquals("18", associationOne.getId());
        Assertions.assertEquals("9999", associationOne.getUserId());
        Assertions.assertEquals("scrooge.mcduck@disney.land", associationOne.getUserEmail());
        Assertions.assertEquals("Scrooge McDuck", associationOne.getDisplayName());
        Assertions.assertEquals("333333", associationOne.getCompanyNumber());
        Assertions.assertEquals("Tesco", associationOne.getCompanyName());
        Assertions.assertEquals(StatusEnum.CONFIRMED, associationOne.getStatus());
        Assertions.assertNotNull(associationOne.getCreatedAt());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.plusDays(1)), localDateTimeToNormalisedString(associationOne.getApprovedAt().toLocalDateTime()));
        Assertions.assertEquals(localDateTimeToNormalisedString(now.plusDays(2)), localDateTimeToNormalisedString(associationOne.getRemovedAt().toLocalDateTime()));
        Assertions.assertEquals(DEFAULT_KIND, associationOne.getKind());
        Assertions.assertEquals(ApprovalRouteEnum.AUTH_CODE, associationOne.getApprovalRoute());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.plusDays(3)), reduceTimestampResolution(associationOne.getApprovalExpiryAt()));
        Assertions.assertEquals("/18", associationOne.getLinks().getSelf());
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
        mockMvc.perform(get("/associations/{id}", "1")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationDetailsWithMalformedInputReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations/{id}", "$")
                .header("X-Request-Id", "theId123")
                .header("Eric-identity", "000")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*")).andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationUserDetailsWithNonexistentIdReturnsNotFound() throws Exception {
        Mockito.doReturn(Optional.empty()).when(associationsService).findAssociationById("11");
        final var response = mockMvc.perform(get("/associations/{id}", "11")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isNotFound()).andReturn();
        String error = "{\"errors\":[{\"error\":\"Cannot find Association for the Id: 11\",\"type\":\"ch:service\"}]}";
        assertEquals(error, response.getResponse().getContentAsString());
    }

    @Test
    void getAssociationDetailsFetchesAssociationDetails() throws Exception {
        Mockito.doReturn(Optional.of(associationOne)).when(associationsService).findAssociationById("18");
        final var response =
                mockMvc.perform(get("/associations/{id}", "18")
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "000")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        final Association associations = objectMapper.readValue(response.getContentAsByteArray(), Association.class);

        Assertions.assertEquals("18", associationOne.getId());

    }

    @Test
    void addAssociationWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/associations")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"000000\"}"))
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
        associationDaoOne.setStatus(StatusEnum.CONFIRMED.getValue());
        Page<AssociationDao> page = new PageImpl<>(List.of(associationDaoOne), PageRequest.of(1,15),15);
        Mockito.doReturn(page).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(scrooge,List.of(AWAITING_APPROVAL.getValue(), CONFIRMED.getValue(), REMOVED.getValue()),0,15,"333333");

        mockMvc.perform(post("/associations")
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addAssociationWithNonexistentCompanyNumberReturnsNotFound() throws Exception {
        Mockito.doThrow(new NotFoundRuntimeException("accounts-association-api", "Not found")).when(companyService).fetchCompanyProfile("919191");

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
        final var associationDao = new AssociationDao();
        associationDao.setId("99");
        Mockito.doReturn(associationDao).when(associationsService).createAssociation("000000", "000", null, ApprovalRouteEnum.AUTH_CODE, null);

        Mockito.doReturn(new PageImpl<AssociationDao>(new ArrayList<>())).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(any(),any(), any(),any(),anyString());

        final var responseJson =
                mockMvc.perform(post("/associations")
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "000")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"company_number\":\"000000\"}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final var objectMapper = new ObjectMapper();
        final var response = objectMapper.readValue(responseJson, ResponseBodyPost.class);

        Assertions.assertEquals("99", response.getAssociationId());

    }

    @Test
    void addAssociationWithNonExistentUserReturnsForbidden() throws Exception {
        Mockito.doThrow( new NotFoundRuntimeException( "accounts-association-api", "Not found." ) ).when( usersService ).fetchUserDetails( any() );

        mockMvc.perform(post("/associations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"000000\"}"))
                .andExpect(status().isForbidden());
    }

    ArgumentMatcher<CompanyDetails> companyDetailsMatcher( final String companyNumber, final String companyName ){
        return companyDetails -> companyNumber.equals( companyDetails.getCompanyNumber() ) && companyName.equals( companyDetails.getCompanyName() );
    }

    @Test
    void addAssociationWithUserThatHasNoDisplayNameSetsDisplayNameToEmailAddress() throws Exception {
        Supplier<User> userSupplier = () -> new User().userId("9999");

        final var associationDao = new AssociationDao();
        associationDao.setId("99");

        Mockito.doReturn( new User().email( "homer.simpson@springfield.com" ) ).when( usersService ).fetchUserDetails( anyString() );
        Mockito.doReturn( new CompanyDetails().companyNumber( "444444" ).companyName( "Sainsbury's" ) ).when( companyService ).fetchCompanyProfile( anyString() );


        Mockito.doReturn(new PageImpl<AssociationDao>(new ArrayList<>())).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(any(),any(), any(),any(),anyString());


        Mockito.doReturn( List.of( userSupplier ) ).when( emailService ).createRequestsToFetchAssociatedUsers( "444444" );
        Mockito.doReturn(associationDao).when(associationsService).createAssociation("444444", "666", null, ApprovalRouteEnum.AUTH_CODE, null);

        mockMvc.perform(post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "666")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\"}" ) )
                .andExpect( status().isCreated() );

        Mockito.verify( emailService ).sendAuthCodeConfirmationEmailToAssociatedUsers( eq( "theId123" ), argThat( companyDetailsMatcher( "444444", "Sainsbury's" ) ), eq( "homer.simpson@springfield.com" ), argThat( list -> list.size() == 1 ) );
    }

    @Test
    void existingAssociationWithStatusAwaitingApprovalWhenPostedShouldUpdateAssociationWithStatusConfirmed() throws Exception {
        associationDaoOne.setStatus(AWAITING_APPROVAL.getValue());
        Mockito.doReturn(new CompanyDetails("active","Sainsbury's","333333")).when(companyService).fetchCompanyProfile("333333");

        Page<AssociationDao> page = new PageImpl<>(List.of(associationDaoOne), PageRequest.of(1,15),15);
        Mockito.doReturn(page).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(scrooge,List.of(AWAITING_APPROVAL.getValue(), CONFIRMED.getValue(), REMOVED.getValue()),0,15,"333333");
        Mockito.doReturn(associationDaoOne).when(associationsService).upsertAssociation(any(AssociationDao.class));
        mockMvc.perform(post("/associations")
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\"}"))
                .andExpect(status().isCreated());

        Mockito.verify( emailService ).sendAuthCodeConfirmationEmailToAssociatedUsers( eq( "theId123" ), argThat( companyDetailsMatcher( "333333", "Sainsbury's" ) ), eq( "Scrooge McDuck" ), argThat(List::isEmpty) );
    }

    @Test
    void existingAssociationWithStatusRemovedWhenPostedShouldUpdateAssociationWithStatusConfirmed() throws Exception {
        associationDaoOne.setStatus(REMOVED.getValue());
        Mockito.doReturn(new CompanyDetails("active","Sainsbury's","333333")).when(companyService).fetchCompanyProfile("333333");

        Page<AssociationDao> page = new PageImpl<>(List.of(associationDaoOne), PageRequest.of(1,15),15);
        Mockito.doReturn(page).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(scrooge,List.of(AWAITING_APPROVAL.getValue(), CONFIRMED.getValue(), REMOVED.getValue()),0,15,"333333");
        Mockito.doReturn(associationDaoOne).when(associationsService).upsertAssociation(any(AssociationDao.class));
        mockMvc.perform(post("/associations")
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\"}"))
                .andExpect(status().isCreated());

        Mockito.verify( emailService ).sendAuthCodeConfirmationEmailToAssociatedUsers( eq( "theId123" ), argThat( companyDetailsMatcher( "333333", "Sainsbury's" ) ), eq( "Scrooge McDuck" ), argThat(List::isEmpty) );
    }

    @Test
    void addAssociationWithUserThatHasDisplayNameUsesDisplayName() throws Exception {
        Supplier<User> userSupplier = () -> new User().userId("9999");

        final var associationDao = new AssociationDao();
        associationDao.setId("99");

        Mockito.doReturn( new User().email( "homer.simpson@springfield.com" ).displayName( "Homer Simpson" ) ).when( usersService ).fetchUserDetails( anyString() );
        Mockito.doReturn( new CompanyDetails().companyNumber( "444444" ).companyName( "Sainsbury's" ) ).when( companyService ).fetchCompanyProfile( anyString() );
        Mockito.doReturn(new PageImpl<AssociationDao>(new ArrayList<>())).when(associationsService).fetchAssociationsDaoForUserStatusAndCompany(any(),any(), any(),any(),anyString());
        Mockito.doReturn( List.of( userSupplier ) ).when( emailService ).createRequestsToFetchAssociatedUsers( "444444" );
        Mockito.doReturn(associationDao).when(associationsService).createAssociation("444444", "666", null, ApprovalRouteEnum.AUTH_CODE, null);

        mockMvc.perform(post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "666")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\"}" ) )
                .andExpect( status().isCreated() );

        Mockito.verify( emailService ).sendAuthCodeConfirmationEmailToAssociatedUsers( eq( "theId123" ), argThat( companyDetailsMatcher( "444444", "Sainsbury's" ) ), eq( "Homer Simpson" ), argThat( list -> list.size() == 1 ) );
    }

    @Test
    void updateAssociationStatusForIdWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/associations/{associationId}", "18")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"removed\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithMalformedAssociationIdReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/associations/{associationId}", "$$$")
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
        mockMvc.perform(patch("/associations/{associationId}", "18")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithoutStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/associations/{associationId}", "18")
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
        mockMvc.perform(patch("/associations/{associationId}", "18")
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
        Mockito.doReturn(Optional.empty()).when(associationsService).findAssociationById("9191");

        mockMvc.perform(patch("/associations/{associationId}", "9191")
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
        associationDaoZero.setStatus("confirmed");
        associationDaoZero.setStatus( StatusEnum.CONFIRMED.getValue() );

        mockMvc.perform(patch("/associations/{associationId}", "0")
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
        Mockito.doReturn( null ).when(usersService).searchUserDetails(List.of("light.yagami@death.note"));

        mockMvc.perform(patch("/associations/{associationId}", "0")
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

        mockMvc.perform(patch("/associations/{associationId}", "0")
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

        Mockito.doReturn(new UsersList()).when(usersService).searchUserDetails(List.of("light.yagami@death.note"));

        mockMvc.perform(patch("/associations/{associationId}", "0")
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
        associationDaoZero.setStatus( StatusEnum.CONFIRMED.getValue() );

        mockMvc.perform(patch("/associations/{associationId}", "0")
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
        associationDaoZero.setStatus( StatusEnum.CONFIRMED.getValue() );

        Mockito.doReturn(new UsersList()).when(usersService).searchUserDetails(List.of("light.yagami@death.note"));

        mockMvc.perform(patch("/associations/{associationId}", "0")
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
        final var associationFour = new AssociationDao();
        associationFour.setCompanyNumber("x999999");
        associationFour.setUserId("444");
        associationFour.setUserEmail("robin@gotham.city");
        associationFour.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFour.setId("40");

        Mockito.doReturn(Optional.of(associationFour)).when(associationsService).findAssociationDaoById("40");

        final var robin = new User()
                .userId( "444" )
                .email( "robin@gotham.city" );
        Mockito.doReturn( robin ).when( usersService ).fetchUserDetails( "444" );

        final var companyDetails = new CompanyDetails().companyName( "Instram" ).companyNumber( "x999999" );
        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( "x999999" );

        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "the.joker@gotham.city" ) );
        Mockito.doReturn( requestsToFetchAssociatedUsers ).when( emailService ).createRequestsToFetchAssociatedUsers( "x999999" );

        mockMvc.perform( patch( "/associations/{associationId}", "40" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendAuthorisationRemovedEmailToAssociatedUsers( eq( "theId123" ), eq( companyDetails ), eq( "harley.quinn@gotham.city" ), eq( "robin@gotham.city" ), any() );
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereTargetUserIdDoesNotExistSendsNotification() throws Exception {
        final var associationFour = new AssociationDao();
        associationFour.setCompanyNumber("x999999");
        associationFour.setUserEmail("robin@gotham.city");
        associationFour.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFour.setId("40");

        Mockito.doReturn(Optional.of(associationFour)).when(associationsService).findAssociationDaoById("40");

        final var user = new User().userId("444").email("robin@gotham.city");
        final var users = new UsersList();
        users.add( user );
        Mockito.doReturn(users).when(usersService).searchUserDetails(List.of("robin@gotham.city"));

        final var requestingUser = new User()
                .userId("333")
                .email("harley.quinn@gotham.city");
        Mockito.doReturn( requestingUser ).when( usersService ).fetchUserDetails( "333" );

        final var companyDetails = new CompanyDetails().companyName( "Instram" );
        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( "x999999" );

        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "the.joker@gotham.city" ) );
        Mockito.doReturn( requestsToFetchAssociatedUsers ).when( emailService ).createRequestsToFetchAssociatedUsers( "x999999" );

        mockMvc.perform( patch( "/associations/{associationId}", "40" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendAuthorisationRemovedEmailToAssociatedUsers( eq( "theId123" ), eq( companyDetails ), eq( "harley.quinn@gotham.city" ), eq( "robin@gotham.city" ), any() );
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereUserCannotBeFoundSendsNotification() throws Exception {
        final var associationFour = new AssociationDao();
        associationFour.setCompanyNumber("x999999");
        associationFour.setUserEmail("robin@gotham.city");
        associationFour.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFour.setId("40");

        Mockito.doReturn(Optional.of(associationFour)).when(associationsService).findAssociationDaoById("40");

        Mockito.doReturn(new UsersList()).when(usersService).searchUserDetails(List.of("robin@gotham.city"));

        final var requestingUser = new User()
                .userId("333")
                .email("harley.quinn@gotham.city");
        Mockito.doReturn( requestingUser ).when( usersService ).fetchUserDetails( "333" );

        final var companyDetails = new CompanyDetails().companyName( "Instram" );
        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( "x999999" );

        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "the.joker@gotham.city" ) );
        Mockito.doReturn( requestsToFetchAssociatedUsers ).when( emailService ).createRequestsToFetchAssociatedUsers( "x999999" );

        mockMvc.perform( patch( "/associations/{associationId}", "40" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendAuthorisationRemovedEmailToAssociatedUsers( eq( "theId123" ), eq( companyDetails ), eq( "harley.quinn@gotham.city" ), eq( "robin@gotham.city" ), any() );
    }

    @Test
    void updateAssociationStatusForIdNotificationsUsesDisplayNamesWhenAvailable() throws Exception {
        final var associationFour = new AssociationDao();
        associationFour.setCompanyNumber("x999999");
        associationFour.setUserId("444");
        associationFour.setUserEmail("robin@gotham.city");
        associationFour.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFour.setId("40");

        Mockito.doReturn(Optional.of(associationFour)).when(associationsService).findAssociationDaoById("40");

        final var requestingUser = new User()
                .userId("333")
                .email("harley.quinn@gotham.city")
                .displayName("Harley Quinn");
        Mockito.doReturn( requestingUser ).when( usersService ).fetchUserDetails( "333" );

        final var companyDetails = new CompanyDetails().companyName( "Instram" );
        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( "x999999" );

        final var targetUser = new User()
                .userId("444")
                .email("robin@gotham.city")
                .displayName("Robin");
        UsersList list = new UsersList();
        list.add(targetUser);
        Mockito.doReturn( list ).when( usersService ).searchUserDetails( List.of("robin@gotham.city"));

        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "the.joker@gotham.city" ) );
        Mockito.doReturn( requestsToFetchAssociatedUsers ).when( emailService ).createRequestsToFetchAssociatedUsers( "x999999" );

        mockMvc.perform( patch( "/associations/{associationId}", "40" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendAuthorisationRemovedEmailToRemovedUser( eq( "theId123" ), eq( companyDetails ), eq( "Harley Quinn" ), any() );

        Mockito.verify( emailService ).sendAuthorisationRemovedEmailToAssociatedUsers( eq( "theId123" ), eq( companyDetails ), eq( "Harley Quinn" ),eq("Robin") ,any() );
    }

    @Test
    void updateAssociationStatusForIdUserAcceptedInvitationNotificationsSendsNotification() throws Exception {

        final var invitationFortyTwoA = new InvitationDao();
        invitationFortyTwoA.setInvitedBy("222");
        invitationFortyTwoA.setInvitedAt( now.plusDays(16) );

        final var invitationFortyTwoB = new InvitationDao();
        invitationFortyTwoB.setInvitedBy("444");
        invitationFortyTwoB.setInvitedAt( now.plusDays(14) );

        final var associationFortyTwo = new AssociationDao();
        associationFortyTwo.setCompanyNumber("x888888");
        associationFortyTwo.setUserId("333");

        associationFortyTwo.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationFortyTwo.setId("42");
        associationFortyTwo.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFortyTwo.setInvitations( List.of( invitationFortyTwoA, invitationFortyTwoB ) );

        Mockito.doReturn(Optional.of(associationFortyTwo)).when(associationsService).findAssociationDaoById("42");

        final var requestingUser = new User()
                .userId("333")
                .email("harley.quinn@gotham.city");
        Mockito.doReturn( requestingUser ).when( usersService ).fetchUserDetails( "333" );

        final var invitedByUser = new User()
                .userId("222")
                .email("the.joker@gotham.city");
        Mockito.doReturn( invitedByUser ).when( usersService ).fetchUserDetails( "222" );

        final var companyDetails = new CompanyDetails().companyName( "Twitter" );
        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( "x888888" );

        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "the.joker@gotham.city" ), () -> new User().email( "robin@gotham.city" ) );
        Mockito.doReturn( requestsToFetchAssociatedUsers ).when( emailService ).createRequestsToFetchAssociatedUsers( "x888888" );

        mockMvc.perform( patch( "/associations/{associationId}", "42" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendInvitationAcceptedEmailToAssociatedUsers( eq( "theId123" ), eq( companyDetails ), eq( "the.joker@gotham.city" ), eq( "harley.quinn@gotham.city" ), any() );
    }

    @Test
    void confirmUserStatusForExistingAssociationWithoutOneLoginUserShouldThrow400BadRequest() throws Exception {
        final var invitationFourtyTwoA = new InvitationDao();
        invitationFourtyTwoA.setInvitedBy("222");
        invitationFourtyTwoA.setInvitedAt( now.plusDays(16) );

        final var invitationFourtyTwoB = new InvitationDao();
        invitationFourtyTwoB.setInvitedBy("444");
        invitationFourtyTwoB.setInvitedAt( now.plusDays(14) );

        final var associationFourtyTwo = new AssociationDao();
        associationFourtyTwo.setCompanyNumber("x888888");
        associationFourtyTwo.setUserEmail("harley.quinn@gotham.city");
        associationFourtyTwo.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationFourtyTwo.setId("42");
        associationFourtyTwo.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFourtyTwo.setInvitations( List.of( invitationFourtyTwoA, invitationFourtyTwoB ) );

        Mockito.doReturn(Optional.of(associationFourtyTwo)).when(associationsService).findAssociationDaoById("42");


        final var companyDetails = new CompanyDetails().companyName( "Twitter" );
        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( "x888888" );

       var result =  mockMvc.perform( patch( "/associations/{associationId}", "42" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isBadRequest() )
                .andReturn();
       Assertions.assertEquals("{\"errors\":[{\"error\":\"Could not find data for user harley.quinn@gotham.city\",\"type\":\"ch:service\"}]}", result.getResponse().getContentAsString());

    }

    @Test
    void confirmUserStatusRequestForExistingAssociationWithoutOneLoginUserAndDifferentRequestingUserShouldThrow400BadRequest() throws Exception {
        final var invitationFourtyTwoA = new InvitationDao();
        invitationFourtyTwoA.setInvitedBy("222");
        invitationFourtyTwoA.setInvitedAt( now.plusDays(16) );

        final var invitationFourtyTwoB = new InvitationDao();
        invitationFourtyTwoB.setInvitedBy("444");
        invitationFourtyTwoB.setInvitedAt( now.plusDays(14) );

        final var associationFourtyTwo = new AssociationDao();
        associationFourtyTwo.setCompanyNumber("x888888");
        associationFourtyTwo.setUserEmail("harley.quinn@gotham.city");
        associationFourtyTwo.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationFourtyTwo.setId("42");
        associationFourtyTwo.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFourtyTwo.setInvitations( List.of( invitationFourtyTwoA, invitationFourtyTwoB ) );

        Mockito.doReturn(Optional.of(associationFourtyTwo)).when(associationsService).findAssociationDaoById("42");

        final var invitedByUser = new User()
                .userId("222")
                .email("the.joker@gotham.city");
        Mockito.doReturn( invitedByUser ).when( usersService ).fetchUserDetails( "222" );

        final var companyDetails = new CompanyDetails().companyName( "Twitter" );
        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( "x888888" );

        var result =  mockMvc.perform( patch( "/associations/{associationId}", "42" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "222")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isBadRequest() )
                .andReturn();
        Assertions.assertEquals("{\"errors\":[{\"error\":\"requesting user does not have access to perform the action\",\"type\":\"ch:service\"}]}", result.getResponse().getContentAsString());

    }

    @Test
    void updateAssociationStatusForIdUserAcceptedInvitationNotificationsUsesDisplayNamesWhenAvailable() throws Exception {
        final var invitationFourtyTwoA = new InvitationDao();
        invitationFourtyTwoA.setInvitedBy("222");
        invitationFourtyTwoA.setInvitedAt( now.plusDays(16) );

        final var invitationFourtyTwoB = new InvitationDao();
        invitationFourtyTwoB.setInvitedBy("444");
        invitationFourtyTwoB.setInvitedAt( now.plusDays(14) );

        final var associationFourtyTwo = new AssociationDao();
        associationFourtyTwo.setCompanyNumber("x888888");
        associationFourtyTwo.setUserId("333");

        associationFourtyTwo.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationFourtyTwo.setId("42");
        associationFourtyTwo.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFourtyTwo.setInvitations( List.of( invitationFourtyTwoA, invitationFourtyTwoB ) );

        Mockito.doReturn(Optional.of(associationFourtyTwo)).when(associationsService).findAssociationDaoById("42");

        final var requestingUser = new User()
                .userId("333")
                .email("harley.quinn@gotham.city")
                .displayName("Harley Quinn");
        Mockito.doReturn( requestingUser ).when( usersService ).fetchUserDetails( "333" );

        final var invitedByUser = new User()
                .userId("222")
                .email("the.joker@gotham.city")
                .displayName("Joker");
        Mockito.doReturn( invitedByUser ).when( usersService ).fetchUserDetails( "222" );

        final var companyDetails = new CompanyDetails().companyName( "Twitter" );
        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( "x888888" );

        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "the.joker@gotham.city" ), () -> new User().email( "robin@gotham.city" ) );
        Mockito.doReturn( requestsToFetchAssociatedUsers ).when( emailService ).createRequestsToFetchAssociatedUsers( "x888888" );

        mockMvc.perform( patch( "/associations/{associationId}", "42" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );


        Mockito.verify( emailService ).sendInvitationAcceptedEmailToAssociatedUsers( eq( "theId123" ), eq( companyDetails ), eq( "Joker" ), eq( "Harley Quinn" ), any() );
    }

    @Test
    void updateAssociationStatusForIdUserCancelledInvitationNotificationsSendsNotification() throws Exception {

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
        associationFortyTwo.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFortyTwo.setInvitations( List.of( invitationFortyTwoA, invitationFortyTwoB ) );

        Mockito.doReturn(Optional.of(associationFortyTwo)).when(associationsService).findAssociationDaoById("42");

        final var requestingUser = new User()
                .userId("222")
                .email("the.joker@gotham.city");
        Mockito.doReturn( requestingUser ).when( usersService ).fetchUserDetails( "222" );

        final var cancelledUser = new User()
                .userId("333")
                .email("harley.quinn@gotham.city");
        Mockito.doReturn( cancelledUser ).when( usersService ).fetchUserDetails( "333" );

        final var companyDetails = new CompanyDetails().companyName( "Twitter" );
        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( "x888888" );

        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "robin@gotham.city" ) );
        Mockito.doReturn( requestsToFetchAssociatedUsers ).when( emailService ).createRequestsToFetchAssociatedUsers( "x888888" );

        mockMvc.perform( patch( "/associations/{associationId}", "42" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "222")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendInvitationCancelledEmailToAssociatedUsers( eq( "theId123" ), eq( companyDetails ), eq( "the.joker@gotham.city" ), eq( "harley.quinn@gotham.city" ), any() );
    }

    @Test
    void updateAssociationStatusForIdUserRejectedInvitationNotificationsSendsNotification() throws Exception {

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
        associationFortyTwo.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFortyTwo.setInvitations( List.of( invitationFortyTwoA, invitationFortyTwoB ) );

        Mockito.doReturn(Optional.of(associationFortyTwo)).when(associationsService).findAssociationDaoById("42");

        final var requestingUser = new User()
                .userId("333")
                .email("harley.quinn@gotham.city");
        Mockito.doReturn( requestingUser ).when( usersService ).fetchUserDetails( "333" );

        final var companyDetails = new CompanyDetails().companyName( "Twitter" );
        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( "x888888" );

        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "robin@gotham.city" ), () -> new User().email( "joker@gotham.city" ) );
        Mockito.doReturn( requestsToFetchAssociatedUsers ).when( emailService ).createRequestsToFetchAssociatedUsers( "x888888" );

        mockMvc.perform( patch( "/associations/{associationId}", "42" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( emailService ).sendInvitationRejectedEmailToAssociatedUsers( eq( "theId123" ), eq( companyDetails ), eq( "harley.quinn@gotham.city" ), any() );
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
        Mockito.doThrow(new BadRequestRuntimeException("Bad request")).when(usersService).fetchUserDetails("$$$$");

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "$$$$")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithNonexistentEricIdentityReturnsForbidden() throws Exception {
        Mockito.doThrow(new NotFoundRuntimeException("accounts-association-api", "Not found")).when(usersService).fetchUserDetails("9191");

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().is(403));
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
        Mockito.doThrow(new NotFoundRuntimeException("accounts-association-api", "Not found")).when(companyService).fetchCompanyProfile("919191");

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

    private ArgumentMatcher<AssociationDao> associationDaoMatches(final AssociationDao baseAssociation, final String expectedUserId, final String expectedEmailAddress) {
        return associationDao -> {
            baseAssociation.setUserId(expectedUserId);
            baseAssociation.setUserEmail(expectedEmailAddress);
            return associationDao.equals(baseAssociation);
        };
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeEmailAndCompanyNumberExistsAndInviteeUserIsFoundPerformsSwapAndUpdateOperations() throws Exception {

        final var company = new CompanyDetails().companyNumber("333333").companyName("Tesco");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile("333333");

        final var usersList = new UsersList();
        usersList.add(new User().userId("8888").email("russell.howard@comedy.com"));
        Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of("russell.howard@comedy.com"));

        Mockito.doReturn(Optional.of(associationDaoOne)).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("333333", "russell.howard@comedy.com");

        final var association = new AssociationDao();
        association.setId("34");
        association.setApprovalExpiryAt( now );
        Mockito.doReturn(association).when(associationsService).sendNewInvitation(eq("9999"), any());

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"russell.howard@comedy.com\"}"))
                .andExpect(status().isCreated());

        Mockito.verify(associationsService).sendNewInvitation(eq("9999"), argThat(associationDaoMatches(associationDaoOne, "8888", null)));
        Mockito.verify( emailService ).sendInviteEmail( eq( "theId123" ), argThat( companyDetailsMatcher("333333", "Tesco" ) ), eq("Scrooge McDuck"), anyString(), eq( "russell.howard@comedy.com" ) );
        Mockito.verify( emailService ).sendInvitationEmailToAssociatedUsers(  eq("theId123") ,
                argThat( companyDetailsMatcher(
                        "333333",
                        "Tesco" ) ),
                eq("Scrooge McDuck"),
                eq("russell.howard@comedy.com"),
                argThat( List::isEmpty ) );
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeEmailAndCompanyNumberExistsAndInviteeUserIsNotFoundDoesNotPerformSwapButDoesPerformUpdateOperation() throws Exception {

        final var company = new CompanyDetails().companyNumber("333333").companyName("Tesco");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile("333333");

        Mockito.doReturn(new UsersList()).when(usersService).searchUserDetails(List.of("russell.howard@comedy.com"));

        Mockito.doReturn(Optional.of(associationDaoOne)).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("333333", "russell.howard@comedy.com");

        final var association = new AssociationDao();
        association.setId("34");
        association.setApprovalExpiryAt( now );
        Mockito.doReturn(association).when(associationsService).sendNewInvitation(eq("9999"), any());

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"russell.howard@comedy.com\"}"))
                .andExpect(status().isCreated());

        Mockito.verify(associationsService).sendNewInvitation(eq("9999"), argThat(associationDaoMatches(associationDaoOne, null, "russell.howard@comedy.com")));
        Mockito.verify( emailService ).sendInviteEmail( eq( "theId123" ), argThat( companyDetailsMatcher("333333", "Tesco" ) ), eq("Scrooge McDuck"), anyString(), eq( "russell.howard@comedy.com" ) );
        Mockito.verify( emailService ).sendInvitationEmailToAssociatedUsers(  eq("theId123") ,
                argThat( companyDetailsMatcher(
                        "333333",
                        "Tesco" ) ),
                eq("Scrooge McDuck"),
                eq("russell.howard@comedy.com"),
                argThat( List::isEmpty ) );
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberExistsDoesNotPerformSwapButDoesPerformUpdateOperation() throws Exception {

        final var company = new CompanyDetails().companyNumber("333333").companyName("Tesco");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile("333333");

        final var usersList = new UsersList();
        usersList.add(new User().userId("111").email("bruce.wayne@gotham.city"));
        Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of("bruce.wayne@gotham.city"));

        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("333333", "bruce.wayne@gotham.city");

        Mockito.doReturn(Optional.of(associationDaoTwo)).when(associationsService).fetchAssociationForCompanyNumberAndUserId("333333", "111");

        final var association = new AssociationDao();
        association.setId("35");
        association.setApprovalExpiryAt( now );
        Mockito.doReturn(association).when(associationsService).sendNewInvitation(eq("9999"), any());

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isCreated());

        Mockito.verify(associationsService).sendNewInvitation(eq("9999"), argThat(associationDaoMatches(associationDaoTwo, "111", null)));
        Mockito.verify( emailService ).sendInviteEmail( eq( "theId123" ), argThat( companyDetailsMatcher("333333", "Tesco" ) ), eq("Scrooge McDuck"), anyString(), eq( "bruce.wayne@gotham.city" ) );
        Mockito.verify( emailService ).sendInvitationEmailToAssociatedUsers(  eq("theId123") ,
                argThat( companyDetailsMatcher(
                        "333333",
                        "Tesco" ) ),
                eq("Scrooge McDuck"),
                eq("bruce.wayne@gotham.city"),
                argThat( List::isEmpty ) );
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberDoesNotExistCreatesNewAssociation() throws Exception {

        final var company = new CompanyDetails().companyNumber("444444").companyName("Sainsbury's");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile("444444");

        final var usersList = new UsersList();
        usersList.add(new User().userId("111").email("bruce.wayne@gotham.city"));
        Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of("bruce.wayne@gotham.city"));

        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("444444", "bruce.wayne@gotham.city");

        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserId("444444", "111");

        final var association = new AssociationDao();
        association.setId("99");
        association.setApprovalExpiryAt( now );
        Mockito.doReturn(association).when(associationsService).createAssociation("444444", "111", null, ApprovalRouteEnum.INVITATION, "9999");

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isCreated());
        Mockito.verify( emailService ).sendInviteEmail( eq( "theId123" ), argThat( companyDetailsMatcher("444444", "Sainsbury's" ) ), eq("Scrooge McDuck"), anyString(), eq( "bruce.wayne@gotham.city" ) );
        Mockito.verify( emailService ).sendInvitationEmailToAssociatedUsers(  eq("theId123") ,
                argThat( companyDetailsMatcher(
                        "444444",
                        "Sainsbury's" ) ),
                eq("Scrooge McDuck"),
                eq("bruce.wayne@gotham.city"),
                argThat( List::isEmpty ) );
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeUserEmailAndCompanyNumberDoesNotExistAndInviteeUserIsNotFoundCreatesNewAssociation() throws Exception {

        final var company = new CompanyDetails().companyNumber("333333").companyName("Tesco");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile("333333");

        Mockito.doReturn(new UsersList()).when(usersService).searchUserDetails(List.of("madonna@singer.com"));

        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("333333", "madonna@singer.com");

        final var association = new AssociationDao();
        association.setId("99");
        association.setApprovalExpiryAt( now );
        Mockito.doReturn(association).when(associationsService).createAssociation("333333", null, "madonna@singer.com", ApprovalRouteEnum.INVITATION, "9999");

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"madonna@singer.com\"}"))
                .andExpect(status().isCreated());
        Mockito.verify( emailService ).sendInviteEmail( eq( "theId123" ), argThat( companyDetailsMatcher("333333", "Tesco" ) ), eq("Scrooge McDuck"), anyString(), eq( "madonna@singer.com" ) );
        Mockito.verify( emailService ).sendInvitationEmailToAssociatedUsers(  eq("theId123") ,
                argThat( companyDetailsMatcher(
                        "333333",
                        "Tesco" ) ),
                eq("Scrooge McDuck"),
                eq("madonna@singer.com"),
                argThat( List::isEmpty ) );
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeUserEmailAndCompanyNumberExistAndInviteeUserIsFoundThrowsBadRequest() throws Exception {

        final var company = new CompanyDetails().companyNumber("333333").companyName("Tesco");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile("333333");

        final var usersList = new UsersList();
        usersList.add(new User().userId("111").email("bruce.wayne@gotham.city"));
        Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of("bruce.wayne@gotham.city"));

        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("333333", "bruce.wayne@gotham.city");

        Mockito.doReturn(Optional.of(associationDaoOne)).when(associationsService).fetchAssociationForCompanyNumberAndUserId("333333", "111");

        Mockito.doThrow(new BadRequestRuntimeException("There is an existing association with Confirmed status for the user")).when(associationsService).fetchAssociationForCompanyNumberAndUserId("333333", "111");


        final var response =mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isBadRequest()).andReturn();
        assertEquals("{\"errors\":[{\"error\":\"There is an existing association with Confirmed status for the user\",\"type\":\"ch:service\"}]}",
                response.getResponse().getContentAsString());
    }

    @Test
    void getInvitationsForAssociationWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations/{id}/invitations", "1")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInvitationsForAssociationWithMalformedInputReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations/{id}/invitations", "$")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInvitationsForAssociationWithNonexistentIdReturnsNotFound() throws Exception {
        Mockito.doReturn(Optional.empty()).when(associationsService).findAssociationDaoById("11");
        final var response = mockMvc.perform(get("/associations/{id}/invitations", "11")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isNotFound())
                .andReturn();
        String error = "{\"errors\":[{\"error\":\"Association 11 was not found.\",\"type\":\"ch:service\"}]}";
        assertEquals(error, response.getResponse().getContentAsString());
    }

    @Test
    void getInvitationsForAssociationFetchesInvitations() throws Exception {
        Invitation invitation1 = new Invitation();
        invitation1.setAssociationId("18");
        invitation1.setIsActive(true);
        invitation1.setInvitedBy("user1");
        invitation1.setInvitedAt("2023-05-28T12:34:56");

        Invitation invitation2 = new Invitation();
        invitation2.setAssociationId("18");
        invitation2.setIsActive(false);
        invitation2.setInvitedBy("user2");
        invitation2.setInvitedAt("2023-05-29T12:34:56");

        InvitationsList invitationsList = new InvitationsList();
        invitationsList.items(List.of(invitation1, invitation2));
        AssociationDao associationDao = new AssociationDao();
        Mockito.doReturn(Optional.of(associationDao)).when(associationsService).findAssociationDaoById("18");
        Mockito.doReturn(invitationsList).when(associationsService).fetchInvitations(associationDao, 0, 15);

        final var response = mockMvc.perform(get("/associations/{id}/invitations", "18")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        InvitationsList resultInvitationsList = objectMapper.readValue(response.getContentAsByteArray(), new TypeReference<InvitationsList>() {});

        List<Invitation> resultInvitations = resultInvitationsList.getItems();
        List<Invitation> invitations = invitationsList.getItems();

        Assertions.assertEquals(invitations.size(), resultInvitations.size());
        Assertions.assertEquals(invitations.get(0).getAssociationId(), resultInvitations.get(0).getAssociationId());
        Assertions.assertEquals(invitations.get(0).getIsActive(), resultInvitations.get(0).getIsActive());
        Assertions.assertEquals(invitations.get(0).getInvitedBy(), resultInvitations.get(0).getInvitedBy());
        Assertions.assertEquals(invitations.get(0).getInvitedAt(), resultInvitations.get(0).getInvitedAt());

        Assertions.assertEquals(invitations.get(1).getAssociationId(), resultInvitations.get(1).getAssociationId());
        Assertions.assertEquals(invitations.get(1).getIsActive(), resultInvitations.get(1).getIsActive());
        Assertions.assertEquals(invitations.get(1).getInvitedBy(), resultInvitations.get(1).getInvitedBy());
        Assertions.assertEquals(invitations.get(1).getInvitedAt(), resultInvitations.get(1).getInvitedAt());
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
        Mockito.doThrow( new NotFoundRuntimeException( "accounts-association-api", "Not found." ) ).when( usersService ).fetchUserDetails( any() );

        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "$$$$")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isForbidden() );
    }

    @Test
    void fetchActiveInvitationsForUserWithNonexistentEricIdentityReturnsForbidden() throws Exception {
        Mockito.doThrow( new NotFoundRuntimeException( "accounts-association-api", "Not found." ) ).when( usersService ).fetchUserDetails( any() );

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
        when(usersService.fetchUserDetails("99999")).thenReturn(new User().userId("99999"));

        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "99999")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*") )
                    .andExpect( status().isOk() )
                    .andReturn()
                    .getResponse();

        Mockito.verify( associationsService ).fetchActiveInvitations( argThat( user -> user.getUserId().equals("99999") ), eq( 1 ), eq( 1 ) );
    }

    @Test
    void fetchActiveInvitationsForUserWithoutPageIndexAndItemsPerPageUsesDefaults() throws Exception {
        when(usersService.fetchUserDetails("99999")).thenReturn(new User().userId("99999"));

        mockMvc.perform( get( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "99999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isOk() )
                .andReturn()
                .getResponse();

        Mockito.verify( associationsService ).fetchActiveInvitations( argThat( user -> user.getUserId().equals("99999") ), eq( 0 ), eq( 15 ) );
    }

    @Test
    void fetchActiveInvitationsForUserWithPaginationAndVerifyResponse() throws Exception {
        when(usersService.fetchUserDetails("99999")).thenReturn(new User().userId("99999"));

        Invitation invitation1 = new Invitation();
        invitation1.setInvitedBy("user1@example.com");
        invitation1.setInvitedAt(LocalDateTime.now().toString());

        Invitation invitation2 = new Invitation();
        invitation2.setInvitedBy("user2@example.com");
        invitation2.setInvitedAt(LocalDateTime.now().toString());

        List<Invitation> mockInvitations = List.of(invitation1, invitation2);
        InvitationsList mockInvitationsList = new InvitationsList();
        mockInvitationsList.setItemsPerPage(1);
        mockInvitationsList.setPageNumber(1);
        mockInvitationsList.setTotalResults(2);
        mockInvitationsList.setTotalPages(2);
        mockInvitationsList.setItems(mockInvitations);

        Links mockLinks = new Links();
        mockLinks.setSelf("/associations/invitations?page_index=1&items_per_page=1");
        mockLinks.setNext("/associations/invitations?page_index=2&items_per_page=1");
        mockInvitationsList.setLinks(mockLinks);

        when(associationsService.fetchActiveInvitations(argThat( user -> user.getUserId().equals("99999") ), eq(1), eq(1)))
                .thenReturn(mockInvitationsList);

        MvcResult result = mockMvc.perform(get("/associations/invitations?page_index=1&items_per_page=1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "99999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();

        InvitationsList invitationsList = new ObjectMapper().readValue(responseContent, InvitationsList.class);

        assertNotNull(invitationsList);
        assertEquals(1, invitationsList.getItemsPerPage().intValue());
        assertEquals(1, invitationsList.getPageNumber().intValue());
        assertEquals(2, invitationsList.getTotalResults().intValue());
        assertEquals(2, invitationsList.getTotalPages().intValue());
        assertNotNull(invitationsList.getLinks());
        assertEquals("/associations/invitations?page_index=1&items_per_page=1", invitationsList.getLinks().getSelf());
        assertEquals("/associations/invitations?page_index=2&items_per_page=1", invitationsList.getLinks().getNext());

        Mockito.verify(associationsService).fetchActiveInvitations(argThat( user -> user.getUserId().equals("99999") ), eq(1), eq(1));
    }

    @Test
    void getInvitationsForAssociationWithPaginationAndVerifyResponse() throws Exception {
        Invitation invitation1 = new Invitation();
        invitation1.setInvitedBy("user1@example.com");
        invitation1.setInvitedAt(LocalDateTime.now().toString());

        Invitation invitation2 = new Invitation();
        invitation2.setInvitedBy("user2@example.com");
        invitation2.setInvitedAt(LocalDateTime.now().toString());

        List<Invitation> mockInvitations = List.of(invitation1, invitation2);
        InvitationsList mockInvitationsList = new InvitationsList();
        mockInvitationsList.setItemsPerPage(1);
        mockInvitationsList.setPageNumber(1);
        mockInvitationsList.setTotalResults(2);
        mockInvitationsList.setTotalPages(2);
        mockInvitationsList.setItems(mockInvitations);

        Links mockLinks = new Links();
        mockLinks.setSelf("/associations/12345/invitations?page_index=1&items_per_page=1");
        mockLinks.setNext("/associations/12345/invitations?page_index=2&items_per_page=1");
        mockInvitationsList.setLinks(mockLinks);

        AssociationDao mockAssociationDao = new AssociationDao();
        when(associationsService.findAssociationDaoById(eq("12345")))
                .thenReturn(Optional.of(mockAssociationDao));
        when(associationsService.fetchInvitations(eq(mockAssociationDao), eq(1), eq(1)))
                .thenReturn(mockInvitationsList);

        MvcResult result = mockMvc.perform(get("/associations/12345/invitations?page_index=1&items_per_page=1")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "99999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();

        InvitationsList invitationsList = new ObjectMapper().readValue(responseContent, InvitationsList.class);

        assertNotNull(invitationsList);
        assertEquals(1, invitationsList.getItemsPerPage().intValue());
        assertEquals(1, invitationsList.getPageNumber().intValue());
        assertEquals(2, invitationsList.getTotalResults().intValue());
        assertEquals(2, invitationsList.getTotalPages().intValue());
        assertNotNull(invitationsList.getLinks());
        assertEquals("/associations/12345/invitations?page_index=1&items_per_page=1", invitationsList.getLinks().getSelf());
        assertEquals("/associations/12345/invitations?page_index=2&items_per_page=1", invitationsList.getLinks().getNext());

        Mockito.verify(associationsService).findAssociationDaoById(eq("12345"));
        Mockito.verify(associationsService).fetchInvitations(eq(mockAssociationDao), eq(1), eq(1));
    }

    @Test
    void whenConfirmedAssociationDoesNotExist_thenThrowsBadRequestException() throws Exception {
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