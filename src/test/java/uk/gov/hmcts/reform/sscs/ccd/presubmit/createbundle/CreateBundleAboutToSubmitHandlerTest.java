package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_EVIDENCE_BUNDLE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_RESPONSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;
import uk.gov.hmcts.reform.sscs.service.bundle.BundleAudioVideoPdfService;

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

    @Mock
    private BundleAudioVideoPdfService bundleAudioVideoPdfService;

    private SscsCaseData sscsCaseData;

    private DwpDocumentService dwpDocumentService;

    @Before
    public void setUp() {
        openMocks(this);
        dwpDocumentService = new DwpDocumentService();
        handler = new CreateBundleAboutToSubmitHandler(serviceRequestExecutor, dwpDocumentService, bundleAudioVideoPdfService, "bundleUrl.com", "bundleEnglishConfig", "bundleWelshConfig",
                "bundleEnglishEditedConfig", "bundleWelshEditedConfig");

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
    @Parameters({"Yes, bundleWelshConfig", " No, bundleEnglishConfig"})
    public void givenCaseWithLanguagePreference_thenPopulateConfigFileName(String languagePreference, String expectedConfigFile) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        caseData.setDwpDocuments(dwpDocuments);

        caseData.setLanguagePreferenceWelsh(languagePreference);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(expectedConfigFile, response.getData().getMultiBundleConfiguration().get(0).getValue());
    }

    @Test
    public void givenEnglishCaseWithEdited_thenPopulateEnglishEditedAndUneditedConfigFileName() {
        addMandatoryDwpDocuments();

        callback.getCaseDetails().getCaseData().setDwpPhme(YES.getValue());
        callback.getCaseDetails().getCaseData().setPhmeGranted(YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(NO.getValue());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
        assertEquals(2, response.getData().getMultiBundleConfiguration().size());
        assertEquals("bundleEnglishEditedConfig", response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("bundleEnglishConfig", response.getData().getMultiBundleConfiguration().get(1).getValue());
    }

    @Test
    public void givenWelshCaseWithEdited_thenPopulateWelshEditedAndUneditedConfigFileName() {
        addMandatoryDwpDocuments();

        callback.getCaseDetails().getCaseData().setDwpPhme(YES.getValue());
        callback.getCaseDetails().getCaseData().setPhmeGranted(YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(YES.getValue());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
        assertEquals(2, response.getData().getMultiBundleConfiguration().size());
        assertEquals("bundleWelshEditedConfig", response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("bundleWelshConfig", response.getData().getMultiBundleConfiguration().get(1).getValue());
    }

    @Test
    @Parameters({"Yes, bundleWelshConfig", " No, bundleEnglishConfig"})
    public void givenCaseWithEditedDwpDocsAndPhmeNotGranted_thenReturnErrorMessageAndDoNotSendRequestToBundleService(String languagePreference, String expectedConfigFile) {
        addMandatoryDwpDocuments();
        addNonEditedSscsDocuments();
        sscsCaseData.setIsConfidentialCase(NO);

        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(languagePreference);
        callback.getCaseDetails().getCaseData().setDwpPhme(YES.getValue());
        callback.getCaseDetails().getCaseData().setPhmeGranted(NO);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
        assertEquals(1, response.getData().getMultiBundleConfiguration().size());
        assertEquals(expectedConfigFile, response.getData().getMultiBundleConfiguration().get(0).getValue());
    }

    @Test
    public void givenDwpResponseDocumentHasEmptyFileName_thenPopulateFileName() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(DWP_RESPONSE.getLabel(), response.getData().getDwpDocuments().stream().filter(e -> e.getValue().getDocumentType().equals(DWP_RESPONSE.getValue())).collect(toList()).get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenDwpEvidenceDocumentHasEmptyFileName_thenPopulateFileName() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(DWP_EVIDENCE_BUNDLE.getLabel(), response.getData().getDwpDocuments().stream().filter(e -> e.getValue().getDocumentType().equals(DWP_EVIDENCE_BUNDLE.getValue())).collect(toList()).get(0).getValue().getDocumentFileName());
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
    public void givenCreateBundleEventWithAudioVideoEvidence_thenTriggerTheExternalCreateBundleEvent() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        List<SscsDocument> audioVideoEvidences = new ArrayList<>();
        audioVideoEvidences.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentDateAdded(LocalDate.now().toString())
                .documentLink(DocumentLink.builder().documentFilename("Myfilename.mp3").documentUrl("dm-store-url/123").documentBinaryUrl("dm-store-url/123/binary").build()).build())
                .build());
        caseDetails.getCaseData().setSscsDocument(audioVideoEvidences);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(bundleAudioVideoPdfService).createAudioVideoPdf(sscsCaseData);
        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
    }

    @Test
    public void givenCaseWithEditedDwpDocsAndPhmeUnderReview_thenReturnErrorMessageAndDoNotSendRequestToBundleService() {
        addMandatoryDwpDocuments();

        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(NO.getValue());
        callback.getCaseDetails().getCaseData().setDwpPhme(YES.getValue());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("There is a pending PHME request on this case", error);
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    @Parameters({"appellant, YES", "appellant, NO", "jointParty, YES", "jointParty, NO"})
    public void givenCaseWithPendingEnhancedConfidentiality_thenReturnErrorMessage(String party, YesNo phmeGranted) {
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(NO.getValue());
        if (party.equals("appellant")) {
            callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeAppellant(getDatedRequestOutcome(RequestOutcome.IN_PROGRESS));
        } else {
            callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeJointParty(getDatedRequestOutcome(RequestOutcome.IN_PROGRESS));
        }

        if (isYes(phmeGranted)) {
            sscsCaseData.setDwpPhme(phmeGranted.getValue());
            sscsCaseData.setPhmeGranted(phmeGranted);
            addMandatoryDwpDocuments();
        } else {
            addMandatoryNonEditedDwpDocuments();
        }

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertEquals("There is a pending enhanced confidentiality request on this case", response.getErrors().iterator().next());
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    @Parameters({"appellant", "jointParty"})
    public void givenCaseWithPendingEnhancedConfidentialityAndPendingPhmeRequest_thenReturnTwoErrorMessages(String party) {
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(NO.getValue());
        if (party.equals("appellant")) {
            sscsCaseData.setConfidentialityRequestOutcomeAppellant(getDatedRequestOutcome(RequestOutcome.IN_PROGRESS));
        } else {
            sscsCaseData.setConfidentialityRequestOutcomeJointParty(getDatedRequestOutcome(RequestOutcome.IN_PROGRESS));
        }
        callback.getCaseDetails().getCaseData().setDwpPhme(YES.getValue());

        addMandatoryDwpDocuments();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(2));
        assertEquals("There is a pending PHME request on this case", response.getErrors().toArray()[0]);
        assertEquals("There is a pending enhanced confidentiality request on this case", response.getErrors().toArray()[1]);
        verifyNoInteractions(serviceRequestExecutor);
    }

    private DatedRequestOutcome getDatedRequestOutcome(RequestOutcome outcome) {
        return DatedRequestOutcome.builder().requestOutcome(outcome).build();
    }

    @Test
    @Parameters({"No, bundleEnglishEditedConfig, bundleEnglishConfig", "Yes, bundleWelshEditedConfig, bundleWelshConfig"})
    public void givenEnhancedConfidentialityCaseWithEditedDocuments_thenPopulateEditedAndUneditedConfigFileName(String langPreference, String expectedBundleConfig1, String expectedBundleConfig2) {
        addMandatoryNonEditedDwpDocuments();
        addEditedSscsDocuments();

        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(langPreference);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
        assertEquals(2, response.getData().getMultiBundleConfiguration().size());
        assertEquals(expectedBundleConfig1, response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals(expectedBundleConfig2, response.getData().getMultiBundleConfiguration().get(1).getValue());
    }

    @Test
    public void givenPhmeGrantedAndEnhancedConfidentiality_thenPopulateEditedAndUneditedConfigFilename() {
        addMandatoryDwpDocuments();
        addEditedSscsDocuments();
        sscsCaseData.setDwpPhme(YES.getValue());
        sscsCaseData.setPhmeGranted(YES);
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
        assertEquals(0, response.getWarnings().size());
        assertEquals(2, response.getData().getMultiBundleConfiguration().size());
        assertEquals("bundleEnglishEditedConfig", response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("bundleEnglishConfig", response.getData().getMultiBundleConfiguration().get(1).getValue());
    }

    @Test
    public void givenDocumentsWithSameAddition_thenShowWarning() {

        List<SscsDocument> documents = new ArrayList<>();
        documents.add(
            SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("test.pdf").editedDocumentLink(DocumentLink.builder().documentFilename("test.pdf").build()).bundleAddition("A").build()).build()
        );
        documents.add(
            SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("test2.pdf").editedDocumentLink(DocumentLink.builder().documentFilename("test2.pdf").build()).bundleAddition("B").build()).build()
        );
        documents.add(
            SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("test3.pdf").editedDocumentLink(DocumentLink.builder().documentFilename("test3.pdf").build()).bundleAddition("a").build()).build()
        );

        sscsCaseData.setSscsDocument(documents);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getWarnings().size());
        assertEquals("Some documents in this Bundle contain the same addition letter. Are you sure you want to proceed?", response.getWarnings().toArray()[0]);
    }

    @Test
    @Parameters({"Yes, bundleWelshConfig", "No, bundleEnglishConfig"})
    public void givenEnhancedConfidentialityCaseWithNoEditedDocs_thenPopulateUneditedConfigFileName(String langPreference, String expectedBundleName) {
        addMandatoryNonEditedDwpDocuments();

        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(langPreference);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
        assertEquals(1, response.getData().getMultiBundleConfiguration().size());
        assertEquals(expectedBundleName, response.getData().getMultiBundleConfiguration().get(0).getValue());
    }

    @Test
    @Parameters({"Yes, bundleWelshConfig", "No, bundleEnglishConfig"})
    public void givenEnhancedConfidentialityCaseAndPhmeGrantedWithNoEditedDocs_thenPopulateUneditedConfigFileName(String langPreference, String expectedBundleName) {
        addMandatoryNonEditedDwpDocuments();
        addNonEditedSscsDocuments();
        callback.getCaseDetails().getCaseData().setPhmeGranted(YES);
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(langPreference);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
        assertEquals(1, response.getData().getMultiBundleConfiguration().size());
        assertEquals(expectedBundleName, response.getData().getMultiBundleConfiguration().get(0).getValue());
    }

    private void addEditedSscsDocuments() {
        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("test.pdf").editedDocumentLink(DocumentLink.builder().documentFilename("test.pdf").build()).build()).build();
        sscsCaseData.setSscsDocument(Collections.singletonList(sscsDocument));
    }

    private void addNonEditedSscsDocuments() {
        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("test.pdf").build()).build();
        sscsCaseData.setSscsDocument(Collections.singletonList(sscsDocument));
    }

    @Test
    public void givenCaseWithPreviouslyCreatedBundles_thenClearAllBundles() {
        addMandatoryDwpDocuments();
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(NO.getValue());
        callback.getCaseDetails().getCaseData().setPhmeGranted(YES);

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(Bundle.builder().value(BundleDetails.builder().build()).build());
        callback.getCaseDetails().getCaseData().setCaseBundles(bundles);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
        assertNull(response.getData().getCaseBundles());
    }


    private void addMandatoryDwpDocuments() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);
    }

    private void addMandatoryNonEditedDwpDocuments() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);
    }

}
