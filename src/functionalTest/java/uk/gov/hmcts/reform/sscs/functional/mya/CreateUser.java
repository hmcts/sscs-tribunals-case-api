package uk.gov.hmcts.reform.sscs.functional.mya;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateUser {
    private String email;
    private String forename;
    private String surname;
    private String password;
    private List<Role> roles;

    public CreateUser(String email, String forename, String surname, String password, List<Role> roles) {
        this.email = email;
        this.forename = forename;
        this.surname = surname;
        this.password = password;
        this.roles = roles;
    }

    @JsonProperty(value = "email")
    public String getEmail() {
        return email;
    }

    @JsonProperty(value = "forename")
    public String getForename() {
        return forename;
    }

    @JsonProperty(value = "surname")
    public String getSurname() {
        return surname;
    }

    @JsonProperty(value = "password")
    public String getPassword() {
        return password;
    }

    @JsonProperty(value = "roles")
    public List<Role> getRoles() {
        return roles;
    }

}
