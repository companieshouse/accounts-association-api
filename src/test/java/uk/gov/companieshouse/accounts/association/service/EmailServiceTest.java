package uk.gov.companieshouse.accounts.association.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.email_producer.EmailProducer;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class EmailServiceTest {

    @Mock
    EmailProducer emailProducer;

    @InjectMocks
    EmailService emailService;

}
