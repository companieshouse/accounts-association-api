package uk.gov.companieshouse.accounts.association.models.emails;

import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonFrom;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
        final var emailContent = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var sender = testDataManager.fetchUserDtos( "111", "222", "333" ).getFirst();
        final var recipient = testDataManager.fetchUserDtos( "444", "555", "666" ).getFirst();

        final var emailBuilder = new EmailBuilder<CompanyDetails>();
        emailBuilder.setTemplateId( "greeting_email" );
        emailBuilder.setTemplateVersion( 1 );
        emailBuilder.setTemplateContent( emailContent );
        emailBuilder.setSender( sender );
        emailBuilder.setRecipient( recipient );

        Assertions.assertEquals( "greeting_email", emailBuilder.getTemplateId() );
        Assertions.assertEquals( 1, emailBuilder.getTemplateVersion() );
        Assertions.assertEquals( emailContent, emailBuilder.getTemplateContent() );
        Assertions.assertEquals( sender, emailBuilder.getSender() );
        Assertions.assertEquals( recipient, emailBuilder.getRecipient() );
    }

    @Test
    void builderApproachCorrectlySetsValues(){
        final var emailContent = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var sender = testDataManager.fetchUserDtos( "111", "222", "333" ).getFirst();
        final var recipient = testDataManager.fetchUserDtos( "444", "555", "666" ).getFirst();

        final var emailBuilder = new EmailBuilder<CompanyDetails>()
                .templateId( "greeting_email" )
                .templateVersion( 1 )
                .templateContent( emailContent )
                .sender( sender )
                .recipient( recipient );

        Assertions.assertEquals( "greeting_email", emailBuilder.getTemplateId() );
        Assertions.assertEquals( 1, emailBuilder.getTemplateVersion() );
        Assertions.assertEquals( emailContent, emailBuilder.getTemplateContent() );
        Assertions.assertEquals( sender, emailBuilder.getSender() );
        Assertions.assertEquals( recipient, emailBuilder.getRecipient() );
    }

    private EmailBuilder<CompanyDetails> createPopulatedEmailBuilder(){
        final var emailContent = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var sender = testDataManager.fetchUserDtos( "111", "222", "333" ).getFirst();
        final var recipient = testDataManager.fetchUserDtos( "444", "555", "666" ).getFirst();

        return new EmailBuilder<CompanyDetails>()
                .templateId( "greeting_email" )
                .templateContent( emailContent )
                .sender( sender )
                .recipient( recipient );
    }

    @Test
    void buildWithIncompleteDataThrowsIllegalArgumentException(){
        Assertions.assertThrows( IllegalArgumentException.class, () -> createPopulatedEmailBuilder().templateVersion( 1 ).templateId( null ).build() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> createPopulatedEmailBuilder().build() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> createPopulatedEmailBuilder().templateVersion( 1 ).recipient( null ).build() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> createPopulatedEmailBuilder().templateVersion( 1 ).templateContent( null ).build() );
    }

    private static Stream<Arguments> buildScenarios(){
        return Stream.of(
                Arguments.of( "111", "555", "Batman", "Batwoman" ),
                Arguments.of( "222", "666", "the.joker@gotham.city", "homer.simpson@springfield.com" )
        );
    }

    @ParameterizedTest
    @MethodSource( "buildScenarios" )
    void buildCorrectlyBuildsEmailWhenEverythingIsSpecified( final String senderUserId, final String recipientUserId, final String senderName, final String recipientName ){
        final var emailContent = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var sender = testDataManager.fetchUserDtos( senderUserId ).getFirst();
        final var recipient = testDataManager.fetchUserDtos( recipientUserId ).getFirst();

        final var email = new EmailBuilder<CompanyDetails>()
                .templateId( "greeting_email" )
                .templateVersion( 1 )
                .templateContent( emailContent )
                .sender( sender )
                .recipient( recipient )
                .build();

        Assertions.assertEquals( "greeting_email", email.getEmailDetails().getTemplateId() );
        Assertions.assertEquals( new BigDecimal( 1 ), email.getEmailDetails().getTemplateVersion() );
        Assertions.assertEquals( parseJsonFrom( emailContent ), email.getEmailDetails().getPersonalisationDetails() );

        Assertions.assertEquals( "accounts-association-api", email.getSenderDetails().getAppId() );
        Assertions.assertNotNull( email.getSenderDetails().getReference() );
        Assertions.assertEquals( senderUserId, email.getSenderDetails().getUserId() );
        Assertions.assertEquals( sender.getEmail(), email.getSenderDetails().getEmailAddress() );
        Assertions.assertEquals( senderName, email.getSenderDetails().getName() );

        Assertions.assertEquals( recipient.getEmail(), email.getRecipientDetails().getEmailAddress() );
        Assertions.assertEquals( recipientName, email.getRecipientDetails().getName() );

        Assertions.assertNotNull( email.getCreatedAt() );
    }

    @Test
    void buildCorrectlyBuildsEmailWithoutSender(){
        final var emailContent = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var recipient = testDataManager.fetchUserDtos( "555" ).getFirst();

        final var email = new EmailBuilder<CompanyDetails>()
                .templateId( "greeting_email" )
                .templateVersion( 1 )
                .templateContent( emailContent )
                .recipient( recipient )
                .build();

        Assertions.assertEquals( "greeting_email", email.getEmailDetails().getTemplateId() );
        Assertions.assertEquals( new BigDecimal( 1 ), email.getEmailDetails().getTemplateVersion() );
        Assertions.assertEquals( parseJsonFrom( emailContent ), email.getEmailDetails().getPersonalisationDetails() );

        Assertions.assertEquals( "accounts-association-api", email.getSenderDetails().getAppId() );
        Assertions.assertNotNull( email.getSenderDetails().getReference() );
        Assertions.assertNull( email.getSenderDetails().getUserId() );
        Assertions.assertNull( email.getSenderDetails().getEmailAddress() );
        Assertions.assertNull( email.getSenderDetails().getName() );

        Assertions.assertEquals( recipient.getEmail(), email.getRecipientDetails().getEmailAddress() );
        Assertions.assertEquals( "Batwoman", email.getRecipientDetails().getName() );

        Assertions.assertNotNull( email.getCreatedAt() );

    }

}
