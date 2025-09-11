package uk.gov.companieshouse.accounts.association.utils;

import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_READ_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_UPDATE_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.UNKNOWN;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentityType;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.hasAdminPrivilege;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.context.RequestContext;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData.RequestContextDataBuilder;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class RequestContextUtilTest {

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void getXRequestIdIsUnknownWhenXRequestIdIsMissing(){
        RequestContext.setRequestContext(new RequestContextDataBuilder().build());
        Assertions.assertEquals(UNKNOWN, getXRequestId());
    }

    @Test
    void getXRequestIdRetrievesXRequestId(){
        final var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id","theId123");
        RequestContext.setRequestContext(new RequestContextDataBuilder().setXRequestId(request).build());
        Assertions.assertEquals("theId123", getXRequestId());
    }

    @Test
    void getEricIdentityIsUnknownWhenEricIdentityIsMissing(){
        RequestContext.setRequestContext(new RequestContextDataBuilder().build());
        Assertions.assertEquals(UNKNOWN, getEricIdentity());
    }

    @Test
    void getEricIdentityRetrievesEricIdentity(){
        final var request = new MockHttpServletRequest();
        request.addHeader(ERIC_IDENTITY,"COMU002");
        RequestContext.setRequestContext(new RequestContextDataBuilder().setEricIdentity(request).build());
        Assertions.assertEquals("COMU002", getEricIdentity());
    }

    @Test
    void getEricIdentityTypeIsUnknownWhenEricIdentityTypeIsMissing(){
        RequestContext.setRequestContext(new RequestContextDataBuilder().build());
        Assertions.assertEquals(UNKNOWN, getEricIdentityType());
    }

    @Test
    void getEricIdentityTypeRetrievesEricIdentityType(){
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity-Type","oauth2");
        RequestContext.setRequestContext(new RequestContextDataBuilder().setEricIdentityType(request).build());
        Assertions.assertEquals("oauth2", getEricIdentityType());
    }

    @Test
    void hasAdminPrivilegeReturnsFalseWhenUserDoesNotHaveEricAuthorisedRoles(){
        RequestContext.setRequestContext(new RequestContextDataBuilder().setAdminPrivileges(new MockHttpServletRequest()).build());
        Assertions.assertFalse(hasAdminPrivilege(ADMIN_READ_PERMISSION));
    }

    @Test
    void hasAdminPrivilegeWithNullInputReturnsFalse(){
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Roles", ADMIN_READ_PERMISSION);
        RequestContext.setRequestContext(new RequestContextDataBuilder().setAdminPrivileges(request).build());
        Assertions.assertFalse(hasAdminPrivilege(null));
    }

    private static Stream<Arguments> adminPrivilegeScenarios(){
        return Stream.of(
                Arguments.of(ADMIN_READ_PERMISSION, ADMIN_READ_PERMISSION),
                Arguments.of(ADMIN_UPDATE_PERMISSION, ADMIN_UPDATE_PERMISSION),
                Arguments.of(String.format("%s %s" , ADMIN_READ_PERMISSION, ADMIN_UPDATE_PERMISSION), ADMIN_READ_PERMISSION),
                Arguments.of(String.format("%s %s /admin/something/else" , ADMIN_READ_PERMISSION, ADMIN_UPDATE_PERMISSION), ADMIN_UPDATE_PERMISSION),
                Arguments.of(String.format("%s %s /admin/something/else" , ADMIN_READ_PERMISSION, ADMIN_UPDATE_PERMISSION), "/admin/Permission/404")
       );
    }

    @ParameterizedTest
    @MethodSource("adminPrivilegeScenarios")
    void hasAdminPrivilegeReturnsTrueWhenRequestingUserHasSpecifiedPrivilegeOrOtherwiseFalse(final String ericAuthorisedRoles, final String privilege){
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Roles", ericAuthorisedRoles);
        RequestContext.setRequestContext(new RequestContextDataBuilder().setAdminPrivileges(request).build());
        Assertions.assertEquals(ericAuthorisedRoles.contains(privilege), hasAdminPrivilege(privilege));
    }

    @Test
    void getUserNullWhenUserIsMissing(){
        RequestContext.setRequestContext(new RequestContextDataBuilder().build());
        Assertions.assertNull(getUser());
    }

    @Test
    void getUserRetrievesUser(){
        final var user = testDataManager.fetchUserDtos("111").getFirst();
        RequestContext.setRequestContext(new RequestContextDataBuilder().setUser(user).build());
        Assertions.assertEquals(user, getUser());
    }

}


