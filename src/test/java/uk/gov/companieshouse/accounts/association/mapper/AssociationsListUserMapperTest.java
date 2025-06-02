package uk.gov.companieshouse.accounts.association.mapper;

import static org.mockito.ArgumentMatchers.any;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;

@Tag( "unit-test" )
@ExtendWith( MockitoExtension.class )
class AssociationsListUserMapperTest {

    @Mock
    private UsersService usersService;

    @Mock
    private CompanyService companyService;

    private AssociationsListUserMapper associationsListUserMapper;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup(){
        associationsListUserMapper = new AssociationsListUserMapperImpl();
        associationsListUserMapper.usersService = usersService;
        associationsListUserMapper.companyService = companyService;
    }





    @Test
    void test(){
        final var associationDaos = testDataManager.fetchAssociationDaos( "18", "19" );
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var companies = testDataManager.fetchCompanyDetailsDtos( "333333", "444444" );

        final var content = new PageImpl<>( associationDaos, PageRequest.of( 0, 15 ), associationDaos.size() );

        Mockito.doReturn( Map.of( "333333", companies.getFirst(), "444444", companies.getLast() ) ).when( companyService ).fetchCompanyProfiles( any( Stream.class ) );

        associationsListUserMapper.daoToDto( content, user );

    }




    // daoToDto( final Page<AssociationDao> associationsList, final User user )
    // - associationsList is null
    // - associationsList is not null
    // - associationsList is empty
    // - associationsList contains *
    // - user is null
    // - user is not null
    // - is last page
    // - is not last page



}
