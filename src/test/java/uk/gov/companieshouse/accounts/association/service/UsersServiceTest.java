package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.models.Constants.OAUTH2;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.context.RequestContext;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData.RequestContextDataBuilder;
import uk.gov.companieshouse.api.accounts.user.model.User;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class UsersServiceTest {

    @Mock
    private RestClient usersRestClient;

    @InjectMocks
    private UsersService usersService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private Mockers mockers;

    @BeforeEach
    void setup() {
        mockers = new Mockers(usersRestClient, null, null, null );
    }

    @Test
    void fetchUserDetailsForNullOrNonexistentUsersReturnsNotFoundRuntimeException() {
        mockers.mockRestClientForFetchUserDetailsErrorResponse( null, 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> usersService.fetchUserDetails( null, "id123" ) );

        mockers.mockRestClientForFetchUserDetailsErrorResponse( "404User", 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> usersService.fetchUserDetails( "404User", "id123" ) );
    }

    @Test
    void fetchUserDetailsWithMalformedUsersIdReturnsInternalServerErrorRuntimeException() {
        mockers.mockRestClientForFetchUserDetailsErrorResponse( "£$@123", 400 );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( "£$@123" , "id123") );
    }

    @Test
    void fetchUserDetailsWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        mockers.mockRestClientForFetchUserDetailsJsonParsingError( "111" );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( "111", "id123" ) );
    }

    @Test
    void fetchUserDetailsReturnsSpecifiedUsers() throws JsonProcessingException {
        mockers.mockRestClientForFetchUserDetails( "111" );
        Assertions.assertEquals( "Batman", usersService.fetchUserDetails( "111", "id123" ).getDisplayName() );
    }

    @Test
    void fetchUserDetailsWithNullStreamThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> usersService.fetchUsersDetails( (Stream<AssociationDao>) null ) );
    }

    @Test
    void fetchUserDetailsWithEmptyStreamReturnsEmptyMap() {
        Assertions.assertEquals( 0, usersService.fetchUsersDetails( Stream.of() ).size() );
    }

    @Test
    void fetchUserDetailsWithStreamThatHasNonExistentUsersReturnsNotFoundRuntimeException(){
        final var associationDao = new AssociationDao();
        associationDao.setUserId( "404User" );
        mockers.mockRestClientForFetchUserDetailsErrorResponse( "404User", 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> usersService.fetchUsersDetails( Stream.of( associationDao ) ) );
    }

    @Test
    void fetchUserDetailsWithStreamThatHasMalformedUsersIdReturnsInternalServerErrorRuntimeException(){
        final var associationDao = new AssociationDao();
        associationDao.setUserId( "£$@123" );
        mockers.mockRestClientForFetchUserDetailsErrorResponse( "£$@123", 400 );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUsersDetails( Stream.of( associationDao ) ) );
    }

    @Test
    void fetchUserDetailsWithStreamWithArbitraryErrorReturnsInternalServerErrorRuntimeException(){
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        mockers.mockRestClientForFetchUserDetailsJsonParsingError( "111" );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUsersDetails( Stream.of( associationDao ) ) );
    }

    @Test
    void fetchUserDetailsWithStreamReturnsMap() throws JsonProcessingException {
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        mockers.mockRestClientForFetchUserDetails( "111" );
        final var users = usersService.fetchUsersDetails( Stream.of( associationDao, associationDao ) );

        Assertions.assertEquals( 1, users.size() );
        Assertions.assertTrue( users.containsKey( "111" ) );
        Assertions.assertTrue( users.values().stream().map( User::getUserId ).toList().contains( "111" ) );
    }

    @Test
    void searchUserDetailsWithNullListThrowsIllegalArgumentException(){
        Assertions.assertThrows( IllegalArgumentException.class, () -> usersService.searchUserDetailsByEmail(null));
    }

    @Test
    void searchUserDetailWithNullOrMalformedUserEmailThrowsInternalServerErrorRuntimeException() {
        final var emails = new ArrayList<String>();
        emails.add( null );
        mockers.mockRestClientForSearchUserDetailsErrorResponse( null, 400 );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetailsByEmail( emails ) );

        mockers.mockRestClientForSearchUserDetailsErrorResponse( "£$@123", 400 );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetailsByEmail( List.of( "£$@123" ) ) );
    }

    @Test
    void searchUserDetailsReturnsUsersList() throws JsonProcessingException {
        mockers.mockRestClientForSearchUserDetails( "111" );
        final var result = usersService.searchUserDetailsByEmail( List.of( "bruce.wayne@gotham.city" ) );
        Assertions.assertEquals( 1, result.size() );
        Assertions.assertEquals( "Batman", result.getFirst().getDisplayName() );
    }

    @Test
    void searchUserDetailsWithNonexistentEmailReturnsNull() {
        // Why do we want to return null here rather than an empty list?
        mockers.mockRestClientForSearchUserDetailsNonexistentEmail( "404@email.com" );
        Assertions.assertNull( usersService.searchUserDetailsByEmail( List.of( "404@email.com" ) ) );
    }

    @Test
    void searchUserDetailsWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        mockers.mockRestClientForSearchUserDetailsJsonParsingError( "bruce.wayne@gotham.city" );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetailsByEmail( List.of( "bruce.wayne@gotham.city" ) ) );
    }

    @Test
    void fetchUserDetailsWithNullAssociationOrNullUserIdAndUsersEmailReturnsNull(){
        Assertions.assertNull( usersService.fetchUserDetails( new AssociationDao() ) );
    }

    @Test
    void fetchUserDetailsWithNonexistentUsersIdThrowsNotFoundRuntimeException(){
        final var requestingUser = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation002" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( ERIC_IDENTITY, requestingUser.getUserId() );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setEricIdentity( request ).setUser( requestingUser ).build() );

        mockers.mockRestClientForFetchUserDetailsErrorResponse( "MKUser002", 404 );

        Assertions.assertThrows( NotFoundRuntimeException.class, () -> usersService.fetchUserDetails( targetAssociation ) );
    }

    @Test
    void fetchUserDetailsWithUserIdAssociationAndSameUsersReturnsEricUsers() {
        final var targetUser = testDataManager.fetchUserDtos( "MKUser002" ).getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation002" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( ERIC_IDENTITY, targetUser.getUserId() );
        request.addHeader( ERIC_IDENTITY_TYPE, OAUTH2 );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setEricIdentity( request ).setEricIdentityType( request ).setUser( targetUser ).build() );

        Assertions.assertEquals( targetUser, usersService.fetchUserDetails( targetAssociation ) );
    }

    @Test
    void fetchUserDetailsWithUserIdAssociationAndDifferentUsersRetrievesUsers() throws JsonProcessingException {
        final var requestingUser = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos( "MKUser002" ).getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation002" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( ERIC_IDENTITY, requestingUser.getUserId() );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setEricIdentity( request ).setUser( requestingUser ).build() );

        mockers.mockRestClientForFetchUserDetails( "MKUser002" );

        Assertions.assertEquals( targetUser, usersService.fetchUserDetails( targetAssociation ) );
    }

    @Test
    void fetchUserDetailsWithNonexistentUsersEmailReturnsNull(){
        final var requestingUser = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( ERIC_IDENTITY, requestingUser.getUserId() );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setEricIdentity( request ).setUser( requestingUser ).build() );

        mockers.mockRestClientForSearchUserDetailsNonexistentEmail( "mario@mushroom.kingdom" );

        Assertions.assertNull( usersService.fetchUserDetails( targetAssociation ) );
    }

    @Test
    void fetchUserDetailsWithUserEmailAssociationAndSameUsersReturnsEricUsers() throws JsonProcessingException {
        final var targetUser = testDataManager.fetchUserDtos( "MKUser001" ).getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( ERIC_IDENTITY, targetUser.getUserId() );
        request.addHeader( ERIC_IDENTITY_TYPE, OAUTH2 );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setEricIdentity( request ).setEricIdentityType( request ).setUser( targetUser ).build() );

        Assertions.assertEquals( targetUser, usersService.fetchUserDetails( targetAssociation ) );
    }

    @Test
    void fetchUserDetailsWithUserEmailAssociationAndDifferentUsersRetrievesUsers() throws JsonProcessingException {
        final var requestingUser = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos( "MKUser001" ).getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( ERIC_IDENTITY, requestingUser.getUserId() );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setEricIdentity( request ).setUser( requestingUser ).build() );

        mockers.mockRestClientForSearchUserDetails( "MKUser001" );

        Assertions.assertEquals( targetUser, usersService.fetchUserDetails( targetAssociation ) );
    }

}
