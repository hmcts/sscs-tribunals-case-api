package uk.gov.hmcts.reform.sscs.ccd.presubmit.createwelshnotice;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;

@RunWith(JUnitParamsRunner.class)
public class CreateWelshNoticeAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private CreateWelshNoticeAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new CreateWelshNoticeAboutToStartHandler();
        when(callback.getEvent()).thenReturn(EventType.CREATE_WELSH_NOTICE);
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAUploadWelshDocumentEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenANonUploadWelshDocument_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test(expected = NullPointerException.class)
    public void givenNullCallback_shouldThrowException() {
        handler.handle(CallbackType.ABOUT_TO_SUBMIT, null, "user token");
    }

    @Test
    public void originalDocumentDropDownWhenNoDocumentWithSscsDocumentTranslationStatus() {
        sscsCaseData = CaseDataUtils.buildMinimalCaseData();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(response.getData().getDocumentTypes().getValue().getCode(),"-");
        assertEquals(response.getData().getOriginalNoticeDocuments().getValue().getCode(),"-");
    }

    @Test
    @Parameters({"DIRECTION_NOTICE", "ADJOURNMENT_NOTICE", "DECISION_NOTICE", "AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE", "POSTPONEMENT_REQUEST_DIRECTION_NOTICE"})
    public void originalDocumentDropDownWhenSscsDocumentTranslationStatusIsSet(DocumentType documentType) {
        sscsCaseData = SscsCaseData.builder()
                .sscsDocument(getSscsDocument(documentType))
                .appeal(Appeal.builder().build())
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(response.getData().getDocumentTypes().getValue().getCode(),documentType.getValue());
        assertEquals(response.getData().getDocumentTypes().getValue().getLabel(),documentType.getLabel());
        assertEquals(response.getData().getOriginalNoticeDocuments().getValue().getCode(),"test.pdf");
    }


    public List<SscsDocument> getSscsDocument(DocumentType documentType) {
        SscsDocument sscs1Doc = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentLink(DocumentLink.builder()
                                .documentUrl("/anotherUrl")
                                .documentFilename("test.pdf")
                                .build())
                        .documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED)
                        .documentType(documentType.getValue())
                        .build())
                .build();

        return singletonList(sscs1Doc);
    }
}
