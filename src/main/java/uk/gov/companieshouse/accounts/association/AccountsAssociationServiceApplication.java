package uk.gov.companieshouse.accounts.association;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;

@SpringBootApplication
public class AccountsAssociationServiceApplication {

    final StaticPropertyUtil staticPropertyUtil;

    @Autowired
    public AccountsAssociationServiceApplication(final StaticPropertyUtil staticPropertyUtil) {
        this.staticPropertyUtil = staticPropertyUtil;
    }

    public static void main(String[] args) {
        SpringApplication.run(AccountsAssociationServiceApplication.class, args);
    }

}