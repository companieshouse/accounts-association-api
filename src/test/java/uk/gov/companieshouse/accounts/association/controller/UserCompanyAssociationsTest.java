package uk.gov.companieshouse.accounts.association.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class UserCompanyAssociationsTest {

    @Mock
    private AssociationsService associationsService;

    @Mock
    private UsersService usersService;

    @Mock
    private CompanyService companyService;

    @InjectMocks
    UserCompanyAssociations userCompanyAssociations;

    @BeforeEach
    public void setup(){
        userCompanyAssociations=new UserCompanyAssociations(usersService,associationsService,companyService);
    }
    @Test
    void fetchAssociationsByTestShouldThrowBadRequestWhenUserNotProvided() {


    }
}