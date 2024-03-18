package uk.gov.companieshouse.accounts.association.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.user.model.User;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserCompanyAssociations.class)
@Tag("unit-test")
class UserCompanyAssociationsTest {
    @Autowired
    public MockMvc mockMvc;


    @MockBean
    private UsersService usersService;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private AssociationsService associationsService;

    @BeforeEach
    public void setup() {
    }

    @Test
    void fetchAssociationsByTestShouldThrow401ErrorRequestWhenEricIdNotProvided() throws Exception {
        var response = mockMvc.perform(get("/associations").header("X-Request-Id", "theId")).andReturn();
        assertEquals(401, response.getResponse().getStatus());
    }

    @Test
    void fetchAssociationsByTestShouldThrow405ErrorRequestWhenPatchApplied() throws Exception {
        var response = mockMvc.perform(patch("/associations").header("X-Request-Id", "theId")).andReturn();
        assertEquals(405, response.getResponse().getStatus());
    }

    @Test
    void fetchAssociationsByTestShouldThrow400ErrorWhenRequestIdNotProvided() throws Exception {
        var response = mockMvc.perform(get("/associations").header("Eric-identity", "abcd12345")
                .header("Eric-identity", "abcd12345")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Roles", "*")).andExpect(status().isBadRequest()).andReturn();
        assertEquals("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Required header 'X-Request-Id' is not present.\",\"instance\":\"/associations\"}",
                response.getResponse().getContentAsString());
    }


    @Test
    void fetchAssociationsByTestShouldThrowBadRequestIfUserIdFromEricNotFound() throws Exception {
        var response = mockMvc.perform(get("/associations").header("Eric-identity", "abcd12345")
                .header("X-Request-Id", "theId")
                .header("Eric-identity", "abcd12345")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Roles", "*")).andExpect(status().isBadRequest()).andReturn();
        assertEquals("{\"errors\":[{\"error\":\"Eric id is not valid\",\"location\":\"accounts_association_api\",\"location_type\":\"request-body\",\"type\":\"ch:validation\"}]}",
                response.getResponse().getContentAsString());

    }

    @Test
    void fetchAssociationsByTestShouldReturnEmptyDataWhenNoAssociationsFoundForEricIdentity() throws Exception {
        when(usersService.fetchUserDetails("abcd12345")).thenReturn(new User("abc", "abc@abc.com").userId("abcd12345"));
        var response = mockMvc.perform(get("/associations")
                .header("Eric-identity", "abcd12345")
                .header("X-Request-Id", "theId")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Roles", "*")).andExpect(status().is2xxSuccessful()).andReturn();
        assertEquals(0, response.getResponse().getContentLength());
    }

    @Test
    void fetchAssociationsByTestShouldReturnDataWhenAssociationsFoundForEricIdentity() throws Exception {
        User user = new User("abc", "abc@abc.com").userId("abcd12345");
        AssociationsList associationsList = new AssociationsList().itemsPerPage(15).pageNumber(0).totalPages(1).totalResults(1);
        when(usersService.fetchUserDetails("abcd12345")).thenReturn(user);
        when(associationsService
                .fetchAssociationsForUserStatusAndCompany(user, Collections.singletonList("confirmed"), 0, 15, "123"))
                .thenReturn(associationsList);
        var response = mockMvc.perform(get("/associations?page_index=0&items_per_page=15&company_number=123")
                .header("Eric-identity", "abcd12345")
                .header("X-Request-Id", "theId")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Roles", "*")).andReturn();

        String list = response.getResponse().getContentAsString();
        assertTrue(list.contains("\"items_per_page\":15"));
        assertTrue(list.contains("\"total_results\":1"));

    }

    @Test
    void fetchAssociationsByTestShouldTrowErrorWhenCompanyNumberIsOfWrongFormat() throws Exception {

        var response = mockMvc.perform(get("/associations?page_index=0&items_per_page=15&company_number=abc")
                .header("Eric-identity", "abcd12345")
                .header("X-Request-Id", "theId")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Roles", "*")).andExpect(status().isBadRequest()).andReturn();

        String error = "{\"errors\":[{\"error\":\"Please check the request and try again\",\"location\":\"accounts_association_api\",\"location_type\":\"request-body\",\"type\":\"ch:validation\"}]}";
        assertEquals(error, response.getResponse().getContentAsString());
    }

    @Test
    void fetchAssociationsByTestShouldTrow500WhenInternalServerError() throws Exception {
        when(usersService.fetchUserDetails("abcd12345")).thenThrow(new InternalServerErrorRuntimeException("test"));
        mockMvc.perform(get("/associations?page_index=0&items_per_page=15&company_number=123")
                .header("Eric-identity", "abcd12345")
                .header("X-Request-Id", "theId")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Roles", "*")).andExpect(status().is5xxServerError());

    }
}