package uk.gov.companieshouse.accounts.association.integration;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.service.client.CompanyClient;
import uk.gov.companieshouse.accounts.association.service.client.UserClient;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class AssociationsListForCompanyControllerITest extends AbstractBaseIntegrationITest {

    @Autowired
    private AssociationsRepository associationsRepository;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private UsersService usersService;

    // Mock Kafka
    // TODO: Replace with testcontainer instance
    @MockitoBean
    private KafkaProducerFactory kafkaProducerFactory;
    @MockitoBean
    private EmailProducer emailProducer;

    // Mock external service client layer
    @MockitoBean
    private CompanyClient companyClient;
    @MockitoBean
    private UserClient userClient;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    private final LocalDateTime now = LocalDateTime.now();
    private final String ASSOCIATIONS_COMPANIES_URL = "/associations/companies/";

    @Test
    void getAssociationsForCompanyWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + "$$$$$$")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "111")
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyWithNonexistentCompanyReturnsForbidden() throws Exception {
        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + "919191")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "111")
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAssociationsForCompanyWithoutQueryParamsUsesDefaults() throws Exception {
        final var userIds = List.of("111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777");
        final var requestingUser = userIds.getFirst();
        final var companyNumber = "111111";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17"));

        userIds.forEach(userId -> when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst()));
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber)
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, requestingUser)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);
        final var links = associationsList.getLinks();

        final var items = associationsList.getItems()
                .stream()
                .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                .toList();

        Assertions.assertTrue(items.containsAll(List.of ("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13")));
        Assertions.assertEquals(ASSOCIATIONS_COMPANIES_URL + companyNumber + "?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(15, associationsList.getItemsPerPage());
        Assertions.assertEquals(13, associationsList.getTotalResults());
        Assertions.assertEquals(1, associationsList.getTotalPages());
    }

    @Test
    void getAssociationsForCompanySupportsRequestsFromAdminUsers() throws Exception {
        final var requestingUserId = "111";
        final var targetUserId = "MKUser001";
        final var companyNumber = "MKCOMP001";
        final var associationId = "MKAssociation001";
        final var targetUserList = new UsersList();
        targetUserList.add(testDataManager.fetchUserDtos(targetUserId).getFirst());

        associationsRepository.insert(testDataManager.fetchAssociationDaos(associationId));

        when(userClient.requestUserDetailsByEmail(testDataManager.fetchAssociationDaos(associationId).getFirst().getUserEmail(), X_REQUEST_ID.value)).thenReturn(targetUserList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

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
        final var targetUserId = "MKUser001";
        final var companyNumber = "MKCOMP001";
        final var associationId = "MKAssociation001";
        final var targetUserList = new UsersList();
        targetUserList.add(testDataManager.fetchUserDtos(targetUserId).getFirst());

        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation001"));

        when(userClient.requestUserDetailsByEmail(testDataManager.fetchAssociationDaos(associationId).getFirst().getUserEmail(), X_REQUEST_ID.value)).thenReturn(targetUserList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        final var response = mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);

        final var items =
                associationsList.getItems()
                        .stream()
                        .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                        .toList();

        Assertions.assertTrue(items.contains("MKAssociation001"));
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedFalseAppliesFilter() throws Exception {
        final var userIds = List.of("111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777");
        final var requestingUserId = userIds.getFirst();
        final var companyNumber = "111111";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17"));

        userIds.forEach(userId -> when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst()));
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber + "?include_removed=false")
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, requestingUserId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                        .toList();

        Assertions.assertTrue(items.containsAll(List.of ("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13")));
        Assertions.assertEquals(ASSOCIATIONS_COMPANIES_URL + "111111?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(15, associationsList.getItemsPerPage());
        Assertions.assertEquals(13, associationsList.getTotalResults());
        Assertions.assertEquals(1, associationsList.getTotalPages());
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedTrueDoesNotApplyFilter() throws Exception {
        final var userIds = List.of("111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777");
        final var requestingUserId = userIds.getFirst();
        final var companyNumber = "111111";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17"));

        userIds.forEach(userId -> when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst()));
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber +"?include_removed=true")
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, requestingUserId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                        .toList();

        Assertions.assertTrue(items.containsAll(List.of ("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15")));
        Assertions.assertEquals(ASSOCIATIONS_COMPANIES_URL + "111111?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals(ASSOCIATIONS_COMPANIES_URL + "111111?page_index=1&items_per_page=15", links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(15, associationsList.getItemsPerPage());
        Assertions.assertEquals(16, associationsList.getTotalResults());
        Assertions.assertEquals(2, associationsList.getTotalPages());
    }

    @Test
    void getAssociationsForCompanyPaginatesCorrectly() throws Exception {
        final var userIds = List.of("111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777");
        final var requestingUserId = userIds.getFirst();
        final var companyNumber = "111111";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17"));

        userIds.forEach(userId -> when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst()));
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber +"?include_removed=true&items_per_page=3&page_index=2")
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, requestingUserId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                        .toList();

        Assertions.assertTrue(items.containsAll(List.of ("7", "8", "9")));
        Assertions.assertEquals(ASSOCIATIONS_COMPANIES_URL + "111111?page_index=2&items_per_page=3", links.getSelf());
        Assertions.assertEquals(ASSOCIATIONS_COMPANIES_URL + "111111?page_index=3&items_per_page=3", links.getNext());
        Assertions.assertEquals(2, associationsList.getPageNumber());
        Assertions.assertEquals(3, associationsList.getItemsPerPage());
        Assertions.assertEquals(16, associationsList.getTotalResults());
        Assertions.assertEquals(6, associationsList.getTotalPages());
    }

    @Test
    void getAssociationsForCompanyWhereAccountsUserEndpointCannotFindUserReturnsNotFound() throws Exception {
        final var requestingUserId = "9999";
        final var targetUserId = "111";
        final var companyNumber = "111111";
        final var associations = testDataManager.fetchAssociationDaos("1");

        associations.add(testDataManager.fetchAssociationDaos("18").getFirst().companyNumber(companyNumber));

        associationsRepository.insert(associations);

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetails(targetUserId, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("User not found"));
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationsForCompanyWhereCompanyProfileEndpointCannotFindCompanyReturnsNotFound() throws Exception {
        final var requestingUserId = "111";
        final var companyNumber = "111111";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("1"));

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("Comapny not found"));

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationsForCompanyDoesMappingCorrectly() throws Exception {
        final var userIds = List.of("111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777");
        final var requestingUserId = userIds.getFirst();
        final var companyNumber = "111111";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17"));

        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());
        userIds.forEach(userId -> when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst()));

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber +"?include_removed=true&items_per_page=2&page_index=0")
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, requestingUserId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);

        final var associations = associationsList.getItems();
        final var associationOne = associations.getFirst();

        Assertions.assertEquals("a", associationOne.getEtag());
        Assertions.assertEquals("1", associationOne.getId());
        Assertions.assertEquals("111", associationOne.getUserId());
        Assertions.assertEquals("bruce.wayne@gotham.city", associationOne.getUserEmail());
        Assertions.assertEquals("Batman", associationOne.getDisplayName());
        Assertions.assertEquals("111111", associationOne.getCompanyNumber());
        Assertions.assertEquals("Wayne Enterprises", associationOne.getCompanyName());
        Assertions.assertEquals(StatusEnum.CONFIRMED, associationOne.getStatus());
        Assertions.assertNotNull(associationOne.getCreatedAt());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.plusDays(1)), localDateTimeToNormalisedString(associationOne.getApprovedAt().toLocalDateTime()));
        Assertions.assertEquals(localDateTimeToNormalisedString(now.plusDays(2)), localDateTimeToNormalisedString(associationOne.getRemovedAt().toLocalDateTime()));
        String DEFAULT_KIND = "association";
        Assertions.assertEquals(DEFAULT_KIND, associationOne.getKind());
        Assertions.assertEquals(ApprovalRouteEnum.AUTH_CODE, associationOne.getApprovalRoute());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.plusDays(3)), reduceTimestampResolution(associationOne.getApprovalExpiryAt()));
        Assertions.assertEquals("/associations/1", associationOne.getLinks().getSelf());

        final var associationTwo = associations.get(1);
        Assertions.assertEquals("222", associationTwo.getUserId());
        String DEFAULT_DISPLAY_NAME = "Not provided";
        Assertions.assertEquals(DEFAULT_DISPLAY_NAME, associationTwo.getDisplayName());
        Assertions.assertEquals("Wayne Enterprises", associationTwo.getCompanyName());
    }

    @Test
    void getAssociationsForCompanyWithUnacceptablePaginationParametersShouldReturnBadRequest() throws Exception {
        final var companyNumber = "111111";
        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber + "?include_removed=true&items_per_page=1&page_index=-1")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "111")
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber + "?include_removed=true&items_per_page=0&page_index=0")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "111")
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyFetchesAssociation() throws Exception {
        final var userIds = List.of("111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777");
        final var requestingUserId = userIds.getFirst();
        final var companyNumber = "111111";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17"));

        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());
        userIds.forEach(userId -> when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst()));

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + companyNumber)
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, requestingUserId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);

        Assertions.assertEquals(13, associationsList.getTotalResults());
    }

    @Test
    void getAssociationsForCompanyCanRetrieveMigratedAssociations() throws Exception {
        final var userIds = List.of("MKUser001", "MKUser002", "MKUser003");
        final var requestingUserId = userIds.get(1);
        final var companyNumber = "MKCOMP001";
        final var userWithoutIdOnAssociation = testDataManager.fetchUserDtos(userIds.getFirst()).getFirst();
        final var usersList = new UsersList();
        usersList.add(userWithoutIdOnAssociation);


        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation001", "MKAssociation002", "MKAssociation003"));

        userIds.forEach(userId -> when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst()));
        when(userClient.requestUserDetailsByEmail(userWithoutIdOnAssociation.getEmail(), X_REQUEST_ID.value)).thenReturn(usersList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

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
        associationsRepository.insert(testDataManager.fetchAssociationDaos("1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17"));
//        mockers.mockUsersServiceFetchUserDetails("111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777", "MKUser001");
// mockers.mockUsersServiceFetchUserDetailsmockCompanyServiceFetchCompanyProfile("111111");

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES_URL + "111111")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "MKUser001")
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAssociationsForCompanyUserAndStatusWithUserIdFetchesAssociations() throws Exception {
        final var targetedUserId = "MKUser002";
        final var companyNumber = "MKCOMP001";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation002"));

        when(userClient.requestUserDetails(targetedUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(targetedUserId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

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
        final var targetedUser = "MKUser002";
        final var companyNumber = "MKCOMP001";
        final var userWithoutIdOnAssociation = testDataManager.fetchUserDtos(targetedUser).getFirst();
        final var usersList = new UsersList();
        usersList.add(userWithoutIdOnAssociation);

        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation002"));

        when(userClient.requestUserDetailsByEmail(userWithoutIdOnAssociation.getEmail(), X_REQUEST_ID.value)).thenReturn(usersList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

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
        final var companyNumber = "MKCOMP001";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation001"));

        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());
// mockers.mockUsersServiceFetchUserDetailsmockCompanyServiceFetchCompanyProfile("MKCOMP001");
//        Mockito.doReturn(null).when(usersService).retrieveUserDetails(null, "mario@mushroom.kingdom");

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
        mockMvc.perform(post(ASSOCIATIONS_COMPANIES_URL + String.format("%s/search", companyNumber))
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
                        .content("{\"user_email\":\"$\\() {}$$$$@mushroomkingdom\" }"))
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
                        .content("\"user_email\":\"$$$$$@mushroomkingdom\" ,\"status\":[\"confirmed\", \"removed\"]} "))
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
        final var targetedUserId = "MKUser002";
        final var companyNumber = "MKCOMP001";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation002"));

        when(userClient.requestUserDetails(targetedUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(targetedUserId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("Company not found"));

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
        final var targetedUserId = "MKUser002";
        final var companyNumber = "MKCOMP001";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation002"));

        when(userClient.requestUserDetails(targetedUserId, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("User not found"));
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

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
        final var companyNumber = "MKCOMP001";

        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(post(ASSOCIATIONS_COMPANIES_URL + companyNumber + "/search")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "test")
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} "))
                .andExpect(status().isNotFound());
    }

    @AfterEach
    public void after() {
        associationsRepository.deleteAll();
    }

}