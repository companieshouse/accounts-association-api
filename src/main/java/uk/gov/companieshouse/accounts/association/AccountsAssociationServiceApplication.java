package uk.gov.companieshouse.accounts.association;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AccountsAssociationServiceApplication {

    @Value("${spring.application.name}")
    public static String applicationNameSpace;

    public static void main(String[] args) {
        SpringApplication.run(AccountsAssociationServiceApplication.class, args);
    }

}