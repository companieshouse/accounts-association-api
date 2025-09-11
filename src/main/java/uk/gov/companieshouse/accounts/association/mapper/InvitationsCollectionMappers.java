package uk.gov.companieshouse.accounts.association.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.Links;

@Component
public class InvitationsCollectionMappers {

    private final InvitationMapper invitationsMapper;

    private static final String FETCH_ACTIVE_INVITATIONS_FOR_USER_URI = "/associations/invitations";
    private static final String GET_INVITATIONS_FOR_ASSOCIATION_URI = "/associations/%s/invitations";

    public InvitationsCollectionMappers( final InvitationMapper invitationsMapper ) {
        this.invitationsMapper = invitationsMapper;
    }

    private Function<List<Invitation>, InvitationsList> mapToInvitationsList( final String basePath, final int totalResults, final int pageIndex, final int itemsPerPage ){
        return items -> {
            final var totalPages = (int) Math.ceil( (double) totalResults / itemsPerPage );
            return new InvitationsList()
                    .items( items )
                    .pageNumber( pageIndex )
                    .itemsPerPage( itemsPerPage )
                    .totalResults( totalResults )
                    .totalPages( totalPages )
                    .links( new Links()
                            .self( String.format( basePath + "?page_index=%d&items_per_page=%d", pageIndex, itemsPerPage ) )
                            .next( pageIndex + 1 < totalPages ? String.format( basePath + "?page_index=%d&items_per_page=%d", pageIndex + 1, itemsPerPage ) : "" ) );
        };
    }

    public InvitationsList daoToDto( final AssociationDao association, final int pageIndex, final int itemsPerPage ){
        return association.getInvitations()
                .stream()
                .skip((long) pageIndex * itemsPerPage )
                .limit( itemsPerPage )
                .map( invitationDao -> invitationsMapper.daoToDto( invitationDao, association.getId() ) )
                .collect( Collectors.collectingAndThen( Collectors.toList(), mapToInvitationsList( String.format( GET_INVITATIONS_FOR_ASSOCIATION_URI, association.getId() ), association.getInvitations().size(), pageIndex, itemsPerPage ) ) );
    }

    private Invitation mapToMostRecentInvitation( final AssociationDao association ){
        final var mostRecentInvitation = association.getInvitations()
                .stream()
                .max( Comparator.comparing( InvitationDao::getInvitedAt ) ).orElseThrow(NullPointerException::new);
        return invitationsMapper.daoToDto(mostRecentInvitation, association.getId() );
    }

    public InvitationsList daoToDto( final List<AssociationDao> associationsWithActiveInvitations, final int pageIndex, final int itemsPerPage ){
        return associationsWithActiveInvitations.stream()
                //.sorted( Comparator.comparing( AssociationDao::getApprovalExpiryAt ).reversed() )
                .skip((long) pageIndex * itemsPerPage )
                .limit( itemsPerPage )
                .map( this::mapToMostRecentInvitation )
                .collect( Collectors.collectingAndThen( Collectors.toList(), mapToInvitationsList( FETCH_ACTIVE_INVITATIONS_FOR_USER_URI, associationsWithActiveInvitations.size(), pageIndex, itemsPerPage ) ) );
    }

}
