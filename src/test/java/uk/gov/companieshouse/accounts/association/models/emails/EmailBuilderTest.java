package uk.gov.companieshouse.accounts.association.models.emails;

import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonFrom;

import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.company.CompanyDetails;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class EmailBuilderTest {

    @MockBean
    private StaticPropertyUtil staticPropertyUtil;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField( StaticPropertyUtil.class, "APPLICATION_NAMESPACE", "accounts-association-api" );
    }

    @Test
    void traditionalSetterApproachCorrectlySetsValues(){
        final var emailContent = testDataManager.fetchCompanyDetailsDtos( "111111" );
        final var senders = testDataManager.fetchUserDtos( "111", "222", "333" );
        final var recipients = testDataManager.fetchUserDtos( "444", "555", "666" );

        final var emailBuilder = new EmailBuilder<CompanyDetails>();
        emailBuilder.setTemplateId( "greeting_email" );
        emailBuilder.setTemplateVersion( 1 );
        emailBuilder.setTemplateContents( emailContent );
        emailBuilder.setSystemIsASender( true );
        emailBuilder.setSenders( senders );
        emailBuilder.setRecipients( recipients );

        Assertions.assertEquals( "greeting_email", emailBuilder.getTemplateId() );
        Assertions.assertEquals( 1, emailBuilder.getTemplateVersion() );
        Assertions.assertEquals( emailContent, emailBuilder.getTemplateContents() );
        Assertions.assertTrue( emailBuilder.getSystemIsASender() );
        Assertions.assertEquals( senders, emailBuilder.getSenders() );
        Assertions.assertEquals( recipients, emailBuilder.getRecipients() );
    }

    @Test
    void builderApproachCorrectlySetsValues(){
        final var emailContent = testDataManager.fetchCompanyDetailsDtos( "111111" );
        final var senders = testDataManager.fetchUserDtos( "111", "222", "333" );
        final var recipients = testDataManager.fetchUserDtos( "444", "555", "666" );

        final var emailBuilder = new EmailBuilder<CompanyDetails>()
                .templateId( "greeting_email" )
                .templateVersion( 1 )
                .templateContents( emailContent )
                .systemIsASender( true )
                .senders( senders )
                .recipients( recipients );

        Assertions.assertEquals( "greeting_email", emailBuilder.getTemplateId() );
        Assertions.assertEquals( 1, emailBuilder.getTemplateVersion() );
        Assertions.assertEquals( emailContent, emailBuilder.getTemplateContents() );
        Assertions.assertTrue( emailBuilder.getSystemIsASender() );
        Assertions.assertEquals( senders, emailBuilder.getSenders() );
        Assertions.assertEquals( recipients, emailBuilder.getRecipients() );
    }

    @Test
    void listBasedSettersThrowIllegalArgumentExceptionWhenArgumentsAreNull(){
        final var emailBuilder = new EmailBuilder<CompanyDetails>();
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailBuilder.setTemplateContents( null ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailBuilder.setSenders( null ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailBuilder.setRecipients( null ) );
    }

    @Test
    void addMethodsEnrichListsAndClearMethodsResetLists(){
        final var emailContent = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var sender = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var recipient = testDataManager.fetchUserDtos( "444" ).getFirst();

        final var emailBuilder = new EmailBuilder<CompanyDetails>();
        emailBuilder.addTemplateContent( emailContent );
        emailBuilder.addSender( sender );
        emailBuilder.addRecipient( recipient );

        Assertions.assertEquals( emailContent, emailBuilder.getTemplateContents().getFirst() );
        Assertions.assertEquals( sender, emailBuilder.getSenders().getFirst() );
        Assertions.assertEquals( recipient, emailBuilder.getRecipients().getFirst() );

        emailBuilder.clearTemplateContents();
        emailBuilder.clearSenders();
        emailBuilder.clearRecipients();

        Assertions.assertTrue( emailBuilder.getTemplateContents().isEmpty() );
        Assertions.assertTrue( emailBuilder.getSenders().isEmpty() );
        Assertions.assertTrue( emailBuilder.getRecipients().isEmpty() );
    }

    private EmailBuilder<CompanyDetails> createPopulatedEmailBuilder(){
        final var emailContent = testDataManager.fetchCompanyDetailsDtos( "111111" );
        final var senders = testDataManager.fetchUserDtos( "111", "222", "333" );
        final var recipients = testDataManager.fetchUserDtos( "444", "555", "666" );

        return new EmailBuilder<CompanyDetails>()
                .templateId( "greeting_email" )
                .templateContents( emailContent )
                .systemIsASender( true )
                .senders( senders )
                .recipients( recipients );
    }

    @Test
    void buildWithIncompleteDataThrowsIllegalArgumentException(){
        Assertions.assertThrows( IllegalArgumentException.class, () -> createPopulatedEmailBuilder().templateVersion( 1 ).templateId( null ).build() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> createPopulatedEmailBuilder().build() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> createPopulatedEmailBuilder().templateVersion( 1 ).addSender( null ).build() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> createPopulatedEmailBuilder().templateVersion( 1 ).addRecipient( null ).build() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> createPopulatedEmailBuilder().templateVersion( 1 ).addTemplateContent( null ).build() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> createPopulatedEmailBuilder().templateVersion( 1 ).clearTemplateContents().build() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> createPopulatedEmailBuilder().templateVersion( 1 ).clearSenders().build() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> createPopulatedEmailBuilder().templateVersion( 1 ).clearRecipients().build() );
    }

    @Test
    void buildCorrectlyBuildsEmailWhenEverythingIsSpecified(){
        final var emailContent = testDataManager.fetchCompanyDetailsDtos( "111111" );
        final var senders = testDataManager.fetchUserDtos( "111", "222" );
        final var recipients = testDataManager.fetchUserDtos( "555", "666" );

        final var email = new EmailBuilder<CompanyDetails>()
                .templateId( "greeting_email" )
                .templateVersion( 1 )
                .templateContents( emailContent )
                .systemIsASender( true )
                .senders( senders )
                .recipients( recipients )
                .build();

        Assertions.assertEquals( 1, email.getEmailDetails().size() );
        Assertions.assertEquals( "greeting_email", email.getEmailDetails().getFirst().getTemplateId() );
        Assertions.assertEquals( new BigDecimal( 1 ), email.getEmailDetails().getFirst().getTemplateVersion() );
        Assertions.assertEquals( parseJsonFrom( emailContent.getFirst() ), email.getEmailDetails().getFirst().getPersonalisationDetails() );
        Assertions.assertEquals( 3, email.getSenderDetails().size() );
        Assertions.assertEquals( "accounts-association-api", email.getSenderDetails().getFirst().getAppId() );
        Assertions.assertNotNull( email.getSenderDetails().getFirst().getReference() );
        Assertions.assertEquals( "111", email.getSenderDetails().getFirst().getUserId() );
        Assertions.assertEquals( "bruce.wayne@gotham.city", email.getSenderDetails().getFirst().getEmailAddress() );
        Assertions.assertEquals( "Batman", email.getSenderDetails().getFirst().getName() );
        Assertions.assertEquals( "accounts-association-api", email.getSenderDetails().get( 1 ).getAppId() );
        Assertions.assertNotNull( email.getSenderDetails().get( 1 ).getReference() );
        Assertions.assertEquals( "222", email.getSenderDetails().get( 1 ).getUserId() );
        Assertions.assertEquals( "the.joker@gotham.city", email.getSenderDetails().get( 1 ).getEmailAddress() );
        Assertions.assertEquals( "the.joker@gotham.city", email.getSenderDetails().get( 1 ).getName() );
        Assertions.assertEquals( "accounts-association-api", email.getSenderDetails().getLast().getAppId() );
        Assertions.assertNotNull( email.getSenderDetails().getLast().getReference() );
        Assertions.assertNull( email.getSenderDetails().getLast().getUserId() );
        Assertions.assertNull( email.getSenderDetails().getLast().getEmailAddress() );
        Assertions.assertNull( email.getSenderDetails().getLast().getName() );
        Assertions.assertEquals( 2, email.getRecipientDetails().size() );
        Assertions.assertEquals( "barbara.gordon@gotham.city", email.getRecipientDetails().getFirst().getEmailAddress() );
        Assertions.assertEquals( "Batwoman", email.getRecipientDetails().getFirst().getName() );
        Assertions.assertEquals( "homer.simpson@springfield.com", email.getRecipientDetails().getLast().getEmailAddress() );
        Assertions.assertEquals( "homer.simpson@springfield.com", email.getRecipientDetails().getLast().getName() );
        Assertions.assertNotNull( email.getCreatedAt() );
    }

}
