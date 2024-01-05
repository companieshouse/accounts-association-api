package uk.gov.companieshouse.accounts.association.mappers;


import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import uk.gov.companieshouse.accounts.association.models.Users;
import uk.gov.companieshouse.api.accounts.associations.model.UserInfo;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UsersMapperTest {

    private final UsersMapper mapper = Mappers.getMapper(UsersMapper.class);

    @Test
    void testUsersMapping() {
        Users users = new Users("last", "first", "test@test", "Test User");
        List<UserInfo> userInfo = mapper.mapUserInfo(Collections.singletonList(users));
    }

}