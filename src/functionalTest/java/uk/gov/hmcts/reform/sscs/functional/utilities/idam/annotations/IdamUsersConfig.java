package uk.gov.hmcts.reform.sscs.functional.utilities.idam.annotations;

import java.util.List;
import java.util.Map;

record IdamUsersConfig(
    Map<String, UserProfile> profiles,
    Map<String, UserProfile> overridesByEmail
) {
    record UserProfile(String forename, String surname, List<String> roles) {}

    UserProfile merge(String profileKey, String email) {
        UserProfile base = profiles != null ? profiles.get(profileKey) : null;
        if (base == null) {
            throw new IllegalArgumentException("Profile not found: " + profileKey);
        }

        UserProfile override = overridesByEmail != null ? overridesByEmail.get(email) : null;
        if (override == null) return base;

        return new UserProfile(
            override.forename() != null ? override.forename() : base.forename(),
            override.surname() != null ? override.surname() : base.surname(),
            (override.roles() != null && !override.roles().isEmpty()) ? override.roles() : base.roles()
        );
    }
}
