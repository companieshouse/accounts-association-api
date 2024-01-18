package uk.gov.companieshouse.accounts.association.service;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.rest.CompanyProfileMockEndpoint;
import uk.gov.companieshouse.api.accounts.associations.model.CompanyInfo;

@Service
public class CompaniesService {

    private final CompanyProfileMockEndpoint companyProfileMockEndpoint;

    public CompaniesService(CompanyProfileMockEndpoint companyProfileMockEndpoint) {
        this.companyProfileMockEndpoint = companyProfileMockEndpoint;
    }

    public List<CompanyInfo> fetchCompanies( List<String> companyNumbers ){
        return companyProfileMockEndpoint.fetchCompanies( companyNumbers );
    }

}
