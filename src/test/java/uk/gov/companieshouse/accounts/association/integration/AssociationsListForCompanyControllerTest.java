package uk.gov.companieshouse.accounts.association.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.configuration.InterceptorConfig;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.accounts.association.rest.CompanyProfileEndpoint;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;

@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class AssociationsListForCompanyControllerTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyProfileEndpoint companyProfileEndpoint;

    @MockBean
    private AccountsUserEndpoint accountsUserEndpoint;

    @MockBean
    private EmailProducer emailProducer;

    @MockBean
    private KafkaProducerFactory kafkaProducerFactory;

    @Autowired
    private AssociationsRepository associationsRepository;

    @MockBean
    private InterceptorConfig interceptorConfig;

    private static final String DEFAULT_KIND = "association";
    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    private final LocalDateTime now = LocalDateTime.now();

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private Mockers mockers;

    @BeforeEach
    void setup(){
        mockers = new Mockers( accountsUserEndpoint, companyProfileEndpoint, emailProducer, null, null );
    }

    @Test
    void getAssociationsForCompanyWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/companies/$$$$$$" )
                        .header("X-Request-Id", "theId123") )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyWithNonexistentCompanyReturnsNotFound() throws Exception {
        mockers.mockFetchCompanyProfileNotFound( "919191" );
        mockMvc.perform( get( "/associations/companies/919191" )
                        .header("X-Request-Id", "theId123") )
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationsForCompanyWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/companies/111111" ) )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyWithoutQueryParamsUsesDefaults() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
        mockers.mockGetUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
        mockers.mockFetchCompanyProfile( "111111" );

        final var response =
                mockMvc.perform( get( "/associations/companies/111111" )
                                .header("X-Request-Id", "theId123") )
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items = associationsList.getItems()
                .stream()
                .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" ) ) );
        Assertions.assertEquals( "/associations/companies/111111?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 13, associationsList.getTotalResults() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedFalseAppliesFilter() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
        mockers.mockGetUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
        mockers.mockFetchCompanyProfile( "111111" );

        final var response =
                mockMvc.perform( get( "/associations/companies/111111?include_removed=false" )
                                .header("X-Request-Id", "theId123") )
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" ) ) );
        Assertions.assertEquals( "/associations/companies/111111?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 13, associationsList.getTotalResults() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedTrueDoesNotApplyFilter() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
        mockers.mockGetUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
        mockers.mockFetchCompanyProfile( "111111" );

        final var response =
                mockMvc.perform( get( "/associations/companies/111111?include_removed=true" )
                                .header("X-Request-Id", "theId123") )
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15" ) ) );
        Assertions.assertEquals( "/associations/companies/111111?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "/associations/companies/111111?page_index=1&items_per_page=15", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 16, associationsList.getTotalResults() );
        Assertions.assertEquals( 2, associationsList.getTotalPages() );
    }

    @Test
    void getAssociationsForCompanyPaginatesCorrectly() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
        mockers.mockGetUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
        mockers.mockFetchCompanyProfile( "111111" );

        final var response =
                mockMvc.perform( get( "/associations/companies/111111?include_removed=true&items_per_page=3&page_index=2" )
                                .header("X-Request-Id", "theId123") )
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "7", "8", "9" ) ) );
        Assertions.assertEquals( "/associations/companies/111111?page_index=2&items_per_page=3", links.getSelf() );
        Assertions.assertEquals( "/associations/companies/111111?page_index=3&items_per_page=3", links.getNext() );
        Assertions.assertEquals( 2, associationsList.getPageNumber() );
        Assertions.assertEquals( 3, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 16, associationsList.getTotalResults() );
        Assertions.assertEquals( 6, associationsList.getTotalPages() );
    }

    @Test
    void getAssociationsForCompanyWhereAccountsUserEndpointCannotFindUserReturnsNotFound() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        mockers.mockGetUserDetailsNotFound( "111" );
        mockers.mockFetchCompanyProfile( "111111" );

        mockMvc.perform( get( "/associations/companies/111111" )
                        .header("X-Request-Id", "theId123") )
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationsForCompanyWhereCompanyProfileEndpointCannotFindCompanyReturnsNotFound() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        mockers.mockGetUserDetails( "111" );
        mockers.mockFetchCompanyProfileNotFound( "111111" );

        mockMvc.perform( get( "/associations/companies/111111" )
                        .header("X-Request-Id", "theId123") )
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationsForCompanyDoesMappingCorrectly() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
        mockers.mockGetUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
        mockers.mockFetchCompanyProfile( "111111" );

        final var response =
                mockMvc.perform( get( "/associations/companies/111111?include_removed=true&items_per_page=2&page_index=0" )
                                .header("X-Request-Id", "theId123") )
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );

        final var associations = associationsList.getItems();
        final var associationOne = associations.getFirst();

        Assertions.assertEquals( "a", associationOne.getEtag() );
        Assertions.assertEquals( "1", associationOne.getId() );
        Assertions.assertEquals( "111", associationOne.getUserId() );
        Assertions.assertEquals( "bruce.wayne@gotham.city", associationOne.getUserEmail() );
        Assertions.assertEquals( "Batman", associationOne.getDisplayName() );
        Assertions.assertEquals( "111111", associationOne.getCompanyNumber() );
        Assertions.assertEquals( "Wayne Enterprises", associationOne.getCompanyName() );
        Assertions.assertEquals( StatusEnum.CONFIRMED, associationOne.getStatus() );
        Assertions.assertNotNull( associationOne.getCreatedAt() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(1) ), localDateTimeToNormalisedString( associationOne.getApprovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(2) ), localDateTimeToNormalisedString( associationOne.getRemovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( DEFAULT_KIND, associationOne.getKind() );
        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE, associationOne.getApprovalRoute() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(3) ), reduceTimestampResolution( associationOne.getApprovalExpiryAt() ) );
        Assertions.assertEquals( "/associations/1", associationOne.getLinks().getSelf() );

        final var associationTwo = associations.get( 1 );
        Assertions.assertEquals( "222", associationTwo.getUserId() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, associationTwo.getDisplayName() );
        Assertions.assertEquals( "Wayne Enterprises", associationTwo.getCompanyName() );
    }

    @Test
    void getAssociationsForCompanyWithUnacceptablePaginationParametersShouldReturnBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/companies/111111?include_removed=true&items_per_page=1&page_index=-1" )
                        .header("X-Request-Id", "theId123") )
                .andExpect(status().isBadRequest());

        mockMvc.perform( get( "/associations/companies/111111?include_removed=true&items_per_page=0&page_index=0" )
                        .header("X-Request-Id", "theId123") )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyFetchesAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
        mockers.mockGetUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
        mockers.mockFetchCompanyProfile( "111111" );

        final var response =
        mockMvc.perform( get( "/associations/companies/111111" )
                        .header("X-Request-Id", "theId123") )
                .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );

        Assertions.assertEquals( 13, associationsList.getTotalResults() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }

}