package uk.gov.companieshouse.accounts.association.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import org.springframework.test.web.servlet.ResultActions;

public class ParsingUtils {

    public static String reduceTimestampResolution(final String timestamp) {
        return timestamp.substring(0, timestamp.indexOf(":"));
    }

    public static String localDateTimeToNormalisedString(final LocalDateTime localDateTime) {
        final var timestamp = localDateTime.toString();
        return reduceTimestampResolution(timestamp);
    }

    public static OffsetDateTime localDateTimeToOffsetDateTime(final LocalDateTime localDateTime) {
        return Objects.isNull(localDateTime) ? null : OffsetDateTime.of(localDateTime, ZoneOffset.UTC);
    }

    public static <T> T parseResponseTo(final ResultActions response, Class<T> responseType) throws IOException {
        final var responseContent = response.andReturn().getResponse().getContentAsByteArray();
        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper.readValue(responseContent, responseType);
    }

    public static <T> Map<String, Object> toMap(final T object) {
        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper.convertValue(object, new TypeReference<>() {});
    }

    public static <T> T fromMap(final Map<String, Object> map, Class<T> clazz) {
        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper.convertValue(map, clazz);
    }

}
