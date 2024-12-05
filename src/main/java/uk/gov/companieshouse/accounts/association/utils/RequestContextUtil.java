package uk.gov.companieshouse.accounts.association.utils;

import java.util.Optional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestContextUtil {

    private static final String X_REQUEST_ID = "X-Request-Id";
    private static final String UNKNOWN_X_REQUEST_ID = "unknown";

    public static String getXRequestId() {
        return Optional.ofNullable( RequestContextHolder.getRequestAttributes() )
                .map( requestAttributes -> (ServletRequestAttributes) requestAttributes )
                .map( ServletRequestAttributes::getRequest )
                .map( httpServletRequest -> httpServletRequest.getHeader( X_REQUEST_ID ) )
                .orElse( UNKNOWN_X_REQUEST_ID );
    }

}
