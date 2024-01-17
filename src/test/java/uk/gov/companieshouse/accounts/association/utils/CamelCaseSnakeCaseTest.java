package uk.gov.companieshouse.accounts.association.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit-test")
class CamelCaseSnakeCaseTest {

    @Test
    void testToSnakeCase() {
        assertEquals("word", CamelCaseSnakeCase.toSnakeCase("word"));
        assertEquals("two_words", CamelCaseSnakeCase.toSnakeCase("twoWords"));
        assertEquals("", CamelCaseSnakeCase.toSnakeCase(""));
    }

}