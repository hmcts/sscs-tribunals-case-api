package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

public class DwpUploadResponseAboutToStartHandlerTest {

    private DwpUploadResponseAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private DwpAddressLookupService dwpAddressLookupService;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
        dwpAddressLookupService = new DwpAddressLookupService();
        handler = new DwpUploadResponseAboutToStartHandler(dwpAddressLookupService);

        when(callback.getEvent()).thenReturn(EventType.DWP_UPLOAD_RESPONSE);

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenADwpUploadResponseEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void populateOriginatingAndPresentingOfficeDropdownsWhenHandlerFires_withCorrectSelectedOffice() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback);

        assertEquals("DWP PIP (3)", response.getData().getDwpOriginatingOffice().getValue().getCode());
        assertEquals("DWP PIP (3)", response.getData().getDwpPresentingOffice().getValue().getCode());
    }

    @Test
    public void givenMrnIsNull_populateOriginatingAndPresentingOfficeDropdownsWhenHandlerFires_withDefaultSelectedOffice() {
        callback.getCaseDetails().getCaseData().getAppeal().setMrnDetails(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback);

        assertEquals("DWP PIP (1)", response.getData().getDwpOriginatingOffice().getValue().getCode());
        assertEquals("DWP PIP (1)", response.getData().getDwpPresentingOffice().getValue().getCode());
    }

}