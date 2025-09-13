package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.client.CompanyClient;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.company.CompanyDetails;

@Service
public class CompanyService {

    private static final String BLANK_COMPANY_NUMBER = "Company number cannot be blank";
    private final CompanyClient companyClient;

    @Autowired
    public CompanyService(final CompanyClient companyClient) {
        this.companyClient = companyClient;
    }

    public Map<String, CompanyDetails> fetchCompanyProfiles(final Stream<AssociationDao> associations) {
        final var xRequestId = getXRequestId();
        final var companyDetailsMap = new ConcurrentHashMap<String, CompanyDetails>();

        associations.parallel()
                .map(AssociationDao::getCompanyNumber)
                .forEach(companyNumber -> {
                    final var companyDetails = fetchCompanyProfile(companyNumber, xRequestId);
                    companyDetailsMap.put(companyNumber, companyDetails);
                });

        return companyDetailsMap;
    }

    public CompanyDetails fetchCompanyProfile(final String companyNumber) {
        return fetchCompanyProfile(companyNumber, getXRequestId());
    }

    public CompanyDetails fetchCompanyProfile(final String companyNumber, String xRequestId) {
        xRequestId = xRequestId != null ? xRequestId : getXRequestId();
        if (StringUtils.isBlank(companyNumber)) {
            NotFoundRuntimeException exception = new NotFoundRuntimeException(BLANK_COMPANY_NUMBER, new Exception(BLANK_COMPANY_NUMBER));
            LOGGER.errorContext(xRequestId, BLANK_COMPANY_NUMBER, exception, null);
            throw exception;
        }
        return companyClient.requestCompanyProfile(companyNumber, xRequestId);
    }
}



