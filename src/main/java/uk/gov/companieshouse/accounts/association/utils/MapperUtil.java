package uk.gov.companieshouse.accounts.association.utils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.mapper.RemoveInvitationsFromAssociationMapper;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.*;
import uk.gov.companieshouse.api.accounts.user.model.User;

@Component
public class MapperUtil {

    protected UsersService usersService;
    protected CompanyService companyService;
    private final RemoveInvitationsFromAssociationMapper removeInvitationsFromAssociationMapper;


    private static final String DEFAULT_DISPLAY_NAME = "Not provided";

    @Autowired
    public MapperUtil(final UsersService usersService, final CompanyService companyService, RemoveInvitationsFromAssociationMapper removeInvitationsFromAssociationMapper) {
        this.usersService = usersService;
        this.companyService = companyService;
        this.removeInvitationsFromAssociationMapper = removeInvitationsFromAssociationMapper;
    }

    public AssociationWithInvitations enrichAssociationWithUserDetails(final AssociationWithInvitations association) {
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

    public AssociationWithInvitations enrichInvitations(final AssociationWithInvitations association) {
        if (Objects.nonNull(association.getInvitations())) {
            List<Invitation> invitationsList = association.
                    getInvitations().stream()
                    .map(this::enrichInvitation).collect(Collectors.toList());

            association.setInvitations(invitationsList);
        }
        return association;
    }

    private Invitation enrichInvitation(Invitation invitation) {
        invitation.setInvitedBy(usersService.fetchUserDetails(invitation.getInvitedBy()).getEmail());
        return invitation;
    }

    public AssociationWithInvitations enrichAssociationWithCompanyName(final AssociationWithInvitations association) {
        final var companyProfile = companyService.fetchCompanyProfile(association.getCompanyNumber());
        association.setCompanyName(companyProfile.getCompanyName());
        return association;
    }

    public AssociationsList enrichWithMetadata(final Page<AssociationWithInvitations> page, final String endpointUrl) {


        AssociationsList list = new AssociationsList();
        final var pageIndex = page.getNumber();
        final var itemsPerPage = page.getSize();
        final var totalPages = page.getTotalPages();
        final var totalResults = page.getTotalElements();
        final var isLastPage = page.isLast();
        final var associations = "/associations";
        final var self = totalResults == 0 || pageIndex >= totalResults ? "" : String.format("%s%s?page_index=%d&items_per_page=%d", associations, endpointUrl, pageIndex, itemsPerPage);
        final var next = isLastPage ? "" : String.format("%s%s?page_index=%d&items_per_page=%d", associations, endpointUrl, pageIndex + 1, itemsPerPage);
        final var links = new AssociationsListLinks().self(self).next(next);


        List<Association> associationsWithoutInvitations = page.getContent()
                .stream()
                .map(removeInvitationsFromAssociationMapper::removeInvitationsFromAssociation).toList();
        list.setItems(associationsWithoutInvitations);
        list.links(links)
                .pageNumber(pageIndex)
                .itemsPerPage(itemsPerPage)
                .totalResults((int) totalResults)
                .totalPages(totalPages);

        return list;

    }

}
