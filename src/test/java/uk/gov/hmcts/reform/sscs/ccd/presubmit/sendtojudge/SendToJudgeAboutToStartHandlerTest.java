package uk.gov.hmcts.reform.sscs.ccd.presubmit.sendtojudge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.TCW_REFER_TO_JUDGE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@ExtendWith(MockitoExtension.class)
public class SendToJudgeAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private SendToJudgeAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    public void setUp() {
        handler = new SendToJudgeAboutToStartHandler(false);
        sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build())
                .prePostHearing(PrePostHearing.POST)
                .build();
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }


    @Test
    void givenPostHearingsBisNotEnabled_thenDontCleanPrePostHearingField() {
        setupCallback();
        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(result.getData().getPrePostHearing(), PrePostHearing.POST);
    }

    @Test
    void givenPostHearingsBisEnabled_thenCleanPrePostHearingField() {
        setupCallback();
        ReflectionTestUtils.setField(handler, "postHearingsB", true);
        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertNull(result.getData().getPrePostHearing());
    }

    private void setupCallback() {
        when(callback.getEvent()).thenReturn(TCW_REFER_TO_JUDGE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

}
