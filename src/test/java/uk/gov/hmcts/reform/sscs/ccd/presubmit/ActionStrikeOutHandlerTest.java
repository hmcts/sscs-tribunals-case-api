package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.assertj.core.api.Assertions;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.PoDetails;

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

    @Before
    public void setUp() {
        actionStrikeOutHandler = new ActionStrikeOutHandler();

        sscsCaseData = SscsCaseData.builder().build();

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
        "ACTION_STRIKE_OUT, strikeOut, STRIKE_OUT_ACTIONED",
        "ACTION_STRIKE_OUT, ,null",
        "ACTION_STRIKE_OUT, null,null",
    })
    public void givenEvent_thenSetDwpStateToExpected(EventType eventType, @Nullable String decisionType,
                                                     @Nullable DwpState expectedDwpState) {
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
    }

    @Test(expected = IllegalStateException.class)
    public void throwExceptionIfCannotHandleEventType() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);

        sscsCaseData = SscsCaseData.builder().dwpState(DwpState.IN_PROGRESS).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        actionStrikeOutHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenActionStrikeOut_thenClearPoFields() {
        when(callback.getEvent()).thenReturn(EventType.ACTION_STRIKE_OUT);
        sscsCaseData.setDecisionType("strikeOut");

        sscsCaseData.setPoAttendanceConfirmed(YES);
        sscsCaseData.setPresentingOfficersDetails(PoDetails.builder().name(Name.builder().build()).build());
        sscsCaseData.setPresentingOfficersHearingLink("link");

        actionStrikeOutHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertThat(sscsCaseData.getPoAttendanceConfirmed()).isEqualTo(NO);
        Assertions.assertThat(sscsCaseData.getPresentingOfficersDetails()).isEqualTo(PoDetails.builder().build());
        Assertions.assertThat(sscsCaseData.getPresentingOfficersHearingLink()).isNull();
    }
}
