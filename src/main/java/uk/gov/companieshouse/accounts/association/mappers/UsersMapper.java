package uk.gov.companieshouse.accounts.association.mappers;

import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.models.Users;
import uk.gov.companieshouse.api.accounts.associations.model.UserInfo;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UsersMapper {
    List<UserInfo> mapUserInfo(List<Users> users);
}
