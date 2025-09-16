package uk.gov.companieshouse.accounts.association.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.accounts.association.models.Constants.OAUTH2;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;

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
import uk.gov.companieshouse.accounts.association.service.client.UserClient;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.context.RequestContext;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData.RequestContextDataBuilder;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class UsersServiceTest {

    @Mock
    private UserClient userClient;

    @InjectMocks
    private UsersService usersService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup() {
    }

    @Test
    void fetchUserDetailsForNullOrBlankUsersReturnsNotFoundRuntimeException() {
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> usersService.fetchUserDetails(null, "id123"));
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> usersService.fetchUserDetails("", "id123"));
    }

    @Test
    void fetchUserDetailsWithMalformedUsersIdReturnsInternalServerErrorRuntimeException() {
        when(userClient.requestUserDetails(any(), any())).thenThrow(InternalServerErrorRuntimeException.class);
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails("£$@123" , "id123"));
    }

    @Test
    void fetchUserDetailsWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        when(userClient.requestUserDetails(any(), any())).thenThrow(InternalServerErrorRuntimeException.class);
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails("111", "id123"));
    }

    @Test
    void fetchUserDetailsReturnsSpecifiedUsers() {
        when(userClient.requestUserDetails(eq("111"), any())).thenReturn(testDataManager.fetchUserDtos("111").getFirst());
        Assertions.assertEquals("Batman", usersService.fetchUserDetails("111", "id123").getDisplayName());
    }

    @Test
    void fetchUserDetailsWithNullStreamThrowsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> usersService.fetchUsersDetails(null));
    }

    @Test
    void fetchUserDetailsWithEmptyStreamReturnsEmptyMap() {
        Assertions.assertEquals(0, usersService.fetchUsersDetails(Stream.of()).size());
    }

    @Test
    void fetchUserDetailsWithStreamThatHasNonExistentUsersReturnsNotFoundRuntimeException() {
        final var associationDao = new AssociationDao();
        associationDao.setUserId("404User");
        when(userClient.requestUserDetails(any(), any())).thenThrow(NotFoundRuntimeException.class);

        Assertions.assertThrows(NotFoundRuntimeException.class, () -> usersService.fetchUsersDetails(Stream.of(associationDao)));
    }

    @Test
    void fetchUserDetailsWithStreamThatHasMalformedUsersIdReturnsInternalServerErrorRuntimeException() {
        final var associationDao = new AssociationDao();
        associationDao.setUserId("£$@123");
        when(userClient.requestUserDetails(any(), any())).thenThrow(InternalServerErrorRuntimeException.class);

        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> usersService.fetchUsersDetails(Stream.of(associationDao)));
    }

    @Test
    void fetchUserDetailsWithStreamWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        final var associationDao = testDataManager.fetchAssociationDaos("1").getFirst();
        when(userClient.requestUserDetails(eq(associationDao.getUserId()), any())).thenThrow(InternalServerErrorRuntimeException.class);
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> usersService.fetchUsersDetails(Stream.of(associationDao)));
    }

    @Test
    void fetchUserDetailsWithStreamReturnsMap() {
        final var associationDao111 = testDataManager.fetchAssociationDaos("1").getFirst();
        final var associationDao222 = testDataManager.fetchAssociationDaos("2").getFirst();
        when(userClient.requestUserDetails(any(), any())).thenReturn(testDataManager.fetchUserDtos("111").getFirst(), testDataManager.fetchUserDtos("222").getFirst());

        final var users = usersService.fetchUsersDetails(Stream.of(associationDao111, associationDao222));

        Assertions.assertEquals(2, users.size());
        Assertions.assertTrue(users.containsKey("111"));
        Assertions.assertTrue(users.containsKey("222"));
        Assertions.assertTrue(users.values().stream().map(User::getUserId).toList().contains("111"));
        Assertions.assertTrue(users.values().stream().map(User::getUserId).toList().contains("222"));
    }

    @Test
    void searchUserDetailsWithNullListThrowsIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> usersService.searchUsersDetailsByEmail(null));
    }

    @Test
    void searchUserDetailWithNullOrMalformedUserEmailThrowsInternalServerErrorRuntimeException() {
        final var emails = new ArrayList<String>();
        emails.add(null);
        when(userClient.requestUserDetailsByEmail(any(), any())).thenThrow(InternalServerErrorRuntimeException.class);

        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> usersService.searchUsersDetailsByEmail(emails));
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> usersService.searchUsersDetailsByEmail(List.of("£$@123")));
    }

    @Test
    void searchUserDetailsReturnsUsersList() {
        final var usersList = new UsersList();
        usersList.add(testDataManager.fetchUserDtos("111").getFirst());
        when(userClient.requestUserDetailsByEmail(eq("bruce.wayne@gotham.city"), any())).thenReturn(usersList);

        final var result = usersService.searchUsersDetailsByEmail(List.of("bruce.wayne@gotham.city"));
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Batman", result.getFirst().getDisplayName());
    }

    @Test
    void searchUserDetailsWithNonexistentEmailReturnsNull() {
        // Why do we want to return null here rather than an empty list?
//        mockers.mockRestClientForSearchUserDetailsNonexistentEmail("404@email.com");
        Assertions.assertNull(usersService.searchUsersDetailsByEmail(List.of("404@email.com")));
    }

    @Test
    void searchUserDetailsWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        when(userClient.requestUserDetailsByEmail(any(), any())).thenThrow(InternalServerErrorRuntimeException.class);
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> usersService.searchUsersDetailsByEmail(List.of("bruce.wayne@gotham.city")));
    }

    @Test
    void fetchUserDetailsWithNullAssociationOrNullUserIdAndUsersEmailReturnsNull() {
//        Assertions.assertNull(usersService.fetchUserDetails(new AssociationDao()));
    }

    @Test
    void fetchUserDetailsWithNonexistentUsersIdThrowsNotFoundRuntimeException() {
        final var requestingUser = testDataManager.fetchUserDtos("111").getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos("MKAssociation002").getFirst();
        final var request = new MockHttpServletRequest();

        request.addHeader(ERIC_IDENTITY, requestingUser.getUserId());
        RequestContext.setRequestContext(new RequestContextDataBuilder().setEricIdentity(request).setUser(requestingUser).build());
        when(userClient.requestUserDetails(eq(targetAssociation.getUserId()), any())).thenThrow(NotFoundRuntimeException.class);

//        Assertions.assertThrows(NotFoundRuntimeException.class, () -> usersService.fetchUserDetails(targetAssociation));
    }

    @Test
    void fetchUserDetailsWithUserIdAssociationAndSameUsersReturnsEricUsers() {
        final var targetUser = testDataManager.fetchUserDtos("MKUser002").getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos("MKAssociation002").getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader(ERIC_IDENTITY, targetUser.getUserId());
        request.addHeader(ERIC_IDENTITY_TYPE, OAUTH2);
        RequestContext.setRequestContext(new RequestContextDataBuilder().setEricIdentity(request).setEricIdentityType(request).setUser(targetUser).build());

//        Assertions.assertEquals(targetUser, usersService.fetchUserDetails(targetAssociation));
    }

    @Test
    void fetchUserDetailsWithUserIdAssociationAndDifferentUsersRetrievesUsers() {
        final var requestingUser = testDataManager.fetchUserDtos("111").getFirst();
        final var targetUser = testDataManager.fetchUserDtos("MKUser002").getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos("MKAssociation002").getFirst();

        // TODO: confirm what this was doing, cover in integration test
        final var request = new MockHttpServletRequest();
        request.addHeader(ERIC_IDENTITY, requestingUser.getUserId());
        RequestContext.setRequestContext(new RequestContextDataBuilder().setEricIdentity(request).setUser(requestingUser).build());

        when(userClient.requestUserDetails(eq(targetUser.getUserId()), any())).thenReturn(targetUser);

//        Assertions.assertEquals(targetUser, usersService.fetchUserDetails(targetAssociation));
    }

    @Test
    void fetchUserDetailsWithNonexistentUsersEmailReturnsNull() {
        final var requestingUser = testDataManager.fetchUserDtos("111").getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos("MKAssociation001").getFirst();

        // TODO: confirm what this was doing, cover in integration test
//        final var request = new MockHttpServletRequest();
//        request.addHeader(ERIC_IDENTITY, requestingUser.getUserId());
//        RequestContext.setRequestContext(new RequestContextDataBuilder().setEricIdentity(request).setUser(requestingUser).build());

//        mockers.mockRestClientForSearchUserDetailsNonexistentEmail("mario@mushroom.kingdom");

//        Assertions.assertNull(usersService.fetchUserDetails(targetAssociation));
    }

    @Test
    void fetchUserDetailsWithUserEmailAssociationAndSameUsersReturnsEricUsers() {
        final var targetUser = testDataManager.fetchUserDtos("MKUser001").getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos("MKAssociation001").getFirst();
        final var usersList = new UsersList();
        usersList.add(targetUser);

        when(userClient.requestUserDetailsByEmail(eq(targetUser.getEmail()), any())).thenReturn(usersList);

        // TODO: confirm what this was doing, cover in integration test
//        final var request = new MockHttpServletRequest();
//        request.addHeader(ERIC_IDENTITY, targetUser.getUserId());
//        request.addHeader(ERIC_IDENTITY_TYPE, OAUTH2);
//        RequestContext.setRequestContext(new RequestContextDataBuilder().setEricIdentity(request).setEricIdentityType(request).setUser(targetUser).build());

//        Assertions.assertEquals(targetUser, usersService.fetchUserDetails(targetAssociation));
    }

    @Test
    void fetchUserDetailsWithUserEmailAssociationAndDifferentUsersRetrievesUsers() {
//        final var requestingUser = testDataManager.fetchUserDtos("111").getFirst();
        final var targetUser = testDataManager.fetchUserDtos("MKUser001").getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos("MKAssociation001").getFirst();
        final var targetUserList = new UsersList();
//        final var request = new MockHttpServletRequest();

        // TODO: confirm what this was doing, cover in integration test

        targetUserList.add(targetUser);
//        request.addHeader(ERIC_IDENTITY, requestingUser.getUserId());
//        RequestContext.setRequestContext(new RequestContextDataBuilder().setEricIdentity(request).setUser(requestingUser).build());
        when(userClient.requestUserDetailsByEmail(eq(targetUser.getEmail()), any())).thenReturn(targetUserList);

//        Assertions.assertEquals(targetUser, usersService.fetchUserDetails(targetAssociation));
    }

}
