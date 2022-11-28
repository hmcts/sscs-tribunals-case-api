package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

@RunWith(SpringRunner.class)
public class UserDetailsServiceTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private IdamClient idamClient;

    private UserDetailsService userDetailsService;

    @Before
    public void setUp() {
        openMocks(this);
        userDetailsService = new UserDetailsService(idamClient);
    }

    @Test
    public void givenUserAuthorisation_thenReturnUserFullName() {
        when(idamClient.getUserInfo(USER_AUTHORISATION)).thenReturn(UserInfo.builder()
                .givenName("John").familyName("Lewis").build());

        assertEquals("John Lewis", userDetailsService.buildLoggedInUserName(USER_AUTHORISATION));
    }

    @Test(expected = IllegalStateException.class)
    public void givenUserNotFound_thenThrowAnException() {
        when(idamClient.getUserInfo(USER_AUTHORISATION)).thenReturn(null);
        userDetailsService.buildLoggedInUserName(USER_AUTHORISATION);
    }

    @Test
    public void givenUserAuthorisation_thenReturnUserSurname() {
        when(idamClient.getUserInfo(USER_AUTHORISATION)).thenReturn(UserInfo.builder()
                .givenName("John").familyName("Lewis").build());

        assertEquals("Lewis", userDetailsService.buildLoggedInUserSurname(USER_AUTHORISATION));
    }


}
