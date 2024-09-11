package uk.gov.companieshouse.accounts.association.utils;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StaticPropertyUtil {

    @Value( "${spring.application.name}" )
    private String applicationNameSpace;

    public static String APPLICATION_NAMESPACE;

    public static final int DAYS_SINCE_INVITE_TILL_EXPIRES = 7;

    @PostConstruct
    public void init(){
        StaticPropertyUtil.APPLICATION_NAMESPACE = applicationNameSpace;
    }

}
