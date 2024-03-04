package uk.gov.companieshouse.accounts.association.mapper.abstracts;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.service.ApiClientService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AssociationDaoToDtoMapperTest {

    @Mock
    ApiClientService apiClientService;

    AssociationDaoToDtoMapper associationDaoToDtoMapper = new AssociationDaoToDtoMapperImpl();

    private static final String DEFAULT_KIND = "association";

    @Test
    void enrichAssociationWithDerivedValuesWithNullInputThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> associationDaoToDtoMapper.enrichAssociationWithDerivedValues( null ) );
    }

    @Test
    void enrichAssociationWithDerivedValuesWithoutKindSetsLinksAndDefaultKind(){
        final var association = new Association().id( "111111" );
        associationDaoToDtoMapper.enrichAssociationWithDerivedValues( association );

        Assertions.assertEquals( "/111111", association.getLinks().getSelf() );
        Assertions.assertEquals( DEFAULT_KIND, association.getKind() );
    }

    @Test
    void enrichAssociationWithDerivedValuesSetsLinksAndKind(){
        final var association =
        new Association().id( "111111" )
                         .kind("Another kind");

        associationDaoToDtoMapper.enrichAssociationWithDerivedValues( association );

        Assertions.assertEquals( "/111111", association.getLinks().getSelf() );
        Assertions.assertEquals( "Another kind", association.getKind() );
    }

}
