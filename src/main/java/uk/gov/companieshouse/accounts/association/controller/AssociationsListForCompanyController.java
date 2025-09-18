package uk.gov.companieshouse.accounts.association.controller;

import static org.springframework.http.HttpStatus.OK;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_READ_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.ASSOCIATION_NOT_FOUND;
import static uk.gov.companieshouse.accounts.association.models.Constants.ONLY_ONE_MUST_BE_PRESENT;
import static uk.gov.companieshouse.accounts.association.models.Constants.PAGINATION_IS_MALFORMED;
import static uk.gov.companieshouse.accounts.association.models.Constants.REQUESTING_USER_NOT_PERMITTED;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.fetchAllStatusesWithout;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.hasAdminPrivilege;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isOAuth2Request;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.ForbiddenRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsTransactionService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.api.AssociationDataForCompanyInterface;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.FetchRequestBodyPost;

@RestController
public class AssociationsListForCompanyController implements AssociationDataForCompanyInterface {

    private final CompanyService companyService;
    private final AssociationsTransactionService associationsTransactionService;
    private final UsersService usersService;

    public AssociationsListForCompanyController(final CompanyService companyService, final AssociationsTransactionService associationsTransactionService, final UsersService usersService) {
        this.companyService = companyService;
        this.associationsTransactionService = associationsTransactionService;
        this.usersService = usersService;
    }

    @Override
    public ResponseEntity<AssociationsList> getAssociationsForCompany(final String companyNumber, final Boolean includeRemoved, final Integer pageIndex, final Integer itemsPerPage) {
        LOGGER.infoContext(getXRequestId(), String.format("Received request with company_number=%s, includeRemoved=%b, itemsPerPage=%d, pageIndex=%d.", companyNumber, includeRemoved, itemsPerPage, pageIndex),null);

        if (pageIndex < 0 || itemsPerPage <= 0){
            throw new BadRequestRuntimeException(PAGINATION_IS_MALFORMED);
        }

        if (!associationsTransactionService.confirmedAssociationExists(companyNumber, getEricIdentity()) && !hasAdminPrivilege(ADMIN_READ_PERMISSION) && isOAuth2Request()){
            throw new ForbiddenRuntimeException(REQUESTING_USER_NOT_PERMITTED);
        }

        final var companyProfile = companyService.fetchCompanyProfile(companyNumber);
        final var statuses = includeRemoved ? fetchAllStatusesWithout(Set.of()) : fetchAllStatusesWithout(Set.of(StatusEnum.REMOVED));
        final var associationsList = associationsTransactionService.fetchUnexpiredAssociationsForCompanyAndStatuses(companyProfile, statuses, null, null, pageIndex, itemsPerPage);

        return new ResponseEntity<>(associationsList, OK);
    }

    @Override
    public ResponseEntity<Association> getAssociationsForCompanyUserAndStatus(final String companyNumber, final FetchRequestBodyPost requestBodyPost) {
        final var xRequestId = getXRequestId();
        LOGGER.infoContext(xRequestId, String.format("Received request with company_number=%s and user_id=%s. user_email was provided: %b.", companyNumber, requestBodyPost.getUserId(), Objects.nonNull(requestBodyPost.getUserEmail())),null);

        final var userId = requestBodyPost.getUserId();
        final var userEmail = requestBodyPost.getUserEmail();
        final var statuses =  Optional.ofNullable(requestBodyPost.getStatus())
                .filter(statusEnums -> !statusEnums.isEmpty())
                .orElse(List.of(FetchRequestBodyPost.StatusEnum.CONFIRMED))
                .stream()
                .map(FetchRequestBodyPost.StatusEnum::getValue)
                .map(StatusEnum::fromValue)
                .collect(Collectors.toSet());

        if (Objects.nonNull(userId) == Objects.nonNull(userEmail)){
            throw new BadRequestRuntimeException(ONLY_ONE_MUST_BE_PRESENT);
        }

        final var companyProfile = companyService.fetchCompanyProfile(companyNumber);
        final var user = usersService.retrieveUserDetails(userId, userEmail, xRequestId);
        final var targetUserEmail =  Objects.nonNull(user) ? user.getEmail() : userEmail;

        return associationsTransactionService.fetchUnexpiredAssociationsForCompanyUserAndStatuses(companyProfile, statuses, user, targetUserEmail)
                .map(association -> new ResponseEntity<>(association, OK))
                .orElseThrow(() -> new NotFoundRuntimeException(ASSOCIATION_NOT_FOUND));

    }

}