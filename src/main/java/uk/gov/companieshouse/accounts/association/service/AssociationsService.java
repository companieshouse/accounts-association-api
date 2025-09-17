package uk.gov.companieshouse.accounts.association.service;

import static java.time.LocalDateTime.now;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_UPDATE_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.API_KEY_CANNOT_CHANGE_TO_REMOVED;
import static uk.gov.companieshouse.accounts.association.models.Constants.API_KEY_CANNOT_CHANGE_VAR_TO_CONFIRMED;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.models.Constants.REQUESTING_USER_CANNOT_CHANGE_MIGRATED_TO_CONFIRMED;
import static uk.gov.companieshouse.accounts.association.models.Constants.REQUESTING_USER_CANNOT_CHANGE_TO_UNAUTHORISED;
import static uk.gov.companieshouse.accounts.association.models.Constants.REQUESTING_USER_VAR_CANNOT_CHANGE_ANOTHER_USER_TO_VAR_OR_IS_NOT_ASSOCIATED_WITH_COMPANY_VAR;
import static uk.gov.companieshouse.accounts.association.models.Constants.REQUESTING_USER_VAR_CANNOT_CHANGE_TO_UNAUTHORISED;
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
                    .orElseThrow(() -> new BadRequestRuntimeException(String.format(
                            API_KEY_CANNOT_CHANGE_VAR_TO_CONFIRMED, oldStatus.getValue())));
            case REMOVED -> throw new BadRequestRuntimeException(API_KEY_CANNOT_CHANGE_TO_REMOVED);
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
                        .orElseThrow(() -> new BadRequestRuntimeException(REQUESTING_USER_CANNOT_CHANGE_MIGRATED_TO_CONFIRMED));
                case REMOVED -> mapToRemovedUpdate(targetAssociation, targetUser, getEricIdentity());
                case UNAUTHORISED -> throw new BadRequestRuntimeException(REQUESTING_USER_CANNOT_CHANGE_TO_UNAUTHORISED);
            };
        }
        return switch (proposedStatus){
            case CONFIRMED -> Optional.of(CONFIRMED)
                    .filter(status -> MIGRATED.equals(oldStatus) || UNAUTHORISED.equals(oldStatus))
                    .filter(status -> associationsTransactionService.confirmedAssociationExists(targetAssociation.getCompanyNumber(), getEricIdentity()))
                    .map(status -> mapToInvitationUpdate(targetAssociation, targetUser, getEricIdentity(), now()))
                    .orElseThrow(() -> new BadRequestRuntimeException(String.format(
                            REQUESTING_USER_VAR_CANNOT_CHANGE_ANOTHER_USER_TO_VAR_OR_IS_NOT_ASSOCIATED_WITH_COMPANY_VAR, getEricIdentity(), proposedStatus.getValue(), targetAssociation.getCompanyNumber())));
            case REMOVED -> Optional.of(REMOVED)
                    .filter(status -> associationsTransactionService.confirmedAssociationExists(targetAssociation.getCompanyNumber(), getEricIdentity()) || hasAdminPrivilege(ADMIN_UPDATE_PERMISSION))
                    .map(status -> mapToRemovedUpdate(targetAssociation, targetUser, getEricIdentity()))
                    .orElseThrow(() -> new BadRequestRuntimeException(String.format(REQUESTING_USER_VAR_CANNOT_CHANGE_ANOTHER_USER_TO_VAR_OR_IS_NOT_ASSOCIATED_WITH_COMPANY_VAR, getEricIdentity(), proposedStatus.getValue(), targetAssociation.getCompanyNumber())));
            case UNAUTHORISED -> throw new BadRequestRuntimeException(String.format(REQUESTING_USER_VAR_CANNOT_CHANGE_TO_UNAUTHORISED, getEricIdentity()));
        };
    }

}
