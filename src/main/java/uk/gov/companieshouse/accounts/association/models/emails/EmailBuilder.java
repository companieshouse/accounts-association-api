package uk.gov.companieshouse.accounts.association.models.emails;

import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonFrom;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
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

    private List<T> templateContents = new LinkedList<>();

    private boolean systemIsASender = false;

    private List<User> senders = new LinkedList<>();

    private List<User> recipients = new LinkedList<>();

    private static final Logger LOG = LoggerFactory.getLogger( APPLICATION_NAMESPACE );

    private static final Supplier<SenderDetails> baseSenderDetailsSupplier = () -> new SenderDetails()
            .appId( APPLICATION_NAMESPACE )
            .reference( UUID.randomUUID().toString() );

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

    public void setTemplateContents( final List<T> templateContents ){
        if ( Objects.isNull( templateContents ) ){
            final var exception = new IllegalArgumentException( "templateContents is not defined" );
            LOG.errorContext( getXRequestId(), exception, null );
            throw exception;
        }
        this.templateContents = templateContents;
    }

    public EmailBuilder<T> templateContents( final List<T> templateContents ){
        setTemplateContents( templateContents );
        return this;
    }

    public EmailBuilder<T> addTemplateContent( final T templateContent ){
        templateContents.add( templateContent );
        return this;
    }

    public EmailBuilder<T> clearTemplateContents(){
        templateContents.clear();
        return this;
    }

    public List<T> getTemplateContents(){
        return templateContents;
    }

    public void setSenders( final List<User> senders ){
        if ( Objects.isNull( senders ) ){
            final var exception = new IllegalArgumentException( "senders is not defined" );
            LOG.errorContext( getXRequestId(), exception, null );
            throw exception;
        }
        this.senders = senders;
    }

    public EmailBuilder<T> senders( final List<User> senders ){
        setSenders( senders );
        return this;
    }

    public EmailBuilder<T> addSender( final User sender ){
        senders.add( sender );
        return this;
    }

    public EmailBuilder<T> clearSenders(){
        senders.clear();
        return this;
    }

    public List<User> getSenders(){
        return senders;
    }

    public void setSystemIsASender( final boolean systemIsASender ){
        this.systemIsASender = systemIsASender;
    }

    public EmailBuilder<T> systemIsASender( final boolean systemIsASender ){
        setSystemIsASender( systemIsASender );
        return this;
    }

    public boolean getSystemIsASender(){
        return systemIsASender;
    }

    public void setRecipients( final List<User> recipients ){
        if ( Objects.isNull( recipients ) ){
            final var exception = new IllegalArgumentException( "recipients is not defined" );
            LOG.errorContext( getXRequestId(), exception, null );
            throw exception;
        }
        this.recipients = recipients;
    }

    public EmailBuilder<T> recipients( final List<User> recipients ){
        setRecipients( recipients );
        return this;
    }

    public EmailBuilder<T> addRecipient( final User recipient ){
        recipients.add( recipient );
        return this;
    }

    public EmailBuilder<T> clearRecipients(){
        recipients.clear();
        return this;
    }

    public List<User> getRecipients(){
        return recipients;
    }

    private List<SenderDetails> computeSenderDetails(){
        final var senders = new LinkedList<SenderDetails>();
        for ( final User sender: getSenders() ){
            if ( Objects.isNull( sender ) ){
                final var exception = new IllegalArgumentException( "sender is not defined" );
                LOG.errorContext( getXRequestId(), exception, null );
                throw exception;
            }

            final var senderDetails = baseSenderDetailsSupplier.get()
                    .userId( sender.getUserId() )
                    .emailAddress( sender.getEmail() )
                    .name( Optional.ofNullable( sender.getDisplayName() ).orElse( sender.getEmail() ) );

            senders.add( senderDetails );
        }

        if ( getSystemIsASender() ){
            senders.add( baseSenderDetailsSupplier.get() );
        }

        return senders;
    }

    private List<RecipientDetailsEmail> computeRecipientDetails(){
        final var recipients = new LinkedList<RecipientDetailsEmail>();
        for ( final User recipient: getRecipients() ){
            if ( Objects.isNull( recipient ) ){
                final var exception = new IllegalArgumentException( "recipient cannot be null" );
                LOG.errorContext( getXRequestId(), exception, null );
                throw exception;
            }

            final var recipientsDetails = new RecipientDetailsEmail()
                    .emailAddress( recipient.getEmail() )
                    .name( Optional.ofNullable( recipient.getDisplayName() ).orElse( recipient.getEmail() ) );

            recipients.add( recipientsDetails );
        }
        return recipients;
    }

    private List<EmailDetails> computeEmailDetails(){
        final var emails = new LinkedList<EmailDetails>();
        for ( final T templateContent: getTemplateContents() ){
            if ( Objects.isNull( templateContent ) ){
                final var exception = new IllegalArgumentException( "templateContent cannot be null" );
                LOG.errorContext( getXRequestId(), exception, null );
                throw exception;
            }

            final var emailDetails =  new EmailDetails()
                    .templateId( getTemplateId() )
                    .templateVersion( new BigDecimal( getTemplateVersion() ) )
                    .personalisationDetails( parseJsonFrom( templateContent ) );

            emails.add( emailDetails );
        }
        return emails;
    }

    public GovUkEmailDetailsRequest build(){
        if ( Objects.isNull( getTemplateId() ) || Objects.isNull( getTemplateVersion() ) || getTemplateContents().isEmpty() || getSenders().isEmpty() || getRecipients().isEmpty() ){
            final var exception = new IllegalArgumentException( "templateId, templateVersion, templateContents, senders, and recipients must be defined" );
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
