package uk.gov.companieshouse.accounts.association.models.email;

import java.time.LocalDateTime;
import uk.gov.companieshouse.accounts.association.utils.MessageType;

public class EmailNotification {

    private final MessageType messageType;
    private final String sentFrom;
    private final String sentTo;
    private final String companyNumber;
    private final LocalDateTime sentTime;


    public EmailNotification(MessageType messageType, String sentFrom, String sentTo, String companyNumber) {
        this.messageType = messageType;
        this.sentFrom = sentFrom;
        this.sentTo = sentTo;
        this.companyNumber = companyNumber;
        this.sentTime = LocalDateTime.now();
    }

    public String toMessage(){
        return String.format( "%s notification sent to %s at %s from %s, regarding company %s.", messageType, sentTo, sentTime.toString(), sentFrom, companyNumber );
    }

    @Override
    public String toString() {
        return "EmailNotification{" +
                "messageType=" + messageType +
                ", sentFrom='" + sentFrom + '\'' +
                ", sentTo='" + sentTo + '\'' +
                ", companyNumber='" + companyNumber + '\'' +
                ", sentTime=" + sentTime +
                '}';
    }
}
