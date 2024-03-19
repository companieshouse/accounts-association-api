package uk.gov.companieshouse.accounts.association.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.web.client.HttpServerErrorException;
import uk.gov.companieshouse.accounts.association.configuration.InterceptorConfig;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;

import javax.validation.ConstraintViolationException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit-test")
@WebMvcTest( UserCompanyAssociations.class )
class ControllerAdviceTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AssociationsService associationsService;
    @MockBean
    private UsersService usersService;
    @MockBean
    InterceptorConfig interceptorConfig;

    @BeforeEach
    void setup() {
        Mockito.doNothing().when(interceptorConfig).addInterceptors(any());
    }


    @Test
    void testNotFoundRuntimeError() throws Exception {
        Mockito.doThrow(new NotFoundRuntimeException("accounts-association-api", "Couldn't find association"))
                .when(usersService).fetchUserDetails(any());

        mockMvc.perform(get("/associations")
                        .header("X-Request-Id", "theId123").header("ERIC-Identity", "111"))
                .andExpect(status().isNotFound());

    }

    @Test
    void testBadRequestRuntimeError() throws Exception {

        mockMvc.perform(get( "/associations" )
                        .header("X-Request-Id", "theId123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testConstraintViolationError() throws Exception {

        mockMvc.perform(get( "/associations?company_number=&&" )
                        .header("X-Request-Id", "theId123").header("ERIC-Identity", "111"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void testOnInternalServerError() throws Exception {
        Mockito.doThrow(new NullPointerException("Couldn't find association"))
                .when(usersService).fetchUserDetails(any());

        mockMvc.perform(get("/associations?company_number=123445")
                        .header("X-Request-Id", "theId123").header("ERIC-Identity", "111"))
                .andExpect(status().isInternalServerError());
    }
    @Test
    void testOnInternalServerErrorRuntimeException() throws Exception {
        Mockito.doThrow(new InternalServerErrorRuntimeException("Couldn't find association"))
                .when(usersService).fetchUserDetails(any());

        mockMvc.perform(get("/associations")
                        .header("X-Request-Id", "theId123").header("ERIC-Identity", "111"))
                .andExpect(status().isInternalServerError());
    }
}

