package uk.gov.companieshouse.accounts.association.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.function.Supplier;
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
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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
import uk.gov.companieshouse.api.accounts.associations.model.AssociationLinks;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsListLinks;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;
import uk.gov.companieshouse.api.accounts.associations.model.ResponseBodyPost;
import uk.gov.companieshouse.api.accounts.user.model.User;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.company.CompanyDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private EmailService emailService;
    private Association associationOne;
    private Association associationTwo;
    private AssociationDao associationDaoOne;
    private AssociationDao associationDaoTwo;


    @BeforeEach
    public void setup() {

        final var invitationOne =
                new Invitation().invitedBy("homer.simpson@springfield.com")
                        .invitedAt(now.plusDays(4).toString());

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
                        .invitations(List.of(invitationOne))
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
                        .invitations(List.of(invitationTwo))
                        .links(new AssociationLinks().self("/19"));

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
        var response = mockMvc.perform(get("/associations").header("Eric-identity", "abcd12345")
                .header("Eric-identity", "abcd12345")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*")).andExpect(status().isBadRequest()).andReturn();
        assertEquals("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Required header 'X-Request-Id' is not present.\",\"instance\":\"/associations\"}",
                response.getResponse().getContentAsString());
    }


    @Test
    void fetchAssociationsByTestShouldThrowBadRequestIfUserIdFromEricNotFound() throws Exception {
        var response = mockMvc.perform(get("/associations").header("Eric-identity", "abcd12345")
                .header("X-Request-Id", "theId")
                .header("Eric-identity", "abcd12345")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*")).andExpect(status().isBadRequest()).andReturn();
        assertEquals("{\"errors\":[{\"error\":\"Eric id is not valid\",\"type\":\"ch:service\"}]}",
                response.getResponse().getContentAsString());

    }

    @Test
    void fetchAssociationsByTestShouldReturnEmptyDataWhenNoAssociationsFoundForEricIdentity() throws Exception {
        when(usersService.fetchUserDetails("abcd12345")).thenReturn(new User("abc", "abc", "abc@abc.com").userId("abcd12345"));
        var response = mockMvc.perform(get("/associations")
                .header("Eric-identity", "abcd12345")
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
                .header("Eric-identity", "abcd12345")
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
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fetchAssociationsByWithInvalidItemsPerPageReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations?items_per_page=0")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
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
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");

        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setTotalResults(0);
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of("$$$$"), 0, 15, null);

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
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");

        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setItems(List.of(associationOne));
        expectedAssociationsList.setLinks(new AssociationsListLinks().self("/associations?page_index=0&items_per_page=15").next(""));
        expectedAssociationsList.setItemsPerPage(15);
        expectedAssociationsList.setPageNumber(0);
        expectedAssociationsList.setTotalPages(1);
        expectedAssociationsList.setTotalResults(1);
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of(StatusEnum.CONFIRMED.getValue()), 0, 15, null);

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
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");

        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setItems(List.of(associationTwo));
        expectedAssociationsList.setLinks(new AssociationsListLinks().self("/associations?page_index=0&items_per_page=15").next(""));
        expectedAssociationsList.setItemsPerPage(15);
        expectedAssociationsList.setPageNumber(0);
        expectedAssociationsList.setTotalPages(1);
        expectedAssociationsList.setTotalResults(1);
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of(StatusEnum.REMOVED.getValue()), 0, 15, null);

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
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");

        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setItems(List.of(associationOne, associationTwo));
        expectedAssociationsList.setLinks(new AssociationsListLinks().self("/associations?page_index=0&items_per_page=15").next(""));
        expectedAssociationsList.setItemsPerPage(15);
        expectedAssociationsList.setPageNumber(0);
        expectedAssociationsList.setTotalPages(1);
        expectedAssociationsList.setTotalResults(2);
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of(StatusEnum.CONFIRMED.getValue(), StatusEnum.REMOVED.getValue()), 0, 15, null);

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
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");

        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setItems(List.of(associationTwo));
        expectedAssociationsList.setLinks(new AssociationsListLinks().self("/associations?page_index=1&items_per_page=1").next(""));
        expectedAssociationsList.setItemsPerPage(1);
        expectedAssociationsList.setPageNumber(1);
        expectedAssociationsList.setTotalPages(2);
        expectedAssociationsList.setTotalResults(2);
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of(StatusEnum.CONFIRMED.getValue(), StatusEnum.REMOVED.getValue()), 1, 1, null);

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
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");

        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setItems(List.of(associationTwo));
        expectedAssociationsList.setLinks(new AssociationsListLinks().self("/associations?page_index=0&items_per_page=15").next(""));
        expectedAssociationsList.setItemsPerPage(15);
        expectedAssociationsList.setPageNumber(0);
        expectedAssociationsList.setTotalPages(1);
        expectedAssociationsList.setTotalResults(1);
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of(StatusEnum.CONFIRMED.getValue()), 0, 15, "444444");

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

        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");

        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setItems(List.of(associationOne));
        expectedAssociationsList.setLinks(new AssociationsListLinks().self("/associations?page_index=0&items_per_page=15").next(""));
        expectedAssociationsList.setItemsPerPage(15);
        expectedAssociationsList.setPageNumber(0);
        expectedAssociationsList.setTotalPages(1);
        expectedAssociationsList.setTotalResults(1);
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociationsForUserStatusAndCompany(user, List.of(StatusEnum.CONFIRMED.getValue()), 0, 15, null);

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
        final var invitationsOne = associationOne.getInvitations();

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
        Assertions.assertEquals(1, invitationsOne.size());
        Assertions.assertEquals("homer.simpson@springfield.com", invitationsOne.get(0).getInvitedBy());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.plusDays(4)), reduceTimestampResolution(invitationsOne.get(0).getInvitedAt()));
        Assertions.assertEquals("/18", associationOne.getLinks().getSelf());
    }

    @Test
    void getAssociationDetailsWithoutPathVariableReturnsNotFound() throws Exception {
        mockMvc.perform(get("/associations/")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationsDetailsWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations/{id}", "1")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationDetailsWithMalformedInputReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/associations/{id}", "$")
                .header("X-Request-Id", "theId123")
                .header("Eric-identity", "9999")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*")).andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationUserDetailsWithNonexistentIdReturnsNotFound() throws Exception {
        Mockito.doReturn(Optional.empty()).when(associationsService).findAssociationById("11");
        final var response = mockMvc.perform(get("/associations/{id}", "11")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
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
                                .header("Eric-identity", "9999")
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
        Mockito.doReturn(true).when(associationsService).associationExists("333333", "9999");

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
        Mockito.doReturn(false).when(associationsService).associationExists("000000", "000");

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
    void updateAssociationStatusForIdWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/associations/{associationId}", "18")
                        .header("Eric-identity", "9999")
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
                        .header("Eric-identity", "9999")
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
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithoutStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/associations/{associationId}", "18")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
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
                        .header("Eric-identity", "9999")
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

        final var associationZero = new AssociationDao();

        associationZero.setId("0");
        associationZero.setUserEmail("light.yagami@death.note");
        associationZero.setStatus("confirmed");

        Mockito.doReturn(Optional.of(associationZero)).when(associationsService).findAssociationDaoById("0");

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
        final var associationZero = new AssociationDao();

        associationZero.setId("0");
        associationZero.setUserEmail("light.yagami@death.note");
        associationZero.setStatus("awaiting-approval");

        Mockito.doReturn(Optional.of(associationZero)).when(associationsService).findAssociationDaoById("0");

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
        final var associationZero = new AssociationDao();

        associationZero.setId("0");
        associationZero.setUserEmail("light.yagami@death.note");
        associationZero.setStatus("awaiting-approval");

        Mockito.doReturn(Optional.of(associationZero)).when(associationsService).findAssociationDaoById("0");

        final var user = new User().userId("000").displayName("Kira");
        final var usersList = new UsersList();
        usersList.add(user);
        Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of("light.yagami@death.note"));

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
        final var associationZero = new Association()
                .id("0")
                .userEmail("light.yagami@death.note");

        Mockito.doReturn(Optional.of(associationZero)).when(associationsService).findAssociationById("0");

        Mockito.doReturn(new UsersList()).when(usersService).searchUserDetails(List.of("light.yagami@death.note"));

        mockMvc.perform(patch("/associations/{associationId}", "0")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "000")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"confirmed\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndExistingUserAndRemovedUpdatesAssociationStatus() throws Exception {
        final var associationZero = new AssociationDao();

        associationZero.setId("0");
        associationZero.setUserEmail("light.yagami@death.note");

        Mockito.doReturn(Optional.of(associationZero)).when(associationsService).findAssociationDaoById("0");

        final var user = new User().userId("000").displayName("Kira");
        final var usersList = new UsersList();
        usersList.add(user);
        Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of("light.yagami@death.note"));

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
        final var associationZero = new AssociationDao();

        associationZero.setId("0");
        associationZero.setUserEmail("light.yagami@death.note");

        Mockito.doReturn(Optional.of(associationZero)).when(associationsService).findAssociationDaoById("0");

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
    void inviteUserWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/associations/invitations")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithMalformedEricIdentityReturnsBadRequest() throws Exception {
        Mockito.doThrow(new NotFoundRuntimeException("accounts-association-api", "Not found")).when(usersService).fetchUserDetails("$$$$");

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
    void inviteUserWithNonexistentEricIdentityReturnsBadRequest() throws Exception {
        Mockito.doThrow(new NotFoundRuntimeException("accounts-association-api", "Not found")).when(usersService).fetchUserDetails("9191");

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithoutRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithoutCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
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
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"$$$$$$\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithNonexistentCompanyNumberReturnsBadRequest() throws Exception {
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");

        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");
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
                        .header("Eric-identity", "9999")
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
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");

        final var company = new CompanyDetails().companyNumber("333333").companyName("Tesco");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile("333333");

        final var usersList = new UsersList();
        usersList.add(new User().userId("8888").email("russell.howard@comedy.com"));
        Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of("russell.howard@comedy.com"));

        Mockito.doReturn(Optional.of(associationDaoOne)).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("333333", "russell.howard@comedy.com");

        final var association = new AssociationDao();
        association.setId("34");
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
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeEmailAndCompanyNumberExistsAndInviteeUserIsNotFoundDoesNotPerformSwapButDoesPerformUpdateOperation() throws Exception {
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");

        final var company = new CompanyDetails().companyNumber("333333").companyName("Tesco");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile("333333");

        Mockito.doReturn(new UsersList()).when(usersService).searchUserDetails(List.of("russell.howard@comedy.com"));

        Mockito.doReturn(Optional.of(associationDaoOne)).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("333333", "russell.howard@comedy.com");

        final var association = new AssociationDao();
        association.setId("34");
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
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberExistsDoesNotPerformSwapButDoesPerformUpdateOperation() throws Exception {
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");

        final var company = new CompanyDetails().companyNumber("333333").companyName("Tesco");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile("333333");

        final var usersList = new UsersList();
        usersList.add(new User().userId("111").email("bruce.wayne@gotham.city"));
        Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of("bruce.wayne@gotham.city"));

        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("333333", "bruce.wayne@gotham.city");

        Mockito.doReturn(Optional.of(associationDaoTwo)).when(associationsService).fetchAssociationForCompanyNumberAndUserId("333333", "111");

        final var association = new AssociationDao();
        association.setId("35");
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
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberDoesNotExistCreatesNewAssociation() throws Exception {
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");

        final var company = new CompanyDetails().companyNumber("444444").companyName("Sainsbury's");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile("444444");

        final var usersList = new UsersList();
        usersList.add(new User().userId("111").email("bruce.wayne@gotham.city"));
        Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of("bruce.wayne@gotham.city"));

        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("444444", "bruce.wayne@gotham.city");

        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserId("444444", "111");

        final var association = new AssociationDao();
        association.setId("99");
        Mockito.doReturn(association).when(associationsService).createAssociation("444444", "111", null, ApprovalRouteEnum.INVITATION, "9999");

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeUserEmailAndCompanyNumberDoesNotExistAndInviteeUserIsNotFoundCreatesNewAssociation() throws Exception {
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");

        final var company = new CompanyDetails().companyNumber("333333").companyName("Tesco");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile("333333");

        Mockito.doReturn(new UsersList()).when(usersService).searchUserDetails(List.of("madonna@singer.com"));

        Mockito.doReturn(Optional.empty()).when(associationsService).fetchAssociationForCompanyNumberAndUserEmail("333333", "madonna@singer.com");

        final var association = new AssociationDao();
        association.setId("99");
        Mockito.doReturn(association).when(associationsService).createAssociation("333333", null, "madonna@singer.com", ApprovalRouteEnum.INVITATION, "9999");

        mockMvc.perform(post("/associations/invitations")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"madonna@singer.com\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeUserEmailAndCompanyNumberExistAndInviteeUserIsFoundThrowsBadRequest() throws Exception {
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");

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
    void sendInvitationEmailToAssociatedUsersWhenAssociationWithUserEmailIsPresent() throws Exception{
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName("Scrooge McDuck");
        Mockito.doReturn(user).when(usersService).fetchUserDetails("9999");
        assertEquals("Scrooge McDuck", user.getDisplayName());

        final var company = new CompanyDetails().companyNumber("333333").companyName("Tesco");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile("333333");

        Supplier<User> userSupplier = () -> new User().userId("9999");
        Mockito.doReturn( List.of( userSupplier ) ).when( emailService ).createRequestsToFetchAssociatedUsers( "333333" );

        final var usersList = new UsersList();
        usersList.add(new User().userId("111").email("bruce.wayne@gotham.city").displayName("Bruce Wayne"));
        Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of("bruce.wayne@gotham.city"));

        final var inviteeDisplayName = usersList.getFirst().getDisplayName();
        assertEquals("Bruce Wayne", inviteeDisplayName);

        final var response =mockMvc.perform(post("/associations/invitations")
                .header("X-Request-Id", "theId123")
                .header("Eric-identity", "9999")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isOk());


    }
}