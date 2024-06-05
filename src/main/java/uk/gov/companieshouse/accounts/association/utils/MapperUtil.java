package uk.gov.companieshouse.accounts.association.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;
import uk.gov.companieshouse.api.accounts.associations.model.Links;
import uk.gov.companieshouse.api.accounts.user.model.User;

import java.util.Objects;
import java.util.Optional;

@Component
public class MapperUtil {

    protected UsersService usersService;

    protected CompanyService companyService;


    private static final String DEFAULT_DISPLAY_NAME = "Not provided";

    @Autowired
    public MapperUtil(final UsersService usersService, final CompanyService companyService) {
        this.usersService = usersService;
        this.companyService = companyService;
    }

    public Association enrichAssociationWithUserDetails(final Association association) {
        var userEmail = "";
        var displayName = "";
        if (Objects.nonNull(association.getUserId())) {
            final var userId = association.getUserId();

            final var userDetails = usersService.fetchUserDetails(userId);
            userEmail = userDetails.getEmail();

            displayName =
                    Optional.of(userDetails)
                            .map(User::getDisplayName)
                            .orElse(DEFAULT_DISPLAY_NAME);
        } else {
            displayName = DEFAULT_DISPLAY_NAME;
            userEmail = association.getUserEmail();
        }
        association.setUserEmail(userEmail);
        association.setDisplayName(displayName);

        return association;
    }

    public Invitation enrichInvitation(Invitation invitation) {
        invitation.setInvitedBy(usersService.fetchUserDetails(invitation.getInvitedBy()).getEmail());
        return invitation;
    }

    public Association enrichAssociationWithCompanyName(final Association association) {
        final var companyProfile = companyService.fetchCompanyProfile(association.getCompanyNumber());
        association.setCompanyName(companyProfile.getCompanyName());
        return association;
    }

    public AssociationsList enrichWithMetadata(final Page<Association> page, final String endpointUrl) {
        AssociationsList list = new AssociationsList();
        final var pageIndex = page.getNumber();
        final var itemsPerPage = page.getSize();
        final var totalPages = page.getTotalPages();
        final var totalResults = page.getTotalElements();
        final var isLastPage = page.isLast();
        final var associations = "/associations";
        final var self = String.format("%s%s?page_index=%d&items_per_page=%d", associations, endpointUrl, pageIndex, itemsPerPage);
        final var next = isLastPage ? "" : String.format("%s%s?page_index=%d&items_per_page=%d", associations, endpointUrl, pageIndex + 1, itemsPerPage);
        final var links = new Links().self(self).next(next);

        list.setItems(page.getContent());
        list.links(links)
                .pageNumber(pageIndex)
                .itemsPerPage(itemsPerPage)
                .totalResults((int) totalResults)
                .totalPages(totalPages);

        return list;

    }

}
