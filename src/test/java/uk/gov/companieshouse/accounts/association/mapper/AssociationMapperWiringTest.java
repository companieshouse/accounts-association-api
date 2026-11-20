package uk.gov.companieshouse.accounts.association.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;

@Tag( "unit-test" )
class AssociationMapperWiringTest {

    @Test
    void springConstructsMappersAndRunsEnrichment() {
        new ApplicationContextRunner()
                .withBean( UsersService.class, () -> mock( UsersService.class ) )
                .withBean( CompanyService.class, () -> mock( CompanyService.class ) )
                .withUserConfiguration( AssociationDaoMapperImpl.class, AssociationMapper.class,
                        AssociationsListCompanyMapper.class, AssociationsListUserMapper.class,
                        InvitationDaoMapperImpl.class, InvitationMapper.class )
                .run( context -> {
                    final var data = TestDataManager.getInstance();
                    final var dao = data.fetchAssociationDaos( "1" ).getFirst();
                    final var user = data.fetchUserDtos( "111" ).getFirst();
                    final var company = data.fetchCompanyDetailsDtos( "111111" ).getFirst();
                    final var mapper = context.getBean( AssociationsListCompanyMapper.class );
                    final var association = mapper.daoToDto( dao, user, company );

                    assertEquals( user.getEmail(), association.getUserEmail() );
                    assertEquals( company.getCompanyName(), association.getCompanyName() );
                    assertEquals( "/associations/" + association.getId(), association.getLinks().getSelf() );
                    final var userMapper = context.getBean( AssociationsListUserMapper.class );
                    assertNotNull( userMapper.daoToDto( dao, user, company ).getLinks() );

                    final var usersService = context.getBean( UsersService.class );
                    when( usersService.fetchUserDetails( anyString(), any() ) ).thenReturn( user );
                    final var invitation = context.getBean( InvitationMapper.class )
                            .daoToDto( dao.getInvitations().getFirst(), dao.getId() );
                    assertEquals( user.getEmail(), invitation.getInvitedBy() );
                    assertEquals( dao.getId(), invitation.getAssociationId() );
                    assertNotNull( invitation.getIsActive() );
                } );
    }
}
