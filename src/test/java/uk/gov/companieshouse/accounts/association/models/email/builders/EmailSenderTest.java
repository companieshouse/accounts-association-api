package uk.gov.companieshouse.accounts.association.models.email.builders;

import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;
import uk.gov.companieshouse.email_producer.model.EmailData;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class EmailSenderTest {

    @Mock
    EmailProducer emailProducer;

    @Mock
    Consumer<String> sendEmailForEmailAddress;

    AuthCodeConfirmationEmailBuilder authCodeConfirmationEmailBuilder;

    @BeforeEach
    public void setup(){
        authCodeConfirmationEmailBuilder = new AuthCodeConfirmationEmailBuilder( emailProducer );
    }

    @Test
    void sendEmailThrowsDataOnKafkaQueue(){
        final var email = new EmailData();
        email.setTo( "Jordan Peterson" );
        email.setSubject( "Hello!" );

        authCodeConfirmationEmailBuilder.sendEmail( email, "Greetings!" );
        Mockito.verify( emailProducer ).sendEmail( email, "Greetings!" );
    }

    @Test
    void sendEmailWithUnexpectedIssueThrowsEmailSendingException(){
        final var email = new EmailData();
        email.setTo( "Jordan Peterson" );
        email.setSubject( "Hello!" );

        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> authCodeConfirmationEmailBuilder.sendEmail( email, "Greetings!" ) );
    }

    @Test
    void sendEmailToUsersAssociatedWithCompanyWithNullCompanyDetailsOrNullConsumerOrNullSupplierThrowsNullPointerException(){
        final List<Supplier<User>> userSuppliers = List.of( () -> new User().email( "bruce.wayne@gotham.city" ) );
        Assertions.assertThrows( NullPointerException.class, () -> authCodeConfirmationEmailBuilder.sendEmailToUsersAssociatedWithCompany( "theId12345", null, sendEmailForEmailAddress, userSuppliers ) );
        Assertions.assertThrows( NullPointerException.class, () -> authCodeConfirmationEmailBuilder.sendEmailToUsersAssociatedWithCompany( "theId12345", new CompanyDetails(), null, userSuppliers ) );
        Assertions.assertThrows( NullPointerException.class, () -> authCodeConfirmationEmailBuilder.sendEmailToUsersAssociatedWithCompany( "theId12345", new CompanyDetails(), sendEmailForEmailAddress, null ) );
    }

    @Test
    void sendEmailToUsersAssociatedWithCompanyConsumesEmailAddress(){
        final List<Supplier<User>> userSuppliers = List.of( () -> new User().email( "bruce.wayne@gotham.city" ) );
        authCodeConfirmationEmailBuilder.sendEmailToUsersAssociatedWithCompany( "theId12345", new CompanyDetails(), sendEmailForEmailAddress, userSuppliers );
        Mockito.verify(sendEmailForEmailAddress).accept( "bruce.wayne@gotham.city" );
    }

    @Test
    void sendEmailToUsersAssociatedWithCompanyWithEmptyListDoesNotConsume(){
        authCodeConfirmationEmailBuilder.sendEmailToUsersAssociatedWithCompany( "theId12345", new CompanyDetails(), sendEmailForEmailAddress, List.of() );
        Mockito.verify(sendEmailForEmailAddress, Mockito.never() ).accept( "bruce.wayne@gotham.city" );
    }

}
