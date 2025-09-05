package uk.gov.companieshouse.accounts.association.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
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

    public Mono<InvitationsList> daoToDto( final AssociationDao association, final int pageIndex, final int itemsPerPage){
        final var offset = (long) pageIndex * itemsPerPage;
        final var href = String.format(GET_INVITATIONS_FOR_ASSOCIATION_URI, association.getId());
        final var total = association.getInvitations().size();

        return Flux.fromIterable(association.getInvitations())
                .skip(offset)
                .take(itemsPerPage)
                .flatMapSequential(invitationDao -> Mono
                        .fromCallable(() -> invitationsMapper.daoToDto(invitationDao, association.getId()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .collectList()
                .map(list -> mapToInvitationsList(href, total, pageIndex, itemsPerPage).apply(list));
    }

    private Invitation mapToMostRecentInvitation( final AssociationDao association ){
        final var mostRecentInvitation = Stream.of( association.getInvitations() )
                .filter( invitations -> invitations.size() > 1 )
                .flatMap( List::stream )
                .max( Comparator.comparing( InvitationDao::getInvitedAt ) )
                .orElse( association.getInvitations().getFirst() );
        return invitationsMapper.daoToDto( mostRecentInvitation, association.getId() );
    }

    public Mono<InvitationsList> daoToDto(
            final List<AssociationDao> associationsWithActiveInvitations,
            final int pageIndex,
            final int itemsPerPage) {

        final long offset = (long) pageIndex * itemsPerPage;
        final int total = associationsWithActiveInvitations.size();

        return Flux.fromIterable(associationsWithActiveInvitations)
                .sort(Comparator.comparing(AssociationDao::getApprovalExpiryAt).reversed())
                .skip(offset)
                .take(itemsPerPage)
                .flatMapSequential(
                        assoc -> Mono.fromCallable(() -> mapToMostRecentInvitation(assoc)))
                .subscribeOn(Schedulers.boundedElastic())
                .collectList()
                .map(list -> mapToInvitationsList(
                        FETCH_ACTIVE_INVITATIONS_FOR_USER_URI,
                        total,
                        pageIndex,
                        itemsPerPage).apply(list));
    }

}
