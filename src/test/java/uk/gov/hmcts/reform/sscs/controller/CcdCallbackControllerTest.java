package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.TEST_SERVICE_AUTH_TOKEN;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.TEST_USER_AUTH_TOKEN;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.TEST_USER_ID;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.exceptionRecord;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ACTION_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INTERLOC_INFORMATION_RECEIVED;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.handlers.CcdCallbackHandler;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptionhandlers.ResponseExceptionHandler;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.UnauthorizedException;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@SuppressWarnings("unchecked")
@RunWith(JUnitParamsRunner.class)
public class CcdCallbackControllerTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    public static final String JURISDICTION = "SSCS";
    public static final long ID = 1234L;

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private MockMvc mockMvc;

    @MockitoBean
    private AuthorisationService authorisationService;

    @MockitoBean
    private SscsCaseCallbackDeserializer deserializer;

    @MockitoBean
    private PreSubmitCallbackDispatcher dispatcher;

    @MockitoBean
    private CcdService ccdService;

    @MockitoBean
    private IdamService idamService;

    @MockitoBean
    private CcdCallbackHandler ccdCallbackHandler;

    private CcdCallbackController controller;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private Callback<SscsCaseData> callback;

    @Before
    public void setUp() {
        controller = new CcdCallbackController(authorisationService, deserializer, dispatcher, ccdCallbackHandler);
        mockMvc = standaloneSetup(controller)
            .setControllerAdvice(new ResponseExceptionHandler())
            .setMessageConverters(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(),
                new ResourceHttpMessageConverter(false), new SourceHttpMessageConverter<>(),
                new AllEncompassingFormHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter())
            .build();
    }

    @Test
    public void handleCcdAboutToStartCallbackAndUpdateCaseData() throws Exception {
        String path = getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json").getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();
        when(deserializer.deserialize(content)).thenReturn(new Callback<>(
            new CaseDetails<>(ID, JURISDICTION, State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now(), "Benefit"),
            Optional.empty(), ACTION_FURTHER_EVIDENCE, false));

        PreSubmitCallbackResponse response = new PreSubmitCallbackResponse(SscsCaseData.builder().originalSender(
            new DynamicList(new DynamicListItem("1", "2"), null)).build());

        when(dispatcher.handle(any(CallbackType.class), any(), anyString()))
            .thenReturn(response);

        mockMvc.perform(post("/ccdAboutToStart")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "")
            .header("Authorization", "")
            .content(content))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"data\": {\"originalSender\": {\"value\": {\"code\": \"1\", \"label\": \"2\"}}}}"));
    }

    @Test
    public void handleCcdAboutToSubmitCallbackAndUpdateCaseData() throws Exception {
        String path = getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json").getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();
        when(deserializer.deserialize(content)).thenReturn(new Callback<>(
            new CaseDetails<>(ID, JURISDICTION, State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now(), "Benefit"),
            Optional.empty(), INTERLOC_INFORMATION_RECEIVED, false));

        PreSubmitCallbackResponse response =
            new PreSubmitCallbackResponse(SscsCaseData.builder().interlocReviewState(InterlocReviewState.WELSH_TRANSLATION).build());
        when(dispatcher.handle(any(CallbackType.class), any(), anyString()))
            .thenReturn(response);

        mockMvc.perform(post("/ccdAboutToSubmit")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "")
            .header("Authorization", "")
            .content(content))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"data\": {\"interlocReviewState\": \"welshTranslation\"}}"));
    }

    @Test
    public void givenSubmittedCallbackForActionFurtherEvidenceEvent_shouldReturnOk() throws Exception {
        Callback<SscsCaseData> callback = buildCallbackForTestScenarioForGivenEvent();
        given(deserializer.deserialize(anyString())).willReturn(callback);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

        PreSubmitCallbackResponse response = new PreSubmitCallbackResponse(SscsCaseData.builder()
            .interlocReviewState(InterlocReviewState.REVIEW_BY_TCW)
            .build());
        when(dispatcher.handle(any(CallbackType.class), any(), anyString())).thenReturn(response);

        mockMvc.perform(post("/ccdSubmittedEvent")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "s2s")
            .header("Authorization", "Bearer token")
            .content("something"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"data\": {\"interlocReviewState\": \"reviewByTcw\"}}"));
    }

    private Callback<SscsCaseData> buildCallbackForTestScenarioForGivenEvent() {
        CaseDetails<SscsCaseData> caseDetail = new CaseDetails<>(ID, JURISDICTION, State.INTERLOCUTORY_REVIEW_STATE,
            SscsCaseData.builder().build(), LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetail, Optional.empty(), EventType.ACTION_FURTHER_EVIDENCE, false);
    }

    @Test
    @Parameters(method = "getEdgeScenariosForTheRequest")
    public void givenCcdSubmittedEventCallbackForActionFurtherEvidenceEvent_messageAndServiceAuthShouldNotBeNull(
        MockHttpServletRequestBuilder requestBuilder) throws Exception {

        mockMvc.perform(requestBuilder);

        verifyNoInteractions(deserializer);
        verifyNoInteractions(authorisationService);
        verifyNoInteractions(ccdService);
    }

    public Object[] getEdgeScenariosForTheRequest() {
        String urlTemplate = "/ccdSubmittedEvent";
        MockHttpServletRequestBuilder requestWithS2SNull = post(urlTemplate)
            .contentType(MediaType.APPLICATION_JSON)
            .content("hi world!");

        MockHttpServletRequestBuilder requestWithNullContent = post(urlTemplate)
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "s2s");

        MockHttpServletRequestBuilder requestWithContentAndHeaderNull = post(urlTemplate)
            .contentType(MediaType.APPLICATION_JSON);

        return new Object[]{
            new Object[]{requestWithS2SNull},
            new Object[]{requestWithNullContent},
            new Object[]{requestWithContentAndHeaderNull}
        };
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

    @Test
    public void should_throw_exception_when_handler_fails() throws Exception {
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(deserializer.deserialize(anyString())).willReturn(callback);
        given(ccdCallbackHandler.handleValidationAndUpdate(
            ArgumentMatchers.any(),
            eq(IdamTokens.builder().idamOauth2Token(TEST_USER_AUTH_TOKEN).serviceAuthorization(TEST_SERVICE_AUTH_TOKEN).userId(TEST_USER_ID).build()))
        ).willThrow(RuntimeException.class);

        given(authorisationService.authenticate("test-header"))
            .willReturn("some-service");

        doNothing().when(authorisationService).assertIsAllowedToHandleCallback("some-service");

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
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(deserializer.deserialize(anyString())).willReturn(callback);
        willThrow(UnauthorizedException.class)
            .given(authorisationService).authenticate(null);

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
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(deserializer.deserialize(anyString())).willReturn(callback);
        willThrow(ForbiddenException.class)
            .given(authorisationService).authenticate(null);

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
