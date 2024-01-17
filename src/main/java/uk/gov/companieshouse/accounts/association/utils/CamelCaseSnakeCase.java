package uk.gov.companieshouse.accounts.association.utils;

import org.apache.commons.lang3.StringUtils;

public final class CamelCaseSnakeCase {

    private CamelCaseSnakeCase(){}

    public static String toSnakeCase(String camelCaseString) {

        if (StringUtils.isBlank(camelCaseString)) {
            return camelCaseString;
        }

        return camelCaseString.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2").replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

}