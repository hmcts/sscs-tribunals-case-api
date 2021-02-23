package uk.gov.hmcts.reform.sscs.ccd.presubmit.supplementaryresponse;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;


@RunWith(JUnitParamsRunner.class)
public class SupplementaryResponseMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private SupplementaryResponseMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new SupplementaryResponseMidEventHandler();

        when(callback.getEvent()).thenReturn(EventType.DWP_SUPPLEMENTARY_RESPONSE);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED"})
    public void givenANonSupplementaryResponseEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenASupplementaryResponseEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }


    @Test
    public void givenASupplementaryResponseWithDwpSupplementaryResponseDocAndAudioVideoDwpOtherDoc_thenSetShowRipDocumentPage() {
        sscsCaseData.setDwpSupplementaryResponseDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test1.doc").documentUrl("myurl1").build()).build());
        sscsCaseData.setDwpOtherDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test2.mp3").documentUrl("myurl2").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.YES, response.getData().getShowRip1DocPage());
    }

    @Test
    public void givenASupplementaryResponseWithDwpSupplementaryResponseDocAndNonAudioVideoDwpOtherDoc_thenHideShowRipDocumentPage() {
        sscsCaseData.setDwpSupplementaryResponseDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test1.doc").documentUrl("myurl1").build()).build());
        sscsCaseData.setDwpOtherDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test2.pdf").documentUrl("myurl2").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.NO, response.getData().getShowRip1DocPage());
    }

    @Test
    public void givenASupplementaryResponseWithDwpSupplementaryResponseDocWithoutDwpOtherDoc_thenHideShowRipDocumentPage() {
        sscsCaseData.setDwpSupplementaryResponseDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test1.doc").documentUrl("myurl1").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.NO, response.getData().getShowRip1DocPage());
    }
}
