package uk.gov.companieshouse.accounts.association.models.email.senders;

import static org.mockito.ArgumentMatchers.any;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_ACCEPTED_MESSAGE_TYPE;

import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationAcceptedEmailData;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class InvitationAcceptedEmailSenderTest {

    @Mock
    EmailProducer emailProducer;

    InvitationAcceptedEmailSender invitationAcceptedEmailSender;

    @BeforeEach
    public void setup(){
        invitationAcceptedEmailSender = new InvitationAcceptedEmailSender( emailProducer );
    }

    @Test
    void sendInvitationAcceptedEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> invitationAcceptedEmailSender.sendEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( null ) );
        Assertions.assertThrows( NullPointerException.class, () -> invitationAcceptedEmailSender.sendEmail( null, "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> invitationAcceptedEmailSender.sendEmail( "Krishna Patel", null, "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> invitationAcceptedEmailSender.sendEmail( "Krishna Patel", "Elon Musk", null ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendInvitationAcceptedEmailWithUnexpectedIssueThrowsBadRequestRuntimeException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> invitationAcceptedEmailSender.sendEmail(  "Krishna Patel", "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendInvitationAcceptedEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new InvitationAcceptedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk is now authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setAuthorisedPerson( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        invitationAcceptedEmailSender.sendEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" );
        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_ACCEPTED_MESSAGE_TYPE );
    }

    @Test
    void sendEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        final var emailData = new InvitationAcceptedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk is now authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setAuthorisedPerson( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Assertions.assertThrows( NullPointerException.class, () -> invitationAcceptedEmailSender.sendEmailToAssociatedUsers( "theId12345", null, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> invitationAcceptedEmailSender.sendEmailToAssociatedUsers( "theId12345", new CompanyDetails(), "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> invitationAcceptedEmailSender.sendEmailToAssociatedUsers( "theId12345", companyDetails, null, "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> invitationAcceptedEmailSender.sendEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", null, requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> invitationAcceptedEmailSender.sendEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", null ) );
    }

    @Test
    void sendEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var emailData = new InvitationAcceptedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk is now authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setAuthorisedPerson( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        invitationAcceptedEmailSender.sendEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers );

        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_ACCEPTED_MESSAGE_TYPE );
    }

    @Test
    void sendEmailToAssociatedUsersDoesNotThrowEmailOnKafkaQueue(){
        final var emailData = new InvitationAcceptedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk is now authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setAuthorisedPerson( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        invitationAcceptedEmailSender.sendEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", List.of() );

        Mockito.verify( emailProducer, Mockito.never() ).sendEmail( emailData, INVITATION_ACCEPTED_MESSAGE_TYPE );
    }

    @Test
    void sendEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var emailData = new InvitationAcceptedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk is now authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setAuthorisedPerson( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> invitationAcceptedEmailSender.sendEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
    }
}
