package uk.gov.companieshouse.accounts.association.rest;

import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.utils.ApiClientUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut;
import uk.gov.companieshouse.api.accounts.associations.model.ResponseBodyPost;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

@Service
public class AssociationsEndpointTest {

    @Value("${internal.api.url}")
    private String internalApiUrl;

    private final ApiClientUtil apiClientUtil;

    @Autowired
    public AssociationsEndpointTest(ApiClientUtil apiClientUtil) {
        this.apiClientUtil = apiClientUtil;
    }

    public ApiResponse<AssociationsList> getAssociationsForCompany( final String companyNumber, final Boolean includeRemoved, final Integer pageIndex, final Integer itemsPerPage ) throws IOException, URIValidationException {
        final var getAssociationsForCompanyUrl = String.format( "/associations/companies/%s", companyNumber ) ;
        return apiClientUtil.getInternalApiClientForSession(internalApiUrl)
                .privateAccountsAssociationResourceHandler()
                .getAssociationsForCompany( getAssociationsForCompanyUrl, includeRemoved, pageIndex, itemsPerPage )
                .execute();
    }

    public ApiResponse<ResponseBodyPost> addAssociation( final String companyNumber ) throws IOException, URIValidationException {
        final var addAssociationUrl = "/associations";
        return apiClientUtil.getInternalApiClientForSession( internalApiUrl )
                .privateAccountsAssociationResourceHandler()
                .addAssociation( addAssociationUrl, companyNumber )
                .execute();
    }

    public ApiResponse<InvitationsList> fetchActiveInvitationsForUser( final Integer pageIndex, final Integer itemsPerPage ) throws IOException, URIValidationException {
        final var fetchActiveInvitationsForUserUrl = "/associations/invitations";
        return apiClientUtil.getInternalApiClientForSession( internalApiUrl )
                .privateAccountsAssociationResourceHandler()
                .fetchActiveInvitationsForUser( fetchActiveInvitationsForUserUrl, pageIndex, itemsPerPage )
                .execute();
    }

    public ApiResponse<AssociationsList> fetchAssociationsBy( final List<String> status, final Integer pageIndex, final Integer itemsPerPage, final String companyNumber ) throws IOException, URIValidationException {
        final var fetchAssociationsByUrl = "/associations";
        return apiClientUtil.getInternalApiClientForSession( internalApiUrl )
                .privateAccountsAssociationResourceHandler()
                .fetchAssociationsBy( fetchAssociationsByUrl, status, pageIndex, itemsPerPage, companyNumber )
                .execute();
    }

    public ApiResponse<Association> getAssociationForId( final String associationId ) throws IOException, URIValidationException {
        final var getAssociationForIdUrl = String.format( "/associations/%s", associationId );
        return apiClientUtil.getInternalApiClientForSession( internalApiUrl )
                .privateAccountsAssociationResourceHandler()
                .getAssociationForId( getAssociationForIdUrl )
                .execute();
    }

    public ApiResponse<InvitationsList> getInvitationsForAssociation( final String associationId ) throws IOException, URIValidationException {
        final var getInvitationsForAssociationUrl = String.format( "/associations/%s/invitations", associationId );
        return apiClientUtil.getInternalApiClientForSession( internalApiUrl )
                .privateAccountsAssociationResourceHandler()
                .getInvitationsForAssociation( getInvitationsForAssociationUrl )
                .execute();
    }

    public ApiResponse<ResponseBodyPost> inviteUser( final String inviteeEmailId, final String companyNumber ) throws IOException, URIValidationException {
        final var inviteUserUrl = "/associations/invitations";
        return apiClientUtil.getInternalApiClientForSession( internalApiUrl )
                .privateAccountsAssociationResourceHandler()
                .inviteUser( inviteUserUrl, inviteeEmailId, companyNumber )
                .execute();
    }

    public ApiResponse<Void> updateAssociationStatusForId( final String associationId, RequestBodyPut.StatusEnum statusEnum ) throws IOException, URIValidationException {
        final var updateAssociationStatusForIdUrl = String.format( "/associations/%s", associationId );
        return apiClientUtil.getInternalApiClientForSession( internalApiUrl )
                .privateAccountsAssociationResourceHandler()
                .updateAssociationStatusForId( updateAssociationStatusForIdUrl, statusEnum )
                .execute();
    }

}
