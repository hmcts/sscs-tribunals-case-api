package uk.gov.hmcts.reform.sscs.functional.utilities.idam.model;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.With;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Builder
public record User(@With UUID id, String email, String forename, String surname, String displayName, List<String> roles,
                   @With IdamTokens tokens) {
    public boolean isSame(User other) {
        if (other == null) {
            return false;
        }
        return email.equals(other.email) && forename.equals(other.forename) && surname.equals(other.surname)
            && roles.equals(other.roles);
    }
}
