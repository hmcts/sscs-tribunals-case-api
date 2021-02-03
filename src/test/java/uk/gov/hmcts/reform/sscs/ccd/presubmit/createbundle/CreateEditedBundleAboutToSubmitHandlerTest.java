package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.*;

import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;


@RunWith(JUnitParamsRunner.class)
public class CreateEditedBundleAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private CreateEditedBundleAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private ServiceRequestExecutor serviceRequestExecutor;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new CreateEditedBundleAboutToSubmitHandler(serviceRequestExecutor, "bundleUrl.com",
                "bundleEditedConfig", "bundleWelshEditedConfig", false);

        when(callback.getEvent()).thenReturn(EventType.CREATE_EDITED_BUNDLE);

        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(serviceRequestExecutor.post(any(), any())).thenReturn(new PreSubmitCallbackResponse<>(sscsCaseData));

        callback.getCaseDetails().getCaseData().setPhmeGranted(YesNo.YES);
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonCreateBundleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenDwpEditedResponseDocumentHasEmptyFileName_thenPopulateFileName() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument((DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()));
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(DWP_DOCUMENT_EDITED_RESPONSE_FILENAME_PREFIX, response.getData().getDwpEditedResponseDocument().getDocumentFileName());
    }

    @Test
    public void givenDwpEditedEvidenceDocumentHasEmptyFileName_thenPopulateFileName() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(DWP_DOCUMENT_EDITED_EVIDENCE_FILENAME_PREFIX, response.getData().getDwpEditedEvidenceBundleDocument().getDocumentFileName());
    }

    @Test
    @Parameters({"Yes, bundleWelshEditedConfig", " No, bundleEditedConfig"})
    public void givenWelsh_thenPopulateWelshConfigFileName(String languagePreference, String configFile) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        caseData.setDwpEditedResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        caseData.setLanguagePreferenceWelsh(languagePreference);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        if (caseData.isLanguagePreferenceWelsh()) {
            assertEquals(configFile, response.getData().getBundleConfiguration());
        } else {
            assertEquals(configFile, response.getData().getBundleConfiguration());
        }
    }

    @Test
    public void givenSscsDocumentHasEmptyFileName_thenPopulateFileName() {
        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName(null).documentLink(
            DocumentLink.builder().documentFilename("test.com").build()).build()).build();
        List<SscsDocument> docs = new ArrayList<>();

        docs.add(sscsDocument);

        callback.getCaseDetails().getCaseData().setSscsDocument(docs);
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("test.com", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenCreateBundleEvent_thenTriggerTheExternalCreateBundleEvent() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
    }

    @Test
    public void givenEmptyDwpEditedEvidenceBundleDocumentLink_thenReturnError() {

        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder().build());
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
            .findFirst()
            .orElse("");
        assertEquals("The edited bundle cannot be created as mandatory edited DWP documents are missing", error);
    }

    @Test
    public void givenEmptyDwpEditedResponseDocumentLink_thenReturnError() {

        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
            .findFirst()
            .orElse("");
        assertEquals("The edited bundle cannot be created as mandatory edited DWP documents are missing", error);
    }

    @Test
    public void givenPhmeNoReviewDwpEditedResponseDocumentLink_thenReturnError() {

        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpPhme("Yes");
        callback.getCaseDetails().getCaseData().setPhmeGranted(YesNo.NO);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("The edited bundle cannot be created as PHME status has not been granted", error);
    }

    @Test
    public void givenPhmeNullReviewDwpEditedResponseDocumentLink_thenReturnError() {

        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpPhme("Yes");
        callback.getCaseDetails().getCaseData().setPhmeGranted(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("The edited bundle cannot be created as PHME status has not been granted", error);
    }


}