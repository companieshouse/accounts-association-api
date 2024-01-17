package uk.gov.companieshouse.accounts.association.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit-test")
class StatusEnumTest {

    @Test
    void containsWithInvalidAssociationStatusReturnsFalse() {
        Assertions.assertFalse(StatusEnum.contains(null));
        Assertions.assertFalse(StatusEnum.contains("It's complicated..."));
    }

    @Test
    void containsWithValidAssociationStatusReturnsTrue() {
        Assertions.assertTrue(StatusEnum.contains("Confirmed"));
        Assertions.assertTrue(StatusEnum.contains("Removed"));
    }

}
