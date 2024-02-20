package uk.gov.companieshouse.accounts.association.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.AccountsAssociationServiceApplication;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationsInterface;
import uk.gov.companieshouse.api.accounts.associations.model.*;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Objects;

@RestController
public class GetCurrentUserAssociationsController implements UserCompanyAssociationsInterface {
    private final AssociationsService associationsService;
    private static final Logger LOG = LoggerFactory.getLogger(AccountsAssociationServiceApplication.applicationNameSpace);

    public GetCurrentUserAssociationsController(AssociationsService associationsService) {
        this.associationsService = associationsService;
    }

    @Override
    public ResponseEntity<ResponseBodyPost> addAssociation(@NotNull String s, @Valid RequestBodyPost requestBodyPost) {
        return null;
    }

    @Override
    public ResponseEntity<Association> getAssociationForId(@NotNull String s, @Pattern(regexp = "^[a-zA-Z0-9]*$") String s1) {
        return null;
    }

    @Override
    public ResponseEntity<AssociationsList> getAssociations(final String xRequestId, final String ericIdentity, Boolean includeRemoved, Integer pageIndex, Integer itemsPerPage, String companyNumber) {
        LOG.debug(String.format("%s: Attempting to get for the details of user: %s", xRequestId, ericIdentity));

// Check for ericIdentity header
        if (Objects.isNull(ericIdentity) || ericIdentity.isEmpty()) {
            LOG.error(String.format("%s: No id is provided.", ericIdentity));
            throw new BadRequestRuntimeException("Please check the request and try again");
        }

// Check for x-request-id header
        if (Objects.isNull(xRequestId) || xRequestId.isEmpty()) {
            LOG.error(String.format("%s:  No id is provided.", xRequestId));
            throw new BadRequestRuntimeException("Please check the request and try again");
        }

// Check and set default values for pageIndex and itemsPerPage if they are null
        pageIndex = Objects.isNull(pageIndex) ? 0 : pageIndex;
        itemsPerPage = Objects.isNull(itemsPerPage) ? 15 : itemsPerPage;

// If pageIndex is not provided but itemsPerPage is provided, set pageIndex to default value 1
        if (Objects.isNull(pageIndex)) {
            if (itemsPerPage != null && !itemsPerPage.toString().isEmpty()) {
                pageIndex = 1;
            }
        }

// If itemsPerPage is provided but pageIndex is not, set itemsPerPage to default value 15
        if (Objects.isNull(itemsPerPage)) {
            if (pageIndex != null && !pageIndex.toString().isEmpty()) {
                itemsPerPage = 15;
            }
        }

        final var users = associationsService.findByUserIdAndCompanyNumberLike(ericIdentity, companyNumber, includeRemoved, pageIndex, itemsPerPage);
        return null;
    }

    @Override
    public ResponseEntity<Void> updateAssociationStatusForId(@NotNull String s, @Pattern(regexp = "^[a-zA-Z0-9]*$") String s1, @Valid RequestBodyPut requestBodyPut) {
        return null;
    }
}
