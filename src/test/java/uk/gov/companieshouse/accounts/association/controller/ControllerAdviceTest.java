package uk.gov.companieshouse.accounts.association.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import uk.gov.companieshouse.accounts.association.configuration.InterceptorConfig;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit-test")
@WebMvcTest( UserCompanyAssociations.class )
class ControllerAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsersService usersService;
    @MockBean
    private AssociationsService associationsService;

    @MockBean
    private CompanyService companyService;
    @MockBean
    private UserCompanyAssociations userCompanyAssociations;

    @MockBean
    InterceptorConfig interceptorConfig;

    @BeforeEach
    void setup() {
        Mockito.doNothing().when(interceptorConfig).addInterceptors(any());
    }

    @Test
    void testBadRequestRuntimeError() throws Exception {
        mockMvc.perform( get( "/associations" ).header("X-Request-Id", "theId123") ).andExpect(status().isBadRequest());
    }

    @Test
    void testConstraintViolationError() throws Exception {
        mockMvc.perform( get( "/associations?status=done" ).header("X-Request-Id", "theId123")).andExpect(status().isBadRequest());
    }

    @Test
    void testBadRequestWhenStatusIncorrect() throws Exception {
        mockMvc.perform( get( "/associations?status=" ).header("X-Request-Id", "theId123")).andExpect(status().isBadRequest());
    }

    @Test
    void testThrowNullPointerExceptionWhenStatusEmpty() throws Exception {
        Mockito.doThrow(NullPointerException.class)
                .when(associationsService).fetchAssociationsForUserStatusAndCompany(any(), any(), eq(1), eq(1), eq("1234"));
        try {
            mockMvc.perform(get("/associations?status=")
                            .header("X-Request-Id", "theId123").header("ERIC-Identity", "111"))
                    .andExpect(status().isOk());
        } catch (NullPointerException e) {

            e.printStackTrace();
        }
    }

    @Test
    void testThrowNullPointerExceptionWhenStatusMissing() throws Exception {
        Mockito.doThrow(NullPointerException.class)
                .when(associationsService).fetchAssociationsForUserStatusAndCompany(any(), any(), eq(1), eq(1), eq("1234"));
        try {
            mockMvc.perform(get("/associations?items_per_page=15&page_index=1&company_number=123456" )
                            .header("X-Request-Id", "theId123").header("ERIC-Identity", "111"))
                    .andExpect(status().isOk());
        } catch (NullPointerException e) {

            e.printStackTrace();
        }
    }

    @Test
    void testThrowNullPointerExceptionWhenEricHeaderMissing() throws Exception {
            mockMvc.perform(get("/associations?items_per_page=15&page_index=1&company_number=123456" )
                            .header("X-Request-Id", "theId123"))
                    .andExpect(status().isBadRequest());
    }

    @Test
    void testStatusOKWhenCorrectValues() throws Exception {
        mockMvc.perform(get( "/associations?items_per_page=15&page_index=1&status=confirmed&company_number=123456" )
                        .header("X-Request-Id", "theId123") .header("ERIC-Identity", "111"))
                .andExpect(status().isOk());
    }
}

