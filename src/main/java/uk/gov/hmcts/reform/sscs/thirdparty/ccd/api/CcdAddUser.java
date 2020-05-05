package uk.gov.hmcts.reform.sscs.thirdparty.ccd.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class CcdAddUser {
    private final String userId;

    public CcdAddUser(String userId) {
        this.userId = userId;
    }

    @JsonProperty(value = "id")
    public String getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CcdAddUser that = (CcdAddUser) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "CcdAddUser{"
                + "userId='" + userId + '\''
                + '}';
    }
}
