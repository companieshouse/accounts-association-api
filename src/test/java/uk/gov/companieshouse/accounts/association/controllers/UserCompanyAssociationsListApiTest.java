package uk.gov.companieshouse.accounts.association.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.companieshouse.accounts.association.controller.UserCompanyAssociationsListApi;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompaniesService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.CompanyInfo;
import uk.gov.companieshouse.api.accounts.associations.model.UserCompanyAssociationsResponse;
import uk.gov.companieshouse.api.accounts.associations.model.UserInfo;

@Tag("unit-test")
@WebMvcTest(UserCompanyAssociationsListApi.class)
public class UserCompanyAssociationsListApiTest {

    @Autowired
    public MockMvc mockMvc;

    @MockBean
    UsersService usersService;

    @MockBean
    AssociationsService associationsService;

    @MockBean
    CompaniesService companiesService;

    private Association batmanConfirmedAssociation;
    private Association batmanAwaitingConfirmationAssociation;
    private Association batmanRemovedAssociation;
    private Association batmanGhostAssociation;
    private Association michaelJacksonConfirmedAssociation;
    private Association michaelJacksonRemovedAssociation;
    private Association devilRemovedAssociation;

    @BeforeEach
    public void setup(){
        batmanConfirmedAssociation = new Association();
        batmanConfirmedAssociation.setCompanyNumber( "111111" );
        batmanConfirmedAssociation.setUserId( "111" );
        batmanConfirmedAssociation.setStatus( "Confirmed" );

        batmanAwaitingConfirmationAssociation = new Association();
        batmanAwaitingConfirmationAssociation.setCompanyNumber( "222222" );
        batmanAwaitingConfirmationAssociation.setUserId( "111" );
        batmanAwaitingConfirmationAssociation.setStatus( "Awaiting Confirmation" );

        batmanRemovedAssociation = new Association();
        batmanRemovedAssociation.setCompanyNumber( "333333" );
        batmanRemovedAssociation.setUserId( "111" );
        batmanRemovedAssociation.setStatus( "Removed" );

        batmanGhostAssociation = new Association();
        batmanGhostAssociation.setCompanyNumber( "444444" );
        batmanGhostAssociation.setUserId( "111" );
        batmanGhostAssociation.setStatus( "Confirmed" );

        michaelJacksonConfirmedAssociation = new Association();
        michaelJacksonConfirmedAssociation.setCompanyNumber( "111111" );
        michaelJacksonConfirmedAssociation.setUserId( "222" );
        michaelJacksonConfirmedAssociation.setStatus( "Confirmed" );

        michaelJacksonRemovedAssociation = new Association();
        michaelJacksonRemovedAssociation.setCompanyNumber( "222222" );
        michaelJacksonRemovedAssociation.setUserId( "222" );
        michaelJacksonRemovedAssociation.setStatus( "Removed" );

        devilRemovedAssociation = new Association();
        devilRemovedAssociation.setCompanyNumber( "222222" );
        devilRemovedAssociation.setUserId( "666" );
        devilRemovedAssociation.setStatus( "Removed" );
    }

    @Test
    void getCompaniesAssociatedWithUserWithMissingPathVariablesReturnsNotFound() throws Exception {
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "" ).header( "X-Request-Id", "theId" ) ).andExpect( status().isNotFound() );
    }

    @Test
    void getCompaniesAssociatedWithUserWithMalformedOrNonexistentEmailReturnsBadRequest() throws Exception {
        Mockito.doReturn(Optional.empty()).when(usersService).fetchUserInfo( any() );

        mockMvc.perform( get( "/associations/users/{user_email}/companies", "123" ).header( "X-Request-Id", "theId" ) ).andExpect( status().isBadRequest() );
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "guybrush.threepwood@monkeyisland.com" ).header( "X-Request-Id", "theId" ) ).andExpect( status().isBadRequest() );
    }

    @Test
    void getCompaniesAssociatedWithUserWithNoAssociationsReturnsEmptyList() throws Exception {
        Mockito.doReturn(Optional.of( new UserInfo().userId("333") ) ).when(usersService).fetchUserInfo( any() );
        Mockito.doReturn( List.of() ).when(associationsService).findAllByUserId( any(), any() );

        final var response =
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "mr.blobby@nightmare.co.uk" ).queryParam( "include-unauthorized", "true" ).header( "X-Request-Id", "theId" ) )
               .andExpect( status().isOk() )
               .andReturn()
               .getResponse()
               .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        final var companies =
        mapper.readValue( response, UserCompanyAssociationsResponse.class )
              .getSuccessBody();

        Assertions.assertTrue( companies.isEmpty() );
    }

    @Test
    void getCompaniesAssociatedWithUserWithOneRemovedAssociationAndIncludeUnauthorisedIsTrueShouldReturnOneCompany() throws Exception {

        final var associatedCompany = new Association();
        associatedCompany.setCompanyNumber("222222");

        final var associatedCompanyInfo = new CompanyInfo();
        associatedCompanyInfo.setCompanyName("Springfield Nuclear Power Plant");

        Mockito.doReturn(Optional.of( new UserInfo().userId("666") ) ).when(usersService).fetchUserInfo( any() );
        Mockito.doReturn( List.of( associatedCompany ) ).when(associationsService).findAllByUserId( any(), any() );
        Mockito.doReturn( List.of( associatedCompanyInfo ) ).when( companiesService ).fetchCompanies( any() );

        final var response =
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "devil@hell.com" ).queryParam( "include-unauthorized", "true" ).header( "X-Request-Id", "theId" ) )
               .andExpect( status().isOk() )
               .andReturn()
               .getResponse()
               .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        final var companies =
        mapper.readValue( response, UserCompanyAssociationsResponse.class )
              .getSuccessBody();

        Assertions.assertEquals( 1, companies.size() );
        Assertions.assertEquals( "Springfield Nuclear Power Plant", companies.get( 0 ).getCompanyName() );
    }

    @Test
    void getCompaniesAssociatedWithUserWithOneRemovedAssociationAndIncludeUnauthorisedIsFalseShouldReturnEmptyList() throws Exception {
        Mockito.doReturn(Optional.of( new UserInfo().userId("666") ) ).when(usersService).fetchUserInfo( any() );
        Mockito.doReturn( List.of() ).when(associationsService).findAllByUserId( any(), any() );

        final var response =
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "devil@hell.com" ).queryParam( "include-unauthorized", "false" ).header( "X-Request-Id", "theId" ) )
               .andExpect( status().isOk() )
               .andReturn()
               .getResponse()
               .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        final var companies =
        mapper.readValue( response, UserCompanyAssociationsResponse.class )
              .getSuccessBody();

        Assertions.assertEquals( 0, companies.size() );
    }

    @Test
    void getCompaniesAssociatedWithUserWithOneRemovedAssociationAndIncludeUnauthorisedIsNotSpecifiedShouldReturnEmptyList() throws Exception {
        Mockito.doReturn(Optional.of( new UserInfo().userId("666") ) ).when(usersService).fetchUserInfo( any() );
        Mockito.doReturn( List.of() ).when(associationsService).findAllByUserId( any(), any() );

        final var response =
                mockMvc.perform( get( "/associations/users/{user_email}/companies", "devil@hell.com" ).header( "X-Request-Id", "theId" ) )
                        .andExpect( status().isOk() )
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();

        final var companies =
                mapper.readValue( response, UserCompanyAssociationsResponse.class )
                        .getSuccessBody();

        Assertions.assertEquals( 0, companies.size() );
    }

    @Test
    void getCompaniesAssociatedWithUserWithAVarietyOfAssociationsAndIncludeUnauthorisedIsTrueShouldReturnAllAssociatedCompanies() throws Exception {

       final var wayneEnterprisesAssociation = new Association();
       wayneEnterprisesAssociation.setCompanyNumber("111111");

        final var wayneEnterprisesCompanyInfo = new CompanyInfo();
        wayneEnterprisesCompanyInfo.setCompanyName("Wayne Enterprises");

        final var springfieldNuclearPowerPlantAssociation = new Association();
        springfieldNuclearPowerPlantAssociation.setCompanyNumber("222222");

        final var springfieldNuclearPowerPlantCompanyInfo = new CompanyInfo();
        springfieldNuclearPowerPlantCompanyInfo.setCompanyName("Springfield Nuclear Power Plant");

        final var queenVictoriaPubAssociation = new Association();
        queenVictoriaPubAssociation.setCompanyNumber("333333");

        final var queenVictoriaPubCompanyInfo = new CompanyInfo();
        queenVictoriaPubCompanyInfo.setCompanyName("Queen Victoria Pub");

        Mockito.doReturn(Optional.of( new UserInfo().userId("111") ) ).when(usersService).fetchUserInfo( any() );
        Mockito.doReturn( List.of( wayneEnterprisesAssociation, springfieldNuclearPowerPlantAssociation, queenVictoriaPubAssociation ) ).when(associationsService).findAllByUserId( any(), any() );
        Mockito.doReturn( List.of( wayneEnterprisesCompanyInfo, springfieldNuclearPowerPlantCompanyInfo, queenVictoriaPubCompanyInfo ) ).when( companiesService ).fetchCompanies( any() );

        final var response =
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "bruce.wayne@gotham.city" ).queryParam( "include-unauthorized", "true" ).header( "X-Request-Id", "theId" ) )
               .andExpect( status().isOk() )
               .andReturn()
               .getResponse()
               .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        final var companies =
                mapper.readValue( response, UserCompanyAssociationsResponse.class )
                        .getSuccessBody();

        Assertions.assertEquals( 3, companies.size() );
        Assertions.assertTrue(  companies.stream().map( CompanyInfo::getCompanyName ).toList().containsAll( List.of( "Wayne Enterprises", "Springfield Nuclear Power Plant", "Queen Victoria Pub" ) ) );
    }

    @Test
    void getCompaniesAssociatedWithUserWithAVarietyOfAssociationsAndIncludeUnauthorisedIsFalseShouldReturnSomeAssociatedCompanies() throws Exception {

        final var wayneEnterprisesAssociation = new Association();
        wayneEnterprisesAssociation.setCompanyNumber("111111");

        final var wayneEnterprisesCompanyInfo = new CompanyInfo();
        wayneEnterprisesCompanyInfo.setCompanyName("Wayne Enterprises");

        final var springfieldNuclearPowerPlantAssociation = new Association();
        springfieldNuclearPowerPlantAssociation.setCompanyNumber("222222");

        final var springfieldNuclearPowerPlantCompanyInfo = new CompanyInfo();
        springfieldNuclearPowerPlantCompanyInfo.setCompanyName("Springfield Nuclear Power Plant");

        Mockito.doReturn(Optional.of( new UserInfo().userId("111") ) ).when(usersService).fetchUserInfo( any() );
        Mockito.doReturn( List.of( wayneEnterprisesAssociation, springfieldNuclearPowerPlantAssociation ) ).when(associationsService).findAllByUserId( any(), any() );
        Mockito.doReturn( List.of( wayneEnterprisesCompanyInfo, springfieldNuclearPowerPlantCompanyInfo ) ).when( companiesService ).fetchCompanies( any() );

        final var response =
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "bruce.wayne@gotham.city" ).queryParam( "include-unauthorized", "false" ).header( "X-Request-Id", "theId" ) )
               .andExpect( status().isOk() )
               .andReturn()
               .getResponse()
               .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        final var companies =
        mapper.readValue( response, UserCompanyAssociationsResponse.class )
              .getSuccessBody();

        Assertions.assertEquals( 2, companies.size() );
        Assertions.assertTrue(  companies.stream().map( CompanyInfo::getCompanyName ).toList().containsAll( List.of( "Wayne Enterprises", "Springfield Nuclear Power Plant" ) ) );
    }

    @Test
    void getCompaniesAssociatedWithUserWithAVarietyOfAssociationsAndIncludeUnauthorisedIsNotSpecifiedShouldReturnSomeAssociatedCompanies() throws Exception {

        final var wayneEnterprisesAssociation = new Association();
        wayneEnterprisesAssociation.setCompanyNumber("111111");

        final var wayneEnterprisesCompanyInfo = new CompanyInfo();
        wayneEnterprisesCompanyInfo.setCompanyName("Wayne Enterprises");

        final var springfieldNuclearPowerPlantAssociation = new Association();
        springfieldNuclearPowerPlantAssociation.setCompanyNumber("222222");

        final var springfieldNuclearPowerPlantCompanyInfo = new CompanyInfo();
        springfieldNuclearPowerPlantCompanyInfo.setCompanyName("Springfield Nuclear Power Plant");

        Mockito.doReturn(Optional.of( new UserInfo().userId("111") ) ).when(usersService).fetchUserInfo( any() );
        Mockito.doReturn( List.of( wayneEnterprisesAssociation, springfieldNuclearPowerPlantAssociation ) ).when(associationsService).findAllByUserId( any(), any() );
        Mockito.doReturn( List.of( wayneEnterprisesCompanyInfo, springfieldNuclearPowerPlantCompanyInfo ) ).when( companiesService ).fetchCompanies( any() );

        final var response =
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "bruce.wayne@gotham.city" ).header( "X-Request-Id", "theId" ) )
               .andExpect( status().isOk() )
               .andReturn()
               .getResponse()
               .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        final var companies =
        mapper.readValue( response, UserCompanyAssociationsResponse.class )
              .getSuccessBody();

        Assertions.assertEquals( 2, companies.size() );
        Assertions.assertTrue(  companies.stream().map( CompanyInfo::getCompanyName ).toList().containsAll( List.of( "Wayne Enterprises", "Springfield Nuclear Power Plant" ) ) );
    }

}
