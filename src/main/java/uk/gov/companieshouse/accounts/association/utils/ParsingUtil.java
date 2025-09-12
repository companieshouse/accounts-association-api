package uk.gov.companieshouse.accounts.association.utils;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;

public class ParsingUtil {

    public static <T> T parseJsonTo(final String json, final Class<T> clazz) {
        LOGGER.trace(String.format("Attempting to parse JSON:%n%s%nto class: %s", json, clazz.getSimpleName()));
        if (json == null) {
            return null;
        }
        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        try {
            return objectMapper.readValue(json, clazz);
        }
        catch (IOException e) {
            throw new InternalServerErrorRuntimeException("Unable to parse json", e);

        }
    }

    public static <T> String parseJsonFrom(final T object, final String fallback) {
        LOGGER.trace(String.format("Attempting to parse JSON from:%n%s", object.toString()));
        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException exception) {
            LOGGER.errorContext(getXRequestId(), "Unable to parse json", exception, null);
            return fallback;
        }
    }

}
