package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

@ExtendWith(MockitoExtension.class)
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
    private PreviewDocumentService previewDocumentService;
    @Mock
    private DecisionNoticeOutcomeService decisionNoticeOutcomeService;

    @BeforeEach
    public void setUp() {
        handler = new WriteFinalDecisionAboutToSubmitHandler(decisionNoticeService, previewDocumentService);
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
        assertThat(response.getData().getPreviousState()).isEqualTo(State.APPEAL_CREATED);
    }

    @Test
    public void givenWriteFinalDecisionAboutToSubmitCallbackForEvent_shouldNotUpdatePreviousStateWhenCurrentStateIsReadyToList() {
        sscsCaseData.setPreviousState(State.VOID_STATE);
        sscsCaseData.setState(State.READY_TO_LIST);
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getPreviousState()).isEqualTo(State.VOID_STATE);
    }

    @Test
    public void givenWriteFinalDecisionAboutToSubmitCallbackForEvent_shouldNotUpdatePreviousStateWhenCurrentStateIsWithFta() {
        sscsCaseData.setPreviousState(State.VOID_STATE);
        sscsCaseData.setState(State.WITH_DWP);
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getPreviousState()).isEqualTo(State.VOID_STATE);
    }

    @Test
    public void givenPostHearingsDisabled_shouldNotUpdateFinalDecisionGeneratedDate() {
        ReflectionTestUtils.setField(handler, "isPostHearingsEnabled", false);
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getFinalDecisionGeneratedDate()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = YesNo.class, names = "NO")
    @NullSource
    public void givenPostHearingsEnabled_andNotACorrection_shouldUpdateFinalDecisionGeneratedDate(YesNo yesNo) {
        ReflectionTestUtils.setField(handler, "isPostHearingsEnabled", true);
        sscsCaseData.getPostHearing().getCorrection().setCorrectionFinalDecisionInProgress(yesNo);
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getFinalDecisionGeneratedDate()).isEqualTo(LocalDate.now());
    }

    @Test
    public void givenPostHearingsEnabled_andIsACorrection_shouldNotUpdateFinalDecisionGeneratedDate() {
        ReflectionTestUtils.setField(handler, "isPostHearingsEnabled", true);
        sscsCaseData.getPostHearing().getCorrection().setCorrectionFinalDecisionInProgress(YesNo.YES);
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getFinalDecisionGeneratedDate()).isNull();
    }
}
