package uk.gov.companieshouse.accounts.association.mapper;

import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.PreviousStatesDao;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class PreviousStatesMapperTest {

    private final PreviousStatesMapper previousStatesMapper = new PreviousStatesMapperImpl();

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void daoToDtoWithNullDaoReturnsNull(){
        Assertions.assertNull( previousStatesMapper.daoToDto( null ) );
    }

    private static Stream<Arguments> daoToDtoMappingScenarios(){
        return testDataManager.fetchAssociationDaos( "MKAssociation003" )
                .getFirst()
                .getPreviousStates()
                .stream()
                .map( Arguments::of );
    }

    @ParameterizedTest
    @MethodSource( "daoToDtoMappingScenarios" )
    void daoToDtoCorrectlyMapsData( final PreviousStatesDao dao ){
        final var dto = previousStatesMapper.daoToDto( dao );
        Assertions.assertEquals( dto.getStatus().getValue(), dao.getStatus() );
        Assertions.assertEquals( dto.getChangedBy(), dao.getChangedBy() );
        Assertions.assertEquals( reduceTimestampResolution( dto.getChangedAt() ), localDateTimeToNormalisedString( dao.getChangedAt() ) );
    }

}
