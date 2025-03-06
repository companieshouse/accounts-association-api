package uk.gov.companieshouse.accounts.association.service;

import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.models.emails.EmailBuilder;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.company.CompanyDetails;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class BaseEmailServiceTest {

    @Mock
    private WebClient emailWebClient;

    @MockBean
    private StaticPropertyUtil staticPropertyUtil;

    private BaseEmailService baseEmailService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup(){
        ReflectionTestUtils.setField( StaticPropertyUtil.class, "APPLICATION_NAMESPACE", "accounts-association-api" );
        baseEmailService = new BaseEmailService( emailWebClient ){};
    }

    @Test
    void sendEmailWithNullInputThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> baseEmailService.sendEmail( null ) );
    }

    @Test
    void sendEmailsPropagatesErrorCorrectly(){
        final var emailContent = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var sender = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var recipient = testDataManager.fetchUserDtos( "444" ).getFirst();

        final var requestBodyUriSpec = Mockito.mock( WebClient.RequestBodyUriSpec.class );
        final var requestBodySpec = Mockito.mock( WebClient.RequestBodySpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        Mockito.doReturn( requestBodyUriSpec ).when( emailWebClient ).post();
        Mockito.doReturn( requestBodySpec ).when( requestBodyUriSpec ).uri("/notification-sender/email");
        Mockito.doReturn( requestHeadersSpec ).when( requestBodySpec ).bodyValue( Mockito.any() );
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( Mono.error( new WebClientResponseException( 500, "Error", null, null, null ) ) ).when( responseSpec ).bodyToMono( Void.class );

        final var email = new EmailBuilder<CompanyDetails>()
                .templateId( "greeting_email" )
                .templateVersion( 1 )
                .templateContent( emailContent )
                .sender( sender )
                .recipient( recipient )
                .build();

        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> baseEmailService.sendEmail( email ).block( Duration.ofSeconds( 20L ) ) );
    }

    @Test
    void sendEmailsCorrectlySendsEmails(){
        final var emailContent = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var sender = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var recipient = testDataManager.fetchUserDtos( "444" ).getFirst();

        final var requestBodyUriSpec = Mockito.mock( WebClient.RequestBodyUriSpec.class );
        final var requestBodySpec = Mockito.mock( WebClient.RequestBodySpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        Mockito.doReturn( requestBodyUriSpec ).when( emailWebClient ).post();
        Mockito.doReturn( requestBodySpec ).when( requestBodyUriSpec ).uri("/notification-sender/email");
        Mockito.doReturn( requestHeadersSpec ).when( requestBodySpec ).bodyValue( Mockito.any() );
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( Mono.empty() ).when( responseSpec ).bodyToMono( Void.class );

        final var email = new EmailBuilder<CompanyDetails>()
                .templateId( "greeting_email" )
                .templateVersion( 1 )
                .templateContent( emailContent )
                .sender( sender )
                .recipient( recipient )
                .build();

        final var reference = baseEmailService.sendEmail( email ).block( Duration.ofSeconds( 20L ) );

        Assertions.assertNotNull( reference );
    }

}
