package uk.gov.companieshouse.accounts.association.rest;

import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.api.accounts.associations.model.CompanyInfo;
import uk.gov.companieshouse.api.accounts.associations.model.OfficeAddress;

@Repository
public class CompanyProfileMockEndpoint {

    private final HashMap<String, Supplier<CompanyInfo>> mockCompanies = new HashMap<>();

    public CompanyProfileMockEndpoint(){

        final Supplier<CompanyInfo> wayneEnterpriseSupplier = () -> {
            final var wayenEnterprisesCompanyInfo = new CompanyInfo();

            final var wayneEnterprisesOfficeAddress = new OfficeAddress()
                    .addressLine1( "10 Wayne Tower" )
                    .addressLine2( "New York" )
                    .locality( "Gotham City" )
                    .country( "USA" )
                    .postalCode( "GOTH AM1" )
                    .poBox( "123 GTM" )
                    .careOf( "Bruce Wayne" );

            return wayenEnterprisesCompanyInfo.companyName( "Wayne Enterprises" )
                    .companyNumber( "111111" )
                    .companyStatus( "Created" )
                    .dateOfCreation( "1992-05-01T10:30:00.000000Z" )
                    .dateOfCessation( "1992-05-02T10:30:00.000000Z" )
                    .dateOfDissolution( "1992-05-03T10:30:00.000000Z" )
                    .companyType( "Ltd company" )
                    .registeredAddress(wayneEnterprisesOfficeAddress);
        };

        final Supplier<CompanyInfo> SpringfieldNuclearPowerPlantSupplier = () -> {
            final var SpringfieldNuclearPowerPlantCompanyInfo = new CompanyInfo();

            final var SpringfieldNuclearPowerPlantOfficeAddress = new OfficeAddress()
                    .addressLine1( "742 Evergreen Terrace" )
                    .addressLine2( "North Carolina" )
                    .locality( "Springfield" )
                    .country( "USA" )
                    .postalCode( "SP1 FLD" )
                    .poBox( "SPR 123" )
                    .careOf( "Mr Burns" );

            return SpringfieldNuclearPowerPlantCompanyInfo.companyName( "Springfield Nuclear Power Plant" )
                    .companyNumber( "222222" )
                    .companyStatus( "Created" )
                    .dateOfCreation( "1980-08-21T10:30:00.000000Z" )
                    .dateOfCessation( "1980-08-22T10:30:00.000000Z" )
                    .dateOfDissolution( "1980-08-23T10:30:00.000000Z" )
                    .companyType( "Ltd company" )
                    .registeredAddress(SpringfieldNuclearPowerPlantOfficeAddress);
        };

        final Supplier<CompanyInfo> queenVictoriaPubSupplier = () -> {
            final var queenVictoriaPubCompanyInfo = new CompanyInfo();

            final var queenVictoriaPubOfficeAddress = new OfficeAddress()
                    .addressLine1( "46 Albert Square" )
                    .addressLine2( "Walford" )
                    .locality( "London" )
                    .country( "England" )
                    .postalCode( "E20" )
                    .poBox( "EST NDR" )
                    .careOf( "Alfie Moon" );

            return queenVictoriaPubCompanyInfo.companyName( "Queen Victoria Pub" )
                    .companyNumber( "333333" )
                    .companyStatus( "Dissolved" )
                    .dateOfCreation( "1860-05-01T10:30:00.000000Z" )
                    .dateOfCessation( "1860-05-02T10:30:00.000000Z" )
                    .dateOfDissolution( "1860-05-03T10:30:00.000000Z" )
                    .companyType( "Ltd company" )
                    .registeredAddress(queenVictoriaPubOfficeAddress);
        };

        mockCompanies.put("111111", wayneEnterpriseSupplier);
        mockCompanies.put("222222", SpringfieldNuclearPowerPlantSupplier);
        mockCompanies.put("333333", queenVictoriaPubSupplier);
    }

    public List<CompanyInfo> fetchCompanies( List<String> companyNumbers ){
        return companyNumbers.stream()
                             .filter( mockCompanies::containsKey )
                             .map( mockCompanies::get )
                             .map( Supplier::get )
                             .toList();
    }

}
