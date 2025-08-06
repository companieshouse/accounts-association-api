package uk.gov.companieshouse.accounts.association.utils;

import static uk.gov.companieshouse.accounts.association.utils.UserUtil.isRequestingUser;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.mapToDisplayValue;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.models.context.RequestContext;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData.RequestContextDataBuilder;
import uk.gov.companieshouse.api.accounts.user.model.User;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class UserUtilTest {

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static Stream<Arguments> mapToDisplayValueScenarios(){
        return Stream.of(
                Arguments.of( null, null, null ),
                Arguments.of( null, "Not provided", "Not provided" ),
                Arguments.of( testDataManager.fetchUserDtos( "MKUser001" ).getFirst(), "Not provided", "Mario" ),
                Arguments.of( testDataManager.fetchUserDtos( "222" ).getFirst(), "Not provided", "the.joker@gotham.city" ) );
    }

    @ParameterizedTest
    @MethodSource( "mapToDisplayValueScenarios" )
    void mapToDisplayValueCorrectlyDerivesDisplayValue( final User user, final String defaultValue, final String expectedOutcome ){
        Assertions.assertEquals( expectedOutcome, mapToDisplayValue( user, defaultValue ) );
    }

    @Test
    void isRequestingUserWithNullInputsReturnsFalse(){
        Assertions.assertFalse( isRequestingUser( null ) );
        Assertions.assertFalse( isRequestingUser( new Association() ) );
    }

    private static Stream<Arguments> isRequestingUserCorrectlyClassifiesTargetAssociationScenarios(){
        return Stream.of(
                Arguments.of( new Association().userId( "111" ), true ),
                Arguments.of( new Association().userId( "222" ), false ),
                Arguments.of( new Association().userEmail( "bruce.wayne@gotham.city" ), true ),
                Arguments.of( new Association().userEmail( "joker@gotham.city" ), false )
        );
    }

    @ParameterizedTest
    @MethodSource( "isRequestingUserCorrectlyClassifiesTargetAssociationScenarios" )
    void isRequestingUserCorrectlyClassifiesTargetAssociation(final Association targetAssociation, final boolean expectedOutcome ){
        final var requestingUser = testDataManager.fetchUserDtos( "111" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( ERIC_IDENTITY, requestingUser.getUserId() );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setEricIdentity( request ).setUser( requestingUser ).build() );

        Assertions.assertEquals( expectedOutcome, isRequestingUser( targetAssociation ) );
    }

}
