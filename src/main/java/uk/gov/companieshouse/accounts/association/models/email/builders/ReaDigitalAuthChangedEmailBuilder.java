package uk.gov.companieshouse.accounts.association.models.email.builders;

import java.util.Objects;
import uk.gov.companieshouse.accounts.association.models.email.data.ReaDigitalAuthChangedEmailData;

public class ReaDigitalAuthChangedEmailBuilder extends EmailBuilder<ReaDigitalAuthChangedEmailBuilder, ReaDigitalAuthChangedEmailData> {

    private String companyNumber;

    public ReaDigitalAuthChangedEmailBuilder setCompanyNumber(final String companyNumber) {
        this.companyNumber = companyNumber;
        return this;
    }

    @Override
    protected ReaDigitalAuthChangedEmailBuilder self() {
        return this;
    }

    @Override
    public ReaDigitalAuthChangedEmailData build() {
        if (Objects.isNull(recipientEmail) || Objects.isNull(companyName) || Objects.isNull(companyNumber)) {
            throw new NullPointerException("recipientEmail, companyName and companyNumber cannot be null");
        }

        final var subject = String.format("Companies House: There’s been a change in who is digitally authorised to file for %s", companyName);

        final var emailData = new ReaDigitalAuthChangedEmailData();
        emailData.setTo(recipientEmail);
        emailData.setSubject(subject);
        emailData.setCompanyName(companyName);
        emailData.setCompanyNumber(companyNumber);

        return emailData;
    }
}
