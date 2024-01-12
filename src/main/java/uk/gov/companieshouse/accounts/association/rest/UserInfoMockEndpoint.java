package uk.gov.companieshouse.accounts.association.rest;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.enums.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.UserInfo;


@Repository
public class UserInfoMockEndpoint {

    private final HashMap<String, Supplier<UserInfo>> mockUsers = new HashMap<>();

    public UserInfoMockEndpoint(){

        final Supplier<UserInfo> batmanSupplier = () -> {
            final var batmanUserInfo = new UserInfo();
            batmanUserInfo.setUserId("111");
            batmanUserInfo.setDisplayName("Batman");
            batmanUserInfo.setUserEmail("bruce.wayne@gotham.city");
            batmanUserInfo.setAuthorisationStatus(StatusEnum.CONFIRMED.getValue());
            return batmanUserInfo;
        };

        final Supplier<UserInfo> michaelJacksonSupplier = () -> {
            final var michaelJacksonUserInfo = new UserInfo();
            michaelJacksonUserInfo.setUserId("222");
            michaelJacksonUserInfo.setDisplayName("Michael Jackson");
            michaelJacksonUserInfo.setUserEmail("michael.jackson@singer.com");
            michaelJacksonUserInfo.setAuthorisationStatus(StatusEnum.CONFIRMED.getValue());
            return michaelJacksonUserInfo;
        };

        final Supplier<UserInfo> mrBlobbySupplier = () -> {
            final var mrBlobbyUserInfo = new UserInfo();
            mrBlobbyUserInfo.setUserId("333");
            mrBlobbyUserInfo.setDisplayName("Mr Blobby");
            mrBlobbyUserInfo.setUserEmail("mr.blobby@nightmare.co.uk");
            mrBlobbyUserInfo.setAuthorisationStatus(StatusEnum.CONFIRMED.getValue());
            return mrBlobbyUserInfo;
        };

        mockUsers.put( "bruce.wayne@gotham.city", batmanSupplier );
        mockUsers.put( "michael.jackson@singer.com", michaelJacksonSupplier );
        mockUsers.put( "mr.blobby@nightmare.co.uk", mrBlobbySupplier );
    }

    public Optional<UserInfo> fetchUserInfo( String userEmail ){
        return Optional.of( mockUsers )
                .filter( mockUsers1 -> mockUsers.containsKey( userEmail ) )
                .map( mockUsers2 -> mockUsers.get( userEmail ) )
                .map( Supplier::get );
    }

}
