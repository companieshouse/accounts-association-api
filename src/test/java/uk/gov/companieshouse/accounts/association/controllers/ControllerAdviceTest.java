package uk.gov.companieshouse.accounts.association.controllers;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.companieshouse.accounts.association.controller.UserCompanyAssociationStatusApi;
import uk.gov.companieshouse.accounts.association.enums.StatusEnum;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.UserInfo;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Tag("unit-test")
@WebMvcTest( UserCompanyAssociationStatusApi.class )
class ControllerAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsersService usersService;

    @MockBean
    private AssociationsService associationsService;

    @Test
    void testNotFoundRuntimeError() throws Exception {
        final var userEmail = "bruce.wayne@gotham.city";
        final var userId = "111";
        final var companyNumber = "111111";
        final var status = StatusEnum.CONFIRMED.getValue();

        Mockito.doThrow(new NotFoundRuntimeException( "association", String.format( "Could not find association where companyNumber is %s, and userId is %s.", companyNumber, userId ) ) )
                .when( associationsService ).getByUserIdAndCompanyNumber( any(), any() );

        Mockito.doReturn( Optional.of( new UserInfo().userId( userId ) ) )
                .when( usersService ).fetchUserInfo( any() );

        mockMvc.perform(put( "/associations/companies/{company_number}/users/{user_email}/{status}", companyNumber, userEmail, status )
                       .header("X-Request-Id", "theId") )
                       .andExpect(status().isNotFound() );
    }

    @Test
    void testBadRequestRuntimeError() throws Exception {
        final var userEmail = "bruce.wayne@gotham.city";
        final var companyNumber = "111111";
        final var status = StatusEnum.AWAITING_CONFIRMATION.getValue();

        mockMvc.perform(put( "/associations/companies/{company_number}/users/{user_email}/{status}", companyNumber, userEmail, status )
                        .header("X-Request-Id", "theId") )
                        .andExpect(status().isBadRequest() );
    }

    @Test
    void testConstraintViolationError() throws Exception {
        final var userEmail = "abc";
        final var companyNumber = "111111";
        final var status = StatusEnum.CONFIRMED.getValue();

        mockMvc.perform(put( "/associations/companies/{company_number}/users/{user_email}/{status}", companyNumber, userEmail, status )
                        .header("X-Request-Id", "theId") )
                        .andExpect(status().isBadRequest() );
    }

    @Test
    void testOnInternalServerError() throws Exception {
        final var userEmail = "bruce.wayne@gotham.city";
        final var companyNumber = "111111";
        final var status = StatusEnum.CONFIRMED.getValue();

        Mockito.doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"))
                .when( usersService ).fetchUserInfo( any() );

        mockMvc.perform(put( "/associations/companies/{company_number}/users/{user_email}/{status}", companyNumber, userEmail, status )
                        .header("X-Request-Id", "theId") )
                .andExpect(status().isInternalServerError());
    }

}
