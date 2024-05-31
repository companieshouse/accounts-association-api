package uk.gov.companieshouse.accounts.association.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.rest.AssociationsEndpointTest;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.ResponseBodyPost;

@RestController
public class AccountsAssociationTestController {

    private final AssociationsEndpointTest associationsEndpointTest;

    public AccountsAssociationTestController(AssociationsEndpointTest associationsEndpointTest) {
        this.associationsEndpointTest = associationsEndpointTest;
    }


    @GetMapping( "/associations/api-client/get-associations-for-company" )
    public ResponseEntity<AssociationsList> getAssociationsForCompany() throws Exception {

        final String companyNumber = "AA293846";
        final Boolean includeRemoved = true;
        final Integer pageIndex = 0;
        final Integer itemsPerPage = 3;

        final var response =
        associationsEndpointTest.getAssociationsForCompany( companyNumber, includeRemoved, pageIndex, itemsPerPage )
                .getData();

        return new ResponseEntity<>( response, HttpStatus.OK );
    }

    @GetMapping( "/associations/api-client/add-association" )
    public ResponseEntity<ResponseBodyPost> addAssociation() throws Exception {

        final String companyNumber = "AA293846";

        final var response =
        associationsEndpointTest.addAssociation( companyNumber )
                .getData();

        return new ResponseEntity<>( response, HttpStatus.OK );
    }

    @GetMapping( "/associations/api-client/fetch-active-invitations-for-user" )
    public ResponseEntity<InvitationsList> fetchActiveInvitationsForUser() throws Exception {

        final Integer pageIndex = 0;
        final Integer itemsPerPage = 3;

        final var response =
        associationsEndpointTest.fetchActiveInvitationsForUser( pageIndex, itemsPerPage )
                .getData();

        return new ResponseEntity<>( response, HttpStatus.OK );
    }

    @GetMapping( "/associations/api-client/fetch-association-by" )
    public ResponseEntity<AssociationsList> fetchAssociationsBy() throws Exception {

        final List<String> status = List.of( "confirmed", "awaiting-approval", "removed" );
        final Integer pageIndex = 0;
        final Integer itemsPerPage = 3;
        final String companyNumber = "AA293846";

        final var response =
        associationsEndpointTest.fetchAssociationsBy( status, pageIndex, itemsPerPage, companyNumber )
                .getData();

        return new ResponseEntity<>( response, HttpStatus.OK );
    }

    @GetMapping( "/associations/api-client/get-association-for-id" )
    public ResponseEntity<Association> getAssociationForId() throws Exception {

        final String associationId = "1";

        final var response =
        associationsEndpointTest.getAssociationForId( associationId )
                .getData();

        return new ResponseEntity<>( response, HttpStatus.OK );
    }

    @GetMapping( "/associations/api-client/get-invitations-for-association" )
    public ResponseEntity<InvitationsList> getInvitationsForAssociation() throws Exception {

        final String associationId = "1";

        final var response =
        associationsEndpointTest.getInvitationsForAssociation( associationId )
                .getData();

        return new ResponseEntity<>( response, HttpStatus.OK );
    }

    @GetMapping( "/associations/api-client/invite-user" )
    public ResponseEntity<ResponseBodyPost> inviteUser() throws Exception {

        final String inviteeEmailId = "kpatel@companieshouse.gov.uk";
        final String companyNumber = "AA293846";

        final var response =
        associationsEndpointTest.inviteUser( inviteeEmailId, companyNumber )
                .getData();

        return new ResponseEntity<>( response, HttpStatus.OK );
    }

    @GetMapping( "/associations/api-client/update-association-status-for-id" )
    public ResponseEntity<Void> updateAssociationStatusForId() throws Exception {

        final String associationId = "1";
        final var status = StatusEnum.CONFIRMED;

        final var response =
        associationsEndpointTest.updateAssociationStatusForId( associationId, status )
                .getData();

        return new ResponseEntity<>( response, HttpStatus.OK );
    }

}
