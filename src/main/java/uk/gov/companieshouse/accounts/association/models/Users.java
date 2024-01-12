package uk.gov.companieshouse.accounts.association.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

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


    public void setId(String id) {
        this.id = id;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setForename(String forename) {
        this.forename = forename;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}