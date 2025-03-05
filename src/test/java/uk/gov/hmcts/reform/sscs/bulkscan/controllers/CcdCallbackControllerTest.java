package uk.gov.hmcts.reform.sscs.bulkscan.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.sscs.bulkscan.auth.AuthService;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.handlers.CcdCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.UnauthorizedException;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(SpringRunner.class)
@WebMvcTest(CcdCallbackController.class)
public class CcdCallbackControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Mock
    private CcdCallbackHandler ccdCallbackHandler;

    @Mock
    private AuthService authService;

    @Test
    public void should_successfully_handle_callback_and_return_validate_response() throws Exception {

        given(ccdCallbackHandler.handleValidationAndUpdate(
            ArgumentMatchers.any(),
            eq(IdamTokens.builder().idamOauth2Token(TEST_USER_AUTH_TOKEN).serviceAuthorization(TEST_SERVICE_AUTH_TOKEN).userId(TEST_USER_ID).build()))
        ).willReturn(new PreSubmitCallbackResponse<>(SscsCaseData.builder().state(State.WITH_DWP).build()));

        given(authService.authenticate("test-header"))
            .willReturn("some-service");

        doNothing().when(authService).assertIsAllowedToHandleCallback("some-service");

        mockMvc.perform(post("/validate-record")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", TEST_USER_AUTH_TOKEN)
            .header("serviceauthorization", TEST_SERVICE_AUTH_TOKEN)
            .header("user-id", TEST_USER_ID)
            .content(exceptionRecord("validation/validate-appeal-created-case-request.json")))
            .andExpect(jsonPath("$['data'].state", is("withDwp")));
    }

    @Test
    public void should_throw_exception_when_handler_fails() throws Exception {
        given(ccdCallbackHandler.handleValidationAndUpdate(
            ArgumentMatchers.any(),
            eq(IdamTokens.builder().idamOauth2Token(TEST_USER_AUTH_TOKEN).serviceAuthorization(TEST_SERVICE_AUTH_TOKEN).userId(TEST_USER_ID).build()))
        ).willThrow(RuntimeException.class);

        given(authService.authenticate("test-header"))
            .willReturn("some-service");

        doNothing().when(authService).assertIsAllowedToHandleCallback("some-service");

        MvcResult result =  mockMvc.perform(post("/validate-record")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", TEST_USER_AUTH_TOKEN)
            .header("serviceauthorization", TEST_SERVICE_AUTH_TOKEN)
            .header("user-id", TEST_USER_ID)
            .content(exceptionRecord("validation/validate-appeal-created-case-request.json")))
            .andReturn();

        String content = result.getResponse().getContentAsString();

        assertThat(content, containsString("There was an unknown error when processing the case. If the error persists, please contact the Bulk Scan development team"));
    }

    @Test
    public void should_throw_unauthenticated_exception_when_auth_header_is_missing() throws Exception {
        // given
        willThrow(UnauthorizedException.class)
            .given(authService).authenticate(null);

        // when
        mockMvc.perform(post("/validate-record")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", TEST_USER_AUTH_TOKEN)
            .header("user-id", TEST_USER_ID)
            .content(exceptionRecord("validation/validate-appeal-created-case-request.json")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void should_throw_unauthorized_exception_when_auth_header_is_missing() throws Exception {
        // given
        willThrow(ForbiddenException.class)
            .given(authService).authenticate(null);

        // when
        mockMvc.perform(post("/validate-record")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", TEST_USER_AUTH_TOKEN)
            .header("Authorization", TEST_USER_AUTH_TOKEN)
            .header("user-id", TEST_USER_ID)
            .content(exceptionRecord("validation/validate-appeal-created-case-request.json")))
            .andExpect(status().isForbidden());
    }
}
