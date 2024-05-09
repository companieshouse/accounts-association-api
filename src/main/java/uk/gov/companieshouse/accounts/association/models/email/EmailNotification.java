package uk.gov.companieshouse.accounts.association.models.email;

import uk.gov.companieshouse.accounts.association.utils.MessageType;

import java.time.LocalDateTime;

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
