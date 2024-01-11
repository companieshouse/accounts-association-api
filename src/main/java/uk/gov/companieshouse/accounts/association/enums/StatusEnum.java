package uk.gov.companieshouse.accounts.association.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StatusEnum {


    AWAITING_CONFIRMATION("Awaiting Confirmation"), REMOVED("Removed"), CONFIRMED("Confirmed");

    private final String value;

    StatusEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }

    public static boolean contains( String status ){
        for ( StatusEnum statusEnum: StatusEnum.values() ){
            if ( statusEnum.value.equals( status ) ) {
                return true;
            }
        }
        return false;
    }

}