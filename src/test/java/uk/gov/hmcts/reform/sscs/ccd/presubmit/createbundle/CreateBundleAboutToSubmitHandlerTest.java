package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_EVIDENCE_BUNDLE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_RESPONSE;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX;

import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;

@RunWith(JUnitParamsRunner.class)
public class CreateBundleAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private CreateBundleAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private ServiceRequestExecutor serviceRequestExecutor;

    private SscsCaseData sscsCaseData;

    private DwpDocumentService dwpDocumentService;


    @Before
    public void setUp() {
        openMocks(this);
        dwpDocumentService = new DwpDocumentService();
        handler = new CreateBundleAboutToSubmitHandler(serviceRequestExecutor, dwpDocumentService,"bundleUrl.com", "bundleEnglishConfig", "bundleWelshConfig",
                "bundleUnEditedConfig", "bundleWelshUnEditedConfig",
                "bundleNewEnglishConfig", "bundleNewWelshConfig",
                "bundleNewUnEditedConfig", "bundleNewWelshUnEditedConfig", true);

        when(callback.getEvent()).thenReturn(EventType.CREATE_BUNDLE);

        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(serviceRequestExecutor.post(any(), any())).thenReturn(new PreSubmitCallbackResponse<>(sscsCaseData));
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
    public void givenDwpResponseDocumentHasEmptyFileName_thenPopulateFileName() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(DWP_RESPONSE.getLabel(), response.getData().getDwpDocuments().stream().filter(e -> e.getValue().getDocumentType().equals(DWP_RESPONSE.getValue())).collect(toList()).get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenDwpResponseDocumentHasEmptyFileNameWithDwpDocumentsBundleFeatureFalse_thenPopulateFileName() {
        ReflectionTestUtils.setField(handler, "dwpDocumentsBundleFeature", false);

        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX, response.getData().getDwpResponseDocument().getDocumentFileName());
    }

    @Test
    public void givenDwpEvidenceDocumentHasEmptyFileName_thenPopulateFileName() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(DWP_EVIDENCE_BUNDLE.getLabel(), response.getData().getDwpDocuments().stream().filter(e -> e.getValue().getDocumentType().equals(DWP_EVIDENCE_BUNDLE.getValue())).collect(toList()).get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenDwpEvidenceDocumentHasEmptyFileNameWithDwpDocumentsBundleFeatureFalse_thenPopulateFileName() {
        ReflectionTestUtils.setField(handler, "dwpDocumentsBundleFeature", false);

        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX, response.getData().getDwpEvidenceBundleDocument().getDocumentFileName());
    }

    @Test
    @Parameters({"Yes, bundleNewWelshConfig", " No, bundleNewEnglishConfig"})
    public void givenWelshCase_thenPopulateWelshConfigFileName(String languagePreference, String configFile) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        caseData.setDwpDocuments(dwpDocuments);

        caseData.setLanguagePreferenceWelsh(languagePreference);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(configFile, response.getData().getBundleConfiguration());
    }


    @Test
    @Parameters({"Yes, bundleWelshConfig", " No, bundleEnglishConfig"})
    public void givenWelshCaseWithDwpDocumentsBundleFeatureFalse_thenPopulateWelshConfigFileName(String languagePreference, String configFile) {
        ReflectionTestUtils.setField(handler, "dwpDocumentsBundleFeature", false);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        caseData.setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        caseData.setLanguagePreferenceWelsh(languagePreference);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(configFile, response.getData().getBundleConfiguration());
    }

    @Test
    public void givenSscsDocumentHasEmptyFileName_thenPopulateFileName() {

        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName(null).documentLink(
            DocumentLink.builder().documentFilename("test.com").build()).build()).build();
        List<SscsDocument> docs = new ArrayList<>();

        docs.add(sscsDocument);

        callback.getCaseDetails().getCaseData().setSscsDocument(docs);

        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("test.com", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenCreateBundleEvent_thenTriggerTheExternalCreateBundleEvent() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
    }

    @Test
    public void givenCreateBundleEventWithDwpDocumentsBundleFeatureFalse_thenTriggerTheExternalCreateBundleEvent() {
        ReflectionTestUtils.setField(handler, "dwpDocumentsBundleFeature", false);

        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
    }

    @Test
    public void givenEmptyDwpEvidenceBundleDocumentLinkWithDwpDocumentsPattern_thenReturnError() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("The bundle cannot be created as mandatory DWP documents are missing", error);
    }

    @Test
    public void givenEmptyDwpEvidenceBundleDocumentLinkWithOldPattern_thenReturnError() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("The bundle cannot be created as mandatory DWP documents are missing", error);
    }

    @Test
    public void givenEmptyDwpEvidenceBundleDocumentLinkWithDwpDocumentsBundleFeatureFalse_thenReturnError() {
        ReflectionTestUtils.setField(handler, "dwpDocumentsBundleFeature", false);

        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
            .findFirst()
            .orElse("");
        assertEquals("The bundle cannot be created as mandatory DWP documents are missing", error);
    }

    @Test
    public void givenEmptyDwpResponseDocumentLinkWithDwpDocumentsPattern_thenReturnError() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("The bundle cannot be created as mandatory DWP documents are missing", error);
    }

    @Test
    public void givenEmptyDwpResponseDocumentLinkWithOldPattern_thenReturnError() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("The bundle cannot be created as mandatory DWP documents are missing", error);
    }

    @Test
    public void givenEmptyDwpResponseDocumentLinkWithDwpDocumentsBundleFeatureFalse_thenReturnError() {
        ReflectionTestUtils.setField(handler, "dwpDocumentsBundleFeature", false);

        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
            .findFirst()
            .orElse("");
        assertEquals("The bundle cannot be created as mandatory DWP documents are missing", error);
    }

    @Test
    @Parameters({"Yes, bundleNewWelshUnEditedConfig", " No, bundleNewUnEditedConfig"})
    public void givenWelshWithEdited_thenPopulateUneditedWelshConfigFileName(String languagePreference, String configFile) {

        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(languagePreference);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(configFile, response.getData().getBundleConfiguration());
    }

    @Test
    @Parameters({"Yes, bundleWelshUnEditedConfig", " No, bundleUnEditedConfig"})
    public void givenWelshWithEditedWithDwpDocumentsBundleFeatureFalse_thenPopulateUneditedWelshConfigFileName(String languagePreference, String configFile) {
        ReflectionTestUtils.setField(handler, "dwpDocumentsBundleFeature", false);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        caseData.setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());

        caseData.setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        caseData.setDwpEditedResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        caseData.setLanguagePreferenceWelsh(languagePreference);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(configFile, response.getData().getBundleConfiguration());
    }
}