package uk.gov.companieshouse.accounts.association.utils;

public enum MessageType {

    AUTH_CODE_CONFIRMATION_MESSAGE_TYPE( "associations_authorised_person_added_themselves" ),

    YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE( "associations_authorisation_to_file_online_removed" ),

    AUTHORISATION_REMOVED_MESSAGE_TYPE( "associations_authorised_person_removed" ),

    INVITATION_CANCELLED_MESSAGE_TYPE( "associations_invitation_cancelled" ),

    INVITATION_MESSAGE_TYPE( "associations_invited_to_be_authorised" ),

    INVITATION_ACCEPTED_MESSAGE_TYPE( "associations_invitee_accepted" ),

    INVITATION_REJECTED_MESSAGE_TYPE( "associations_invitee_declined" ),

    INVITE_MESSAGE_TYPE( "associations_invite" ),

    DELEGATED_REMOVAL_OF_MIGRATED_BATCH( "associations_delegated_removal_of_migrated_batch" ),

    DELEGATED_REMOVAL_OF_MIGRATED( "associations_delegated_removal_of_migrated" ),

    REMOVAL_OF_OWN_MIGRATED( "associations_removal_of_own_migrated" ),

    INVITE_CANCELLED_MESSAGE_TYPE( "associations_invite_cancelled" ),

    REA_DIGITAL_AUTHORISATION_ADDED_MESSAGE_TYPE( "associations_rea_digital_authorisation_added" ),

    REA_DIGITAL_AUTHORISATION_REMOVED_MESSAGE_TYPE( "associations_rea_digital_authorisation_removed" );

    private final String value;

    MessageType( final String messageType ) {
        this.value = messageType;
    }

    public String getValue() {
        return value;
    }
}
