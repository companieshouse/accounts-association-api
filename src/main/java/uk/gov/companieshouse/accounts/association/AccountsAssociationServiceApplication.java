package uk.gov.companieshouse.accounts.association;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AccountsAssociationServiceApplication {

    public static final String APPLICATION_NAME_SPACE = "accounts-association-api";

    public static void main(String[] args) {
        SpringApplication.run(AccountsAssociationServiceApplication.class, args);
    }

}