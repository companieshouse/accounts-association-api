package uk.gov.companieshouse.accounts.association.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.PreviousStatesDao;
import uk.gov.companieshouse.api.accounts.associations.model.Links;
import uk.gov.companieshouse.api.accounts.associations.model.PreviousState;
import uk.gov.companieshouse.api.accounts.associations.model.PreviousStatesList;

@Component
public class PreviousStatesCollectionMappers {

    private final PreviousStatesMapper previousStatesMapper;

    private static final String endpointUri = "/associations/%s/previous-states?page_index=%d&items_per_page=%d";

    public PreviousStatesCollectionMappers(final PreviousStatesMapper previousStatesMapper) {
        this.previousStatesMapper = previousStatesMapper;
    }

    private Function<List<PreviousState>, PreviousStatesList> mapToPreviousStatesList(final String associationId, final int totalResults, final int pageIndex, final int itemsPerPage){
        return items -> {
            final var totalPages = (int) Math.ceil((double) totalResults / itemsPerPage);
            return new PreviousStatesList()
                    .items(items)
                    .pageNumber(pageIndex)
                    .itemsPerPage(itemsPerPage)
                    .totalResults(totalResults)
                    .totalPages(totalPages)
                    .links(new Links()
                            .self(String.format(endpointUri, associationId, pageIndex, itemsPerPage))
                            .next(pageIndex + 1 < totalPages ? String.format(endpointUri, associationId, pageIndex + 1, itemsPerPage) : ""));
        };
    }

    public PreviousStatesList daoToDto(final AssociationDao association, final int pageIndex, final int itemsPerPage){
        return association.getPreviousStates()
                .stream()
                .sorted(Comparator.comparing(PreviousStatesDao::getChangedAt).reversed())
                .skip((long) pageIndex * itemsPerPage)
                .limit(itemsPerPage)
                .map(previousStatesMapper::daoToDto)
                .collect(Collectors.collectingAndThen(Collectors.toList(), mapToPreviousStatesList(association.getId(), association.getPreviousStates().size(), pageIndex, itemsPerPage)));
    }

}
