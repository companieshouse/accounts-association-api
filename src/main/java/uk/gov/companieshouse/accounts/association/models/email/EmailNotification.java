package uk.gov.companieshouse.accounts.association.models.email;

import com.google.api.client.util.DateTime;
import uk.gov.companieshouse.accounts.association.utils.MessageType;

import java.time.LocalDateTime;

public class EmailNotification {

    private MessageType messageType;
    private String sentFrom;
    private String sentTo;
    private String companyNumber;
    private LocalDateTime sentTime;

    public EmailNotification(MessageType messageType, String sentFrom, String sentTo, String companyNumber) {
        this.messageType = messageType;
        this.sentFrom = sentFrom;
        this.sentTo = sentTo;
        this.companyNumber = companyNumber;
        this.sentTime = LocalDateTime.now();
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
