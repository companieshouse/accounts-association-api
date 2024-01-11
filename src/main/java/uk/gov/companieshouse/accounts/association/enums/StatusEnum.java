package uk.gov.companieshouse.accounts.association.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StatusEnum {


    AWAITING_CONFIRMATION("Awaiting Confirmation"), DELETED("Deleted"), CONFIRMED("Confirmed");

    private String value;

    private StatusEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    public String toString() {
        return String.valueOf(this.value);
    }


}
