package uk.gov.companieshouse.accounts.association.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.*;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private Association associationOne;
    private Association associationTwo;

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
        assertEquals("{\"errors\":[{\"error\":\"Eric id is not valid\",\"location\":\"accounts_association_api\",\"location_type\":\"request-body\",\"type\":\"ch:validation\"}]}",
                response.getResponse().getContentAsString());

    }

    @Test
    void fetchAssociationsByTestShouldReturnEmptyDataWhenNoAssociationsFoundForEricIdentity() throws Exception {
        when(usersService.fetchUserDetails("abcd12345")).thenReturn(new User("abc", "abc@abc.com").userId("abcd12345"));
        var response = mockMvc.perform(get("/associations")
                .header("Eric-identity", "abcd12345")
                .header("X-Request-Id", "theId")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*")).andExpect(status().is2xxSuccessful()).andReturn();
        assertEquals(0, response.getResponse().getContentLength());
    }

    @Test
    void fetchAssociationsByTestShouldReturnDataWhenAssociationsFoundForEricIdentity() throws Exception {
        User user = new User("abc", "abc@abc.com").userId("abcd12345");
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

        String error = "{\"errors\":[{\"error\":\"Please check the request and try again\",\"location\":\"accounts_association_api\",\"location_type\":\"request-body\",\"type\":\"ch:validation\"}]}";
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
    void getAssociationUserDetailsWithNonexistentUIdReturnsNotFound() throws Exception {
        Mockito.doThrow(new NotFoundRuntimeException("user-company-association-api", "Not found")).when(associationsService).findAssociationById("11");
        mockMvc.perform(get("/associations/{id}", "11")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isNotFound());
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
    void getAssociationDetailsFetchesAssociationDetailsThrowsErrorWhenEmpty() throws Exception {
        Mockito.doThrow(new NotFoundRuntimeException("user-company-association-api", "Not found")).when(associationsService).findAssociationById("12");
        mockMvc.perform(get("/associations/{id}", "12")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*"))
                .andExpect(status().isNotFound());
    }
}