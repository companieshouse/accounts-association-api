package uk.gov.companieshouse.accounts.association.unit.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.accounts.association.enums.StatusEnum;

public class StatusEnumTest {

    @Test
    void containsWithInvalidAssociationStatusReturnsFalse(){
        Assertions.assertFalse( StatusEnum.contains( null ) );
        Assertions.assertFalse( StatusEnum.contains( "It's complicated..." ) );
    }

    @Test
    void containsWithValidAssociationStatusReturnsTrue(){
        Assertions.assertTrue( StatusEnum.contains( "Confirmed" ) );
        Assertions.assertTrue( StatusEnum.contains( "Removed" ) );
    }

}
