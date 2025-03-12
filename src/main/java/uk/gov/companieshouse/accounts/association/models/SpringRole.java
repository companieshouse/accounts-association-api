package uk.gov.companieshouse.accounts.association.models;

public enum SpringRole {

    BASIC_OAUTH_ROLE ( "BASIC_OAUTH" ),
    UNKNOWN_ROLE ( "UNKNOWN" );

    private final String value;

    SpringRole( final String springRole ){ value = springRole; }

    public String getValue(){
        return value;
    }

    public static String[] getValues( final SpringRole... springRoles ){
        final var roles = new String[ springRoles.length ];
        for ( int roleIndex = 0; roleIndex < springRoles.length; roleIndex++ ){
            roles[ roleIndex ] = springRoles[ roleIndex ].getValue();
        }
        return roles;
    }

}
