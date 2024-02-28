package uk.gov.hmcts.reform.sscs.service;

import static java.lang.String.join;
import static java.util.Objects.isNull;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;

@Service
@Slf4j
@AllArgsConstructor
public class UserDetailsService {
    protected final IdamClient idamClient;
    protected final JudicialRefDataService judicialRefDataService;

    public String buildLoggedInUserName(String userAuthorisation) {
        UserInfo userInfo = getUserInfo(userAuthorisation);
        return join(" ", userInfo.getGivenName(), userInfo.getFamilyName());
    }

    public String buildLoggedInUserSurname(String userAuthorisation) {
        return getUserInfo(userAuthorisation).getFamilyName();
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

    public JudicialUserBase getLoggedInUserAsJudicialUser(String userAuthorisation) {
        UserInfo userInfo = getUserInfo(userAuthorisation);

        String idamId = userInfo.getUid();

        log.info("Getting personal code for idamId: {}", idamId);

        return judicialRefDataService.getJudicialUserFromIdamId(idamId);
    }
}
