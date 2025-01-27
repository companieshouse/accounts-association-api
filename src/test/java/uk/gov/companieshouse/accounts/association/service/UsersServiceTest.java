package uk.gov.companieshouse.accounts.association.service;

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
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.accounts.user.model.User;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class UsersServiceTest {

    @Mock
    private WebClient usersWebClient;

    @InjectMocks
    private UsersService usersService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private Mockers mockers;

    @BeforeEach
    void setup() {
        mockers = new Mockers( usersWebClient, null, null, null );
    }

    @Test
    void fetchUserDetailsForNullOrNonexistentUserReturnsNotFoundRuntimeException() {
        mockers.mockWebClientForFetchUserDetailsErrorResponse( null, 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> usersService.fetchUserDetails( (String) null ) );

        mockers.mockWebClientForFetchUserDetailsErrorResponse( "404User", 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> usersService.fetchUserDetails( "404User" ) );
    }

    @Test
    void fetchUserDetailsWithMalformedUserIdReturnsInternalServerErrorRuntimeException() {
        mockers.mockWebClientForFetchUserDetailsErrorResponse( "£$@123", 400 );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( "£$@123" ) );
    }

    @Test
    void fetchUserDetailsWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        mockers.mockWebClientForFetchUserDetailsJsonParsingError( "111" );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( "111" ) );
    }

    @Test
    void fetchUserDetailsReturnsSpecifiedUser() throws JsonProcessingException {
        mockers.mockWebClientForFetchUserDetails( "111" );
        Assertions.assertEquals( "Batman", usersService.fetchUserDetails( "111" ).getDisplayName() );
    }

    @Test
    void fetchUserDetailsWithNullStreamThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> usersService.fetchUserDetails( (Stream<AssociationDao>) null ) );
    }

    @Test
    void fetchUserDetailsWithEmptyStreamReturnsEmptyMap() {
        Assertions.assertEquals( 0, usersService.fetchUserDetails( Stream.of() ).size() );
    }

    @Test
    void fetchUserDetailsWithStreamThatHasNonExistentUserReturnsNotFoundRuntimeException(){
        final var associationDao = new AssociationDao();
        associationDao.setUserId( "404User" );
        mockers.mockWebClientForFetchUserDetailsErrorResponse( "404User", 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> usersService.fetchUserDetails( Stream.of( associationDao ) ) );
    }

    @Test
    void fetchUserDetailsWithStreamThatHasMalformedUserIdReturnsInternalServerErrorRuntimeException(){
        final var associationDao = new AssociationDao();
        associationDao.setUserId( "£$@123" );
        mockers.mockWebClientForFetchUserDetailsErrorResponse( "£$@123", 400 );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( Stream.of( associationDao ) ) );
    }

    @Test
    void fetchUserDetailsWithStreamWithArbitraryErrorReturnsInternalServerErrorRuntimeException(){
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        mockers.mockWebClientForFetchUserDetailsJsonParsingError( "111" );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( Stream.of( associationDao ) ) );
    }

    @Test
    void fetchUserDetailsWithStreamReturnsMap() throws JsonProcessingException {
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        mockers.mockWebClientForFetchUserDetails( "111" );
        final var users = usersService.fetchUserDetails( Stream.of( associationDao, associationDao ) );

        Assertions.assertEquals( 1, users.size() );
        Assertions.assertTrue( users.containsKey( "111" ) );
        Assertions.assertTrue( users.values().stream().map( User::getUserId ).toList().contains( "111" ) );
    }

    @Test
    void searchUserDetailsWithNullListThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> usersService.searchUserDetails( null ) );
    }

    @Test
    void searchUserDetailWithNullOrMalformedUserEmailThrowsInternalServerErrorRuntimeException() {
        final var emails = new ArrayList<String>();
        emails.add( null );
        mockers.mockWebClientForSearchUserDetailsErrorResponse( null, 400 );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails( emails ) );

        mockers.mockWebClientForSearchUserDetailsErrorResponse( "£$@123", 400 );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails( List.of( "£$@123" ) ) );
    }

    @Test
    void searchUserDetailsReturnsUsersList() throws JsonProcessingException {
        mockers.mockWebClientForSearchUserDetails( "111" );
        final var result = usersService.searchUserDetails( List.of( "bruce.wayne@gotham.city" ) );
        Assertions.assertEquals( 1, result.size() );
        Assertions.assertEquals( "Batman", result.getFirst().getDisplayName() );
    }

    @Test
    void searchUserDetailsWithNonexistentEmailReturnsNull() {
        mockers.mockWebClientForSearchUserDetailsNonexistentEmail( "404@email.com" );
        Assertions.assertNull( usersService.searchUserDetails( List.of( "404@email.com" ) ) );
    }

    @Test
    void searchUserDetailsWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        mockers.mockWebClientForSearchUserDetailsJsonParsingError( "bruce.wayne@gotham.city" );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails( List.of( "bruce.wayne@gotham.city" ) ) );
    }

}
