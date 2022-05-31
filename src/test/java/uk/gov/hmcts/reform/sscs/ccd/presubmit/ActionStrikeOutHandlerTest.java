package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.platform.commons.util.StringUtils;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;

@RunWith(JUnitParamsRunner.class)
public class ActionStrikeOutHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private ActionStrikeOutHandler actionStrikeOutHandler;

    private SscsCaseData sscsCaseData;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private ListAssistHearingMessageHelper hearingMessageHelper;

    @Before
    public void setUp() {
        actionStrikeOutHandler = new ActionStrikeOutHandler(hearingMessageHelper, false);

        sscsCaseData = SscsCaseData.builder()
                .schedulingAndListingFields(SchedulingAndListingFields.builder()
                        .hearingRoute(HearingRoute.LIST_ASSIST)
                        .build())
                .state(State.HEARING)
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }


    @Test
    @Parameters({
        "ACTION_STRIKE_OUT, ABOUT_TO_SUBMIT, true"
    })
    public void givenEvent_thenCanHandle(EventType eventType, CallbackType callbackType, boolean expected) {
        when(callback.getEvent()).thenReturn(eventType);
        assertEquals(expected, actionStrikeOutHandler.canHandle(callbackType, callback));
    }

    @Test(expected = NullPointerException.class)
    public void givenNullCallback_shouldThrowException() {
        actionStrikeOutHandler.canHandle(ABOUT_TO_SUBMIT, null);
    }

    @Test
    @Parameters({
        "ACTION_STRIKE_OUT, strikeOut, strikeOutActioned",
        "ACTION_STRIKE_OUT, ,null",
        "ACTION_STRIKE_OUT, null,null",
    })
    public void givenEvent_thenSetDwpStateToExpected_NoHearingsCancel(EventType eventType, @Nullable String decisionType,
                                                     @Nullable String expectedDwpState) {
        when(callback.getEvent()).thenReturn(eventType);
        sscsCaseData.setDecisionType(decisionType);

        PreSubmitCallbackResponse<SscsCaseData> response = actionStrikeOutHandler.handle(ABOUT_TO_SUBMIT, callback,
            USER_AUTHORISATION);

        assertThat(response.getData().getDwpState(), is(expectedDwpState));
        if (StringUtils.isBlank(decisionType)) {
            String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
            assertEquals("The decision type is not \"strike out\". We cannot proceed.", error);
        }
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    @Parameters({
            "ACTION_STRIKE_OUT, strikeOut, strikeOutActioned"
    })
    public void givenEvent_thenSetDwpStateToExpected_HearingsCancel(EventType eventType, @Nullable String decisionType,
                                                     @Nullable String expectedDwpState) {
        actionStrikeOutHandler = new ActionStrikeOutHandler(hearingMessageHelper, true);
        when(callback.getEvent()).thenReturn(eventType);
        sscsCaseData.setDecisionType(decisionType);

        PreSubmitCallbackResponse<SscsCaseData> response = actionStrikeOutHandler.handle(ABOUT_TO_SUBMIT, callback,
                USER_AUTHORISATION);

        assertThat(response.getData().getDwpState(), is(expectedDwpState));
        if (StringUtils.isBlank(decisionType)) {
            String error = response.getErrors().stream()
                    .findFirst()
                    .orElse("");
            assertEquals("The decision type is not \"strike out\". We cannot proceed.", error);
        }
        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()));
        verifyNoMoreInteractions(hearingMessageHelper);
    }

    @Test(expected = IllegalStateException.class)
    public void throwExceptionIfCannotHandleEventType() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);

        sscsCaseData = SscsCaseData.builder().dwpState("someValue").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        actionStrikeOutHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

}