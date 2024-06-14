package uk.gov.companieshouse.accounts.association.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.email.EmailNotification;
import uk.gov.companieshouse.accounts.association.models.email.builders.*;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.utils.MessageType;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;

@Service
public class EmailService {

    @Value( "${invitation.url}" )
    private String invitationLink;

    protected static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    private final AssociationsRepository associationsRepository;
    private final UsersService usersService;
    private final EmailProducer emailProducer;


    @Autowired
    public EmailService(AssociationsRepository associationsRepository, UsersService usersService, EmailProducer emailProducer) {
        this.associationsRepository = associationsRepository;
        this.usersService = usersService;
        this.emailProducer = emailProducer;
    }

    @Transactional(readOnly = true)
    public List<Supplier<User>> createRequestsToFetchAssociatedUsers( final String companyNumber ) {
        return associationsRepository.fetchAssociatedUsers(companyNumber, Set.of(StatusEnum.CONFIRMED.getValue()), Pageable.unpaged())
                .map(AssociationDao::getUserId)
                .map(usersService::createFetchUserDetailsRequest)
                .toList();
    }

    @Async
    public void sendAuthCodeConfirmationEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String displayName, final List<Supplier<User>> requestsToFetchAssociatedUsers) {
        requestsToFetchAssociatedUsers.forEach(userSupplier -> {
            final var user = userSupplier.get();

            final var emailData = new AuthCodeConfirmationEmailBuilder()
                    .setRecipientEmail( user.getEmail() )
                    .setDisplayName( displayName )
                    .setCompanyName( companyDetails.getCompanyName() )
                    .build();

            emailProducer.sendEmail( emailData, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() );

            LOG.infoContext(xRequestId,
                    new EmailNotification(
                            AUTH_CODE_CONFIRMATION_MESSAGE_TYPE,
                            StaticPropertyUtil.APPLICATION_NAMESPACE,
                            user.getEmail(),
                            companyDetails.getCompanyNumber()).toMessage(), null);
        });
    }

    @Async
    public void sendAuthorisationRemovedEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String removedByDisplayName, final String removedUserDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers) {
        requestsToFetchAssociatedUsers.forEach(userSupplier -> {
            final var user = userSupplier.get();

            final var emailData = new AuthorisationRemovedEmailBuilder()
                    .setCompanyName( companyDetails.getCompanyName() )
                    .setRemovedByDisplayName( removedByDisplayName )
                    .setRemovedUserDisplayName( removedUserDisplayName )
                    .setRecipientEmail( user.getEmail() )
                    .build();

            emailProducer.sendEmail( emailData, MessageType.AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() );
            LOG.infoContext(xRequestId,
                    new EmailNotification(
                            MessageType.AUTHORISATION_REMOVED_MESSAGE_TYPE,
                            StaticPropertyUtil.APPLICATION_NAMESPACE,
                            user.getEmail(),
                            companyDetails.getCompanyNumber()).toMessage(), null);
        });
    }

    @Async
    public void sendAuthorisationRemovedEmailToRemovedUser(final String xRequestId, final CompanyDetails companyDetails, final String removedByDisplayName, final Supplier<User> userSupplier) {
        final var user = userSupplier.get();

        final var emailData = new YourAuthorisationRemovedEmailBuilder()
                .setCompanyName( companyDetails.getCompanyName() )
                .setRemovedByDisplayName( removedByDisplayName )
                .setRecipientEmail( user.getEmail() )
                .build();

        emailProducer.sendEmail( emailData, MessageType.YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() );
        LOG.infoContext(xRequestId,
                new EmailNotification(
                        MessageType.YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE,
                        removedByDisplayName,
                        user.getEmail(),
                        companyDetails.getCompanyNumber()).toMessage(), null);
    }

    @Async
    public void sendInvitationCancelledEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String cancelledByDisplayName, final String cancelledUserDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers) {
        requestsToFetchAssociatedUsers.forEach(userSupplier -> {
            final var user = userSupplier.get();

            final var emailData = new InvitationCancelledEmailBuilder()
                    .setCompanyName( companyDetails.getCompanyName() )
                    .setCancelledByDisplayName( cancelledByDisplayName )
                    .setCancelledUserDisplayName( cancelledUserDisplayName )
                    .setRecipientEmail( user.getEmail() )
                    .build();

            emailProducer.sendEmail( emailData, MessageType.INVITATION_CANCELLED_MESSAGE_TYPE.getValue() );
            LOG.infoContext(xRequestId, new EmailNotification(
                    MessageType.INVITATION_CANCELLED_MESSAGE_TYPE,
                    StaticPropertyUtil.APPLICATION_NAMESPACE,
                    user.getEmail(),
                    companyDetails.getCompanyNumber()).toMessage(), null);

        });
    }

    @Async
    public void sendInvitationEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String inviterDisplayName, final String inviteeDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers) {
        requestsToFetchAssociatedUsers.forEach(userSupplier -> {
            final var user = userSupplier.get();

            final var emailData = new InvitationEmailBuilder()
                    .setCompanyName( companyDetails.getCompanyName() )
                    .setInviteeDisplayName( inviteeDisplayName )
                    .setInviterDisplayName( inviterDisplayName )
                    .setRecipientEmail( user.getEmail() )
                    .build();

            emailProducer.sendEmail( emailData, MessageType.INVITATION_MESSAGE_TYPE.getValue() );
            LOG.infoContext(xRequestId,
                    new EmailNotification(
                            MessageType.INVITATION_MESSAGE_TYPE,
                            StaticPropertyUtil.APPLICATION_NAMESPACE,
                            user.getEmail(),
                            companyDetails.getCompanyNumber()).toMessage(), null);

        });
    }

    @Async
    public void sendInvitationAcceptedEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String inviterDisplayName, final String inviteeDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers) {
        requestsToFetchAssociatedUsers.forEach(userSupplier -> {
            final var user = userSupplier.get();

            final var emailData = new InvitationAcceptedEmailBuilder()
                    .setCompanyName( companyDetails.getCompanyName() )
                    .setInviteeDisplayName( inviteeDisplayName )
                    .setInviterDisplayName( inviterDisplayName )
                    .setRecipientEmail( user.getEmail() )
                    .build();

            emailProducer.sendEmail( emailData, MessageType.INVITATION_ACCEPTED_MESSAGE_TYPE.getValue() );

            LOG.infoContext(xRequestId, new EmailNotification(
                    MessageType.INVITATION_ACCEPTED_MESSAGE_TYPE,
                    StaticPropertyUtil.APPLICATION_NAMESPACE,
                    user.getEmail(),
                    companyDetails.getCompanyNumber()).toMessage(), null);

        });
    }

    @Async
    public void sendInvitationRejectedEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String inviteeDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers) {
        requestsToFetchAssociatedUsers.forEach(userSupplier -> {
            final var user = userSupplier.get();

            final var emailData = new InvitationRejectedEmailBuilder()
                    .setCompanyName( companyDetails.getCompanyName() )
                    .setInviteeDisplayName( inviteeDisplayName )
                    .setRecipientEmail( user.getEmail() )
                    .build();

            emailProducer.sendEmail( emailData, MessageType.INVITATION_REJECTED_MESSAGE_TYPE.getValue() );

            LOG.infoContext(xRequestId,
                    new EmailNotification(
                            MessageType.INVITATION_REJECTED_MESSAGE_TYPE,
                            StaticPropertyUtil.APPLICATION_NAMESPACE,
                            user.getEmail(),
                            companyDetails.getCompanyNumber()).toMessage(), null);
        });
    }

    @Async
    public void sendInviteEmail( final String xRequestId, final CompanyDetails companyDetails, final String inviterDisplayName, final String invitationExpiryTimestamp, final String inviteeEmail ){
        final var emailData = new InviteEmailBuilder()
                .setRecipientEmail( inviteeEmail )
                .setInviterDisplayName( inviterDisplayName )
                .setCompanyName( companyDetails.getCompanyName() )
                .setInvitationExpiryTimestamp( invitationExpiryTimestamp )
                .setInvitationLink( invitationLink )
                .build();

        emailProducer.sendEmail( emailData, MessageType.INVITE_MESSAGE_TYPE.getValue() );

        LOG.infoContext( xRequestId,
                new EmailNotification(
                        MessageType.INVITE_MESSAGE_TYPE,
                        StaticPropertyUtil.APPLICATION_NAMESPACE,
                        inviteeEmail,
                        companyDetails.getCompanyNumber() )
                        .setInvitationExpiryTimestamp( invitationExpiryTimestamp )
                        .toMessage(), null );
    }

}
