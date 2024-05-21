package uk.gov.companieshouse.accounts.association.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StaticPropertyUtil {

    public static String APPLICATION_NAMESPACE;

    public StaticPropertyUtil(@Value("${spring.application.name}") String applicationNameSpace ) {
        StaticPropertyUtil.APPLICATION_NAMESPACE = applicationNameSpace;
    }

}
