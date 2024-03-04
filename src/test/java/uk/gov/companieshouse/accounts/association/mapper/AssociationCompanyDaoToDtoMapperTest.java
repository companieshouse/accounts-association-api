package uk.gov.companieshouse.accounts.association.mapper;

import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.model.company.CompanyProfileApi;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AssociationCompanyDaoToDtoMapperTest {

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private AssociationCompanyDaoToDtoMapper associationCompanyDaoToDtoMapper = Mockito.mock(AssociationCompanyDaoToDtoMapper.class, Mockito.CALLS_REAL_METHODS);

    @Test
    void enrichAssociationWithCompanyProfileWithNullInputThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> associationCompanyDaoToDtoMapper.enrichAssociationWithCompanyProfile( null ) );
    }

    @Test
    void enrichAssociationWithCompanyProfileSetsCompanyName(){
        final var association = new Association().companyNumber( "111111" );

        final var company = new CompanyProfileApi();
        company.setCompanyNumber( "111111" );
        company.setCompanyName( "Hogwarts" );
        Mockito.doReturn( company ).when( companyService ).fetchCompanyProfile( any() );

        associationCompanyDaoToDtoMapper.enrichAssociationWithCompanyProfile( association );

        Assertions.assertEquals( "Hogwarts", association.getCompanyName() );
    }

    @Test
    void daoToDtoWithNullInputReturnsNull(){
        Assertions.assertNull( associationCompanyDaoToDtoMapper.daoToDto( null ) );
    }

    @Test
    void daoToDtoWithOnlyMandatoryFieldsSuccessfullyPerformsMapping(){

    }

    // TODO: test daoToDto


}
