package uk.gov.companieshouse.accounts.association.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import uk.gov.companieshouse.accounts.association.controller.AssociationsController;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;

@Tag("unit-test")
@WebMvcTest( AssociationsController.class )
class ControllerAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssociationsService associationsService;

    @MockBean
    InterceptorConfig interceptorConfig;

    @BeforeEach
    void setup() {
        Mockito.doNothing().when(interceptorConfig).addInterceptors( any() );
    }

    @Test
    void testNotFoundRuntimeError() throws Exception {
        Mockito.doThrow(new NotFoundRuntimeException("accounts-association-api", "No association found"))
                .when( associationsService ).getAssociationForId( any() );

        mockMvc.perform(get( "/associations/999" )
                        .header("X-Request-Id", "theId123") )
                .andExpect(status().isNotFound());
    }

    @Test
    void testBadRequestRuntimeError() throws Exception {
        mockMvc.perform( get( "/associations/" ).header("X-Request-Id", "theId123") ).andExpect(status().isBadRequest());
    }

    @Test
    void testConstraintViolationError() throws Exception {
        mockMvc.perform( get( "/associations/$$$" ).header("X-Request-Id", "theId123") ).andExpect(status().isBadRequest());
    }

    @Test
    void testOnInternalServerError() throws Exception {
        Mockito.doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"))
                .when( associationsService ).getAssociationForId( any() );

        mockMvc.perform(get( "/associations/111" )
                        .header("X-Request-Id", "theId123") )
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testOnInternalServerErrorRuntimeException() throws Exception {
        Mockito.doThrow(new InternalServerErrorRuntimeException("Internal Server Error"))
                .when( associationsService ).getAssociationForId( any() );

        mockMvc.perform(get( "/associations/" )
                        .header("X-Request-Id", "theId123") )
                .andExpect(status().isInternalServerError());
    }
}
