package uk.gov.companieshouse.accounts.association.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StaticPropertyUtil {

    public static String APPLICATION_NAMESPACE;
    public static String CHS_URL;

    public StaticPropertyUtil(@Value("${spring.application.name}") String applicationNameSpace, @Value("${chs.url}") String chsUrl ) {
        StaticPropertyUtil.APPLICATION_NAMESPACE = applicationNameSpace;
        StaticPropertyUtil.CHS_URL = chsUrl;
    }

}
