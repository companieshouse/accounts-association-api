package uk.gov.companieshouse.accounts.association.models.context;

import java.util.Objects;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData.RequestContextDataBuilder;

public final class RequestContext {

    private static ThreadLocal<RequestContextData> requestContextDataThreadLocal;

    private RequestContext(){}

    public static void setRequestContext( final RequestContextData requestContext ){
        requestContextDataThreadLocal = new ThreadLocal<>();
        requestContextDataThreadLocal.set( requestContext );
    }

    public static RequestContextData getRequestContext(){
        return Objects.nonNull( requestContextDataThreadLocal ) ? requestContextDataThreadLocal.get() : new RequestContextDataBuilder().build();
    }

    public static void clear(){
        if ( Objects.nonNull( requestContextDataThreadLocal ) ) {
            requestContextDataThreadLocal.remove();
        }
    }

}
