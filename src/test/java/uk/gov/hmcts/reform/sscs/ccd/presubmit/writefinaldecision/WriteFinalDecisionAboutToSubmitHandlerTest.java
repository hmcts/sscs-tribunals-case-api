package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

public class WriteFinalDecisionAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private WriteFinalDecisionAboutToSubmitHandler handler;
    private SscsCaseData sscsCaseData;
    @Mock
    private DecisionNoticeService decisionNoticeService;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private DecisionNoticeOutcomeService decisionNoticeOutcomeService;
    @Mock
    private PreviewDocumentService previewDocumentService;
    @Mock
    private UserDetailsService userDetailsService;

    @Before
    public void setUp() {
        openMocks(this);

        handler = new WriteFinalDecisionAboutToSubmitHandler(decisionNoticeService, previewDocumentService, userDetailsService, false);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();
        sscsCaseData.setAppeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build());
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(decisionNoticeService.getOutcomeService(any(String.class))).thenReturn(decisionNoticeOutcomeService);
        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

    }

    @Test
    public void givenWriteFinalDecisionAboutToSubmitCallbackForEvent_shouldUpdatePreviousStateWhenCurrentStateIsNotReadyToListOrWithFta() {
        sscsCaseData.setPreviousState(State.VOID_STATE);
        sscsCaseData.setState(State.APPEAL_CREATED);
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getPreviousState(), is(State.APPEAL_CREATED));
    }

    @Test
    public void givenWriteFinalDecisionAboutToSubmitCallbackForEvent_shouldNotUpdatePreviousStateWhenCurrentStateIsReadyToList() {
        sscsCaseData.setPreviousState(State.VOID_STATE);
        sscsCaseData.setState(State.READY_TO_LIST);
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getPreviousState(), is(State.VOID_STATE));
    }

    @Test
    public void givenWriteFinalDecisionAboutToSubmitCallbackForEvent_shouldNotUpdatePreviousStateWhenCurrentStateIsWithFta() {
        sscsCaseData.setPreviousState(State.VOID_STATE);
        sscsCaseData.setState(State.WITH_DWP);
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getPreviousState(), is(State.VOID_STATE));
    }
}
