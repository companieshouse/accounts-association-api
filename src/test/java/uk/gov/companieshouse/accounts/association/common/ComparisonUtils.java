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
import uk.gov.companieshouse.accounts.association.models.email.data.*;
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

    public ArgumentMatcher<InvitationAcceptedEmailData> invitationAcceptedEmailDataMatcher( final List<String> to, final InvitationAcceptedEmailData expectedBaseEmail ){
        return emailData -> to.stream()
                .map( expectedBaseEmail::to )
                .map( InvitationAcceptedEmailData::subject )
                .map( expectEmail -> compare( expectEmail, List.of( "authorisedPerson", "personWhoCreatedInvite", "companyName", "to", "subject" ), List.of(), Map.of() ) )
                .map( matcher -> matcher.matches( emailData ) )
                .reduce( (first, second) -> first || second )
                .get();
    }

    public ArgumentMatcher<EmailData> invitationAndInviteEmailDataMatcher( final String inviterEmail, final String inviterDisplayName, final String inviteeEmail, final String inviteeDisplayName, final String companyName, final String invitationLink ){
        final var expectedInvitationEmail = new InvitationEmailData()
                .personWhoCreatedInvite( inviterDisplayName )
                .invitee( inviteeDisplayName )
                .companyName( companyName )
                .to( inviterEmail )
                .subject();

        final var expectedInviteEmail = new InviteEmailData()
                .inviterDisplayName( inviterDisplayName )
                .companyName( companyName )
                .invitationLink( invitationLink )
                .to( inviteeEmail )
                .subject()
                .invitationExpiryTimestamp( now.plusDays( 7 ).toString() );

        return emailData -> {
            if ( emailData instanceof InvitationEmailData) return compare( expectedInvitationEmail, List.of( "personWhoCreatedInvite", "invitee", "companyName", "to", "subject" ), List.of(), Map.of() ).matches( (InvitationEmailData) emailData );
            if ( emailData instanceof InviteEmailData) return compare( expectedInviteEmail, List.of( "inviterDisplayName", "companyName", "invitationLink", "to", "subject" ), List.of(), Map.of() ).matches( (InviteEmailData) emailData );
            return false;
        };
    }

    public ArgumentMatcher<EmailData> authCodeConfirmationEmailMatcher( final String recipientEmail, final String companyName, final String displayName ){
        final var expectedEmail = new AuthCodeConfirmationEmailData()
                .to( recipientEmail )
                .companyName( companyName )
                .authorisedPerson( displayName );

        return compare( expectedEmail, List.of( "authorisedPerson", "companyName", "to", "subject" ), List.of(), Map.of() );
    }

    public ArgumentMatcher<EmailData> authorisationRemovedAndYourAuthorisationRemovedEmailMatcher( final String personWhoRemovedAuthorisation, final String personWhoWasRemoved, final String companyName, final String authorisationRemovedRecipientEmail, final String to ){
        final var expectedAuthorisationRemovedEmail = new AuthorisationRemovedEmailData()
                .personWhoRemovedAuthorisation( personWhoRemovedAuthorisation )
                .personWhoWasRemoved( personWhoWasRemoved )
                .companyName( companyName )
                .to( authorisationRemovedRecipientEmail )
                .subject();

        final var expectedYourAuthorisationRemovedEmail = new YourAuthorisationRemovedEmailData()
                .personWhoRemovedAuthorisation( personWhoRemovedAuthorisation )
                .companyName( companyName )
                .to( to )
                .subject();

        return emailData -> {
            if ( emailData instanceof AuthorisationRemovedEmailData ) return compare( expectedAuthorisationRemovedEmail, List.of( "personWhoWasRemoved", "personWhoRemovedAuthorisation", "companyName", "to", "subject" ), List.of(), Map.of() ).matches( (AuthorisationRemovedEmailData) emailData );
            if ( emailData instanceof YourAuthorisationRemovedEmailData ) return compare( expectedYourAuthorisationRemovedEmail, List.of( "personWhoRemovedAuthorisation", "companyName", "to", "subject" ), List.of(), Map.of() ).matches( (YourAuthorisationRemovedEmailData) emailData );
            return false;
        };
    }

    public ArgumentMatcher<EmailData> invitationCancelledAndInviteCancelledEmailMatcher( final String to, final String personWhoCancelledInvite, final String personWhoWasCancelled, final String companyName, final String cancelledByEmail ){
        final var expectedInvitationCancelledEmail = new InvitationCancelledEmailData()
                .to( to )
                .personWhoCancelledInvite( personWhoCancelledInvite )
                .personWhoWasCancelled( personWhoWasCancelled )
                .companyName( companyName )
                .subject();

        final var expectedCancelledInviteEmail = new InviteCancelledEmailData()
                .to( cancelledByEmail )
                .companyName( companyName )
                .cancelledBy( personWhoCancelledInvite )
                .subject();

        return emailData -> {
            if ( emailData instanceof InvitationCancelledEmailData ) return compare( expectedInvitationCancelledEmail, List.of(  "personWhoWasCancelled", "companyName", "personWhoCancelledInvite", "to", "subject" ), List.of(), Map.of() ).matches( (InvitationCancelledEmailData) emailData );
            if ( emailData instanceof InviteCancelledEmailData ) return compare( expectedCancelledInviteEmail, List.of( "cancelledBy", "companyName", "to", "subject" ), List.of(), Map.of() ).matches( (InviteCancelledEmailData) emailData );
            return false;
        };
    }

    public ArgumentMatcher<EmailData> invitationRejectedEmailMatcher( final String to, final String personWhoDeclined, final String companyName ){
        final var expectedEmail = new InvitationRejectedEmailData()
                .to( to )
                .personWhoDeclined( personWhoDeclined )
                .companyName( companyName )
                .subject();

        return compare( expectedEmail, List.of( "personWhoDeclined", "companyName", "to", "subject" ), List.of(), Map.of() );
    }

    public ArgumentMatcher<EmailData> delegatedRemovalOfMigratedBatchMatcher( final String recipientEmail,  final String companyName, final String removedBy, final String removedUser ){
       // final var subject = String.format("Companies House: %s's digital authorisation not restored for %s", removedUser, companyName);

        final var expectedEmail = new DelegatedRemovalOfMigratedBatchEmailData()
                .removedBy( removedBy )
                .removedUser( removedUser )
                .to( recipientEmail )
                .companyName( companyName )
                .subject();

        return compare( expectedEmail, List.of( "removedBy", "removedUser", "companyName", "to", "subject" ), List.of(), Map.of() );
    }

    public ArgumentMatcher<EmailData> delegatedRemovalOfMigratedMatcher( final String to, final String companyName, final String removedBy ){
        final var expectedEmail = new DelegatedRemovalOfMigratedEmailData()
                .removedBy( removedBy )
                .to( to )
                .companyName( companyName )
                .subject();

        return compare( expectedEmail, List.of( "removedBy", "companyName", "to", "subject" ), List.of(), Map.of() );
    }

    public ArgumentMatcher<EmailData> removalOfOwnMigratedMatcher( final String to, final String companyName ){
        final var expectedEmail = new RemovalOfOwnMigratedEmailData()
                .to( to )
                .companyName( companyName )
                .subject();

        return compare( expectedEmail, List.of( "companyName", "to", "subject" ), List.of(), Map.of() );
    }

    public ArgumentMatcher<EmailData> delegatedRemovalOfMigratedAndBatchEmailMatcher( final String to, final String removedUser, final String confirmedUserEmail, final String removedBy, final String companyName ){
        final var delegatedRemovalOfMigratedEmail = new DelegatedRemovalOfMigratedEmailData()
                .to( to )
                .companyName( companyName )
                .removedBy( removedBy )
                .subject();

        final var delegatedRemovalOfMigratedBatchEmail = new DelegatedRemovalOfMigratedBatchEmailData()
                .to( confirmedUserEmail )
                .companyName( companyName )
                .removedBy( removedBy )
                .removedUser( removedUser )
                .subject();

        return emailData -> {
            if ( emailData instanceof DelegatedRemovalOfMigratedEmailData ) return compare( delegatedRemovalOfMigratedEmail, List.of( "removedBy", "companyName", "to", "subject" ), List.of(), Map.of() ).matches( (DelegatedRemovalOfMigratedEmailData) emailData );
            if ( emailData instanceof DelegatedRemovalOfMigratedBatchEmailData ) return compare( delegatedRemovalOfMigratedBatchEmail, List.of( "removedUser", "removedBy", "companyName", "to", "subject" ), List.of(), Map.of() ).matches( (DelegatedRemovalOfMigratedBatchEmailData) emailData );
            return false;
        };
    }

    public ArgumentMatcher<EmailData> removalOfOwnMigratedEmailAndBatchMatcher( final String to, final String removedUser, final String confirmedUserEmail, final String removedBy, final String companyName ){
        final var removalOfOwnMigratedEmail = new RemovalOfOwnMigratedEmailData()
                .to( to )
                .companyName( companyName )
                .subject();

        final var delegatedRemovalOfMigratedBatchEmail = new DelegatedRemovalOfMigratedBatchEmailData()
                .to( confirmedUserEmail ) //TODO: check this is the correct email, as its above
                .companyName( companyName )
                .removedBy( removedBy )
                .removedUser( removedUser )
                .subject();

        return emailData -> {
            if ( emailData instanceof RemovalOfOwnMigratedEmailData ) return compare( removalOfOwnMigratedEmail, List.of( "companyName", "to", "subject" ), List.of(), Map.of() ).matches( (RemovalOfOwnMigratedEmailData) emailData );
            if ( emailData instanceof DelegatedRemovalOfMigratedBatchEmailData ) return compare( delegatedRemovalOfMigratedBatchEmail, List.of( "removedUser", "removedBy", "companyName", "to", "subject" ), List.of(), Map.of() ).matches( (DelegatedRemovalOfMigratedBatchEmailData) emailData );
            return false;
        };
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
