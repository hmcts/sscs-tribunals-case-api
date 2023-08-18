package uk.gov.hmcts.reform.sscs.bundling;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Bundle;
import uk.gov.hmcts.reform.sscs.ccd.domain.BundleDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DatedRequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpResponseDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;
import uk.gov.hmcts.reform.sscs.service.bundle.BundleAudioVideoPdfService;

@RunWith(JUnitParamsRunner.class)
public class BundlingHandlerTest {

    private BundlingHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private ServiceRequestExecutor serviceRequestExecutor;

    @Mock
    private BundleAudioVideoPdfService bundleAudioVideoPdfService;

    private SscsCaseData sscsCaseData;

    private final ArgumentCaptor<BundleCallback> capture = ArgumentCaptor.forClass(BundleCallback.class);


    @Before
    public void setUp() {
        openMocks(this);
        DwpDocumentService dwpDocumentService = new DwpDocumentService();
        handler = new BundlingHandler(serviceRequestExecutor, dwpDocumentService, bundleAudioVideoPdfService, "bundleUrl.com", "bundleEnglishConfig", "bundleWelshConfig",
                "bundleEnglishEditedConfig", "bundleWelshEditedConfig");

        when(callback.getEvent()).thenReturn(EventType.CREATE_BUNDLE);

        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getCaseTypeId()).thenReturn("Benefit");
        when(caseDetails.getJurisdiction()).thenReturn("SSCS");
        when(serviceRequestExecutor.post(any(), any())).thenReturn(new PreSubmitCallbackResponse<>(sscsCaseData));
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
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        assertEquals(expectedConfigFile, response.getData().getMultiBundleConfiguration().get(0).getValue());
    }

    @Test
    public void givenEnglishCaseWithEdited_thenPopulateEnglishEditedAndUneditedConfigFileName() {
        addMandatoryDwpDocuments();

        callback.getCaseDetails().getCaseData().setDwpPhme(YES.getValue());
        callback.getCaseDetails().getCaseData().setPhmeGranted(YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(NO.getValue());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertEquals(2, response.getData().getMultiBundleConfiguration().size());
        assertEquals("bundleEnglishEditedConfig", response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("bundleEnglishConfig", response.getData().getMultiBundleConfiguration().get(1).getValue());
        assertEquals("Benefit", capture.getValue().getCaseTypeId());
        assertEquals("SSCS", capture.getValue().getJurisdictionId());
        assertEquals(callback.getCaseDetails(), capture.getValue().getCaseDetails());
    }

    @Test
    public void givenWelshCaseWithEdited_thenPopulateWelshEditedAndUneditedConfigFileName() {
        addMandatoryDwpDocuments();

        callback.getCaseDetails().getCaseData().setDwpPhme(YES.getValue());
        callback.getCaseDetails().getCaseData().setPhmeGranted(YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(YES.getValue());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertEquals(2, response.getData().getMultiBundleConfiguration().size());
        assertEquals("bundleWelshEditedConfig", response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("bundleWelshConfig", response.getData().getMultiBundleConfiguration().get(1).getValue());
        assertEquals("Benefit", capture.getValue().getCaseTypeId());
        assertEquals("SSCS", capture.getValue().getJurisdictionId());
        assertEquals(callback.getCaseDetails(), capture.getValue().getCaseDetails());
    }

    @Test
    @Parameters({"Yes, bundleWelshConfig", " No, bundleEnglishConfig"})
    public void givenCaseWithEditedDwpDocsAndPheNotGranted_thenReturnErrorMessageAndDoNotSendRequestToBundleService(String languagePreference, String expectedConfigFile) {
        addMandatoryDwpDocuments();
        addNonEditedSscsDocuments();
        sscsCaseData.setIsConfidentialCase(NO);

        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(languagePreference);
        callback.getCaseDetails().getCaseData().setDwpPhme(YES.getValue());
        callback.getCaseDetails().getCaseData().setPhmeGranted(NO);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertEquals(1, response.getData().getMultiBundleConfiguration().size());
        assertEquals(expectedConfigFile, response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("Benefit", capture.getValue().getCaseTypeId());
        assertEquals("SSCS", capture.getValue().getJurisdictionId());
        assertEquals(callback.getCaseDetails(), capture.getValue().getCaseDetails());
    }

    @Test
    public void givenDwpResponseDocumentHasEmptyFileName_thenPopulateFileName() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        assertEquals(DWP_RESPONSE.getLabel(), response.getData().getDwpDocuments().stream().filter(e -> e.getValue().getDocumentType().equals(DWP_RESPONSE.getValue())).collect(toList()).get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenDwpEvidenceDocumentHasEmptyFileName_thenPopulateFileName() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

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

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

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

        handler.handle(callback);

        verify(bundleAudioVideoPdfService).createAudioVideoPdf(sscsCaseData);
        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertEquals("Benefit", capture.getValue().getCaseTypeId());
        assertEquals("SSCS", capture.getValue().getJurisdictionId());
        assertEquals(callback.getCaseDetails(), capture.getValue().getCaseDetails());
    }

    @Test
    public void givenCaseWithEditedDwpDocsAndPheUnderReview_thenReturnErrorMessageAndDoNotSendRequestToBundleService() {
        addMandatoryDwpDocuments();

        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(NO.getValue());
        callback.getCaseDetails().getCaseData().setDwpPhme(YES.getValue());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("There is a pending PHE request on this case", error);
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    public void givenCaseWithEditedDwpDocsAndChildSupport_thenReturnNoError() {
        addMandatoryDwpDocuments();
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setLanguagePreferenceWelsh(NO.getValue());
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("022");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({"appellant, YES", "appellant, NO", "jointParty, YES", "jointParty, NO"})
    public void givenCaseWithPendingEnhancedConfidentiality_thenReturnErrorMessage(String party, YesNo pheGranted) {
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(NO.getValue());
        if (party.equals("appellant")) {
            callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeAppellant(getDatedRequestOutcome(RequestOutcome.IN_PROGRESS));
        } else {
            callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeJointParty(getDatedRequestOutcome(RequestOutcome.IN_PROGRESS));
        }

        if (isYes(pheGranted)) {
            sscsCaseData.setDwpPhme(pheGranted.getValue());
            sscsCaseData.setPhmeGranted(pheGranted);
            addMandatoryDwpDocuments();
        } else {
            addMandatoryNonEditedDwpDocuments();
        }

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

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

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        assertThat(response.getErrors().size(), is(2));
        assertEquals("There is a pending PHE request on this case", response.getErrors().toArray()[0]);
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
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertEquals(2, response.getData().getMultiBundleConfiguration().size());
        assertEquals(expectedBundleConfig1, response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals(expectedBundleConfig2, response.getData().getMultiBundleConfiguration().get(1).getValue());
        assertEquals("Benefit", capture.getValue().getCaseTypeId());
        assertEquals("SSCS", capture.getValue().getJurisdictionId());
        assertEquals(callback.getCaseDetails(), capture.getValue().getCaseDetails());
    }

    @Test
    public void givenChildSupportedCaseWithEditedSscsDocument_thenPopulateEditedAndUneditedConfigFilename() {
        addEditedSscsDocuments();
        sscsCaseData.setBenefitCode("022");
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertEquals(0, response.getWarnings().size());
        assertEquals(2, response.getData().getMultiBundleConfiguration().size());
        assertEquals("bundleEnglishEditedConfig", response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("bundleEnglishConfig", response.getData().getMultiBundleConfiguration().get(1).getValue());
        assertEquals("Benefit", capture.getValue().getCaseTypeId());
        assertEquals("SSCS", capture.getValue().getJurisdictionId());
        assertEquals(callback.getCaseDetails(), capture.getValue().getCaseDetails());
    }

    @Test
    public void givenChildSupportedCaseWithEditedDwpEvidenceDocument_thenPopulateEditedAndUneditedConfigFilename() {
        addMandatoryDwpEvidenceDocuments();
        sscsCaseData.setBenefitCode("022");
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertEquals(0, response.getWarnings().size());
        assertEquals(2, response.getData().getMultiBundleConfiguration().size());
        assertEquals("bundleEnglishEditedConfig", response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("bundleEnglishConfig", response.getData().getMultiBundleConfiguration().get(1).getValue());
        assertEquals("Benefit", capture.getValue().getCaseTypeId());
        assertEquals("SSCS", capture.getValue().getJurisdictionId());
        assertEquals(callback.getCaseDetails(), capture.getValue().getCaseDetails());
    }

    @Test
    public void givenChildSupportedCaseWithNonEditedDocuments_thenPopulateOnlyUneditedConfigFilename() {
        addMandatoryNonEditedDwpDocuments();
        addMandatoryNonEditedDwpEvidenceDocuments();
        addNonEditedSscsDocuments();
        sscsCaseData.setBenefitCode("022");
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertEquals(0, response.getWarnings().size());
        assertEquals(1, response.getData().getMultiBundleConfiguration().size());
        assertEquals("bundleEnglishConfig", response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("Benefit", capture.getValue().getCaseTypeId());
        assertEquals("SSCS", capture.getValue().getJurisdictionId());
        assertEquals(callback.getCaseDetails(), capture.getValue().getCaseDetails());
    }

    @Test
    public void givenChildSupportedCaseWithEditedDwpDocument_thenPopulateEditedAndUneditedConfigFilename() {
        addMandatoryDwpDocuments();
        sscsCaseData.setBenefitCode("022");
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertEquals(0, response.getWarnings().size());
        assertEquals(2, response.getData().getMultiBundleConfiguration().size());
        assertEquals("bundleEnglishEditedConfig", response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("bundleEnglishConfig", response.getData().getMultiBundleConfiguration().get(1).getValue());
        assertEquals("Benefit", capture.getValue().getCaseTypeId());
        assertEquals("SSCS", capture.getValue().getJurisdictionId());
        assertEquals(callback.getCaseDetails(), capture.getValue().getCaseDetails());
    }


    @Test
    public void givenPhmeGrantedAndEnhancedConfidentiality_thenPopulateEditedAndUneditedConfigFilename() {
        addMandatoryDwpDocuments();
        addEditedSscsDocuments();
        sscsCaseData.setDwpPhme(YES.getValue());
        sscsCaseData.setPhmeGranted(YES);
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertEquals(0, response.getWarnings().size());
        assertEquals(2, response.getData().getMultiBundleConfiguration().size());
        assertEquals("bundleEnglishEditedConfig", response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("bundleEnglishConfig", response.getData().getMultiBundleConfiguration().get(1).getValue());
        assertEquals("Benefit", capture.getValue().getCaseTypeId());
        assertEquals("SSCS", capture.getValue().getJurisdictionId());
        assertEquals(callback.getCaseDetails(), capture.getValue().getCaseDetails());
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
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        assertEquals(1, response.getWarnings().size());
        assertEquals("Some documents in this Bundle contain the same addition letter. Are you sure you want to proceed?", response.getWarnings().toArray()[0]);
    }

    @Test
    @Parameters({"Yes, bundleWelshConfig", "No, bundleEnglishConfig"})
    public void givenEnhancedConfidentialityCaseWithNoEditedDocs_thenPopulateUneditedConfigFileName(String langPreference, String expectedBundleName) {
        addMandatoryNonEditedDwpDocuments();

        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(langPreference);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertEquals(1, response.getData().getMultiBundleConfiguration().size());
        assertEquals(expectedBundleName, response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("Benefit", capture.getValue().getCaseTypeId());
        assertEquals("SSCS", capture.getValue().getJurisdictionId());
        assertEquals(callback.getCaseDetails(), capture.getValue().getCaseDetails());
    }

    @Test
    @Parameters({"Yes, bundleWelshConfig", "No, bundleEnglishConfig"})
    public void givenEnhancedConfidentialityCaseAndPhmeGrantedWithNoEditedDocs_thenPopulateUneditedConfigFileName(String langPreference, String expectedBundleName) {
        addMandatoryNonEditedDwpDocuments();
        addNonEditedSscsDocuments();
        callback.getCaseDetails().getCaseData().setPhmeGranted(YES);
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(langPreference);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertEquals(1, response.getData().getMultiBundleConfiguration().size());
        assertEquals(expectedBundleName, response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("Benefit", capture.getValue().getCaseTypeId());
        assertEquals("SSCS", capture.getValue().getJurisdictionId());
        assertEquals(callback.getCaseDetails(), capture.getValue().getCaseDetails());
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
        bundles.add(Bundle.builder().value(BundleDetails.builder().id("1").build()).build());
        callback.getCaseDetails().getCaseData().setCaseBundles(bundles);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertNull(response.getData().getCaseBundles());
        assertEquals("1", response.getData().getHistoricalBundles().get(0).getValue().getId());
        assertEquals("Benefit", capture.getValue().getCaseTypeId());
        assertEquals("SSCS", capture.getValue().getJurisdictionId());
        assertEquals(callback.getCaseDetails(), capture.getValue().getCaseDetails());
    }

    @Test
    public void givenCaseWithHistoricalBundles_addExistingBundleToHistoricalBundles() {

        List<Bundle> existingBundles = new ArrayList<>();
        existingBundles.add(Bundle.builder().value(BundleDetails.builder().description("3").build()).build());
        callback.getCaseDetails().getCaseData().setCaseBundles(existingBundles);

        List<Bundle> historicalBundles = new ArrayList<>();
        historicalBundles.add(Bundle.builder().value(BundleDetails.builder().description("2").build()).build());
        historicalBundles.add(Bundle.builder().value(BundleDetails.builder().description("1").build()).build());
        callback.getCaseDetails().getCaseData().setHistoricalBundles(historicalBundles);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertEquals(3, response.getData().getHistoricalBundles().size());
    }


    private void addMandatoryDwpDocuments() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);
    }

    private void addMandatoryDwpEvidenceDocuments() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);
    }

    private void addMandatoryNonEditedDwpDocuments() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);
    }

    private void addMandatoryNonEditedDwpEvidenceDocuments() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);
    }
}
