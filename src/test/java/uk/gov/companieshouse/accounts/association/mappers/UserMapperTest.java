package uk.gov.companieshouse.accounts.association.mappers;


import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.companieshouse.accounts.association.models.User;
import uk.gov.companieshouse.api.accounts.associations.model.UserInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMapperTest {

    private final UsersMapper mapper = Mappers.getMapper(UsersMapper.class);

    @Test
    void testUsersMapping() {
        User user = new User("last", "first", "test@test", "Test User");
        UserInfo userInfo = mapper.mapUserInfo(user);
        assertEquals(userInfo.getUserEmail(), user.getEmail());
        assertEquals(userInfo.getDisplayName(), user.getDisplayName());
    }

}