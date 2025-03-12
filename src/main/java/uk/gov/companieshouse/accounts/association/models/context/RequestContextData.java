package uk.gov.companieshouse.accounts.association.models.context;

import static uk.gov.companieshouse.accounts.association.models.Constants.UNKNOWN;
import static uk.gov.companieshouse.accounts.association.models.Constants.X_REQUEST_ID;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.RequestUtils.getRequestHeader;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import uk.gov.companieshouse.api.accounts.user.model.User;

public class RequestContextData {

    private final String xRequestId;
    private final String ericIdentity;
    private final String ericIdentityType;
    private final User user;

    protected RequestContextData( final String xRequestId, final String ericIdentity, final String ericIdentityType, final User user ){
        this.xRequestId = xRequestId;
        this.ericIdentity = ericIdentity;
        this.ericIdentityType = ericIdentityType;
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

    public User getUser(){
        return user;
    }

    public static final class RequestContextDataBuilder {
        private String xRequestId = UNKNOWN;
        private String ericIdentity = UNKNOWN;
        private String ericIdentityType = UNKNOWN;
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

        public RequestContextDataBuilder setUser( final User user ){
            this.user = user;
            return this;
        }

        public RequestContextData build(){
            return new RequestContextData( xRequestId, ericIdentity, ericIdentityType, user );
        }

    }

}
