package uk.gov.companieshouse.accounts.association.service;

import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class EmailService extends EmailSender {

    private final AssociationsRepository associationsRepository;
    private final UsersService usersService;




    @Autowired
    public EmailService(EmailProducer emailProducer, AssociationsRepository associationsRepository, UsersService usersService) {
        super(emailProducer);
        this.associationsRepository = associationsRepository;
        this.usersService = usersService;
    }

    @Transactional(readOnly = true)
    public List<Supplier<User>> createRequestsToFetchAssociatedUsers(final String companyNumber) {
        return associationsRepository.fetchAssociatedUsers(companyNumber, Set.of(StatusEnum.CONFIRMED.getValue()), Pageable.unpaged())
                .map(AssociationDao::getUserId)
                .map(usersService::createFetchUserDetailsRequest)
                .getContent();
    }

    ;

    @Async
    public void sendAuthCodeConfirmationEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String displayName, final List<Supplier<User>> requestsToFetchAssociatedUsers) {
        requestsToFetchAssociatedUsers.forEach(userSupplier -> {
            var builder = new AuthCodeConfirmationEmailBuilder();
            builder.setRecipientEmail(userSupplier.get().getEmail());
            builder.setDisplayName(displayName);
            builder.setCompanyName(companyDetails.getCompanyName());
            sendEmail(builder.buildEmailData(), MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE);
            LOG.infoContext( xRequestId,
                    new EmailNotification(
                            MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE,
                            StaticPropertyUtil.APPLICATION_NAMESPACE,
                            userSupplier.get().getEmail(),
                            companyDetails.getCompanyNumber()).toString(), null );
        });
    }

    @Async
    public void sendAuthorisationRemovedEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String removedByDisplayName, final String removedUserDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers) {
        requestsToFetchAssociatedUsers.forEach(userSupplier -> {
            var builder = new AuthorisationRemovedEmailBuilder();
            builder.setCompanyName(companyDetails.getCompanyName());
            builder.setRemovedByDisplayName(removedByDisplayName);
            builder.setRemovedUserDisplayName(removedUserDisplayName);
            builder.setRecipientEmail(userSupplier.get().getEmail());
            sendEmail(builder.buildEmailData(), MessageType.AUTHORISATION_REMOVED_MESSAGE_TYPE);
            LOG.infoContext( xRequestId,
                    new EmailNotification(
                            MessageType.AUTHORISATION_REMOVED_MESSAGE_TYPE,
                            StaticPropertyUtil.APPLICATION_NAMESPACE,
                            userSupplier.get().getEmail(),
                            companyDetails.getCompanyNumber()).toString(), null );
        });
    }

    @Async
    public void sendInvitationCancelledEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String cancelledByDisplayName, final String cancelledUserDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers) {
        requestsToFetchAssociatedUsers.forEach(userSupplier -> {
            var builder = new InvitationCancelledEmailBuilder();
            builder.setCompanyName(companyDetails.getCompanyName());
            builder.setCancelledByDisplayName(cancelledByDisplayName);
            builder.setCancelledUserDisplayName(cancelledUserDisplayName);
            builder.setRecipientEmail(userSupplier.get().getEmail());
            sendEmail(builder.buildEmailData(), MessageType.INVITATION_CANCELLED_MESSAGE_TYPE);
            LOG.infoContext( xRequestId, new EmailNotification(
                    MessageType.INVITATION_CANCELLED_MESSAGE_TYPE,
                    StaticPropertyUtil.APPLICATION_NAMESPACE,
                    userSupplier.get().getEmail(),
                    companyDetails.getCompanyNumber()).toString(), null );

        });
    }

    @Async
    public void sendInvitationEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String inviterDisplayName, final String inviteeDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers) {
        requestsToFetchAssociatedUsers.forEach(userSupplier -> {
            var builder = new InvitationEmailBuilder();
            builder.setCompanyName(companyDetails.getCompanyName());
            builder.setInviteeDisplayName(inviteeDisplayName);
            builder.setInviterDisplayName(inviterDisplayName);
            builder.setRecipientEmail(userSupplier.get().getEmail());
            sendEmail(builder.buildEmailData(), MessageType.INVITATION_MESSAGE_TYPE);
            LOG.infoContext( xRequestId,
                    new EmailNotification(
                            MessageType.INVITATION_MESSAGE_TYPE,
                            StaticPropertyUtil.APPLICATION_NAMESPACE,
                            userSupplier.get().getEmail(),
                            companyDetails.getCompanyNumber()).toString(), null );

        });
    }

    @Async
    public void sendInvitationAcceptedEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String inviterDisplayName, final String inviteeDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers) {
        requestsToFetchAssociatedUsers.forEach(userSupplier -> {
            var builder = new InvitationAcceptedEmailBuilder();
            builder.setCompanyName(companyDetails.getCompanyName());
            builder.setInviteeDisplayName(inviteeDisplayName);
            builder.setInviterDisplayName(inviterDisplayName);
            builder.setRecipientEmail(userSupplier.get().getEmail());
            sendEmail(builder.buildEmailData(), MessageType.INVITATION_ACCEPTED_MESSAGE_TYPE);
            LOG.infoContext( xRequestId, new EmailNotification(
                    MessageType.INVITATION_ACCEPTED_MESSAGE_TYPE,
                    StaticPropertyUtil.APPLICATION_NAMESPACE,
                    userSupplier.get().getEmail(),
                    companyDetails.getCompanyNumber()).toString() , null );

        });
    }

    @Async
    public void sendInvitationRejectedEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String inviteeDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers) {
        requestsToFetchAssociatedUsers.forEach(userSupplier -> {
            var builder = new InvitationRejectedEmailBuilder();
            builder.setCompanyName(companyDetails.getCompanyName());
            builder.setInviteeDisplayName(inviteeDisplayName);
            builder.setRecipientEmail(userSupplier.get().getEmail());
            sendEmail(builder.buildEmailData(), MessageType.INVITATION_REJECTED_MESSAGE_TYPE);
            LOG.infoContext( xRequestId,
                    new EmailNotification(
                            MessageType.INVITATION_REJECTED_MESSAGE_TYPE,
                            StaticPropertyUtil.APPLICATION_NAMESPACE,
                            userSupplier.get().getEmail(),
                            companyDetails.getCompanyNumber()).toString(), null );
        });
    }

}
