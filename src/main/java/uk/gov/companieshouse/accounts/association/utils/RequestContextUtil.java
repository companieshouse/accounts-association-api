package uk.gov.companieshouse.accounts.association.utils;

import static uk.gov.companieshouse.accounts.association.models.Constants.UNKNOWN;
import static uk.gov.companieshouse.accounts.association.models.context.RequestContext.getRequestContext;

import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData;
import uk.gov.companieshouse.api.accounts.user.model.User;

public final class RequestContextUtil {

    private RequestContextUtil(){}

    private static <T> T getFieldFromRequestContext( final Function<RequestContextData, T> getterMethod, final T defaultValue ){
        return Optional.ofNullable( getRequestContext() ).map( getterMethod ).orElse( defaultValue );
    }

    public static String getXRequestId(){
        return getFieldFromRequestContext( RequestContextData::getXRequestId, UNKNOWN );
    }

    public static String getEricIdentity(){
        return getFieldFromRequestContext( RequestContextData::getEricIdentity, UNKNOWN );
    }

    public static String getEricIdentityType(){
        return getFieldFromRequestContext( RequestContextData::getEricIdentityType, UNKNOWN );
    }

    public static boolean hasAdminPrivilege( final String privilege ){
        return getFieldFromRequestContext( RequestContextData::getAdminPrivileges, new HashSet<>() ).contains( privilege );
    }

    public static User getUser(){
        return getFieldFromRequestContext( RequestContextData::getUser, null );
    }

}
