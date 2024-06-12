package uk.gov.companieshouse.accounts.association.models.email;

import uk.gov.companieshouse.accounts.association.utils.MessageType;

import java.time.LocalDateTime;
import java.util.Objects;

public class EmailNotification {

    private final MessageType messageType;
    private final String sentFrom;
    private final String sentTo;
    private final String companyNumber;
    private final LocalDateTime sentTime;
    private String invitationExpiryTimestamp;


    public EmailNotification(MessageType messageType, String sentFrom, String sentTo, String companyNumber) {
        this.messageType = messageType;
        this.sentFrom = sentFrom;
        this.sentTo = sentTo;
        this.companyNumber = companyNumber;
        this.sentTime = LocalDateTime.now();
    }

    public EmailNotification setInvitationExpiryTimestamp( final String invitationExpiryTimestamp ){
        this.invitationExpiryTimestamp = invitationExpiryTimestamp;
        return this;
    }

    public String toMessage(){
        var message = String.format( "%s notification sent to %s at %s from %s, regarding company %s.", messageType, sentTo, sentTime.toString(), sentFrom, companyNumber );
        message += Objects.isNull( invitationExpiryTimestamp ) ? "" : String.format( " Invitation expires at %s.", invitationExpiryTimestamp );
        return message;
    }

    @Override
    public String toString() {
        return "EmailNotification{" +
                "messageType=" + messageType +
                ", sentFrom='" + sentFrom + '\'' +
                ", sentTo='" + sentTo + '\'' +
                ", companyNumber='" + companyNumber + '\'' +
                ", sentTime=" + sentTime +
                ", invitationExpiryTimestamp='" + invitationExpiryTimestamp + '\'' +
                '}';
    }
}
