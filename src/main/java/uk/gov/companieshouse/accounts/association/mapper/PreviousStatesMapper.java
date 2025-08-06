package uk.gov.companieshouse.accounts.association.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.companieshouse.accounts.association.models.PreviousStates;
import uk.gov.companieshouse.api.accounts.associations.model.PreviousState;

@Mapper( componentModel = "spring" )
public abstract class PreviousStatesMapper {

    @Mapping( target = "status", expression = "java( PreviousState.StatusEnum.fromValue( previousStates.getStatus() ) )" )
    public abstract PreviousState daoToDto( final PreviousStates previousStates);

}
