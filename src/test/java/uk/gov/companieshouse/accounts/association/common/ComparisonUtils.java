package uk.gov.companieshouse.accounts.association.common;

import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.toMap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.mockito.ArgumentMatcher;
import org.springframework.data.domain.Page;
import uk.gov.companieshouse.accounts.association.common.Preprocessors.Preprocessor;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.email.builders.*;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthorisationRemovedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationAcceptedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationCancelledEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InviteCancelledEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InviteEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.YourAuthorisationRemovedEmailData;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class ComparisonUtils {

    private final LocalDateTime now = LocalDateTime.now();

    private boolean mapsMatchOnAll( final Map<String, Object> actualMap, final Map<String, Object> referenceMap, final List<String> matchingFields ){
        for ( final String key: matchingFields ){
            final var referenceValue = referenceMap.getOrDefault( key, null );
            final var actualValue = actualMap.getOrDefault( key, null );

            final var referenceIsNullButActualIsNotNull = Objects.isNull( referenceValue ) && !Objects.isNull( actualValue );
            final var referenceDoesNotMatchActual = !Objects.isNull( referenceValue ) && !referenceValue.equals( actualValue );

            if ( referenceIsNullButActualIsNotNull || referenceDoesNotMatchActual ){
                return false;
            }
        }
        return true;
    }

    private boolean mapsDoNotMatchOnAll( final Map<String, Object> actualMap, final Map<String, Object> referenceMap, final List<String> nonmatchingFields ){
        for ( final String key: nonmatchingFields ){
            final var referenceValue = referenceMap.getOrDefault( key, null );
            final var actualValue = actualMap.getOrDefault( key, null );

            final var referenceIsNullAndActualIsNull = Objects.isNull( referenceValue ) && Objects.isNull( actualValue );
            final var referenceMatchesActual = !Objects.isNull( referenceValue ) && referenceValue.equals( actualValue );

            if ( referenceIsNullAndActualIsNull || referenceMatchesActual ){
                return false;
            }

        }
        return true;
    }

    private void applyPreprocessorsToMap( final Map<String, Object> map, final Map<String, Preprocessor> preprocessors ){
        for ( final Entry<String, Preprocessor> entry: preprocessors.entrySet() ){
            final var key = entry.getKey();
            final var preprocessor = entry.getValue();
            final var value = map.getOrDefault( key, null );
            final var preprocesedValue = preprocessor.preprocess( value );
            map.put( key, preprocesedValue );
        }
    }

    public <T> ArgumentMatcher<T> compare( final T referenceObject, final List<String> matchingFields, final List<String> nonmatchingFields, final Map<String, Preprocessor> preprocessors ) {
        final var referenceMap = toMap( referenceObject );
        applyPreprocessorsToMap( referenceMap, preprocessors );
        return actualObject -> {
            final var actualMap = toMap( actualObject );
            applyPreprocessorsToMap( actualMap, preprocessors );
            return mapsMatchOnAll( referenceMap, actualMap, matchingFields ) && mapsDoNotMatchOnAll( referenceMap, actualMap, nonmatchingFields );
        };
    }

    public ArgumentMatcher<InvitationAcceptedEmailData> invitationAcceptedEmailDataMatcher( final List<String> to, final InvitationAcceptedEmailBuilder expectedBaseEmail ){
        return emailData -> to.stream()
                .map( expectedBaseEmail::setRecipientEmail )
                .map( InvitationAcceptedEmailBuilder::build )
                .map( expectEmail -> compare( expectEmail, List.of( "authorisedPerson", "personWhoCreatedInvite", "companyName", "to", "subject" ), List.of(), Map.of() ) )
                .map( matcher -> matcher.matches( emailData ) )
                .reduce( (first, second) -> first || second )
                .get();
    }

    public ArgumentMatcher<EmailData> invitationAndInviteEmailDataMatcher( final String inviterEmail, final String inviterDisplayName, final String inviteeEmail, final String inviteeDisplayName, final String companyName, final String companyInvitationsUrl ){
        final var expectedInvitationEmail = new InvitationEmailBuilder()
                .setInviterDisplayName( inviterDisplayName )
                .setInviteeDisplayName( inviteeDisplayName )
                .setCompanyName( companyName )
                .setRecipientEmail( inviterEmail )
                .build();

        final var expectedInviteEmail = new InviteEmailBuilder()
                .setInviterDisplayName( inviterDisplayName )
                .setCompanyName( companyName )
                .setInvitationLink( companyInvitationsUrl )
                .setRecipientEmail( inviteeEmail )
                .setInvitationExpiryTimestamp( now.plusDays( 7 ).toString() )
                .build();

        return emailData -> {
            if ( emailData instanceof InvitationEmailData) return compare( expectedInvitationEmail, List.of( "personWhoCreatedInvite", "invitee", "companyName", "to", "subject" ), List.of(), Map.of() ).matches( (InvitationEmailData) emailData );
            if ( emailData instanceof InviteEmailData) return compare( expectedInviteEmail, List.of( "inviterDisplayName", "companyName", "invitationLink", "to", "subject" ), List.of(), Map.of() ).matches( (InviteEmailData) emailData );
            return false;
        };
    }

    public ArgumentMatcher<EmailData> authCodeConfirmationEmailMatcher( final String recipientEmail, final String companyName, final String displayName ){
        final var expectedEmail = new AuthCodeConfirmationEmailBuilder()
                .setRecipientEmail( recipientEmail )
                .setCompanyName( companyName )
                .setDisplayName( displayName )
                .build();

        return compare( expectedEmail, List.of( "authorisedPerson", "companyName", "to", "subject" ), List.of(), Map.of() );
    }

    public ArgumentMatcher<EmailData> authorisationRemovedAndYourAuthorisationRemovedEmailMatcher( final String removedByDisplayName, final String removedUserDisplayName, final String companyName, final String authorisationRemovedRecipientEmail, final String yourAuthorisationRemovedRecipientEmail ){
        final var expectedAuthorisationRemovedEmail = new AuthorisationRemovedEmailBuilder()
                .setRemovedByDisplayName( removedByDisplayName )
                .setRemovedUserDisplayName( removedUserDisplayName )
                .setCompanyName( companyName )
                .setRecipientEmail( authorisationRemovedRecipientEmail )
                .build();

        final var expectedYourAuthorisationRemovedEmail = new YourAuthorisationRemovedEmailBuilder()
                .setRemovedByDisplayName( removedByDisplayName )
                .setCompanyName( companyName )
                .setRecipientEmail( yourAuthorisationRemovedRecipientEmail )
                .build();

        return emailData -> {
            if ( emailData instanceof AuthorisationRemovedEmailData ) return compare( expectedAuthorisationRemovedEmail, List.of( "personWhoWasRemoved", "personWhoRemovedAuthorisation", "companyName", "to", "subject" ), List.of(), Map.of() ).matches( (AuthorisationRemovedEmailData) emailData );
            if ( emailData instanceof YourAuthorisationRemovedEmailData ) return compare( expectedYourAuthorisationRemovedEmail, List.of( "personWhoRemovedAuthorisation", "companyName", "to", "subject" ), List.of(), Map.of() ).matches( (YourAuthorisationRemovedEmailData) emailData );
            return false;
        };
    }

    public ArgumentMatcher<EmailData> invitationCancelledAndInviteCancelledEmailMatcher( final String invitationCancelledRecipientEmail, final String cancelledByDisplayName, final String cancelledUserDisplayName, final String companyName, final String inviteCancelledRecipientEmail ){
        final var expectedInvitationCancelledEmail = new InvitationCancelledEmailBuilder()
                .setRecipientEmail( invitationCancelledRecipientEmail )
                .setCancelledByDisplayName( cancelledByDisplayName )
                .setCancelledUserDisplayName( cancelledUserDisplayName )
                .setCompanyName( companyName )
                .build();

        final var expectedCancelledInviteEmail = new InviteCancelledEmailBuilder()
                .setRecipientEmail( inviteCancelledRecipientEmail )
                .setCompanyName( companyName )
                .setCancelledBy( cancelledByDisplayName )
                .build();

        return emailData -> {
            if ( emailData instanceof InvitationCancelledEmailData ) return compare( expectedInvitationCancelledEmail, List.of( "personWhoWasCancelled", "companyName", "personWhoCancelledInvite", "to", "subject" ), List.of(), Map.of() ).matches( (InvitationCancelledEmailData) emailData );
            if ( emailData instanceof InviteCancelledEmailData ) return compare( expectedCancelledInviteEmail, List.of( "cancelledBy", "companyName", "to", "subject" ), List.of(), Map.of() ).matches( (InviteCancelledEmailData) emailData );
            return false;
        };
    }

    public ArgumentMatcher<EmailData> invitationRejectedEmailMatcher( final String recipientEmail, final String inviteeDisplayName, final String companyName ){
        final var expectedEmail = new InvitationRejectedEmailBuilder()
                .setRecipientEmail( recipientEmail )
                .setInviteeDisplayName( inviteeDisplayName )
                .setCompanyName( companyName )
                .build();

        return compare( expectedEmail, List.of( "personWhoDeclined", "companyName", "to", "subject" ), List.of(), Map.of() );
    }

    public ArgumentMatcher<EmailData> delegatedRemovalOfMigratedBatchMatcher( final String recipientEmail, final String companyName, final String removedBy, final String removedUser ){
        final var expectedEmail = new DelegatedRemovalOfMigratedBatchEmailBuilder()
                .setRemovedBy( removedBy )
                .setRemovedUser( removedUser )
                .setRecipientEmail( recipientEmail )
                .setCompanyName( companyName )
                .build();

        return compare( expectedEmail, List.of( "removedBy", "removedUser", "companyName", "to", "subject" ), List.of(), Map.of() );
    }

    public ArgumentMatcher<EmailData> delegatedRemovalOfMigratedMatcher( final String recipientEmail, final String companyName, final String removedBy ){
        final var expectedEmail = new DelegatedRemovalOfMigratedEmailBuilder()
                .setRemovedBy( removedBy )
                .setRecipientEmail( recipientEmail )
                .setCompanyName( companyName )
                .build();

        return compare( expectedEmail, List.of( "removedBy", "companyName", "to", "subject" ), List.of(), Map.of() );
    }

    public ArgumentMatcher<EmailData> removalOfOwnMigratedMatcher( final String recipientEmail, final String companyName ){
        final var expectedEmail = new RemovalOfOwnMigratedEmailBuilder()
                .setRecipientEmail( recipientEmail )
                .setCompanyName( companyName )
                .build();

        return compare( expectedEmail, List.of( "companyName", "to", "subject" ), List.of(), Map.of() );
    }

    public ArgumentMatcher<Page<AssociationDao>> associationsPageMatches( final int totalElements, final int totalPages, final int numElementsOnPage, final List<String> expectedAssociationIds ){
        return page -> {
            final var associationIds =
                    page.getContent()
                            .stream()
                            .map( AssociationDao::getId )
                            .toList();

            return page.getTotalElements() == totalElements &&
                    page.getTotalPages() == totalPages &&
                    associationIds.size() == numElementsOnPage &&
                    associationIds.containsAll( expectedAssociationIds );
        };
    }



}
