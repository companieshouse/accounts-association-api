package uk.gov.companieshouse.accounts.association.models.emails;

import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonFrom;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.chs_notification_sender.model.EmailDetails;
import uk.gov.companieshouse.api.chs_notification_sender.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_notification_sender.model.RecipientDetailsEmail;
import uk.gov.companieshouse.api.chs_notification_sender.model.SenderDetails;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class EmailBuilder<T> {

    private String templateId;

    private Integer templateVersion;

    private T templateContent;

    private User sender;

    private User recipient;

    private static final Logger LOG = LoggerFactory.getLogger( APPLICATION_NAMESPACE );

    public EmailBuilder(){}

    public void setTemplateId( final String templateId ){
        this.templateId = templateId;
    }

    public EmailBuilder<T> templateId( final String templateId ){
        setTemplateId( templateId );
        return this;
    }

    public String getTemplateId(){
        return templateId;
    }

    public void setTemplateVersion( final int templateVersion ){
        this.templateVersion = templateVersion;
    }

    public EmailBuilder<T> templateVersion( final int templateVersion ){
        setTemplateVersion( templateVersion );
        return this;
    }

    public Integer getTemplateVersion(){
        return templateVersion;
    }

    public void setTemplateContent( final T templateContent ){
        this.templateContent = templateContent;
    }

    public EmailBuilder<T> templateContent( final T templateContent ){
        setTemplateContent( templateContent );
        return this;
    }

    public T getTemplateContent(){
        return templateContent;
    }

    public void setSender( final User sender ){
        this.sender = sender;
    }

    public EmailBuilder<T> sender( final User sender ){
        setSender( sender );
        return this;
    }

    public User getSender(){
        return sender;
    }

    public void setRecipient( final User recipient ){
        this.recipient = recipient;
    }

    public EmailBuilder<T> recipient( final User recipient ){
        setRecipient( recipient );
        return this;
    }

    public User getRecipient(){
        return recipient;
    }

    private SenderDetails computeSenderDetails(){
        final var senderDetails = new SenderDetails()
                .appId( APPLICATION_NAMESPACE )
                .reference( UUID.randomUUID().toString() );

        final var sender = getSender();
        if ( Objects.isNull( sender ) ){
            return senderDetails;
        }

        return senderDetails
                .userId( sender.getUserId() )
                .emailAddress( sender.getEmail() )
                .name( Optional.ofNullable( sender.getDisplayName() ).orElse( sender.getEmail() ) );
    }

    private RecipientDetailsEmail computeRecipientDetails(){
        final var recipient = getRecipient();
        return new RecipientDetailsEmail()
                .emailAddress( recipient.getEmail() )
                .name( Optional.ofNullable( recipient.getDisplayName() ).orElse( recipient.getEmail() ) );
    }

    private EmailDetails computeEmailDetails(){
        return new EmailDetails()
                .templateId( getTemplateId() )
                .templateVersion( new BigDecimal( getTemplateVersion() ) )
                .personalisationDetails( parseJsonFrom( getTemplateContent() ) );
    }

    public GovUkEmailDetailsRequest build(){
        if ( Objects.isNull( getTemplateId() ) || Objects.isNull( getTemplateVersion() ) || Objects.isNull( getTemplateContent() ) || Objects.isNull( getRecipient() ) ){
            final var exception = new IllegalArgumentException( "templateId, templateVersion, templateContent, and recipient must be defined" );
            LOG.errorContext( getXRequestId(), exception, null );
            throw exception;
        }

        return new GovUkEmailDetailsRequest()
                .senderDetails( computeSenderDetails() )
                .recipientDetails( computeRecipientDetails() )
                .emailDetails( computeEmailDetails() )
                .createdAt( LocalDateTime.now().toString() );
    }

}
