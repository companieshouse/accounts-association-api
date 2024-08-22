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
import uk.gov.companieshouse.accounts.association.models.email.builders.AuthCodeConfirmationEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.AuthorisationRemovedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationAcceptedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationCancelledEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationRejectedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InviteCancelledEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InviteEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationAcceptedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InviteEmailData;
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

    public ArgumentMatcher<EmailData> authorisationRemovedEmailMatcher( final String removedByDisplayName, final String removedUserDisplayName, final String companyName, final String recipientEmail ){
        final var expectedEmail = new AuthorisationRemovedEmailBuilder()
                .setRemovedByDisplayName( removedByDisplayName )
                .setRemovedUserDisplayName( removedUserDisplayName )
                .setCompanyName( companyName )
                .setRecipientEmail( recipientEmail )
                .build();

        return compare( expectedEmail, List.of( "personWhoWasRemoved", "personWhoRemovedAuthorisation", "companyName", "to", "subject" ), List.of(), Map.of() );
    }

    public ArgumentMatcher<EmailData> invitationCancelledEmailMatcher( final String recipientEmail, final String cancelledByDisplayName, final String cancelledUserDisplayName, final String companyName ){
        final var expectedEmail = new InvitationCancelledEmailBuilder()
                .setRecipientEmail( recipientEmail )
                .setCancelledByDisplayName( cancelledByDisplayName )
                .setCancelledUserDisplayName( cancelledUserDisplayName )
                .setCompanyName( companyName )
                .build();

        return compare( expectedEmail, List.of( "personWhoWasCancelled", "companyName", "personWhoCancelledInvite", "to", "subject" ), List.of(), Map.of() );
    }

    public ArgumentMatcher<EmailData> invitationRejectedEmailMatcher( final String recipientEmail, final String inviteeDisplayName, final String companyName ){
        final var expectedEmail = new InvitationRejectedEmailBuilder()
                .setRecipientEmail( recipientEmail )
                .setInviteeDisplayName( inviteeDisplayName )
                .setCompanyName( companyName )
                .build();

        return compare( expectedEmail, List.of( "personWhoDeclined", "companyName", "to", "subject" ), List.of(), Map.of() );
    }

    public ArgumentMatcher<EmailData> inviteCancelledEmailMatcher( final String recipientEmail, final String companyName, final String cancelledBy ){
        final var expectedEmail = new InviteCancelledEmailBuilder()
                .setRecipientEmail( recipientEmail )
                .setCompanyName( companyName )
                .setCancelledBy( cancelledBy )
                .build();

        return compare( expectedEmail, List.of( "cancelledBy", "companyName", "to", "subject" ), List.of(), Map.of() );
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
