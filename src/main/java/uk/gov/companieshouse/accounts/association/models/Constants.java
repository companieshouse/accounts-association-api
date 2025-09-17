package uk.gov.companieshouse.accounts.association.models;

public class Constants {

    public static final String X_REQUEST_ID = "X-Request-Id";
    public static final String UNKNOWN = "unknown";
    public static final String OAUTH2 = "oauth2";
    public static final String KEY = "key";
    public static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN = "Please check the request and try again";
    public static final String PAGINATION_IS_MALFORMED = "pageIndex was less than 0 or itemsPerPage was less than or equal to 0 " + PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
    public static final String ONLY_ONE_MUST_BE_PRESENT = "Only one of user_id or user_email must be present " + PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
    public static final String REQUESTING_USER_NOT_PERMITTED = "Requesting user is not permitted to retrieve data.";
    public static final String DEFAULT_KIND = "association";
    public static final String DEFAULT_DISPLAY_NAME = "Not provided";
    public static final String ADMIN_READ_PERMISSION = "/admin/user-company-associations/read";
    public static final String ADMIN_UPDATE_PERMISSION = "/admin/user-company-associations/update";
    public static final String COMPANIES_HOUSE =  "Companies House";
    public static final String ASSOCIATION_NOT_FOUND_WITH_ID_VAR = "Cannot find Association for the id: %s";
    public static final String ASSOCIATION_NOT_FOUND = "Association not found " + PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
    public static final String COMPANY_NOT_FOUND_WITH_ID_VAR = "Company not found for the id: %s";
    public static final String USER_ID_VAR_AND_COMPANY_NUMBER_VAR_ASSOCIATION_EXISTS = "Association between user_id %s and company_number %s already exists";
    public static final String EMAIL_COMPANY_ASSOCIATION_EXISTS_AT_VAR = "This invitee email address already has a confirmed association at company %s " + PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
    public static final String USER_COMPANY_ASSOCIATION_DOES_NOT_EXIST_AT_VAR = "Requesting user %s does not have a confirmed association at company %s " + PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
    public static final String STATUS_IS_INVALID = "Status is invalid " + PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
    public static final String INVITEE_EMAIL_IS_NULL = "invitee_email_id is null " + PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
    public static final String API_KEY_CANNOT_CHANGE_VAR_TO_CONFIRMED = "API Key cannot change a %s association to confirmed " + PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
    public static final String API_KEY_CANNOT_CHANGE_TO_REMOVED = "Unable to change the association status to removed with API Key " + PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
    public static final String REQUESTING_USER_CANNOT_CHANGE_MIGRATED_TO_CONFIRMED = "Requesting user cannot change their status from migrated to confirmed " + PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
    public static final String REQUESTING_USER_CANNOT_CHANGE_TO_UNAUTHORISED = "Requesting user cannot change their status to unauthorised " + PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
    public static final String REQUESTING_USER_VAR_CANNOT_CHANGE_TO_UNAUTHORISED = "Requesting user %s cannot change their status to unauthorised " + PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
    public static final String REQUESTING_USER_VAR_CANNOT_CHANGE_ANOTHER_USER_TO_VAR_OR_IS_NOT_ASSOCIATED_WITH_COMPANY_VAR = "Requesting %s user cannot change another user to %s or the requesting user is not associated with company %s" + PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
    public static final String FAILED_TO_UPDATE_ASSOCIATION_VAR = "Failed to update association with id: %s ";

    public static final String REST_CLIENT_EXCEPTION_CALLING_VAR = "Encountered rest client exception when calling: %s";
    public static final String REST_CLIENT_START_TO_VAR = "Starting request to %s";
    public static final String REST_CLIENT_FINISH_TO_VAR = "Finished request to %s";


}
