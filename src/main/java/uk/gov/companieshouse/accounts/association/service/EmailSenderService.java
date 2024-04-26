package uk.gov.companieshouse.accounts.association.service;

import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import java.util.function.Consumer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class EmailSenderService {

    private final AssociationsRepository associationsRepository;
    private final UsersService usersService;
    private final EmailService emailService;

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    public EmailSenderService(AssociationsRepository associationsRepository, UsersService usersService, EmailService emailService) {
        this.associationsRepository = associationsRepository;
        this.usersService = usersService;
        this.emailService = emailService;
    }

    @Transactional( readOnly = true )
    private Page<String> fetchAllAssociatedUsersEmails( final CompanyDetails companyDetails, final Set<String> statuses ){
        final var companyNumber = companyDetails.getCompanyNumber();
        return associationsRepository.fetchAssociatedUsers( companyNumber, statuses, Pageable.unpaged() )
                .map( AssociationDao::getUserId )
                .map( usersService::fetchUserDetails )
                .map( User::getEmail );
    }

    private void sendEmailToUsersAssociatedWithCompany( final String xRequestId, final CompanyDetails companyDetails, final Consumer<String> sendEmailForEmailAddress ){
        final var companyNumber = companyDetails.getCompanyNumber();
        LOG.debugContext( xRequestId, String.format( "Attempting to send notifications to users from company %s", companyNumber ), null );
        fetchAllAssociatedUsersEmails( companyDetails, Set.of( StatusEnum.CONFIRMED.getValue() ) )
                .forEach( sendEmailForEmailAddress );
        LOG.debugContext( xRequestId, String.format( "Successfully sent notifications to users from company %s", companyNumber ), null );
    }

    @Async
    public void sendAuthCodeConfirmationEmail( final String xRequestId, final CompanyDetails companyDetails, final String displayName ){
        final var companyName = companyDetails.getCompanyName();
        final Consumer<String> sendAuthCodeConfirmationEmail = recipientEmail -> emailService.sendAuthCodeConfirmationEmail( recipientEmail, displayName, companyName );
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendAuthCodeConfirmationEmail );
    }

    @Async
    public void sendAuthorisationRemovedEmail( final String xRequestId, final CompanyDetails companyDetails, final String removedByDisplayName, final String removedUserDisplayName ) {
        final var companyName = companyDetails.getCompanyName();
        final Consumer<String> sendAuthorisationRemovedEmail = recipientEmail -> emailService.sendAuthorisationRemovedEmail( recipientEmail, removedByDisplayName, removedUserDisplayName, companyName );
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendAuthorisationRemovedEmail );
    }

    @Async
    public void sendInvitationCancelledEmail( final String xRequestId, final CompanyDetails companyDetails, final String cancelledByDisplayName, final String cancelledUserDisplayName ){
        final var companyName = companyDetails.getCompanyName();
        final Consumer<String> sendInvitationCancelledEmail = recipientEmail -> emailService.sendInvitationCancelledEmail( recipientEmail, cancelledByDisplayName, cancelledUserDisplayName, companyName );
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendInvitationCancelledEmail );
    }

    @Async
    public void sendInvitationEmail( final String xRequestId, final CompanyDetails companyDetails, final String inviterDisplayName, final String inviteeDisplayName ){
        final var companyName = companyDetails.getCompanyName();
        final Consumer<String> sendInvitationEmail = recipientEmail -> emailService.sendInvitationEmail( recipientEmail, inviterDisplayName, inviteeDisplayName, companyName );
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendInvitationEmail );
    }

    @Async
    public void sendInvitationAcceptedEmail( final String xRequestId, final CompanyDetails companyDetails, final String inviterDisplayName, final String inviteeDisplayName ){
        final var companyName = companyDetails.getCompanyName();
        final Consumer<String> sendInvitationAcceptedEmail = recipientEmail -> emailService.sendInvitationAcceptedEmail( recipientEmail, inviterDisplayName, inviteeDisplayName, companyName );
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendInvitationAcceptedEmail );
    }

    @Async
    public void sendInvitationRejectedEmail( final String xRequestId, final CompanyDetails companyDetails, final String inviteeDisplayName ){
        final var companyName = companyDetails.getCompanyName();
        final Consumer<String> sendInvitationRejectedEmail = recipientEmail -> emailService.sendInvitationRejectedEmail( recipientEmail, inviteeDisplayName, companyName );
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendInvitationRejectedEmail );
    }

}
