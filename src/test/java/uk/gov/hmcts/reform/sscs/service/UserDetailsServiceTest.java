package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private IdamClient idamClient;

    @Mock
    private JudicialRefDataService judicialRefDataService;

    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new UserDetailsService(idamClient, judicialRefDataService);
    }

    @Test
    void givenUserAuthorisation_thenReturnUserFullName() {
        when(idamClient.getUserInfo(USER_AUTHORISATION)).thenReturn(UserInfo.builder()
                .givenName("John").familyName("Lewis").build());

        assertThat(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).isEqualTo("John Lewis");
    }

    @Test
    void givenUserNotFound_thenThrowAnException() {
        assertThatThrownBy(() -> userDetailsService.buildLoggedInUserName(USER_AUTHORISATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Unable to obtain signed in user info");
    }

    @Test
    void givenUserAuthorisation_thenReturnUserSurname() {
        when(idamClient.getUserInfo(USER_AUTHORISATION)).thenReturn(UserInfo.builder()
                .givenName("John").familyName("Lewis").build());

        assertThat(userDetailsService.buildLoggedInUserSurname(USER_AUTHORISATION)).isEqualTo("Lewis");
    }

    @Test
    void givenUserAuthorisation_thenReturnUserInfo() {
        UserInfo userInfo = UserInfo.builder().givenName("A").familyName("B").build();
        when(idamClient.getUserInfo(USER_AUTHORISATION)).thenReturn(userInfo);

        assertThat(userDetailsService.getUserInfo(USER_AUTHORISATION)).isEqualTo(userInfo);
    }

    @Test
    void givenUserInfoIsNull_thenThrowAnException() {
        assertThatThrownBy(() -> userDetailsService.getUserInfo(USER_AUTHORISATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Unable to obtain signed in user info");
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void givenUserAuthorisation_thenReturnUserRole(UserRole userRole) {
        UserInfo userInfo = UserInfo.builder().roles(List.of(userRole.getValue())).build();
        when(idamClient.getUserInfo(USER_AUTHORISATION)).thenReturn(userInfo);

        assertThat(userDetailsService.getUserRole(USER_AUTHORISATION)).isEqualTo(userRole.getLabel());
    }

    @Test
    void givenUserAuthorisation_andRoleIsNull_thenReturnNull() {
        UserInfo userInfo = UserInfo.builder().roles(Collections.emptyList()).build();
        when(idamClient.getUserInfo(USER_AUTHORISATION)).thenReturn(userInfo);

        assertThat(userDetailsService.getUserRole(USER_AUTHORISATION)).isNull();
    }

    @Test
    void givenUserAuthorisation_thenReturnLoggedInUser() {
        String idamId = "123";
        UserInfo userDetails = UserInfo.builder().uid(idamId).build();
        when(idamClient.getUserInfo(USER_AUTHORISATION)).thenReturn(userDetails);
        when(judicialRefDataService.getJudicialUserFromIdamId(idamId)).thenReturn(new JudicialUserBase(idamId, "456"));

        assertThat(userDetailsService.getLoggedInUserAsJudicialUser(USER_AUTHORISATION)).isEqualTo(new JudicialUserBase(idamId, "456"));
    }
}
