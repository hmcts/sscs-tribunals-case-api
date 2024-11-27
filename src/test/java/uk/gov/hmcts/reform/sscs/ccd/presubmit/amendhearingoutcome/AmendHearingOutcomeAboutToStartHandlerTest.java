package uk.gov.hmcts.reform.sscs.ccd.presubmit.amendhearingoutcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collections;
import junitparams.JUnitParamsRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;


@RunWith(JUnitParamsRunner.class)
public class AmendHearingOutcomeAboutToStartHandlerTest {
    private AmendHearingOutcomeAboutToStartHandler handler;
    private static final String USER_AUTHORISATION = "Bearer token";
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setup() {
        openMocks(this);
        handler = new AmendHearingOutcomeAboutToStartHandler();
        when(callback.getEvent()).thenReturn(EventType.AMEND_HEARING_OUTCOME);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .hearingOutcomeValue(HearingOutcomeValue.builder().build())
                .hearingOutcomes(Collections.singletonList((HearingOutcome.builder().build())))
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenAmendHearingOutcomeEventShouldHandle() {
        assertThat(handler.canHandle(CallbackType.ABOUT_TO_START,callback)).isTrue();
    }

    @Test
    void givenHearingOutcomeOnCase_ThenDontReturnError() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        assertThat(response.getData().getHearingOutcomes()).isNotEmpty();
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenNoHearingOutcomesOnCase_ThenReturnError() {
        sscsCaseData.setHearingOutcomes(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        assertThat(response.getErrors()).isNotEmpty();
        assertThat(response.getErrors()).contains("There are no hearing outcomes on the case.");
    }

}