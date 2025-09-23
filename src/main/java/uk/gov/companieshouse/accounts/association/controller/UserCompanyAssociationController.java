package uk.gov.companieshouse.accounts.association.controller;

import static org.springframework.http.HttpStatus.OK;
import static uk.gov.companieshouse.accounts.association.models.Constants.ASSOCIATION_NOT_FOUND_WITH_ID_VAR;
import static uk.gov.companieshouse.accounts.association.models.Constants.PAGINATION_IS_MALFORMED;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getRequestcontext;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isAPIKeyRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.AssociationsTransactionService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.service.email.EmailEventPublisher;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationInterface;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.PreviousStatesList;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut;

@RestController
public class UserCompanyAssociationController implements UserCompanyAssociationInterface {

    private final UsersService usersService;
    private final AssociationsService associationsService;
    private final AssociationsTransactionService associationsTransactionService;
    private final EmailEventPublisher emailEventPublisher;

    public UserCompanyAssociationController(final UsersService usersService, final AssociationsService associationsService, final AssociationsTransactionService associationsTransactionService, final EmailEventPublisher emailEventPublisher) {
        this.usersService = usersService;
        this.associationsService = associationsService;
        this.associationsTransactionService = associationsTransactionService;
        this.emailEventPublisher = emailEventPublisher;
    }

    @Override
    public ResponseEntity<Association> getAssociationForId(final String associationId) {
        LOGGER.infoContext(getXRequestId(), String.format("Received request with id=%s.", associationId),null);
        return associationsTransactionService.fetchAssociationDto(associationId)
                .map(association -> new ResponseEntity<>(association, OK))
                .orElseThrow(() -> new NotFoundRuntimeException(String.format(
                        ASSOCIATION_NOT_FOUND_WITH_ID_VAR, associationId)));
    }

    @Override
    public ResponseEntity<InvitationsList> getInvitationsForAssociation(final String associationId, final Integer pageIndex, final Integer itemsPerPage) {
        LOGGER.infoContext(getXRequestId(), String.format("Received request with id=%s, page_index=%d, items_per_page=%d.", associationId, pageIndex, itemsPerPage),null);

        if (pageIndex < 0 || itemsPerPage <= 0){
            throw new BadRequestRuntimeException(PAGINATION_IS_MALFORMED);
        }

        return associationsTransactionService.fetchInvitations(associationId, pageIndex, itemsPerPage)
                .map(invitations -> new ResponseEntity<>(invitations, OK))
                .orElseThrow(() -> new NotFoundRuntimeException(String.format(
                        ASSOCIATION_NOT_FOUND_WITH_ID_VAR, associationId)));
    }

    @Override
    public ResponseEntity<PreviousStatesList> getPreviousStatesForAssociation(final String associationId, final Integer pageIndex, final Integer itemsPerPage){
        LOGGER.infoContext(getXRequestId(), String.format("Received request with id=%s, page_index=%d, items_per_page=%d.", associationId, pageIndex, itemsPerPage),null);

        if (pageIndex < 0 || itemsPerPage <= 0) {
            throw new BadRequestRuntimeException(PAGINATION_IS_MALFORMED);
        }

        return associationsTransactionService.fetchPreviousStates(associationId, pageIndex, itemsPerPage)
                .map(previousStates -> new ResponseEntity<>(previousStates, OK))
                .orElseThrow(() -> new NotFoundRuntimeException(String.format(
                        ASSOCIATION_NOT_FOUND_WITH_ID_VAR, associationId)));
    }

    @Override
    public ResponseEntity<Void> updateAssociationStatusForId(final String associationId, final RequestBodyPut requestBody) {
        final var xRequestId = getXRequestId();
        LOGGER.infoContext(getXRequestId(), String.format("Received request with id=%s, user_id=%s, status=%s.", associationId, getEricIdentity(), requestBody.getStatus()),null);

        final var targetAssociation = associationsTransactionService
                .fetchAssociationDao(associationId)
                .orElseThrow(() -> new NotFoundRuntimeException(String.format(
                        ASSOCIATION_NOT_FOUND_WITH_ID_VAR, associationId)));

        final var targetUser = usersService.fetchUserDetails(xRequestId, targetAssociation);

        final var update = isAPIKeyRequest() ? associationsService.mapToAPIKeyUpdate(requestBody.getStatus(), targetAssociation, targetUser) : associationsService.mapToOAuth2Update(requestBody.getStatus(), targetAssociation, targetUser);
        associationsTransactionService.updateAssociation(targetAssociation.getId(), update);

        final var newStatus = StatusEnum.fromValue(requestBody.getStatus().getValue());
        emailEventPublisher.publishEmailEvent(targetAssociation, targetUser, newStatus, getRequestcontext());

        return new ResponseEntity<>(OK);
    }
}
