package uk.gov.hmcts.reform.sscs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

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

    public String getUserFullNameByUserId(String userAuthorisation, String idamId) {
        UserDetails userDetails = idamClient.getUserByUserId(userAuthorisation, idamId);
        return userDetails.getFullName();
    }

    protected UserDetails getUserDetails(String userAuthorisation) throws IllegalStateException {
        UserDetails userDetails = idamClient.getUserDetails(userAuthorisation);
        if (userDetails == null) {
            throw new IllegalStateException("Unable to obtain signed in user details");
        }
        return userDetails;
    }
}
