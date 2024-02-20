package uk.gov.companieshouse.accounts.association.controllers.manual;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.api.accounts.user.model.RolesList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

@RestController
public class AccountsUserTestController {

    private final AccountsUserEndpoint accountsUserEndpoint;

    public AccountsUserTestController(AccountsUserEndpoint accountsUserEndpoint) {
        this.accountsUserEndpoint = accountsUserEndpoint;
    }

    @GetMapping( "/users/search" )
    public ResponseEntity<?> searchUserDetails( @RequestParam("user_email") final String userEmail ) throws ApiErrorResponseException, URIValidationException {

        try {
            accountsUserEndpoint.searchUserDetails(null);
            System.err.println( "Test 1/3 failed" );
        } catch ( Exception e ){
            System.err.println( "Test 1/3 passed" );
        }

        try {
            accountsUserEndpoint.searchUserDetails(List.of("xxx"));
            System.err.println( "Test 2/3 failed" );
        } catch ( Exception e ){
            System.err.println( "Test 2/3 passed" );
        }


        if ( accountsUserEndpoint.searchUserDetails(List.of("the.void@space.com")).getData().isEmpty() )
            System.err.println( "Test 3/3 passed" );
        else
            System.err.println( "Test 3/3 failed" );

        return new ResponseEntity<>( accountsUserEndpoint.searchUserDetails( List.of( userEmail ) ).getData(), HttpStatus.OK );
    }

    @GetMapping( "/users/{user_id}" )
    public ResponseEntity<?> getUserDetails( @PathVariable("user_id") final String userId ) throws ApiErrorResponseException, URIValidationException {

        try {
            accountsUserEndpoint.getUserDetails(null);
            System.err.println( "Test 1/3 failed" );
        } catch ( Exception e ){
            System.err.println( "Test 1/3 passed" );
        }

        try {
            accountsUserEndpoint.getUserDetails("$");
            System.err.println( "Test 2/3 failed" );
        } catch ( Exception e ){
            System.err.println( "Test 2/3 passed" );
        }

        try {
            accountsUserEndpoint.getUserDetails("666");
            System.err.println( "Test 3/3 failed" );
        } catch ( Exception e ){
            System.err.println( "Test 3/3 passed" );
        }

        return new ResponseEntity<>( accountsUserEndpoint.getUserDetails( userId ).getData(), HttpStatus.OK );
    }

    @GetMapping( "/users/{user_id}/roles" )
    public ResponseEntity<?> getUserRoles( @PathVariable("user_id") final String userId ) throws ApiErrorResponseException, URIValidationException {

        try {
            accountsUserEndpoint.getUserRoles(null);
            System.err.println( "Test 1/3 failed" );
        } catch ( Exception e ){
            System.err.println( "Test 1/3 passed" );
        }

        try {
            accountsUserEndpoint.getUserRoles("$");
            System.err.println( "Test 2/3 failed" );
        } catch ( Exception e ){
            System.err.println( "Test 2/3 passed" );
        }

        if ( accountsUserEndpoint.getUserRoles("666").getData().isEmpty() )
            System.err.println( "Test 3/3 passed" );
        else
            System.err.println( "Test 3/3 failed" );

        return new ResponseEntity<>( accountsUserEndpoint.getUserRoles( userId ).getData(), HttpStatus.OK );
    }






    @PutMapping( "/users/{user_id}/roles" )
    public ResponseEntity<?> setUserRoles( @PathVariable("user_id") final String userId, @RequestBody final RolesList roles ) throws ApiErrorResponseException, URIValidationException {

        try {
            accountsUserEndpoint.setUserRoles(null, roles );
            System.err.println( "Test 1/4 failed" );
        } catch ( Exception e ){
            System.err.println( "Test 1/4 passed" );
        }

        try {
            accountsUserEndpoint.setUserRoles( userId, null );
            System.err.println( "Test 2/4 failed" );
        } catch ( Exception e ){
            System.err.println( "Test 2/4 passed" );
        }

        try {
            accountsUserEndpoint.setUserRoles( "$", roles );
            System.err.println( "Test 3/4 failed" );
        } catch ( Exception e ){
            System.err.println( "Test 3/4 passed" );
        }

        try {
            accountsUserEndpoint.setUserRoles( "666", roles );
            System.err.println( "Test 4/4 failed" );
        } catch ( Exception e ){
            System.err.println( "Test 4/4 passed" );
        }

        return new ResponseEntity<>( accountsUserEndpoint.setUserRoles( userId, roles ).getData(), HttpStatus.OK );
    }




}
