package uk.gov.hmcts.reform.sscs.functional.utilities.idam.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserRecord {
    private String id;
    private String email;
    private String forename;
    private String surname;
    private String password;
    private String displayName;
    @JsonSerialize(using = RolesAsCodeObjectsSerializer.class)
    private List<String> roles;
    private String ssoId;
    private String ssoProvider;
    private String accountStatus;
    private String recordType;
    private LocalDateTime createDate;
    private LocalDateTime lastModified;
    private LocalDateTime accessLockedDate;
    private LocalDateTime lastLoginDate;

}
