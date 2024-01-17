package uk.gov.companieshouse.accounts.association.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.accounts.association.controller.UserCompanyAssociationStatusApi;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.UserInfo;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit-test")
@WebMvcTest(UserCompanyAssociationStatusApi.class)
class UserCompanyAssociationStatusApiTest {

    @Autowired
    public MockMvc mockMvc;

    @MockBean
    AssociationsService associationsService;

    @MockBean
    UsersService usersService;

    private Association ronnieOSullivanAssociation;
    private Association batmanAssociation;

    @BeforeEach
    void setup() {
        batmanAssociation = new Association();
        batmanAssociation.setCompanyNumber("111111");
        batmanAssociation.setUserId("111");
        batmanAssociation.setStatus("Awaiting approval");

        ronnieOSullivanAssociation = new Association();
        ronnieOSullivanAssociation.setCompanyNumber("222222");
        ronnieOSullivanAssociation.setUserId("ronnie.osullivan@snooker.com");
        ronnieOSullivanAssociation.setStatus("Awaiting approval");
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithMissingPathVariablesReturnsNotFound() throws Exception {
        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "", "Confirmed").header("X-Request-Id", "theId")).andExpect(status().isNotFound());
        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "", "bruce.wayne@gotham.city", "Confirmed").header("X-Request-Id", "theId")).andExpect(status().isNotFound());
        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "bruce.wayne@gotham.city", "").header("X-Request-Id", "theId")).andExpect(status().isNotFound());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithMalformedInputReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "123", "Confirmed").header("X-Request-Id", "theId")).andExpect(status().isBadRequest());
        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "abc", "bruce.wayne@gotham.city", "Confirmed").header("X-Request-Id", "theId")).andExpect(status().isBadRequest());
        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "bruce.wayne@gotham.city", "Complicated").header("X-Request-Id", "theId")).andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithNonexistentUserEmailOrCompanyNumberReturnsBadRequest() throws Exception {

        Mockito.doReturn(Optional.empty())
                .when(usersService).fetchUserInfo("krishna.patel@dahai.art");

        Mockito.doReturn(Optional.of(new UserInfo().userId("111")))
                .when(usersService).fetchUserInfo("bruce.wayne@gotham.city");

        Mockito.doReturn(Optional.empty())
                .when(associationsService).getByUserIdAndCompanyNumber(any(), any());

        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "krishna.patel@dahai.art", "Removed").header("X-Request-Id", "theId")).andExpect(status().isBadRequest());
        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "333333", "bruce.wayne@gotham.city", "Removed").header("X-Request-Id", "theId")).andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithNonexistentAssociationReturnsBadRequest() throws Exception {

        Mockito.doReturn(Optional.of(new UserInfo().userId("111")))
                .when(usersService).fetchUserInfo("bruce.wayne@gotham.city");

        Mockito.doReturn(Optional.of(new UserInfo().userId("222")))
                .when(usersService).fetchUserInfo("bruce.wayne@gotham.city");

        Mockito.doReturn(Optional.empty())
                .when(associationsService).getByUserIdAndCompanyNumber(any(), any());

        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "michael.jackson@singer.com", "Confirmed").header("X-Request-Id", "theId")).andExpect(status().isBadRequest());
        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "222222", "bruce.wayne@gotham.city", "Confirmed").header("X-Request-Id", "theId")).andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithConfirmedStatusAndNonExistentUserReturnsBadRequest() throws Exception {
        Mockito.doReturn(Optional.empty())
                .when(usersService).fetchUserInfo("ronnie.osullivan@snooker.com");

        Mockito.doReturn(Optional.of(ronnieOSullivanAssociation))
                .when(associationsService).getByUserIdAndCompanyNumber("ronnie.osullivan@snooker.com", "222222");

        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "222222", "ronnie.osullivan@snooker.com", "Confirmed").header("X-Request-Id", "theId")).andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithConfirmedStatusAndExistentUserShouldExecuteConfirmAssociation() throws Exception {

        Mockito.doReturn(Optional.of(new UserInfo().userId("111")))
                .when(usersService).fetchUserInfo("bruce.wayne@gotham.city");

        Mockito.doReturn(Optional.of(batmanAssociation))
                .when(associationsService).getByUserIdAndCompanyNumber("111", "111111");

        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "bruce.wayne@gotham.city", "Confirmed").header("X-Request-Id", "theId")).andExpect(status().isOk());

        Mockito.verify(associationsService).confirmAssociation(eq("111"), eq("111111"));
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithRemovedStatusAndNonExistentUserSetsStatusToRemovedAndDeletionTime() throws Exception {
        Mockito.doReturn(Optional.empty())
                .when(usersService).fetchUserInfo("ronnie.osullivan@snooker.com");

        Mockito.doReturn(Optional.of(ronnieOSullivanAssociation))
                .when(associationsService).getByUserIdAndCompanyNumber("ronnie.osullivan@snooker.com", "222222");


        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "222222", "ronnie.osullivan@snooker.com", "Removed").header("X-Request-Id", "theId")).andExpect(status().isOk());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithRemovedStatusAndExistingUserSetsStatusToRemovedAndDeletionTimeAndTemporaryToFalse() throws Exception {

        Mockito.doReturn(Optional.of(new UserInfo().userId("111")))
                .when(usersService).fetchUserInfo("bruce.wayne@gotham.city");

        Mockito.doReturn(Optional.of(batmanAssociation))
                .when(associationsService).getByUserIdAndCompanyNumber("111", "111111");

        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "bruce.wayne@gotham.city", "Removed").header("X-Request-Id", "theId")).andExpect(status().isOk());
    }

}
