package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class ClearHmctsDwpStateHandlerTest {

    private ClearHmctsDwpStateHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new ClearHmctsDwpStateHandler();

        when(callback.getEvent()).thenReturn(EventType.SEND_TO_DWP_OFFLINE);

        sscsCaseData = SscsCaseData.builder().hmctsDwpState("failedSending").build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenASendToDwpOfflineEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(callback));
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(callback));
    }

    @Test
    public void givenACaseWithHmctsDwpStatePopulated_thenClearTheValue() {
        assertNotNull(callback.getCaseDetails().getCaseData().getHmctsDwpState());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        assertNull(response.getData().getHmctsDwpState());
    }
}