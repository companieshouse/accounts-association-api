package uk.gov.companieshouse.accounts.association.utils;

import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isAPIKeyRequest;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.isRequestingUser;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.REMOVED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.UNAUTHORISED;

import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;

public enum EmailBatchType {

    REJECTING_INVITATION,
    AUTHORISATION_IS_BEING_REMOVED,
    ACCEPTING_INVITATION,
    CANCELLING_ANOTHER_USERS_INVITATION,
    REMOVING_ANOTHER_USERS_MIGRATED_ASSOCIATION,
    REMOVING_OWN_MIGRATED_ASSOCIATION,
    INVITING_USER,
    CONFIRMING_WITH_AUTH_CODE,
    NONE;

}


