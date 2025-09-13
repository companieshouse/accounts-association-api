package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_UPDATE_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.DELEGATED_REMOVAL_OF_MIGRATED;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.DELEGATED_REMOVAL_OF_MIGRATED_BATCH;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_ACCEPTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_REJECTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITE_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITE_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.REMOVAL_OF_OWN_MIGRATED;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.hasAdminPrivilege;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isAPIKeyRequest;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.DAYS_SINCE_INVITE_TILL_EXPIRES;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.isRequestingUser;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.mapToDisplayValue;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.REMOVED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.UNAUTHORISED;

import java.time.LocalDateTime;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.exceptions.EmailBatchException;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NullRequiredEmailDataException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.models.email.EmailNotification;
import uk.gov.companieshouse.accounts.association.models.email.builders.AuthCodeConfirmationEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.AuthorisationRemovedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.DelegatedRemovalOfMigratedBatchEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.DelegatedRemovalOfMigratedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationAcceptedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationCancelledEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationRejectedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InviteCancelledEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InviteEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.RemovalOfOwnMigratedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.YourAuthorisationRemovedEmailBuilder;
import uk.gov.companieshouse.accounts.association.utils.EmailBatchType;
import uk.gov.companieshouse.accounts.association.utils.MessageType;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.model.EmailData;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
@ComponentScan(basePackages = "uk.gov.companieshouse.email_producer")
public class EmailService {

    @Value("${invitation.url}")
    private String invitationLink;

    private static final String ASSOCIATION_DAO_OR_USER_IS_NULL = "AssociationDao or userID is null";
    private static final String ASSOCIATED_USER_OR_EMAIL_IS_NULL = "Associated user or their email is null";
    private static final String REQUESTING_USER_OR_EMAIL_IS_NULL = "Cancelled user or their email are null";
    private static final String IMPACTED_USER_OR_EMAIL_IS_NULL = "Impacted user or their email are null";

    protected static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

    private final UsersService usersService;
    private final CompanyService companyService;
    private final AssociationsTransactionService associationsTransactionService;
    private final EmailProducer emailProducer;

    @Autowired
    public EmailService(final UsersService usersService, final EmailProducer emailProducer, final CompanyService companyService, final AssociationsTransactionService associationsTransactionService) {
        this.usersService = usersService;
        this.companyService = companyService;
        this.associationsTransactionService = associationsTransactionService;
        this.emailProducer = emailProducer;
    }

    private void sendEmail(final String xRequestId, final MessageType messageType, final EmailData emailData, final EmailNotification logMessageSupplier){
        try {
            emailProducer.sendEmail(emailData, messageType.getValue());
            LOGGER.infoContext(xRequestId, logMessageSupplier.toMessage(), null);
        } catch (Exception exception) {
            LOGGER.errorContext(xRequestId, exception, null);
            throw exception;
        }
    }

    public void sendStatusUpdateEmails(final AssociationDao targetAssociation, final User targetUser, final StatusEnum newStatus) {
        final var xRequestId = getXRequestId();
        final var requestingUserDisplayValue = isAPIKeyRequest() || hasAdminPrivilege(ADMIN_UPDATE_PERMISSION) ? COMPANIES_HOUSE : mapToDisplayValue(getUser(), getUser().getEmail());
        final var targetUserDisplayValue = mapToDisplayValue(targetUser, targetAssociation.getUserEmail());
        final var targetUserEmail = Optional.ofNullable(targetUser).map(User::getEmail).orElse(targetAssociation.getUserEmail());

        final var cachedCompanyName = companyService.fetchCompanyProfile(targetAssociation.getCompanyNumber()).getCompanyName();
        final var cachedAssociatedUsers = associationsTransactionService.fetchConfirmedUserIds(targetAssociation.getCompanyNumber());
        final var cachedInvitedByDisplayName = targetAssociation.getInvitations().stream()
                .max((firstInvitation, secondInvitation) -> firstInvitation.getInvitedAt().isAfter(secondInvitation.getInvitedAt()) ? 1 : -1)
                .map(InvitationDao::getInvitedBy)
                .map(user -> usersService.fetchUserDetails(user, xRequestId))
                .map(user -> Optional.ofNullable(user.getDisplayName()).orElse(user.getEmail()));

        final var emailBatch = determineEmailBatchType(xRequestId, targetAssociation, newStatus);

        switch (emailBatch) {
            case REJECTING_INVITATION -> cachedAssociatedUsers.parallel().forEach(userId -> sendInvitationRejectedEmailToAssociatedUser(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, userId));
            case AUTHORISATION_IS_BEING_REMOVED -> {
                sendAuthorisationRemovedEmailToRemovedUser(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetAssociation.getUserId());
                cachedAssociatedUsers.parallel().forEach(userId -> sendAuthorisationRemovedEmailToAssociatedUser(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue, userId));
            }
            case ACCEPTING_INVITATION -> cachedAssociatedUsers.parallel().forEach(userId -> sendInvitationAcceptedEmailToAssociatedUser(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, cachedInvitedByDisplayName, requestingUserDisplayValue, userId));
            case CANCELLING_ANOTHER_USERS_INVITATION -> {
                sendInviteCancelledEmail(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetAssociation);
                cachedAssociatedUsers.parallel().forEach(userId -> sendInvitationCancelledEmailToAssociatedUser(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue, userId));
            }
            case REMOVING_ANOTHER_USERS_MIGRATED_ASSOCIATION -> {
                sendDelegatedRemovalOfMigratedEmail(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserEmail);
                cachedAssociatedUsers.parallel().forEach(userId -> sendDelegatedRemovalOfMigratedBatchEmail(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue, userId));
            }
            case REMOVING_OWN_MIGRATED_ASSOCIATION -> {
                sendRemoveOfOwnMigratedEmail(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, getEricIdentity());
                cachedAssociatedUsers.parallel().forEach(userId -> sendDelegatedRemovalOfMigratedBatchEmail(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue, userId));
            }
            case INVITING_USER -> {
                final var invitationExpiryTimestamp = LocalDateTime.now().plusDays(DAYS_SINCE_INVITE_TILL_EXPIRES).toString();
                sendInviteEmail(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, invitationExpiryTimestamp, targetUserEmail);
                cachedAssociatedUsers.parallel().forEach(userId -> sendInvitationEmailToAssociatedUser(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue, userId));
            }
            case CONFIRMING_WITH_AUTH_CODE -> cachedAssociatedUsers.parallel().forEach(userId -> sendAuthCodeConfirmationEmailToAssociatedUser(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, targetUserDisplayValue, userId));
            // TODO: feels wrong
            case NONE -> {
            }
            default -> throw new InternalServerErrorRuntimeException("Issue during email batch processing - default case triggered", new Exception("Issue during email batch processing - default case triggered"));
        }
    }

    public void sendAuthCodeConfirmationEmailToAssociatedUser(final String xRequestId, final String companyNumber, String companyName, final String displayName, final String userId)
            throws NullRequiredEmailDataException {
        final var userDetails = usersService.fetchUserDetails(userId, xRequestId);
        if (userDetails == null || StringUtils.isBlank(userDetails.getEmail())) {
            LOGGER.infoContext(xRequestId, REQUESTING_USER_OR_EMAIL_IS_NULL, null);
            throw new NullRequiredEmailDataException(ASSOCIATED_USER_OR_EMAIL_IS_NULL, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE);
        }

        final var emailData = new AuthCodeConfirmationEmailBuilder()
                .setRecipientEmail(userDetails.getEmail())
                .setDisplayName(displayName)
                .setCompanyName(companyName)
                .build();
        final var logMessageSupplier = new EmailNotification(AUTH_CODE_CONFIRMATION_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber);
        sendEmail(xRequestId, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE, emailData, logMessageSupplier);
    }

    public void sendAuthorisationRemovedEmailToAssociatedUser(final String xRequestId, final String companyNumber, final String companyName, final String removedByDisplayName, final String removedUserDisplayName, final String userId)
            throws NullRequiredEmailDataException {
        final var userDetails = usersService.fetchUserDetails(userId, xRequestId);
        if (userDetails == null || StringUtils.isBlank(userDetails.getEmail())) {
            LOGGER.infoContext(xRequestId, ASSOCIATED_USER_OR_EMAIL_IS_NULL, null);
            throw new NullRequiredEmailDataException(ASSOCIATED_USER_OR_EMAIL_IS_NULL, AUTHORISATION_REMOVED_MESSAGE_TYPE);
        }

        final var emailData = new AuthorisationRemovedEmailBuilder()
                .setRemovedByDisplayName(removedByDisplayName)
                .setRemovedUserDisplayName(removedUserDisplayName)
                .setRecipientEmail(userDetails.getEmail())
                .setCompanyName(companyName)
                .build();
        final var logMessageSupplier = new EmailNotification(AUTHORISATION_REMOVED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber);
        sendEmail(xRequestId, AUTHORISATION_REMOVED_MESSAGE_TYPE, emailData, logMessageSupplier);
    }

    public void sendAuthorisationRemovedEmailToRemovedUser(final String xRequestId, final String companyNumber, final String companyName, final String removedByDisplayName, final String userId)
            throws NullRequiredEmailDataException {
        final var userDetails = usersService.fetchUserDetails(userId, xRequestId);
        if (userDetails == null || StringUtils.isBlank(userDetails.getEmail())) {
            LOGGER.infoContext(xRequestId, IMPACTED_USER_OR_EMAIL_IS_NULL, null);
            throw new NullRequiredEmailDataException(IMPACTED_USER_OR_EMAIL_IS_NULL, YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE);
        }

        final var emailData = new YourAuthorisationRemovedEmailBuilder()
                .setRemovedByDisplayName(removedByDisplayName)
                .setRecipientEmail(userDetails.getEmail())
                .setCompanyName(companyName)
                .build();
        final var logMessageSupplier = new EmailNotification(YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE, removedByDisplayName, emailData.getTo(), companyNumber);
        sendEmail(xRequestId, YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE, emailData, logMessageSupplier);
    }

    public void sendInvitationCancelledEmailToAssociatedUser(final String xRequestId, final String companyNumber, final String companyName, final String cancelledByDisplayName, final String cancelledUserDisplayName, final String userId)
            throws NullRequiredEmailDataException {
        final var userDetails = usersService.fetchUserDetails(userId, xRequestId);
        if (userDetails == null || StringUtils.isBlank(userDetails.getEmail())) {
            LOGGER.infoContext(xRequestId, ASSOCIATED_USER_OR_EMAIL_IS_NULL, null);
            throw new NullRequiredEmailDataException(ASSOCIATED_USER_OR_EMAIL_IS_NULL, INVITATION_CANCELLED_MESSAGE_TYPE);
        }

        final var emailData = new InvitationCancelledEmailBuilder()
                .setCancelledByDisplayName(cancelledByDisplayName)
                .setCancelledUserDisplayName(cancelledUserDisplayName)
                .setRecipientEmail(userDetails.getEmail())
                .setCompanyName(companyName)
                .build();
        final var logMessageSupplier = new EmailNotification(INVITATION_CANCELLED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber);
        sendEmail(xRequestId, INVITATION_CANCELLED_MESSAGE_TYPE, emailData, logMessageSupplier);
    }

    public void sendInvitationEmailToAssociatedUser(final String xRequestId, final String companyNumber, final String companyName, final String inviterDisplayName, final String inviteeDisplayName, final String userId)
            throws NullRequiredEmailDataException {
        final var userDetails = usersService.fetchUserDetails(userId, xRequestId);
        if (userDetails == null || StringUtils.isBlank(userDetails.getEmail())) {
            LOGGER.infoContext(xRequestId, ASSOCIATED_USER_OR_EMAIL_IS_NULL, null);
            throw new NullRequiredEmailDataException(ASSOCIATED_USER_OR_EMAIL_IS_NULL, INVITATION_MESSAGE_TYPE);
        }

        final var emailData = new InvitationEmailBuilder()
                .setInviteeDisplayName(inviteeDisplayName)
                .setInviterDisplayName(inviterDisplayName)
                .setRecipientEmail(userDetails.getEmail())
                .setCompanyName(companyName)
                .build();
        final var logMessageSupplier = new EmailNotification(INVITATION_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber);
        sendEmail(xRequestId, INVITATION_MESSAGE_TYPE, emailData, logMessageSupplier);
    }

    public void sendInvitationAcceptedEmailToAssociatedUser(final String xRequestId, final String companyNumber, final String companyName, final Optional<String> invitedByDisplayName, final String inviteeDisplayName, final String userId)
            throws NullRequiredEmailDataException {
        final var userDetails = usersService.fetchUserDetails(userId, xRequestId);
        if (userDetails == null || StringUtils.isBlank(userDetails.getEmail())) {
            LOGGER.infoContext(xRequestId, ASSOCIATED_USER_OR_EMAIL_IS_NULL, null);
            throw new NullRequiredEmailDataException(ASSOCIATED_USER_OR_EMAIL_IS_NULL, INVITATION_ACCEPTED_MESSAGE_TYPE);
        }

        final var emailData = new InvitationAcceptedEmailBuilder()
                .setInviteeDisplayName(inviteeDisplayName)
                .setInviterDisplayName(invitedByDisplayName.orElse(null))
                .setRecipientEmail(userDetails.getEmail())
                .setCompanyName(companyName)
                .build();
        final var logMessageSupplier = new EmailNotification(INVITATION_ACCEPTED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber);
        sendEmail(xRequestId, INVITATION_ACCEPTED_MESSAGE_TYPE, emailData, logMessageSupplier);
    }

    public void sendInvitationRejectedEmailToAssociatedUser(final String xRequestId, final String companyNumber, final String companyName, final String inviteeDisplayName, final String userId)
            throws NullRequiredEmailDataException {
        final var userDetails = usersService.fetchUserDetails(userId, xRequestId);
        if (userDetails == null || StringUtils.isBlank(userDetails.getEmail())) {
            LOGGER.infoContext(xRequestId, ASSOCIATED_USER_OR_EMAIL_IS_NULL, null);
            throw new NullRequiredEmailDataException(ASSOCIATED_USER_OR_EMAIL_IS_NULL, INVITATION_REJECTED_MESSAGE_TYPE);
        }

        final var emailData = new InvitationRejectedEmailBuilder()
                .setInviteeDisplayName(inviteeDisplayName)
                .setRecipientEmail(userDetails.getEmail())
                .setCompanyName(companyName)
                .build();
        final var logMessageSupplier = new EmailNotification(INVITATION_REJECTED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber);
        sendEmail(xRequestId, INVITATION_REJECTED_MESSAGE_TYPE, emailData, logMessageSupplier);
    }

    public void sendInviteEmail(final String xRequestId, final String companyNumber, final String companyName, final String inviterDisplayName, final String invitationExpiryTimestamp, final String inviteeEmail){
        final var emailData = new InviteEmailBuilder()
                .setRecipientEmail(inviteeEmail)
                .setInviterDisplayName(inviterDisplayName)
                .setInvitationExpiryTimestamp(invitationExpiryTimestamp)
                .setInvitationLink(invitationLink)
                .setCompanyName(companyName)
                .build();
        final var logMessageSupplier = new EmailNotification(INVITE_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber).setInvitationExpiryTimestamp(invitationExpiryTimestamp);
        sendEmail(xRequestId, INVITE_MESSAGE_TYPE, emailData, logMessageSupplier);
    }

    public void sendInviteCancelledEmail(final String xRequestId, final String companyNumber, final String companyName, final String cancelledByDisplayName, final AssociationDao associationDao)
            throws NullRequiredEmailDataException {
        sendInviteCancelledEmailToCancelledUser(xRequestId, companyNumber, companyName, cancelledByDisplayName, associationDao);
        sendInviteCancelationEmailToCancellerUser(xRequestId, companyNumber, companyName, cancelledByDisplayName, associationDao);
    }

    private void sendInviteCancelledEmailToCancelledUser(final String xRequestId, final String companyNumber, final String companyName, final String cancelledByDisplayName, final AssociationDao associationDao)
            throws NullRequiredEmailDataException {
        if (associationDao == null || StringUtils.isBlank(associationDao.getUserId())) {
            LOGGER.infoContext(xRequestId, ASSOCIATION_DAO_OR_USER_IS_NULL, null);
            throw new NullRequiredEmailDataException(IMPACTED_USER_OR_EMAIL_IS_NULL, INVITE_CANCELLED_MESSAGE_TYPE);
        }

        final var cancelledUserDetails = usersService.fetchUserDetails(associationDao.getUserId(), xRequestId);
        if (cancelledUserDetails == null || StringUtils.isBlank(cancelledUserDetails.getEmail())) {
            LOGGER.infoContext(xRequestId, IMPACTED_USER_OR_EMAIL_IS_NULL, null);
            throw new NullRequiredEmailDataException(IMPACTED_USER_OR_EMAIL_IS_NULL, INVITE_CANCELLED_MESSAGE_TYPE);
        }

        final var cancelledUserEmail = cancelledUserDetails.getEmail();
        final var cancelledEmailData = new InviteCancelledEmailBuilder()
                .setRecipientEmail(cancelledUserEmail)
                .setCancelledBy(cancelledByDisplayName)
                .setCompanyName(companyName)
                .build();
        final var cancelledLogMessageSupplier = new EmailNotification(INVITE_CANCELLED_MESSAGE_TYPE, APPLICATION_NAMESPACE, cancelledEmailData.getTo(), companyNumber);
        sendEmail(xRequestId, INVITE_CANCELLED_MESSAGE_TYPE, cancelledEmailData, cancelledLogMessageSupplier);
    }

    private void sendInviteCancelationEmailToCancellerUser(final String xRequestId, final String companyNumber, final String companyName, final String cancelledByDisplayName, final AssociationDao associationDao)
            throws NullPointerException, NullRequiredEmailDataException {
        if (associationDao == null || StringUtils.isBlank(associationDao.getUserId())) {
            LOGGER.infoContext(xRequestId, ASSOCIATION_DAO_OR_USER_IS_NULL, null);
            throw new NullRequiredEmailDataException(IMPACTED_USER_OR_EMAIL_IS_NULL, INVITATION_CANCELLED_MESSAGE_TYPE);
        }

        final var cancelledUserDetails = usersService.fetchUserDetails(associationDao.getUserId(), xRequestId);
        if (cancelledUserDetails == null || StringUtils.isBlank(cancelledUserDetails.getEmail())) {
            LOGGER.infoContext(xRequestId, IMPACTED_USER_OR_EMAIL_IS_NULL, null);
            throw new NullRequiredEmailDataException(IMPACTED_USER_OR_EMAIL_IS_NULL, INVITATION_CANCELLED_MESSAGE_TYPE);
        }
        final var cancellerUserDetails = usersService.fetchUserDetails(cancelledByDisplayName, xRequestId);
        if (cancellerUserDetails == null || StringUtils.isBlank(cancellerUserDetails.getEmail())) {
            LOGGER.infoContext(xRequestId, REQUESTING_USER_OR_EMAIL_IS_NULL, null);
            throw new NullRequiredEmailDataException(IMPACTED_USER_OR_EMAIL_IS_NULL, INVITATION_CANCELLED_MESSAGE_TYPE);
        }

        final var cancellerUserEmail = cancellerUserDetails.getEmail();
        final var cancellerEmailData = new InvitationCancelledEmailBuilder()
                .setCancelledByDisplayName(cancelledByDisplayName)
                .setCancelledUserDisplayName(cancelledUserDetails.getDisplayName())
                .setRecipientEmail(cancellerUserEmail)
                .setCompanyName(companyName)
                .build();
        final var cancellerLogMessageSupplier = new EmailNotification(INVITATION_CANCELLED_MESSAGE_TYPE, APPLICATION_NAMESPACE, cancellerEmailData.getTo(), companyNumber);
        sendEmail(xRequestId, INVITATION_CANCELLED_MESSAGE_TYPE, cancellerEmailData, cancellerLogMessageSupplier);
    }

    public void sendDelegatedRemovalOfMigratedEmail(final String xRequestId, final String companyNumber, final String companyName, final String removedBy, final String recipientEmail) {
        final var emailData = new DelegatedRemovalOfMigratedEmailBuilder()
                .setRemovedBy(removedBy)
                .setRecipientEmail(recipientEmail)
                .setCompanyName(companyName)
                .build();
        final var logMessageSupplier = new EmailNotification(DELEGATED_REMOVAL_OF_MIGRATED, removedBy, emailData.getTo(), companyNumber);
        sendEmail(xRequestId, DELEGATED_REMOVAL_OF_MIGRATED, emailData, logMessageSupplier);
    }

    public void sendRemoveOfOwnMigratedEmail(final String xRequestId, final String companyNumber, final String companyName, final String userId)
            throws NullRequiredEmailDataException {
        final var userDetails = usersService.fetchUserDetails(userId, xRequestId);
        if (userDetails == null || StringUtils.isBlank(userDetails.getEmail())) {
            LOGGER.infoContext(xRequestId, REQUESTING_USER_OR_EMAIL_IS_NULL, null);
            throw new NullRequiredEmailDataException(REQUESTING_USER_OR_EMAIL_IS_NULL, REMOVAL_OF_OWN_MIGRATED);
        }

        final var emailData = new RemovalOfOwnMigratedEmailBuilder()
                .setRecipientEmail(userDetails.getEmail())
                .setCompanyName(companyName)
                .build();
        final var logMessageSupplier = new EmailNotification(REMOVAL_OF_OWN_MIGRATED, userDetails.getEmail(), userDetails.getEmail(), companyNumber);
        sendEmail(xRequestId, REMOVAL_OF_OWN_MIGRATED, emailData, logMessageSupplier);
    }

    public void sendDelegatedRemovalOfMigratedBatchEmail(final String xRequestId, final String companyNumber, final String companyName, final String removedBy, final String removedUser, final String userId)
            throws NullRequiredEmailDataException {
        final var userDetails = usersService.fetchUserDetails(userId, xRequestId);
        if (userDetails == null || StringUtils.isBlank(userDetails.getEmail())) {
            LOGGER.infoContext(xRequestId, IMPACTED_USER_OR_EMAIL_IS_NULL, null);
            throw new NullRequiredEmailDataException(IMPACTED_USER_OR_EMAIL_IS_NULL, DELEGATED_REMOVAL_OF_MIGRATED_BATCH);
        }

        final var emailData = new DelegatedRemovalOfMigratedBatchEmailBuilder()
                .setRemovedBy(removedBy)
                .setRemovedUser(removedUser)
                .setRecipientEmail(userDetails.getEmail())
                .setCompanyName(companyName).build();
        final var logMessageSupplier = new EmailNotification(DELEGATED_REMOVAL_OF_MIGRATED_BATCH, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber);
        sendEmail(xRequestId, DELEGATED_REMOVAL_OF_MIGRATED_BATCH, emailData, logMessageSupplier);
    }

    private EmailBatchType determineEmailBatchType(final String xRequestId, final AssociationDao targetAssociation, final StatusEnum newStatus) {
        final var oldStatus = targetAssociation.getStatus();

        if (isOAuth2Request() && isRequestingUser(targetAssociation) && oldStatus.equals(AWAITING_APPROVAL.getValue()) && newStatus.equals(REMOVED)) {
            return EmailBatchType.REJECTING_INVITATION;
        }
        if (isOAuth2Request() && oldStatus.equals(CONFIRMED.getValue()) && newStatus.equals(REMOVED)) {
            return EmailBatchType.AUTHORISATION_IS_BEING_REMOVED;
        }
        if (isOAuth2Request() && isRequestingUser(targetAssociation) && oldStatus.equals(AWAITING_APPROVAL.getValue()) && newStatus.equals(CONFIRMED)) {
            return EmailBatchType.ACCEPTING_INVITATION;
        }
        if (isOAuth2Request() && !isRequestingUser(targetAssociation) && oldStatus.equals(AWAITING_APPROVAL.getValue()) && newStatus.equals(REMOVED)) {
            return EmailBatchType.CANCELLING_ANOTHER_USERS_INVITATION;
        }
        if (isOAuth2Request() && !isRequestingUser(targetAssociation) && oldStatus.equals(MIGRATED.getValue()) && newStatus.equals(REMOVED)) {
            return EmailBatchType.REMOVING_ANOTHER_USERS_MIGRATED_ASSOCIATION;
        }
        if (isOAuth2Request() && isRequestingUser(targetAssociation) && oldStatus.equals(MIGRATED.getValue()) && newStatus.equals(REMOVED)) {
            return EmailBatchType.REMOVING_OWN_MIGRATED_ASSOCIATION;
        }
        if (isOAuth2Request() && !isRequestingUser(targetAssociation) && (oldStatus.equals(MIGRATED.getValue()) || oldStatus.equals(UNAUTHORISED.getValue()) && newStatus.equals(CONFIRMED))) {
            return EmailBatchType.INVITING_USER;
        }
        if (isAPIKeyRequest() && (oldStatus.equals(MIGRATED.getValue()) || oldStatus.equals(UNAUTHORISED.getValue()) && newStatus.equals(CONFIRMED))) {
            return EmailBatchType.CONFIRMING_WITH_AUTH_CODE;
        }
        final var loggedException = new EmailBatchException(String.format("Unable to determine batch type, defaulting to NONE - isOAuth2Request: %s, isAPIKeyRequest: %s, isRequestingUser: %s, oldStatus: %s, newStatus: %s", isOAuth2Request(), isAPIKeyRequest(), isRequestingUser(targetAssociation), oldStatus, newStatus));
        LOGGER.errorContext (xRequestId, loggedException, null);
        return EmailBatchType.NONE;
    }
}
