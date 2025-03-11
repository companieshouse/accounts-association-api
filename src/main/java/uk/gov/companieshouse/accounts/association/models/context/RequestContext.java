package uk.gov.companieshouse.accounts.association.models.context;

public final class RequestContext {

    private static final ThreadLocal<RequestContextData> requestContextDataThreadLocal = new ThreadLocal<>();

    private RequestContext(){}

    public static void setRequestContext( final RequestContextData requestContext ){
        requestContextDataThreadLocal.set( requestContext );
    }

    public static RequestContextData getRequestContext(){
        return requestContextDataThreadLocal.get();
    }

    public static void clear(){
        requestContextDataThreadLocal.remove();
    }

}
