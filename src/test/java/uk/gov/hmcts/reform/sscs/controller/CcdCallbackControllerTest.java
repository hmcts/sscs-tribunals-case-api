package uk.gov.hmcts.reform.sscs.controller;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ACTION_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INTERLOC_INFORMATION_RECEIVED;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@RunWith(JUnitParamsRunner.class)
@WebMvcTest(CcdCallbackController.class)
public class CcdCallbackControllerTest {

    // begin: needed to use spring runner and junitparamsRunner together
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    // end

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("PMD.UnusedPrivateField")
    @MockBean
    private AuthorisationService authorisationService;

    @MockBean
    private SscsCaseCallbackDeserializer deserializer;

    @MockBean
    private Callback<SscsCaseData> caseDataCallback;

    @MockBean
    private PreSubmitCallbackDispatcher dispatcher;

    @MockBean
    private CcdService ccdService;

    @MockBean
    private IdamService idamService;

    @Test
    public void handleCcdAboutToStartCallbackAndUpdateCaseData() throws Exception {
        String path = getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json").getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();
        when(deserializer.deserialize(content)).thenReturn(new Callback<>(
            new CaseDetails<>(1234L, "SSCS", State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now()),
            Optional.empty(),
            ACTION_FURTHER_EVIDENCE));

        PreSubmitCallbackResponse response = new PreSubmitCallbackResponse(SscsCaseData.builder().originalSender(
            new DynamicList(new DynamicListItem("1", "2"), null)).build());

        when(dispatcher.handle(any(CallbackType.class), any()))
            .thenReturn(response);

        mockMvc.perform(post("/ccdAboutToStart")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "")
            .content(content))
            .andExpect(status().isOk())
            .andExpect(content().json("{'data': {'originalSender': {'value': {'code': '1', 'label': '2'}}}}"));
    }

    @Test
    public void handleCcdAboutToSubmitCallbackAndUpdateCaseData() throws Exception {
        String path = getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json").getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();
        when(deserializer.deserialize(content)).thenReturn(new Callback<>(
            new CaseDetails<>(1234L, "SSCS", State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now()),
            Optional.empty(),
            INTERLOC_INFORMATION_RECEIVED));

        PreSubmitCallbackResponse response = new PreSubmitCallbackResponse(SscsCaseData.builder().interlocReviewState("new_state").build());
        when(dispatcher.handle(any(CallbackType.class), any()))
            .thenReturn(response);

        mockMvc.perform(post("/ccdAboutToSubmit")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "")
            .content(content))
            .andExpect(status().isOk())
            .andExpect(content().json("{'data': {'interlocReviewState': 'new_state'}}"));
    }

    @Test
    @Parameters(method = "getEdgeScenariosForTheRequest")
    public void givenCcdSubmittedEventCallbackForActionFurtherEvidenceEvent_messageAndServiceAuthShouldNotBeNull(
        MockHttpServletRequestBuilder requestBuilder) throws Exception {

        mockMvc.perform(requestBuilder);

        then(deserializer).shouldHaveZeroInteractions();
        then(authorisationService).shouldHaveZeroInteractions();
        then(ccdService).shouldHaveZeroInteractions();
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
}