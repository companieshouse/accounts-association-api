package uk.gov.companieshouse.accounts.association.service;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.email.senders.AuthCodeConfirmationEmailSender;
import uk.gov.companieshouse.accounts.association.models.email.senders.AuthorisationRemovedEmailSender;
import uk.gov.companieshouse.accounts.association.models.email.senders.InvitationAcceptedEmailSender;
import uk.gov.companieshouse.accounts.association.models.email.senders.InvitationCancelledEmailSender;
import uk.gov.companieshouse.accounts.association.models.email.senders.InvitationEmailSender;
import uk.gov.companieshouse.accounts.association.models.email.senders.InvitationRejectedEmailSender;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;

@Service
@ComponentScan("uk.gov.companieshouse.email_producer")
public class EmailService {

    private final AssociationsRepository associationsRepository;
    private final UsersService usersService;
    private final AuthCodeConfirmationEmailSender authCodeConfirmationEmailSender;
    private final AuthorisationRemovedEmailSender authorisationRemovedEmailSender;
    private final InvitationCancelledEmailSender invitationCancelledEmailSender;
    private final InvitationEmailSender invitationEmailSender;
    private final InvitationAcceptedEmailSender invitationAcceptedEmailSender;
    private final InvitationRejectedEmailSender invitationRejectedEmailSender;

    @Autowired
    public EmailService( final EmailProducer emailProducer, AssociationsRepository associationsRepository, UsersService usersService) {
        this.associationsRepository = associationsRepository;
        this.usersService = usersService;
        authCodeConfirmationEmailSender = new AuthCodeConfirmationEmailSender( emailProducer );
        authorisationRemovedEmailSender = new AuthorisationRemovedEmailSender( emailProducer );
        invitationCancelledEmailSender = new InvitationCancelledEmailSender( emailProducer );
        invitationEmailSender = new InvitationEmailSender( emailProducer );
        invitationAcceptedEmailSender = new InvitationAcceptedEmailSender( emailProducer );
        invitationRejectedEmailSender = new InvitationRejectedEmailSender( emailProducer );
    }

    @Transactional( readOnly = true )
    public List<Supplier<User>> createRequestsToFetchAssociatedUsers( final CompanyDetails companyDetails, final Set<String> statuses ){
        final var companyNumber = companyDetails.getCompanyNumber();
        return associationsRepository.fetchAssociatedUsers( companyNumber, statuses, Pageable.unpaged() )
                .map( AssociationDao::getUserId )
                .map( usersService::createFetchUserDetailsRequest )
                .getContent();
    };

    @Async
    public void sendAuthCodeConfirmationEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String displayName, final List<Supplier<User>> requestsToFetchAssociatedUsers ){
        authCodeConfirmationEmailSender.sendEmailToAssociatedUsers( xRequestId, companyDetails, displayName, requestsToFetchAssociatedUsers );
    }

    @Async
    public void sendAuthorisationRemovedEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String removedByDisplayName, final String removedUserDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers ){
        authorisationRemovedEmailSender.sendEmailToAssociatedUsers( xRequestId, companyDetails, removedByDisplayName, removedUserDisplayName, requestsToFetchAssociatedUsers );
    }

    @Async
    public void sendInvitationCancelledEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String cancelledByDisplayName, final String cancelledUserDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers ){
        invitationCancelledEmailSender.sendEmailToAssociatedUsers( xRequestId, companyDetails, cancelledByDisplayName, cancelledUserDisplayName, requestsToFetchAssociatedUsers );
    }

    @Async
    public void sendInvitationEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String inviterDisplayName, final String inviteeDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers ){
        invitationEmailSender.sendEmailToAssociatedUsers( xRequestId, companyDetails, inviterDisplayName, inviteeDisplayName, requestsToFetchAssociatedUsers );
    }

    @Async
    public void sendInvitationAcceptedEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String inviterDisplayName, final String inviteeDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers ){
        invitationAcceptedEmailSender.sendEmailToAssociatedUsers( xRequestId, companyDetails, inviterDisplayName, inviteeDisplayName, requestsToFetchAssociatedUsers );
    }

    @Async
    public void sendInvitationRejectedEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String inviteeDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers ){
        invitationRejectedEmailSender.sendEmailToAssociatedUsers( xRequestId, companyDetails, inviteeDisplayName, requestsToFetchAssociatedUsers );
    }

}
