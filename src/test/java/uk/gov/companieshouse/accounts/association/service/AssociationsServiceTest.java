package uk.gov.companieshouse.accounts.association.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AssociationsServiceTest {

    @InjectMocks
    AssociationsService associationsService;

    @Mock
    AssociationsRepository associationsRepository;

    @Test
    void fetchAssociatedUsersWithNullOrMalformedOrNonexistentCompanyNumberReturnsEmptyPage(){
        Mockito.doReturn( Page.empty() ).when( associationsRepository ).fetchAssociatedUsers( any(), any(), any() );

        Assertions.assertTrue( associationsService.fetchAssociatedUsers( null, true, 15, 0 ).isEmpty() );
        Assertions.assertTrue( associationsService.fetchAssociatedUsers( "$", true, 15, 0 ).isEmpty() );
        Assertions.assertTrue( associationsService.fetchAssociatedUsers( "999999", true, 15, 0 ).isEmpty() );
    }

    @Test
    void fetchAssociatedUsersAppliedIncludeRemovedFilterCorrectly(){
        associationsService.fetchAssociatedUsers( "333333", true, 15, 0 );
        Mockito.verify( associationsRepository ).fetchAssociatedUsers(
                eq("333333"),
                eq(Set.of( StatusEnum.CONFIRMED, StatusEnum.AWAITING_APPROVAL, StatusEnum.REMOVED ) ),
                eq(PageRequest.of(0, 15) ) ) ;

        associationsService.fetchAssociatedUsers( "333333", false, 15, 0 );
        Mockito.verify( associationsRepository ).fetchAssociatedUsers(
                eq("333333"),
                eq(Set.of( StatusEnum.CONFIRMED, StatusEnum.AWAITING_APPROVAL ) ),
                eq(PageRequest.of(0, 15) ) ) ;
    }

    @Test
    void fetchAssociatedUsersPaginatesCorrectly(){
        associationsService.fetchAssociatedUsers( "333333", true, 2, 1 );
        Mockito.verify( associationsRepository ).fetchAssociatedUsers(
                eq("333333"),
                eq(Set.of( StatusEnum.CONFIRMED, StatusEnum.AWAITING_APPROVAL, StatusEnum.REMOVED ) ),
                eq(PageRequest.of(1, 2) ) ) ;
    }

}