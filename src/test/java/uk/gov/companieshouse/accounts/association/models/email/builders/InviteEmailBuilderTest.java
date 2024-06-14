package uk.gov.companieshouse.accounts.association.models.email.builders;


import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.accounts.association.models.email.data.InviteEmailData;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class InviteEmailBuilderTest {

    private final String COMPANY_INVITATIONS_URL = "http://chs.local/your-companies/company-invitations?mtm_campaign=associations_invite";

    @Test
    void buildInstantiatesEmailData() {
        final var expectedEmailData = getInviteEmailData();

        var actualEmailBuilder = new InviteEmailBuilder();
        ReflectionTestUtils.setField(actualEmailBuilder, "invitationLink", COMPANY_INVITATIONS_URL);

        final var actualEmailData = actualEmailBuilder
                .setRecipientEmail( "kpatel@companieshouse.gov.uk" )
                .setInviterDisplayName( "Krishna Patel" )
                .setCompanyName( "Tesla" )
                .setInvitationExpiryTimestamp( "1992-05-01T10:30:00.000000" )
                .build();

        Assertions.assertEquals( expectedEmailData, actualEmailData );
    }

    private @NotNull InviteEmailData getInviteEmailData() {
        final var expectedEmailData = new InviteEmailData();
        expectedEmailData.setTo( "kpatel@companieshouse.gov.uk" );
        expectedEmailData.setSubject( "Companies House: invitation to be authorised to file online for Tesla" );
        expectedEmailData.setInviterDisplayName( "Krishna Patel" );
        expectedEmailData.setCompanyName( "Tesla" );
        expectedEmailData.setInvitationExpiryTimestamp( "1992-05-01T10:30:00.000000" );
        expectedEmailData.setInvitationLink( COMPANY_INVITATIONS_URL );
        return expectedEmailData;
    }

    @Test
    void buildWithNullsThrowsNullPointerException(){
        final var inviteEmailBuilder = new InviteEmailBuilder();
        Assertions.assertThrows( NullPointerException.class, inviteEmailBuilder::build);
    }

}
