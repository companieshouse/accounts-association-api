package uk.gov.companieshouse.accounts.association.utils;

import java.util.Objects;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestContextUtil {

    private static final String X_REQUEST_ID = "X-Request-Id";

    public static String getXRequestId() {
        try {
            final var requestAttributes = RequestContextHolder.getRequestAttributes();
            final var servletRequestAttributes = ( (ServletRequestAttributes) requestAttributes );
            final var httpServletRequest = Objects.requireNonNull( servletRequestAttributes ).getRequest();
            return httpServletRequest.getHeader( X_REQUEST_ID );
        } catch ( Exception e ){
            return null;
        }
    }

}
