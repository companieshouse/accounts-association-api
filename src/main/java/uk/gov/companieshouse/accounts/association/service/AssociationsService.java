package uk.gov.companieshouse.accounts.association.service;

import static java.time.LocalDateTime.now;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_UPDATE_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.models.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToAuthCodeConfirmedUpdated;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToConfirmedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToInvitationUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToRemovedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToUnauthorisedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.hasAdminPrivilege;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.isRequestingUser;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.REMOVED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.UNAUTHORISED;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut;
import uk.gov.companieshouse.api.accounts.user.model.User;

@Service
public class AssociationsService {

    private final AssociationsTransactionService associationsTransactionService;

    @Autowired
    public AssociationsService(AssociationsTransactionService associationsTransactionService) {
        this.associationsTransactionService = associationsTransactionService;
    }

    public Update mapToAPIKeyUpdate(final RequestBodyPut.StatusEnum proposedStatus, final AssociationDao targetAssociation, final User targetUser) {
        final var oldStatus = StatusEnum.fromValue(targetAssociation.getStatus());
        return switch (proposedStatus){
            case CONFIRMED -> Optional.of(CONFIRMED)
                    .filter(status -> MIGRATED.equals(oldStatus) || UNAUTHORISED.equals(oldStatus))
                    .map(status -> mapToAuthCodeConfirmedUpdated(targetAssociation, targetUser, COMPANIES_HOUSE))
                    .orElseThrow(() -> new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception(String.format("API Key cannot change a %s association to confirmed", oldStatus.getValue()))));
            case REMOVED -> throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception("Unable to change the association status to removed with API Key"));
            case UNAUTHORISED -> mapToUnauthorisedUpdate(targetAssociation, targetUser);
        };
    }

    public Update mapToOAuth2Update(final RequestBodyPut.StatusEnum proposedStatus, final AssociationDao targetAssociation, final User targetUser){
        final var oldStatus = StatusEnum.fromValue(targetAssociation.getStatus());
        if (isRequestingUser(targetAssociation)){
            return switch(proposedStatus){
                case CONFIRMED -> Optional.of(CONFIRMED)
                        .filter(status -> !(MIGRATED.equals(oldStatus) || UNAUTHORISED.equals(oldStatus)))
                        .map(status -> mapToConfirmedUpdate(targetAssociation, targetUser, getEricIdentity()))
                        .orElseThrow(() -> new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception("Requesting user cannot change their status from migrated to confirmed")));
                case REMOVED -> mapToRemovedUpdate(targetAssociation, targetUser, getEricIdentity());
                case UNAUTHORISED -> throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception("Requesting user cannot change their status to unauthorised"));
            };
        }
        return switch (proposedStatus){
            case CONFIRMED -> Optional.of(CONFIRMED)
                    .filter(status -> MIGRATED.equals(oldStatus) || UNAUTHORISED.equals(oldStatus))
                    .filter(status -> associationsTransactionService.confirmedAssociationExists(targetAssociation.getCompanyNumber(), getEricIdentity()))
                    .map(status -> mapToInvitationUpdate(targetAssociation, targetUser, getEricIdentity(), now()))
                    .orElseThrow(() -> new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception(String.format("Requesting %s user cannot change another user to confirmed or the requesting user is not associated with company %s", getEricIdentity(), targetAssociation.getCompanyNumber()))));
            case REMOVED -> Optional.of(REMOVED)
                    .filter(status -> associationsTransactionService.confirmedAssociationExists(targetAssociation.getCompanyNumber(), getEricIdentity()) || hasAdminPrivilege(ADMIN_UPDATE_PERMISSION))
                    .map(status -> mapToRemovedUpdate(targetAssociation, targetUser, getEricIdentity()))
                    .orElseThrow(() -> new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception(String.format("Requesting %s user cannot change another user to confirmed or the requesting user is not associated with company %s", getEricIdentity(), targetAssociation.getCompanyNumber()))));
            case UNAUTHORISED -> throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception(String.format("Requesting %s user cannot change another user to unauthorised", getEricIdentity())));
        };
    }

}
