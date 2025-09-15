package uk.gov.companieshouse.accounts.association.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_IDENTITY;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_IDENTITY_TYPE_API_KEY;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_IDENTITY_TYPE_OAUTH;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.X_REQUEST_ID;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.service.client.CompanyClient;
import uk.gov.companieshouse.accounts.association.service.client.UserClient;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.ResponseBodyPost;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@ComponentScan(basePackages = "uk.gov.companieshouse.email_producer")
class UserCompanyAssociationsControllerTest extends AbstractBaseIntegrationTest {

    @Value("${invitation.url}")
    private String COMPANY_INVITATIONS_URL;

    @Autowired
    private StaticPropertyUtil staticPropertyUtil;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EmailService emailService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private UsersService usersService;
    @Autowired
    private AssociationsRepository associationsRepository;

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

    private final String ASSOCIATIONS_URL = "/associations";

    private final ComparisonUtils comparisonUtils = new ComparisonUtils();
    private final TestDataManager testDataManager = TestDataManager.getInstance();
    private static final String DEFAULT_KIND = "association";
    private final LocalDateTime now = LocalDateTime.now();
    private CountDownLatch latch;

    private void setEmailProducerCountDownLatch(int countdown){
        latch = new CountDownLatch(countdown);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(emailProducer).sendEmail(any(), any());
    }

    @Test
    void fetchAssociationsByWithoutEricIdentityReturnsForbidden() throws Exception {
        mockMvc.perform(get(ASSOCIATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                .andExpect(status().isForbidden());
    }

    static Stream<Arguments> malformedQueryParametersTestData(){
        return Stream.of(
                Arguments.of("/associations?page_index=-1"),
                Arguments.of("/associations?items_per_page=0"),
                Arguments.of("/associations?company_number=$$$$$$"),
                Arguments.of("/associations/$"),
                Arguments.of("/associations/invitations?page_index=-1&items_per_page=1"),
                Arguments.of("/associations/invitations?page_index=0&items_per_page=-1")
      );
    }

    @ParameterizedTest
    @MethodSource("malformedQueryParametersTestData")
    void endpointsWithMalformedQueryParametersReturnBadRequest(final String uri) throws Exception {
        final var userId = "9999";

        mockMvc.perform(get(uri)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fetchAssociationsByWithNonExistentUserReturnsForbidden() throws Exception {
        final var userId = "9191";

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(null);

        mockMvc.perform(get(ASSOCIATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                .andExpect(status().isForbidden());
    }

    @Test
    void fetchAssociationsByWithNonexistentCompanyReturnsNotFound() throws Exception {
        final var userId = "8888";
        final var associationId = "17";
        final var companyNumber = "222222";

        associationsRepository.insert(testDataManager.fetchAssociationDaos(associationId));
        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("Company not found", new Exception()));

        mockMvc.perform(get(ASSOCIATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                .andExpect(status().isNotFound());
    }

    @Test
    void fetchAssociationsByWithInvalidStatusReturnsZeroResults() throws Exception {
        final var userId = "9999";
        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());

        mockMvc.perform(get(ASSOCIATIONS_URL + "?status=$$$$")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fetchAssociationsByUsesDefaultsIfValuesAreNotProvided() throws Exception {
        final var userId = "9999";
        final var companyNumbers = List.of("333333", "444444", "555555", "666666", "777777");

        associationsRepository.insert(testDataManager.fetchAssociationDaos("17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33"));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        companyNumbers.forEach(companyNumber -> when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst()));

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_URL)
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, userId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                                .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                        .toList();

        Assertions.assertTrue(items.containsAll(List.of ("18", "19", "20", "21", "22")));
        Assertions.assertEquals("/associations?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(15, associationsList.getItemsPerPage());
        Assertions.assertEquals(5, associationsList.getTotalResults());
        Assertions.assertEquals(1, associationsList.getTotalPages());
    }

    @Test
    void fetchAssociationsByWithOneStatusAppliesStatusFilterCorrectly() throws Exception {
        final var userId = "9999";
        final var companyNumbers = List.of("x777777", "x888888", "x999999");

        associationsRepository.insert(testDataManager.fetchAssociationDaos("17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33"));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        companyNumbers.forEach(companyNumber -> when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst()));

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_URL + "?status=removed")
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, userId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                                .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                        .toList();

        Assertions.assertTrue(items.containsAll(List.of ("31", "32", "33")));
        Assertions.assertEquals("/associations?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(15, associationsList.getItemsPerPage());
        Assertions.assertEquals(3, associationsList.getTotalResults());
        Assertions.assertEquals(1, associationsList.getTotalPages());
    }

    @Test
    void fetchAssociationsByWithMultipleStatusesAppliesStatusFilterCorrectly() throws Exception {
        final var userId = "9999";
        final var companyNumbers = List.of("333333", "444444", "555555", "666666", "777777", "x777777", "x888888", "x999999");

        associationsRepository.insert(testDataManager.fetchAssociationDaos("17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33"));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        companyNumbers.forEach(companyNumber -> when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst()));

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_URL + "?status=confirmed&status=removed")
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, userId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                                .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                        .toList();

        Assertions.assertTrue(items.containsAll(List.of ("18", "19", "20", "21", "22", "31", "32", "33")));
        Assertions.assertEquals("/associations?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(15, associationsList.getItemsPerPage());
        Assertions.assertEquals(8, associationsList.getTotalResults());
        Assertions.assertEquals(1, associationsList.getTotalPages());
    }

    @Test
    void fetchAssociationsByImplementsPaginationCorrectly() throws Exception {
        final var userId = "9999";
        final var companyNumbers = List.of("999999", "x111111", "x222222");

        associationsRepository.insert(testDataManager.fetchAssociationDaos("17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33"));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        companyNumbers.forEach(companyNumber -> when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst()));

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_URL +"?status=confirmed&status=awaiting-approval&status=removed&page_index=2&items_per_page=3")
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, userId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                                .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map(uk.gov.companieshouse.api.accounts.associations.model.Association::getId)
                        .toList();

        Assertions.assertTrue(items.containsAll(List.of ("24", "25", "26")));
        Assertions.assertEquals("/associations?page_index=2&items_per_page=3", links.getSelf());
        Assertions.assertEquals("/associations?page_index=3&items_per_page=3", links.getNext());
        Assertions.assertEquals(2, associationsList.getPageNumber());
        Assertions.assertEquals(3, associationsList.getItemsPerPage());
        Assertions.assertEquals(16, associationsList.getTotalResults());
        Assertions.assertEquals(6, associationsList.getTotalPages());
    }

    @Test
    void fetchAssociationsByFiltersBasedOnCompanyNumberCorrectly() throws Exception {
        final var userId = "9999";
        final var companyNumber = "333333";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33"));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_URL + "?company_number=" + companyNumber)
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, userId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                                .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);
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
    void fetchAssociationsByDoesMappingCorrectly() throws Exception {
        final var userId = "9999";
        final var companyNumber = "333333";

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        associationsRepository.insert(testDataManager.fetchAssociationDaos("18"));

        final var response =
                mockMvc.perform(get( ASSOCIATIONS_URL + "?company_number=" + companyNumber)
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, userId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                                .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                        .andExpect(status().isOk());


        final var associationsList = parseResponseTo(response, AssociationsList.class);
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
        Assertions.assertEquals("/associations/18", associationOne.getLinks().getSelf());
    }

    @Test
    void fetchAssociationsByCanFetchMigratedAssociation() throws Exception {
        final var userId = "MKUser001";
        final var companyNumber = "MKCOMP001";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation001"));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_URL + "?status=" + StatusEnum.MIGRATED)
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, userId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);
        final var associations = associationsList.getItems();
        final var associationOne = associations.getFirst();

        Assertions.assertEquals("MKAssociation001", associationOne.getId());
        Assertions.assertEquals("migrated", associationOne.getStatus().getValue());
        Assertions.assertEquals("migration", associationOne.getApprovalRoute().getValue());
    }

    @Test
    void fetchAssociationsByCanFetchUnauthorisedAssociation() throws Exception {
        final var userId = "MKUser004";
        final var companyNumber = "MKCOMP001";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation004"));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());


        final var response =
                mockMvc.perform(get(ASSOCIATIONS_URL + "?status=" + StatusEnum.UNAUTHORISED)
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, userId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo(response, AssociationsList.class);
        final var associations = associationsList.getItems();
        final var associationOne = associations.getFirst();

        Assertions.assertEquals("MKAssociation004", associationOne.getId());
        Assertions.assertEquals(StatusEnum.UNAUTHORISED, associationOne.getStatus());
        Assertions.assertNotNull(associationOne.getUnauthorisedAt());
    }

    @Test
    void addAssociationWithoutEricIdentityReturnsForbidden() throws Exception {
        mockMvc.perform(post(ASSOCIATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"111111\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addAssociationWithoutRequestBodyReturnsBadRequest() throws Exception {
        final var userId = "9999";
        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());

        mockMvc.perform(post(ASSOCIATIONS_URL)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addAssociationWithEmptyBodyReturnsBadRequest() throws Exception {
        final var userId = "9999";

        mockMvc.perform(post(ASSOCIATIONS_URL)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addAssociationWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        final var userId = "9999";

        mockMvc.perform(post(ASSOCIATIONS_URL)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"$$$$$$\", \"user_id\":\"9999\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addAssociationWithMalformedUserIdReturnsBadRequest() throws Exception {
        final var userId = "9999";

        mockMvc.perform(post(ASSOCIATIONS_URL)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\", \"user_id\":\"$$$$\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addAssociationWithExistingAssociationReturnsBadRequest() throws Exception {
        final var userId = "9999";
        final var companyNumber = "333333";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("18"));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(post(ASSOCIATIONS_URL)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\", \"user_id\":\"9999\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addAssociationWithNonexistentCompanyNumberReturnsNotFound() throws Exception {
        final var userId = "9999";
        final var companyNumber = "919191";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("18"));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("No company found", new Exception()));

        mockMvc.perform(post(ASSOCIATIONS_URL)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"919191\", \"user_id\":\"9999\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addAssociationCreatesNewAssociationCorrectlyAndReturnsAssociationIdWithCreatedHttpStatus() throws Exception {
        final var userId = "9999";
        final var companyNumber = "111111";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("18"));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        final var responseJson =
                mockMvc.perform(post(ASSOCIATIONS_URL)
                                .header(ERIC_IDENTITY.key, userId)
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                                .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"company_number\":\"111111\", \"user_id\":\"9999\"}"))
                        .andExpect(status().isCreated());

        final var response = parseResponseTo(responseJson, ResponseBodyPost.class);

        final var associationLink = response.getAssociationLink();
        final var associationOptional = associationsRepository.findById(associationLink.substring(associationLink.lastIndexOf("/")+1));
        Assertions.assertTrue(associationOptional.isPresent());

        final var association = associationOptional.get();
        Assertions.assertEquals("111111", association.getCompanyNumber());
        Assertions.assertEquals("9999", association.getUserId());
        Assertions.assertEquals(StatusEnum.CONFIRMED.getValue(), association.getStatus());
        Assertions.assertEquals(ApprovalRouteEnum.AUTH_CODE.getValue(), association.getApprovalRoute());
        Assertions.assertNotNull(association.getEtag());
    }

    @Test
    void addAssociationWithNonExistentUserReturnsNotFound() throws Exception {
        final var userId = "9191";

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("User not found", new Exception()));

        mockMvc.perform(post(ASSOCIATIONS_URL)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"111111\", \"user_id\":\"9191\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addAssociationWithUserThatHasDisplayNameUsesDisplayName() throws Exception {
        final var requestingUserId = "111";
        final var targetUserId = "9999";
        final var companyNumber = "444444";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("19"));

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetails(targetUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(targetUserId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(1);

        mockMvc.perform(post(ASSOCIATIONS_URL)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\", \"user_id\":\"111\"}"))
                .andExpect(status().isCreated());

        latch.await(10, TimeUnit.SECONDS);
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.authCodeConfirmationEmailMatcher("scrooge.mcduck@disney.land", "Sainsbury's", "Batman")), eq(AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue()));
    }

    @Test
    void addAssociationReturnsCreatedWhenEmailIsNotSentOut() throws Exception {
        final var requestingUserId = "111";
//        final var targetUserId = "9999";
        final var companyNumber = "444444";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("19"));

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(post(ASSOCIATIONS_URL)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\", \"user_id\":\"111\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void addAssociationCanBeAppliedToMigratedAssociation() throws Exception {
        final var requestingUserId = "MKUser001";
        final var targetUserId = "MKUser002";
        final var companyNumber = "MKCOMP001";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation001", "MKAssociation002"));

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetails(targetUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(targetUserId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(1);

        mockMvc.perform(post(ASSOCIATIONS_URL)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"MKCOMP001\", \"user_id\":\"MKUser001\"}"))
                .andExpect(status().isCreated());

        final var updatedAssociation = associationsRepository.findById("MKAssociation001").get();

        Assertions.assertEquals("confirmed", updatedAssociation.getStatus());
        Assertions.assertEquals("auth_code", updatedAssociation.getApprovalRoute());
        Assertions.assertNotNull(updatedAssociation.getEtag());
        Assertions.assertEquals(1, updatedAssociation.getPreviousStates().size());
        Assertions.assertEquals("migrated", updatedAssociation.getPreviousStates().getFirst().getStatus());
        Assertions.assertEquals(COMPANIES_HOUSE, updatedAssociation.getPreviousStates().getFirst().getChangedBy());
        Assertions.assertNotNull(updatedAssociation.getPreviousStates().getFirst().getChangedAt());

        latch.await(10, TimeUnit.SECONDS);
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.authCodeConfirmationEmailMatcher("luigi@mushroom.kingdom", "Mushroom Kingdom", "Mario")), eq(AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue()));
    }

    @AfterEach
    public void after() {
        associationsRepository.deleteAll();
    }
}