package uk.gov.companieshouse.accounts.association.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.companieshouse.accounts.association.mapper.AssociationMapper;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListCompanyMapper;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListUserMapper;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AssociationsServiceTest {

    @InjectMocks
    AssociationsService associationsService;

    @Mock
    AssociationsRepository associationsRepository;

    @Mock
    AssociationsListCompanyMapper associationsListCompanyMapper;

    @Mock
    AssociationsListUserMapper associationsListUserMapper;

    @Mock
    AssociationMapper associationMapper;


    @BeforeEach
    public void setup() {
        associationsService = new AssociationsService(
                associationsRepository,
                associationsListUserMapper,
                associationsListCompanyMapper,
                associationMapper
        );
    }

    @Test
    void fetchAssociationsForUSerReturnEmptyItemsWhenNoAssociationFound() throws ApiErrorResponseException, URIValidationException {
        User user = new User("kk", "kk@kk.com");
        user.setUserId("111");
        List<String> status = Collections.singletonList("confirmed");
        Page<AssociationDao> page = Page.empty();
        when(associationsRepository
                .findAllByUserIdAndStatusIsInAndCompanyNumberLike(
                        "111",
                        status,
                        "",
                        PageRequest.of(0, 15)))
                .thenReturn(page);
        associationsService.fetchAssociationsForUserStatusAndCompany(user, status, 0, 15, "");
        verify(associationsListUserMapper).daoToDto(page, user);

    }

    @Test
    void fetchAssociationsForUserUsesStatusConfirmedAsDefaultWhenStatusNotProvided() throws ApiErrorResponseException, URIValidationException {
        User user = new User("kk", "kk@kk.com");
        user.setUserId("111");
        Page<AssociationDao> page = Page.empty();

        associationsService.fetchAssociationsForUserStatusAndCompany(user, null, 0, 15, "");
        verify(associationsRepository)
                .findAllByUserIdAndStatusIsInAndCompanyNumberLike(
                        "111",
                        Collections.singletonList("confirmed"),
                        "",
                        PageRequest.of(0, 15));
        verify(associationsListUserMapper).daoToDto(null, user);

    }


}