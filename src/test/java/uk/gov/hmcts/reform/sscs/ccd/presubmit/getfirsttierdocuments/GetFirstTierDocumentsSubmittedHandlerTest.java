package uk.gov.hmcts.reform.sscs.ccd.presubmit.getfirsttierdocuments;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdCallbackMap;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;

@RunWith(JUnitParamsRunner.class)
public class GetFirstTierDocumentsSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private GetFirstTierDocumentsSubmittedHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CcdCallbackMapService ccdCallbackMapService;

    private SscsCaseData sscsCaseData;

    private final ArgumentCaptor<CcdCallbackMap> capture = ArgumentCaptor.forClass(CcdCallbackMap.class);


    @Before
    public void setUp() {
        openMocks(this);
        handler = new GetFirstTierDocumentsSubmittedHandler(ccdCallbackMapService, true);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("1").build();

        when(callback.getEvent()).thenReturn(EventType.GET_FIRST_TIER_DOCUMENTS);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(ccdCallbackMapService.handleCcdCallbackMap(capture.capture(), eq(sscsCaseData))).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenANonCreateBundleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void shouldUpdateEvent() {
        when(callback.getEvent()).thenReturn(EventType.GET_FIRST_TIER_DOCUMENTS);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(ccdCallbackMapService).handleCcdCallbackMap(capture.capture(), eq(sscsCaseData));
        assertEquals(EventType.BUNDLE_CREATED_FOR_UPPER_TRIBUNAL, capture.getValue().getCallbackEvent());
        assertEquals("Bundle created for UT", capture.getValue().getCallbackSummary());
        assertEquals("Bundle created for UT", capture.getValue().getCallbackDescription());
    }
}
