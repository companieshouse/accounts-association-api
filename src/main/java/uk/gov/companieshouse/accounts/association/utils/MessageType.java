package uk.gov.companieshouse.accounts.association.utils;

public enum MessageType {

    AUTH_CODE_CONFIRMATION_MESSAGE_TYPE("associations_authorised_person_added_themselves"),

    AUTHORISATION_REMOVED_MESSAGE_TYPE("associations_authorised_person_removed"),

    INVITATION_CANCELLED_MESSAGE_TYPE("associations_invitation_cancelled"),

    INVITATION_MESSAGE_TYPE("associations_invited_to_be_authorised"),

    INVITATION_ACCEPTED_MESSAGE_TYPE("associations_invitee_accepted"),

    INVITATION_REJECTED_MESSAGE_TYPE("associations_invitee_declined"),

    INVITE_MESSAGE_TYPE("associations_invite");

    private final String messageType;

    MessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageType() {
        return messageType;
    }
}
