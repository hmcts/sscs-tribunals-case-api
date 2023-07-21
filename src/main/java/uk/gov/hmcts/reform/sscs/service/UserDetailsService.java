package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.sscs.idam.UserRole;

@Service
public class UserDetailsService {

    protected final IdamClient idamClient;

    @Autowired
    public UserDetailsService(IdamClient idamClient) {
        this.idamClient = idamClient;
    }

    public String buildLoggedInUserName(String userAuthorisation) {
        UserDetails userDetails = getUserDetails(userAuthorisation);
        return userDetails.getFullName();
    }

    public String buildLoggedInUserSurname(String userAuthorisation) {
        UserDetails userDetails = getUserDetails(userAuthorisation);
        return userDetails.getSurname().orElse("");
    }

    public List<String> getUserRoles(String userAuthorisation) {
        return getUserInfo(userAuthorisation).getRoles();
    }

    public String getUserRole(String userAuthorisation) {
        List<String> users = getUserRoles(userAuthorisation);

        for (UserRole userRole : UserRole.values()) {
            if (users.contains(userRole.getValue())) {
                return userRole.getLabel();
            }
        }

        return null;
    }

    public UserInfo getUserInfo(String userAuthorisation) {
        UserInfo userInfo = idamClient.getUserInfo(userAuthorisation);
        if (isNull(userInfo)) {
            throw new IllegalStateException("Unable to obtain signed in user info");
        }
        return userInfo;
    }

    private UserDetails getUserDetails(String userAuthorisation) throws IllegalStateException {
        UserDetails userDetails = idamClient.getUserDetails(userAuthorisation);
        if (userDetails == null) {
            throw new IllegalStateException("Unable to obtain signed in user details");
        }
        return userDetails;
    }
}
