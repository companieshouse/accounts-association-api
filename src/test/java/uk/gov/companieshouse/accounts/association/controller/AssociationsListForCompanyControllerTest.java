package uk.gov.companieshouse.accounts.association.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.accounts.association.configuration.InterceptorConfig;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.*;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.company.CompanyDetails;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(AssociationsListForCompanyController.class)
@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
 class AssociationsListForCompanyControllerTest {

    @Value("${internal.api.url}")
    private String internalApiUrl;

    @Autowired
    public MockMvc mockMvc;

    @MockBean
    AssociationsService associationsService;

    @MockBean
    CompanyService companyService;

    @MockBean
    UsersService usersService;

    @InjectMocks
    AssociationsListForCompanyController associationsListForCompanyController;

    @MockBean
    InterceptorConfig interceptorConfig;

    @MockBean
    StaticPropertyUtil staticPropertyUtil;

    private static final String DEFAULT_KIND = "association";
    private static final String DEFAULT_DISPLAY_NAME = "Not provided";

    private final LocalDateTime now = LocalDateTime.now();

    private Association associationOne;
    private Association associationTwo;


    @BeforeEach
    public void setup() {

        final var invitationOne =
                new Invitation().invitedBy( "666" )
                        .invitedAt( now.plusDays(4).toString() );

        associationOne =
                new Association().etag("a")
                        .id("1")
                        .userId("111")
                        .userEmail("bruce.wayne@gotham.city")
                        .displayName("Batman")
                        .companyNumber("111111")
                        .companyName("Wayne Enterprises")
                        .status(StatusEnum.CONFIRMED)
                        .createdAt( LocalDateTime.now().atOffset( ZoneOffset.UTC ) )
                        .approvedAt( now.plusDays(1).atOffset( ZoneOffset.UTC ) )
                        .removedAt( now.plusDays(2).atOffset( ZoneOffset.UTC ) )
                        .kind( DEFAULT_KIND )
                        .approvalRoute(ApprovalRouteEnum.AUTH_CODE)
                        .approvalExpiryAt( now.plusDays(3).toString() )
                        .links( new AssociationLinks().self( "/1" ) );

        final var invitationTwo =
                new Invitation().invitedBy( "666" )
                        .invitedAt( now.plusDays(8).toString() );

        associationTwo =
                new Association().etag("b")
                        .id("2")
                        .userId("222")
                        .userEmail("the.joker@gotham.city")
                        .displayName(DEFAULT_DISPLAY_NAME)
                        .companyNumber("111111")
                        .companyName("Wayne Enterprises")
                        .status(StatusEnum.REMOVED)
                        .createdAt( LocalDateTime.now().atOffset( ZoneOffset.UTC ) )
                        .approvedAt( now.plusDays(5).atOffset( ZoneOffset.UTC ) )
                        .removedAt( now.plusDays(6).atOffset( ZoneOffset.UTC ) )
                        .kind( DEFAULT_KIND )
                        .approvalRoute(ApprovalRouteEnum.AUTH_CODE)
                        .approvalExpiryAt( now.plusDays(7).toString() )
                        .links( new AssociationLinks().self( "/2" ) );

        Mockito.doNothing().when(interceptorConfig).addInterceptors( any() );
    }

    @Test
    void getAssociationsForCompanyWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/companies/{company_number}", "$$$$$$" ).header("X-Request-Id", "theId123") )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyWithNonexistentCompanyReturnsNotFound() throws Exception {
        Mockito.doThrow( new NotFoundRuntimeException( "accounts-association-api", "Not found" ) ).when( companyService ).fetchCompanyProfile( any() );

        mockMvc.perform( get( "/associations/companies/{company_number}", "919191" ).header("X-Request-Id", "theId123") )
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse();
    }

    @Test
    void getAssociationsForCompanyWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/companies/{company_number}", "111111" ) )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyWithoutQueryParamsUsesDefaults() throws Exception {
        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setTotalResults( 1 );
        expectedAssociationsList.setTotalPages( 1 );
        expectedAssociationsList.setPageNumber( 0 );
        expectedAssociationsList.setItemsPerPage( 15 );
        expectedAssociationsList.setLinks( new Links().self(String.format("%s/associations", internalApiUrl)).next("") );
        expectedAssociationsList.setItems(List.of( associationOne ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociatedUsers( any(), any(), eq( false ), eq( 15 ), eq( 0 ) );

        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( any() );

        mockMvc.perform( get( "/associations/companies/{company_number}", "111111" ).header("X-Request-Id", "theId123") )
                .andExpect(status().isOk());


        Mockito.verify( associationsService ).fetchAssociatedUsers(  "111111" ,  companyDetails ,  false ,  15 , 0 );
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedFalseAppliesFilter() throws Exception {
        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setTotalResults( 1 );
        expectedAssociationsList.setTotalPages( 1 );
        expectedAssociationsList.setPageNumber( 0 );
        expectedAssociationsList.setItemsPerPage( 15 );
        expectedAssociationsList.setLinks( new Links().self(String.format("%s/associations", internalApiUrl)).next("") );
        expectedAssociationsList.setItems(List.of( associationOne ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociatedUsers( any(), any(), eq( false ), eq( 15 ), eq( 0 ) );

        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( any() );

        mockMvc.perform( get( "/associations/companies/{company_number}?include_removed=false", "111111" ).header("X-Request-Id", "theId123") )
                .andExpect(status().isOk());

        Mockito.verify( associationsService ).fetchAssociatedUsers(  "111111" , companyDetails , false , 15 , 0 );
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedTrueDoesNotApplyFilter() throws Exception {
        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setTotalResults( 2 );
        expectedAssociationsList.setTotalPages( 1 );
        expectedAssociationsList.setPageNumber( 0 );
        expectedAssociationsList.setItemsPerPage( 15 );
        expectedAssociationsList.setLinks( new Links().self(String.format("%s/associations", internalApiUrl)).next("") );
        expectedAssociationsList.setItems(List.of( associationOne, associationTwo ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociatedUsers( any(), any(), eq( true ), eq( 15 ), eq( 0 ) );

        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( any() );

        mockMvc.perform( get( "/associations/companies/{company_number}?include_removed=true", "111111" ).header("X-Request-Id", "theId123") )
                .andExpect(status().isOk());

        Mockito.verify( associationsService ).fetchAssociatedUsers( "111111" ,  companyDetails, true , 15 , 0 );
    }

    @Test
    void getAssociationsForCompanyPaginatesCorrectly() throws Exception {
        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setTotalResults( 2 );
        expectedAssociationsList.setTotalPages( 2 );
        expectedAssociationsList.setPageNumber( 1 );
        expectedAssociationsList.setItemsPerPage( 1 );
        expectedAssociationsList.setLinks( new Links().self(String.format("%s/associations", internalApiUrl)).next("") );
        expectedAssociationsList.setItems(List.of( associationTwo ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociatedUsers( any(), any(), eq( true ), eq( 1 ), eq( 1 ) );

        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( any() );

        final var response =
                mockMvc.perform( get( "/associations/companies/{company_number}?include_removed=true&items_per_page=1&page_index=1", "111111" ).header("X-Request-Id", "theId123") )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        final AssociationsList associationsList = objectMapper.readValue( response.getContentAsByteArray(), AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( Association::getId )
                        .toList();

        Assertions.assertTrue( items.contains( "2" ) );
        Assertions.assertEquals( String.format("%s/associations", internalApiUrl), links.getSelf() );
        Assertions.assertEquals( String.format( "" ), links.getNext() );
        Assertions.assertEquals( 1, associationsList.getPageNumber() );
        Assertions.assertEquals( 1, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 2, associationsList.getTotalResults() );
        Assertions.assertEquals( 2, associationsList.getTotalPages() );
    }










    private String reduceTimestampResolution( String timestamp ){
        return timestamp.substring( 0, timestamp.indexOf( ":" ) );
    }

    private String localDateTimeToNormalisedString( LocalDateTime localDateTime ){
        final var timestamp = localDateTime.toString();
        return reduceTimestampResolution( timestamp );
    }

    @Test
    void getAssociationsForCompanyDoesMappingCorrectly() throws Exception {

        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setTotalResults( 2 );
        expectedAssociationsList.setTotalPages( 1 );
        expectedAssociationsList.setPageNumber( 0 );
        expectedAssociationsList.setItemsPerPage( 2 );
        expectedAssociationsList.setLinks( new Links().self(String.format("%s/associations", internalApiUrl)).next("") );
        expectedAssociationsList.setItems(List.of( associationOne, associationTwo ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociatedUsers( any(), any(), eq( true ), eq( 2 ), eq( 0 ) );

        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( any() );

        final var response =
                mockMvc.perform( get( "/associations/companies/{company_number}?include_removed=true&items_per_page=2&page_index=0", "111111" ).header("X-Request-Id", "theId123") )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        final AssociationsList associationsList = objectMapper.readValue( response.getContentAsByteArray(), AssociationsList.class );

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
        Assertions.assertEquals( "/1", associationOne.getLinks().getSelf() );

        final var associationTwo = associations.get( 1 );
        Assertions.assertEquals( "222", associationTwo.getUserId() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, associationTwo.getDisplayName() );
        Assertions.assertEquals( "Wayne Enterprises", associationTwo.getCompanyName() );
    }

    @Test
    void getAssociationsForCompanyWithUnacceptablePaginationParametersShouldReturnBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/companies/{company_number}?include_removed=true&items_per_page=1&page_index=-1", "111111" ).header("X-Request-Id", "theId123") )
                .andExpect(status().isBadRequest());

        mockMvc.perform( get( "/associations/companies/{company_number}?include_removed=true&items_per_page=0&page_index=0", "111111" ).header("X-Request-Id", "theId123") )
                .andExpect(status().isBadRequest());
    }


    @Test
    void getAssociationsForCompanyFetchesAssociation() throws Exception {
        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( any() );

        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setTotalResults( 1 );
        expectedAssociationsList.setItems(List.of( associationOne ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociatedUsers( eq( "111111" ), eq( companyDetails ), eq( false ), eq( 15 ), eq( 0 ));

        final var response =
                mockMvc.perform( get( "/associations/companies/{company_number}?user_email=bruce.wayne@gotham.city", "111111" ).header("X-Request-Id", "theId123") )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        final var associationsList = objectMapper.readValue( response, AssociationsList.class );

        Assertions.assertEquals( 1, associationsList.getTotalResults() );
        Assertions.assertEquals( "1", associationsList.getItems().getFirst().getId() );
    }

    @Test
    void getAssociationsForCompanyWithNonexistentUserEmailFetchesEmptyList() throws Exception {
        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( any() );

        final var expectedAssociationsList = new AssociationsList();
        expectedAssociationsList.setTotalResults( 0 );
        expectedAssociationsList.setItems(List.of());
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchAssociatedUsers( eq( "111111" ), eq( companyDetails ), eq( false ), eq( 15 ), eq( 0 ) );

        final var response =
                mockMvc.perform( get( "/associations/companies/{company_number}?user_email=the.void@space.com", "111111" ).header("X-Request-Id", "theId123") )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        final var associationsList = objectMapper.readValue( response, AssociationsList.class );

        Assertions.assertEquals( 0, associationsList.getTotalResults() );
    }

}