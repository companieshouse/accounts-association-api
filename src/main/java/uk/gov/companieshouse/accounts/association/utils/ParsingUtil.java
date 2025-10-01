package uk.gov.companieshouse.accounts.association.utils;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.function.Function;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;

public class ParsingUtil {

    public static <T> Function<String, T> parseJsonTo( final Class<T> clazz ) {
        return json -> {
            final var objectMapper = new ObjectMapper();
            objectMapper.registerModule( new JavaTimeModule() );
            try {
                return objectMapper.readValue( json, clazz );
            } catch ( IOException e ){
                throw new InternalServerErrorRuntimeException(getXRequestId(), "Unable to parse json", e );
            }
        };
    }

    public static <T> String parseJsonFrom( final T object, final String fallback ) {
        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        try {
            return objectMapper.writeValueAsString( object );
        } catch ( IOException exception ) {
            LOGGER.errorContext( getXRequestId(), "Unable to parse json", exception, null );
            return fallback;
        }
    }

}
