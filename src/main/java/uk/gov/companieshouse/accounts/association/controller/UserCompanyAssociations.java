package uk.gov.companieshouse.accounts.association.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.AccountsAssociationServiceApplication;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationsInterface;
import uk.gov.companieshouse.api.accounts.associations.model.*;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;
import java.util.Optional;

@RestController
public class UserCompanyAssociations implements UserCompanyAssociationsInterface {

    private static final Logger LOG = LoggerFactory.getLogger(AccountsAssociationServiceApplication.applicationNameSpace);


    private final UsersService usersService;


    private final AssociationsService associationsService;


    @Autowired
    public UserCompanyAssociations(UsersService usersService, AssociationsService associationsService) {
        this.usersService = usersService;
        this.associationsService = associationsService;
    }

    @Override
    public ResponseEntity<ResponseBodyPost> addAssociation(@NotNull String s, @NotNull String s1, @Valid RequestBodyPost requestBodyPost) {
        return null;
    }

    @Override
    public ResponseEntity<AssociationsList> fetchAssociationsBy(
            final String xRequestId,
            final String ericIdentity,
            final List<String> status,
            final Integer pageIndex,
            final Integer itemsPerPage,
            final String companyNumber) {

        LOG.infoContext(xRequestId, "Trying to fetch associations data for user in session :".concat(ericIdentity), null);

        final var user = usersService.fetchUserDetails(ericIdentity);
        Optional.ofNullable(user).orElseThrow(() -> new BadRequestRuntimeException("Eric id is not valid"));//NOSONAR or else throw will be caught by controller advice
        final AssociationsList associationsList = associationsService
                .fetchAssociationsForUserStatusAndCompany(
                        user, status, pageIndex, itemsPerPage, companyNumber);

        return new ResponseEntity<>(associationsList, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Association> getAssociationForId(@NotNull String s, @Pattern(regexp = "^[a-zA-Z0-9]*$") String s1) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseBodyPost> inviteUser(@NotNull String s, @NotNull String s1, @Valid InvitationRequestBodyPost invitationRequestBodyPost) {
        return null;
    }


    @Override
    public ResponseEntity<Void> updateAssociationStatusForId(@NotNull String s, @Pattern(regexp = "^[a-zA-Z0-9]*$") String s1, @Valid RequestBodyPut requestBodyPut) {
        return null;
    }
}
