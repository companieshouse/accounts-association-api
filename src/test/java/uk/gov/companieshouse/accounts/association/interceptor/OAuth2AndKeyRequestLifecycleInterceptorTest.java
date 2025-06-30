package uk.gov.companieshouse.accounts.association.interceptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentityType;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

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
import uk.gov.companieshouse.accounts.association.service.UsersService;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class OAuth2AndKeyRequestLifecycleInterceptorTest {

    @Mock
    private UsersService usersService;

    @InjectMocks
    private OAuth2AndKeyRequestLifecycleInterceptor oAuth2AndKeyRequestLifecycleInterceptor;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void preHandleWithValidAuth2RequestReturnsTrue(){
        final var user = testDataManager.fetchUserDtos( "MiUser001" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", user.getUserId() );
        request.addHeader( "Eric-Identity-Type", "oauth2" );
        request.setMethod( "GET" );
        request.setRequestURI( "/associations/companies/MICOMP001" );

        final var response = new MockHttpServletResponse();

        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( eq(user.getUserId()), any() );

        final var result = oAuth2AndKeyRequestLifecycleInterceptor.preHandle( request, response, null );

        Assertions.assertTrue( result );
        Assertions.assertEquals( "theId123", getXRequestId() );
        Assertions.assertEquals( user.getUserId(), getEricIdentity() );
        Assertions.assertEquals( "oauth2", getEricIdentityType() );
        Assertions.assertEquals( user, getUser() );
        Assertions.assertEquals( 200, response.getStatus() );
    }

    @Test
    void preHandleWithValidAPIKeyRequestReturnsTrue(){
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "MiUser001" );
        request.addHeader( "Eric-Identity-Type", "key" );
        request.addHeader( "ERIC-Authorised-Key-Roles", "*" );
        request.setMethod( "PATCH" );
        request.setRequestURI( "/associations/MiAssociation001" );

        final var response = new MockHttpServletResponse();

        final var result = oAuth2AndKeyRequestLifecycleInterceptor.preHandle( request, response, null );

        Assertions.assertTrue( result );
        Assertions.assertEquals( "theId123", getXRequestId() );
        Assertions.assertEquals( "MiUser001", getEricIdentity() );
        Assertions.assertEquals( "key", getEricIdentityType() );
        Assertions.assertEquals( 200, response.getStatus() );
    }

    @Test
    void preHandleWithInvalidEricIdentityTypeReturnsFalse(){
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "MiUser001" );
        request.addHeader( "Eric-Identity-Type", "test" );
        request.addHeader( "ERIC-Authorised-Key-Roles", "*" );
        request.setMethod( "PATCH" );
        request.setRequestURI( "/associations/MiAssociation001" );

        final var response = new MockHttpServletResponse();

        final var result = oAuth2AndKeyRequestLifecycleInterceptor.preHandle( request, response, null );

        Assertions.assertFalse( result );
        Assertions.assertEquals( 403, response.getStatus() );
    }

}