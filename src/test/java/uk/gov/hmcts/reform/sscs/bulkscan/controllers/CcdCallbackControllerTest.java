package uk.gov.hmcts.reform.sscs.bulkscan.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.*;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.handlers.CcdCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.UnauthorizedException;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.controller.CcdCallbackController;
import uk.gov.hmcts.reform.sscs.controller.RootController;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@RunWith(JUnitParamsRunner.class)
@WebMvcTest(CcdCallbackController.class)
public class CcdCallbackControllerTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    public static final String JURISDICTION = "SSCS";
    public static final long ID = 1234L;

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @MockitoBean
    private AuthorisationService authorisationService;

    @MockitoBean
    private SscsCaseCallbackDeserializer deserializer;

    @MockitoBean
    private PreSubmitCallbackDispatcher dispatcher;

    @MockitoBean
    private CcdCallbackHandler ccdCallbackHandler;

    private MockMvc mockMvc;
    private CcdCallbackController controller;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private Callback<SscsCaseData> callback;

    @Before
    public void setUp() {
        openMocks(this);
        controller = new CcdCallbackController(authorisationService, deserializer, dispatcher, ccdCallbackHandler);
        mockMvc = standaloneSetup(controller)
            .setMessageConverters(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(),
                new ResourceHttpMessageConverter(false), new SourceHttpMessageConverter<>(),
                new AllEncompassingFormHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter())
            .build();
    }

    @Test
    public void should_successfully_handle_callback_and_return_validate_response() throws Exception {
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(deserializer.deserialize(anyString())).willReturn(callback);
        given(ccdCallbackHandler.handleValidationAndUpdate(
            ArgumentMatchers.any(),
            eq(IdamTokens.builder().idamOauth2Token(TEST_USER_AUTH_TOKEN).serviceAuthorization(TEST_SERVICE_AUTH_TOKEN).userId(TEST_USER_ID).build()))
        ).willReturn(new PreSubmitCallbackResponse<>(SscsCaseData.builder().state(State.WITH_DWP).build()));

        given(authorisationService.authenticate("test-header"))
            .willReturn("some-service");

        doNothing().when(authorisationService).assertIsAllowedToHandleCallback("some-service");

        mockMvc.perform(post("/validate-record")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", TEST_USER_AUTH_TOKEN)
            .header("serviceauthorization", TEST_SERVICE_AUTH_TOKEN)
            .header("user-id", TEST_USER_ID)
            .content(exceptionRecord("validation/validate-appeal-created-case-request.json")))
            .andExpect(jsonPath("$['data'].state", is("withDwp")));
    }

//    @Test
//    public void should_throw_exception_when_handler_fails() throws Exception {
//        given(ccdCallbackHandler.handleValidationAndUpdate(
//            ArgumentMatchers.any(),
//            eq(IdamTokens.builder().idamOauth2Token(TEST_USER_AUTH_TOKEN).serviceAuthorization(TEST_SERVICE_AUTH_TOKEN).userId(TEST_USER_ID).build()))
//        ).willThrow(RuntimeException.class);
//
//        given(authorisationService.authenticate("test-header"))
//            .willReturn("some-service");
//
//        doNothing().when(authorisationService).assertIsAllowedToHandleCallback("some-service");
//
//        MvcResult result =  mockMvc.perform(post("/validate-record")
//            .contentType(MediaType.APPLICATION_JSON)
//            .header("Authorization", TEST_USER_AUTH_TOKEN)
//            .header("serviceauthorization", TEST_SERVICE_AUTH_TOKEN)
//            .header("user-id", TEST_USER_ID)
//            .content(exceptionRecord("validation/validate-appeal-created-case-request.json")))
//            .andReturn();
//
//        String content = result.getResponse().getContentAsString();
//
//        assertThat(content, containsString("There was an unknown error when processing the case. If the error persists, please contact the Bulk Scan development team"));
//    }
//
//    @Test
//    public void should_throw_unauthenticated_exception_when_auth_header_is_missing() throws Exception {
//        // given
//        willThrow(UnauthorizedException.class)
//            .given(authorisationService).authenticate(null);
//
//        // when
//        mockMvc.perform(post("/validate-record")
//            .contentType(MediaType.APPLICATION_JSON)
//            .header("Authorization", TEST_USER_AUTH_TOKEN)
//            .header("user-id", TEST_USER_ID)
//            .content(exceptionRecord("validation/validate-appeal-created-case-request.json")))
//            .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    public void should_throw_unauthorized_exception_when_auth_header_is_missing() throws Exception {
//        // given
//        willThrow(ForbiddenException.class)
//            .given(authorisationService).authenticate(null);
//
//        // when
//        mockMvc.perform(post("/validate-record")
//            .contentType(MediaType.APPLICATION_JSON)
//            .header("Authorization", TEST_USER_AUTH_TOKEN)
//            .header("Authorization", TEST_USER_AUTH_TOKEN)
//            .header("user-id", TEST_USER_ID)
//            .content(exceptionRecord("validation/validate-appeal-created-case-request.json")))
//            .andExpect(status().isForbidden());
//    }
}
