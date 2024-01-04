package uk.gov.companieshouse.accounts.association.unit.utils;

import static uk.gov.companieshouse.accounts.association.utils.Date.isBeforeNow;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DateTest {

    @Test
    void isBeforeNowWithNullTimestampShouldThrowNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> isBeforeNow( null ) );
    }

    @Test
    void isBeforeNowWithInvalidTimestampShouldThrowDateTimeParseException(){
        Assertions.assertThrows( DateTimeParseException.class, () -> isBeforeNow( "hello" ) );
    }

    @Test
    void isBeforeNowWithPastTimestampShouldReturnTrue(){
        final var yesterday = Instant.now().minus( 1, ChronoUnit.DAYS ).toString();
        Assertions.assertTrue( isBeforeNow( yesterday ) );
    }

    @Test
    void isBeforeNowWithFutureTimestampShouldReturnFalse(){
        final var tomorrow = Instant.now().plus( 1, ChronoUnit.DAYS ).toString();
        Assertions.assertFalse( isBeforeNow( tomorrow ) );
    }

}