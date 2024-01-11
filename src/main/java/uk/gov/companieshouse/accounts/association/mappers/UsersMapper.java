package uk.gov.companieshouse.accounts.association.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.companieshouse.accounts.association.models.User;
import uk.gov.companieshouse.api.accounts.associations.model.UserInfo;

@Mapper(componentModel = "spring")
public interface UsersMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "autorisationStatus", source = "user.status")
    UserInfo mapUserInfo(User user);

}
