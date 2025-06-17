package uk.gov.companieshouse.accounts.association.interceptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_READ_PERMISSION;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentityType;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.hasAdminPrivilege;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.context.RequestContext;
import uk.gov.companieshouse.accounts.association.service.UsersService;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class RequestLifecycleInterceptorTest {

    @Mock
    private UsersService usersService;

    @InjectMocks
    private OAuthRequestLifecycleInterceptor requestLifecycleInterceptor;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void preHandleWithOAuth2RequestSetsRequestContextWithUserAndReturnsTrue(){
        final var user = testDataManager.fetchUserDtos( "111" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", user.getUserId() );
        request.addHeader( "Eric-Identity-Type", "oauth2" );
        request.addHeader( "Eric-Authorised-Roles", ADMIN_READ_PERMISSION );

        final var response = new MockHttpServletResponse();

        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( eq(user.getUserId()), any() );

        final var result = requestLifecycleInterceptor.preHandle( request, response, null );

        Assertions.assertTrue( result );
        Assertions.assertEquals( "theId123", getXRequestId() );
        Assertions.assertEquals( user.getUserId(), getEricIdentity() );
        Assertions.assertEquals( "oauth2", getEricIdentityType() );
        Assertions.assertTrue( hasAdminPrivilege( ADMIN_READ_PERMISSION ) );
        Assertions.assertEquals( user, getUser() );
        Assertions.assertEquals( 200, response.getStatus() );
    }

    @Test
    void preHandleWithOAuth2RequestWithNonexistentUserDoesNotSetRequestContextAndReturnsFalse(){
        final var user = testDataManager.fetchUserDtos( "111" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", user.getUserId() );
        request.addHeader( "Eric-Identity-Type", "oauth2" );

        final var response = new MockHttpServletResponse();

        Mockito.doThrow( new NotFoundRuntimeException( "Could not find user", new Exception( "Could not find user" )) ).when( usersService ).fetchUserDetails( eq(user.getUserId()), any() );

        final var result = requestLifecycleInterceptor.preHandle( request, response, null );

        Assertions.assertFalse( result );
        Assertions.assertEquals( "unknown", getXRequestId() );
        Assertions.assertEquals( "unknown", getEricIdentity() );
        Assertions.assertEquals( "unknown", getEricIdentityType() );
        Assertions.assertNull( getUser() );
        Assertions.assertEquals( 403, response.getStatus() );
    }

    @Test
    void afterCompletionClearsRequestContext(){
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "111" );
        request.addHeader( "Eric-Identity-Type", "oauth2" );

        final var response = new MockHttpServletResponse();

        requestLifecycleInterceptor.preHandle( request, response, null );

        Assertions.assertEquals( "theId123", getXRequestId() );
        Assertions.assertEquals( "111", getEricIdentity() );
        Assertions.assertEquals( "oauth2", getEricIdentityType() );

        requestLifecycleInterceptor.afterCompletion( request, response, null, null );

        Assertions.assertEquals( "unknown", getXRequestId() );
        Assertions.assertEquals( "unknown", getEricIdentity() );
        Assertions.assertEquals( "unknown", getEricIdentityType() );
    }

    @AfterEach
    void teardown(){
        RequestContext.clear();
    }

}
