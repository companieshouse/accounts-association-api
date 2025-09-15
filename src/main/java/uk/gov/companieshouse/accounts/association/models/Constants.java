package uk.gov.companieshouse.accounts.association.models;

public class Constants {

    public static final String X_REQUEST_ID = "X-Request-Id";
    public static final String UNKNOWN = "unknown";
    public static final String OAUTH2 = "oauth2";
    public static final String KEY = "key";
    public static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN = "Please check the request and try again";
    public static final String PAGINATION_IS_MALFORMED = "pageIndex was less than 0 or itemsPerPage was less than or equal to 0";
    public static final String DEFAULT_KIND = "association";
    public static final String DEFAULT_DISPLAY_NAME = "Not provided";
    public static final String ADMIN_READ_PERMISSION = "/admin/user-company-associations/read";
    public static final String ADMIN_UPDATE_PERMISSION = "/admin/user-company-associations/update";
    public static final String COMPANIES_HOUSE =  "Companies House";

    public static final String REST_CLIENT_EXCEPTION = "Encountered rest client exception when calling: %s";
    public static final String REST_CLIENT_START = "Starting request to %s";
    public static final String REST_CLIENT_FINISH = "Finished request to %s";


}
