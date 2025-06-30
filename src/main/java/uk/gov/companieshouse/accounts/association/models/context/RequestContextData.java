package uk.gov.companieshouse.accounts.association.models.context;

import static uk.gov.companieshouse.accounts.association.models.Constants.UNKNOWN;
import static uk.gov.companieshouse.accounts.association.models.Constants.X_REQUEST_ID;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.RequestUtils.getRequestHeader;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import uk.gov.companieshouse.api.accounts.user.model.User;

public class RequestContextData {

    private final String xRequestId;
    private final String ericIdentity;
    private final String ericIdentityType;
    private final String ericAuthorisedKeyRoles;
    private final HashSet<String> adminPrivileges;
    private final User user;

    protected RequestContextData( final String xRequestId, final String ericIdentity, final String ericIdentityType, final String ericAuthorisedKeyRoles, final HashSet<String> adminPrivileges, final User user ){
        this.xRequestId = xRequestId;
        this.ericIdentity = ericIdentity;
        this.ericIdentityType = ericIdentityType;
        this.ericAuthorisedKeyRoles = ericAuthorisedKeyRoles;
        this.adminPrivileges = adminPrivileges;
        this.user = user;
    }

    public String getXRequestId(){
        return xRequestId;
    }

    public String getEricIdentity(){
        return ericIdentity;
    }

    public String getEricIdentityType(){
        return ericIdentityType;
    }

    public String getEricAuthorisedKeyRoles(){
        return ericAuthorisedKeyRoles;
    }

    public HashSet<String> getAdminPrivileges(){
        return adminPrivileges;
    }

    public User getUser(){
        return user;
    }

    public static final class RequestContextDataBuilder {
        private String xRequestId = UNKNOWN;
        private String ericIdentity = UNKNOWN;
        private String ericIdentityType = UNKNOWN;
        private String ericAuthorisedKeyRoles = UNKNOWN;
        private HashSet<String> adminPrivileges = new HashSet<>();
        private User user;

        public RequestContextDataBuilder setXRequestId( final HttpServletRequest request ){
            xRequestId = Optional.ofNullable( getRequestHeader( request, X_REQUEST_ID ) ).orElse( UNKNOWN );
            return this;
        }

        public RequestContextDataBuilder setEricIdentity( final HttpServletRequest request ){
            ericIdentity = Optional.ofNullable( getRequestHeader( request, ERIC_IDENTITY ) ).orElse( UNKNOWN );
            return this;
        }

        public RequestContextDataBuilder setEricIdentityType( final HttpServletRequest request ){
            ericIdentityType = Optional.ofNullable( getRequestHeader( request, ERIC_IDENTITY_TYPE ) ).orElse( UNKNOWN );
            return this;
        }

        public RequestContextDataBuilder setEricAuthorisedKeyRoles( final HttpServletRequest request ){
            ericAuthorisedKeyRoles = Optional.ofNullable( getRequestHeader( request, ERIC_AUTHORISED_KEY_ROLES ) ).orElse( UNKNOWN );
            return this;
        }

        public RequestContextDataBuilder setAdminPrivileges( final HttpServletRequest request ){
            adminPrivileges = Optional.ofNullable( getRequestHeader( request, ERIC_AUTHORISED_ROLES ) )
                    .map( roles -> roles.split(" ") )
                    .map( roles -> Arrays.stream( roles ).collect( Collectors.toCollection( HashSet::new ) ) )
                    .orElse( new HashSet<>() );
            return this;
        }

        public RequestContextDataBuilder setUser( final User user ){
            this.user = user;
            return this;
        }

        public RequestContextData build(){
            return new RequestContextData( xRequestId, ericIdentity, ericIdentityType, ericAuthorisedKeyRoles, adminPrivileges, user );
        }

    }

}