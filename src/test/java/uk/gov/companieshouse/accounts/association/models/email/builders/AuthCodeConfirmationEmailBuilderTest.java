package uk.gov.companieshouse.accounts.association.models.email.builders;

import static org.mockito.ArgumentMatchers.any;
import static uk.gov.companieshouse.accounts.association.utils.Constants.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;

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
import uk.gov.companieshouse.accounts.association.models.email.data.AuthCodeConfirmationEmailData;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AuthCodeConfirmationEmailBuilderTest {

    @Mock
    EmailProducer emailProducer;

    AuthCodeConfirmationEmailBuilder authCodeConfirmationEmailBuilder;

    @BeforeEach
    public void setup(){
        authCodeConfirmationEmailBuilder = new AuthCodeConfirmationEmailBuilder( emailProducer );
    }

    @Test
    void sendAuthCodeConfirmationEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> authCodeConfirmationEmailBuilder.sendEmail( "Krishna Patel", "Tesla" ).accept( null ) );
        Assertions.assertThrows( NullPointerException.class, () -> authCodeConfirmationEmailBuilder.sendEmail( (String) null, "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> authCodeConfirmationEmailBuilder.sendEmail( "Krishna Patel", null ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendAuthCodeConfirmationEmailWithUnexpectedIssueThrowsBadRequestRuntimeException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> authCodeConfirmationEmailBuilder.sendEmail( "Krishna Patel", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendAuthCodeConfirmationEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Krishna Patel is now authorised to file online for Tesla" );
        emailData.setAuthorisedPerson( "Krishna Patel" );
        emailData.setCompanyName( "Tesla" );

        authCodeConfirmationEmailBuilder.sendEmail( "Krishna Patel", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" );
        Mockito.verify( emailProducer ).sendEmail( emailData, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE );
    }

    @Test
    void sendEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullRequestsThrowsNullPointerException(){
        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Krishna Patel is now authorised to file online for Tesla" );
        emailData.setAuthorisedPerson( "Krishna Patel" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Assertions.assertThrows( NullPointerException.class, () -> authCodeConfirmationEmailBuilder.sendEmailToAssociatedUsers( "theId12345", null, "Krishna Patel", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> authCodeConfirmationEmailBuilder.sendEmailToAssociatedUsers( "theId12345", new CompanyDetails(), "Krishna Patel", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> authCodeConfirmationEmailBuilder.sendEmailToAssociatedUsers( "theId12345", companyDetails, null, requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> authCodeConfirmationEmailBuilder.sendEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", null ) );
    }

    @Test
    void sendEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Krishna Patel is now authorised to file online for Tesla" );
        emailData.setAuthorisedPerson( "Krishna Patel" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );
        authCodeConfirmationEmailBuilder.sendEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", requestsToFetchAssociatedUsers );

        Mockito.verify( emailProducer ).sendEmail( emailData, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE );
    }

    @Test
    void sendEmailToAssociatedUsersDoesNotThrowEmailOnKafkaQueue(){
        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Krishna Patel is now authorised to file online for Tesla" );
        emailData.setAuthorisedPerson( "Krishna Patel" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        authCodeConfirmationEmailBuilder.sendEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", List.of() );

        Mockito.verify( emailProducer, Mockito.never() ).sendEmail( emailData, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE );
    }

    @Test
    void sendEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Krishna Patel is now authorised to file online for Tesla" );
        emailData.setAuthorisedPerson( "Krishna Patel" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> authCodeConfirmationEmailBuilder.sendEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", requestsToFetchAssociatedUsers ) );
    }

}
