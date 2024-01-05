package uk.gov.companieshouse.accounts.association.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "users")
public class Users {
    @Id
    private String id;

    @Field("surname")
    private String surname;

    @Field("forename")
    private String forename;


    @Field("email")
    private String email;

    @Field("display_name")
    private String displayName;

    public String getId() {
        return id;
    }

    public String getSurname() {
        return surname;
    }

    public String getForename() {
        return forename;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Users(String surname, String forename, String email, String displayName) {
        this.surname = surname;
        this.forename = forename;
        this.email = email;
        this.displayName = displayName;
    }
}