package uk.gov.companieshouse.accounts.association.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StaticPropertyUtil {

    public static String APPLICATION_NAMESPACE;
    public static int DAYS_SINCE_INVITE_TILL_EXPIRES = 7;

    public StaticPropertyUtil(
            @Value("${spring.application.name}") String applicationNameSpace,
            @Value("${invite.expiry.days:7}") int daysSinceInviteTillExpires
    ) {
        StaticPropertyUtil.APPLICATION_NAMESPACE = applicationNameSpace;
        StaticPropertyUtil.DAYS_SINCE_INVITE_TILL_EXPIRES = daysSinceInviteTillExpires;
    }
}
