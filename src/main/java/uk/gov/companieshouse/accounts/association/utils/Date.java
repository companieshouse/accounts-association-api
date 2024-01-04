package uk.gov.companieshouse.accounts.association.utils;

import java.time.Instant;

public class Date {

    public static boolean isBeforeNow( String timestamp ){
        return Instant.parse( timestamp )
                      .isBefore( Instant.now() );
    }

    public static String now(){
        return Instant.now()
                      .toString();
    }

}
