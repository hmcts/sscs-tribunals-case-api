package uk.gov.hmcts.reform.sscs.bundling;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;
import uk.gov.hmcts.reform.sscs.service.bundle.BundleAudioVideoPdfService;

class BundlingHandlerTest {

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

    @BeforeEach
    void setUp() {
        openMocks(this);
        final DwpDocumentService dwpDocumentService = new DwpDocumentService();
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

    @ParameterizedTest
    @CsvSource({"Yes, bundleWelshConfig", "No, bundleEnglishConfig"})
    void givenCaseWithLanguagePreference_thenPopulateConfigFileName(String languagePreference, String expectedConfigFile) {

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        final List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        caseData.setDwpDocuments(dwpDocuments);

        caseData.setLanguagePreferenceWelsh(languagePreference);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        assertThat(response.getData().getMultiBundleConfiguration().get(0).getValue()).isEqualTo(expectedConfigFile);
    }

    @Test
    void givenEnglishCaseWithEdited_thenPopulateEnglishEditedAndUneditedConfigFileName() {
        addMandatoryDwpDocuments();

        callback.getCaseDetails().getCaseData().setDwpPhme(YES.getValue());
        callback.getCaseDetails().getCaseData().setPhmeGranted(YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(NO.getValue());
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertThat(response.getData().getMultiBundleConfiguration()).hasSize(2);
        assertThat(response.getData().getMultiBundleConfiguration().get(0).getValue()).isEqualTo("bundleEnglishEditedConfig");
        assertThat(response.getData().getMultiBundleConfiguration().get(1).getValue()).isEqualTo("bundleEnglishConfig");
        assertThat(capture.getValue().getCaseTypeId()).isEqualTo("Benefit");
        assertThat(capture.getValue().getJurisdictionId()).isEqualTo("SSCS");
    }

    @Test
    void givenWelshCaseWithEdited_thenPopulateWelshEditedAndUneditedConfigFileName() {
        addMandatoryDwpDocuments();

        callback.getCaseDetails().getCaseData().setDwpPhme(YES.getValue());
        callback.getCaseDetails().getCaseData().setPhmeGranted(YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(YES.getValue());
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertThat(response.getData().getMultiBundleConfiguration()).hasSize(2);
        assertThat(response.getData().getMultiBundleConfiguration().get(0).getValue()).isEqualTo("bundleWelshEditedConfig");
        assertThat(response.getData().getMultiBundleConfiguration().get(1).getValue()).isEqualTo("bundleWelshConfig");
        assertThat(capture.getValue().getCaseTypeId()).isEqualTo("Benefit");
        assertThat(capture.getValue().getJurisdictionId()).isEqualTo("SSCS");
    }

    @ParameterizedTest
    @CsvSource({"Yes, bundleWelshConfig", "No, bundleEnglishConfig"})
    void givenCaseWithEditedDwpDocsAndPheNotGranted_thenReturnErrorMessageAndDoNotSendRequestToBundleService(String languagePreference, String expectedConfigFile) {
        addMandatoryDwpDocuments();
        addNonEditedSscsDocuments();
        sscsCaseData.setIsConfidentialCase(NO);

        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(languagePreference);
        callback.getCaseDetails().getCaseData().setDwpPhme(YES.getValue());
        callback.getCaseDetails().getCaseData().setPhmeGranted(NO);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertThat(response.getData().getMultiBundleConfiguration()).hasSize(1);
        assertThat(response.getData().getMultiBundleConfiguration().get(0).getValue()).isEqualTo(expectedConfigFile);
        assertThat(capture.getValue().getCaseTypeId()).isEqualTo("Benefit");
        assertThat(capture.getValue().getJurisdictionId()).isEqualTo("SSCS");
    }

    @Test
    void givenDwpResponseDocumentHasEmptyFileName_thenPopulateFileName() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        assertThat(response.getData().getDwpDocuments().stream().filter(e -> e.getValue().getDocumentType().equals(DWP_RESPONSE.getValue())).collect(toList()).get(0).getValue().getDocumentFileName())
                .isEqualTo(DWP_RESPONSE.getLabel());
    }

    @Test
    void givenDwpEvidenceDocumentHasEmptyFileName_thenPopulateFileName() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        assertThat(response.getData().getDwpDocuments().stream().filter(e -> e.getValue().getDocumentType().equals(DWP_EVIDENCE_BUNDLE.getValue())).collect(toList()).get(0).getValue().getDocumentFileName())
                .isEqualTo(DWP_EVIDENCE_BUNDLE.getLabel());
    }

    @Test
    void givenSscsDocumentHasEmptyFileName_thenPopulateFileName() {

        final SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName(null).documentLink(
                DocumentLink.builder().documentFilename("test.com").build()).build()).build();
        final List<SscsDocument> docs = new ArrayList<>();

        docs.add(sscsDocument);

        callback.getCaseDetails().getCaseData().setSscsDocument(docs);

        final List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        assertThat(response.getData().getSscsDocument().get(0).getValue().getDocumentFileName()).isEqualTo("test.com");
    }

    @Test
    void givenCreateBundleEventWithAudioVideoEvidence_thenTriggerTheExternalCreateBundleEvent() {
        final List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        final List<SscsDocument> audioVideoEvidences = new ArrayList<>();
        audioVideoEvidences.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                        .documentType("appellantEvidence")
                        .documentDateAdded(LocalDate.now().toString())
                        .documentLink(DocumentLink.builder().documentFilename("Myfilename.mp3").documentUrl("dm-store-url/123").documentBinaryUrl("dm-store-url/123/binary").build()).build())
                .build());
        caseDetails.getCaseData().setSscsDocument(audioVideoEvidences);

        handler.handle(callback);

        verify(bundleAudioVideoPdfService).createAudioVideoPdf(sscsCaseData);
        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertThat(capture.getValue().getCaseTypeId()).isEqualTo("Benefit");
        assertThat(capture.getValue().getJurisdictionId()).isEqualTo("SSCS");
    }

    @Test
    void givenCaseWithEditedDwpDocsAndPheUnderReview_thenReturnErrorMessageAndDoNotSendRequestToBundleService() {
        addMandatoryDwpDocuments();

        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(NO.getValue());
        callback.getCaseDetails().getCaseData().setDwpPhme(YES.getValue());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        final String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertThat(error).isEqualTo("There is a pending PHE request on this case");
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    void givenCaseWithEditedDwpDocsAndChildSupport_thenReturnNoError() {
        addMandatoryDwpDocuments();
        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setLanguagePreferenceWelsh(NO.getValue());
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("022");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        assertThat(response.getErrors()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"appellant, YES", "appellant, NO", "jointParty, YES", "jointParty, NO"})
    void givenCaseWithPendingEnhancedConfidentiality_thenReturnErrorMessage(String party, YesNo pheGranted) {
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

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        assertThat(response.getErrors()).containsExactly("There is a pending enhanced confidentiality request on this case");
        verifyNoInteractions(serviceRequestExecutor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"appellant", "jointParty"})
    void givenCaseWithPendingEnhancedConfidentialityAndPendingPhmeRequest_thenReturnTwoErrorMessages(String party) {
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(NO.getValue());
        if (party.equals("appellant")) {
            sscsCaseData.setConfidentialityRequestOutcomeAppellant(getDatedRequestOutcome(RequestOutcome.IN_PROGRESS));
        } else {
            sscsCaseData.setConfidentialityRequestOutcomeJointParty(getDatedRequestOutcome(RequestOutcome.IN_PROGRESS));
        }
        callback.getCaseDetails().getCaseData().setDwpPhme(YES.getValue());

        addMandatoryDwpDocuments();

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        assertThat(response.getErrors()).containsExactly(
                "There is a pending PHE request on this case",
                "There is a pending enhanced confidentiality request on this case");
        verifyNoInteractions(serviceRequestExecutor);
    }

    private DatedRequestOutcome getDatedRequestOutcome(RequestOutcome outcome) {
        return DatedRequestOutcome.builder().requestOutcome(outcome).build();
    }

    @ParameterizedTest
    @CsvSource({"No, bundleEnglishEditedConfig, bundleEnglishConfig", "Yes, bundleWelshEditedConfig, bundleWelshConfig"})
    void givenEnhancedConfidentialityCaseWithEditedDocuments_thenPopulateEditedAndUneditedConfigFileName(String langPreference, String expectedBundleConfig1, String expectedBundleConfig2) {
        addMandatoryNonEditedDwpDocuments();
        addEditedSscsDocuments();

        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(langPreference);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertThat(response.getData().getMultiBundleConfiguration()).hasSize(2);
        assertThat(response.getData().getMultiBundleConfiguration().get(0).getValue()).isEqualTo(expectedBundleConfig1);
        assertThat(response.getData().getMultiBundleConfiguration().get(1).getValue()).isEqualTo(expectedBundleConfig2);
        assertThat(capture.getValue().getCaseTypeId()).isEqualTo("Benefit");
        assertThat(capture.getValue().getJurisdictionId()).isEqualTo("SSCS");
    }

    @Test
    void givenChildSupportedCaseWithEditedSscsDocument_thenPopulateEditedAndUneditedConfigFilename() {
        addEditedSscsDocuments();
        sscsCaseData.setBenefitCode("022");
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getMultiBundleConfiguration()).hasSize(2);
        assertThat(response.getData().getMultiBundleConfiguration().get(0).getValue()).isEqualTo("bundleEnglishEditedConfig");
        assertThat(response.getData().getMultiBundleConfiguration().get(1).getValue()).isEqualTo("bundleEnglishConfig");
        assertThat(capture.getValue().getCaseTypeId()).isEqualTo("Benefit");
        assertThat(capture.getValue().getJurisdictionId()).isEqualTo("SSCS");
    }

    @Test
    void givenChildSupportedCaseWithEditedDwpEvidenceDocument_thenPopulateEditedAndUneditedConfigFilename() {
        addMandatoryDwpEvidenceDocuments();
        sscsCaseData.setBenefitCode("022");
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getMultiBundleConfiguration()).hasSize(2);
        assertThat(response.getData().getMultiBundleConfiguration().get(0).getValue()).isEqualTo("bundleEnglishEditedConfig");
        assertThat(response.getData().getMultiBundleConfiguration().get(1).getValue()).isEqualTo("bundleEnglishConfig");
        assertThat(capture.getValue().getCaseTypeId()).isEqualTo("Benefit");
        assertThat(capture.getValue().getJurisdictionId()).isEqualTo("SSCS");
    }

    @Test
    void givenChildSupportedCaseWithNonEditedDocuments_thenPopulateOnlyUneditedConfigFilename() {
        addMandatoryNonEditedDwpDocuments();
        addMandatoryNonEditedDwpEvidenceDocuments();
        addNonEditedSscsDocuments();
        sscsCaseData.setBenefitCode("022");
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getMultiBundleConfiguration()).hasSize(1);
        assertThat(response.getData().getMultiBundleConfiguration().get(0).getValue()).isEqualTo("bundleEnglishConfig");
        assertThat(capture.getValue().getCaseTypeId()).isEqualTo("Benefit");
        assertThat(capture.getValue().getJurisdictionId()).isEqualTo("SSCS");
    }

    @Test
    void givenChildSupportedCaseWithEditedDwpDocument_thenPopulateEditedAndUneditedConfigFilename() {
        addMandatoryDwpDocuments();
        sscsCaseData.setBenefitCode("022");
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getMultiBundleConfiguration()).hasSize(2);
        assertThat(response.getData().getMultiBundleConfiguration().get(0).getValue()).isEqualTo("bundleEnglishEditedConfig");
        assertThat(response.getData().getMultiBundleConfiguration().get(1).getValue()).isEqualTo("bundleEnglishConfig");
        assertThat(capture.getValue().getCaseTypeId()).isEqualTo("Benefit");
        assertThat(capture.getValue().getJurisdictionId()).isEqualTo("SSCS");
    }

    @Test
    void givenPhmeGrantedAndEnhancedConfidentiality_thenPopulateEditedAndUneditedConfigFilename() {
        addMandatoryDwpDocuments();
        addEditedSscsDocuments();
        sscsCaseData.setDwpPhme(YES.getValue());
        sscsCaseData.setPhmeGranted(YES);
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getMultiBundleConfiguration()).hasSize(2);
        assertThat(response.getData().getMultiBundleConfiguration().get(0).getValue()).isEqualTo("bundleEnglishEditedConfig");
        assertThat(response.getData().getMultiBundleConfiguration().get(1).getValue()).isEqualTo("bundleEnglishConfig");
        assertThat(capture.getValue().getCaseTypeId()).isEqualTo("Benefit");
        assertThat(capture.getValue().getJurisdictionId()).isEqualTo("SSCS");
    }

    @Test
    void givenDocumentsWithSameAddition_thenShowWarning() {

        final List<SscsDocument> documents = new ArrayList<>();
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
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        assertThat(response.getWarnings()).containsExactly("Some documents in this Bundle contain the same addition letter. Are you sure you want to proceed?");
    }

    @Test
    void givenSscsDocumentsNotInBundleOrder_thenSortsIntoBundleOrderBeforeSendingToBundleService() {
        final SscsDocument documentB = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("b.pdf").bundleAddition("B").build()).build();
        final SscsDocument documentA = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("a.pdf").bundleAddition("A").build()).build();
        callback.getCaseDetails().getCaseData().setSscsDocument(new ArrayList<>(List.of(documentB, documentA)));

        handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        final SscsCaseData bundlePayloadCaseData = (SscsCaseData) capture.getValue().getCaseDetails().getCaseData();
        final List<String> bundlePayloadFileNames = bundlePayloadCaseData.getSscsDocument().stream()
                .map(document -> document.getValue().getDocumentFileName())
                .collect(toList());

        assertThat(bundlePayloadFileNames).containsExactly("a.pdf", "b.pdf");
    }

    @Test
    void givenSscsWelshDocumentsNotInBundleOrder_thenSortsIntoBundleOrderBeforeSendingToBundleService() {
        final SscsWelshDocument documentB = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder().documentFileName("b.pdf").bundleAddition("B").build()).build();
        final SscsWelshDocument documentA = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder().documentFileName("a.pdf").bundleAddition("A").build()).build();
        callback.getCaseDetails().getCaseData().setSscsWelshDocuments(new ArrayList<>(List.of(documentB, documentA)));

        handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        final SscsCaseData bundlePayloadCaseData = (SscsCaseData) capture.getValue().getCaseDetails().getCaseData();
        final List<String> bundlePayloadFileNames = bundlePayloadCaseData.getSscsWelshDocuments().stream()
                .map(document -> document.getValue().getDocumentFileName())
                .collect(toList());

        assertThat(bundlePayloadFileNames).containsExactly("a.pdf", "b.pdf");
    }

    @ParameterizedTest
    @CsvSource({"Yes, bundleWelshConfig", "No, bundleEnglishConfig"})
    void givenEnhancedConfidentialityCaseWithNoEditedDocs_thenPopulateUneditedConfigFileName(String langPreference, String expectedBundleName) {
        addMandatoryNonEditedDwpDocuments();

        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(langPreference);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertThat(response.getData().getMultiBundleConfiguration()).hasSize(1);
        assertThat(response.getData().getMultiBundleConfiguration().get(0).getValue()).isEqualTo(expectedBundleName);
        assertThat(capture.getValue().getCaseTypeId()).isEqualTo("Benefit");
        assertThat(capture.getValue().getJurisdictionId()).isEqualTo("SSCS");
    }

    @ParameterizedTest
    @CsvSource({"Yes, bundleWelshConfig", "No, bundleEnglishConfig"})
    void givenEnhancedConfidentialityCaseAndPhmeGrantedWithNoEditedDocs_thenPopulateUneditedConfigFileName(String langPreference, String expectedBundleName) {
        addMandatoryNonEditedDwpDocuments();
        addNonEditedSscsDocuments();
        callback.getCaseDetails().getCaseData().setPhmeGranted(YES);
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(langPreference);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertThat(response.getData().getMultiBundleConfiguration()).hasSize(1);
        assertThat(response.getData().getMultiBundleConfiguration().get(0).getValue()).isEqualTo(expectedBundleName);
        assertThat(capture.getValue().getCaseTypeId()).isEqualTo("Benefit");
        assertThat(capture.getValue().getJurisdictionId()).isEqualTo("SSCS");
    }

    private void addEditedSscsDocuments() {
        final SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("test.pdf").editedDocumentLink(DocumentLink.builder().documentFilename("test.pdf").build()).build()).build();
        sscsCaseData.setSscsDocument(Collections.singletonList(sscsDocument));
    }

    private void addNonEditedSscsDocuments() {
        final SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("test.pdf").build()).build();
        sscsCaseData.setSscsDocument(Collections.singletonList(sscsDocument));
    }

    @Test
    void givenCaseWithPreviouslyCreatedBundles_thenClearAllBundles() {
        addMandatoryDwpDocuments();
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(NO.getValue());
        callback.getCaseDetails().getCaseData().setPhmeGranted(YES);

        final List<Bundle> bundles = new ArrayList<>();
        bundles.add(Bundle.builder().value(BundleDetails.builder().id("1").build()).build());
        callback.getCaseDetails().getCaseData().setCaseBundles(bundles);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertThat(response.getData().getCaseBundles()).isNull();
        assertThat(response.getData().getHistoricalBundles().get(0).getValue().getId()).isEqualTo("1");
        assertThat(capture.getValue().getCaseTypeId()).isEqualTo("Benefit");
        assertThat(capture.getValue().getJurisdictionId()).isEqualTo("SSCS");
    }

    @Test
    void givenCaseWithHistoricalBundles_addExistingBundleToHistoricalBundles() {

        final List<Bundle> existingBundles = new ArrayList<>();
        existingBundles.add(Bundle.builder().value(BundleDetails.builder().description("3").build()).build());
        callback.getCaseDetails().getCaseData().setCaseBundles(existingBundles);

        final List<Bundle> historicalBundles = new ArrayList<>();
        historicalBundles.add(Bundle.builder().value(BundleDetails.builder().description("2").build()).build());
        historicalBundles.add(Bundle.builder().value(BundleDetails.builder().description("1").build()).build());
        callback.getCaseDetails().getCaseData().setHistoricalBundles(historicalBundles);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(callback);

        verify(serviceRequestExecutor).post(capture.capture(), eq("bundleUrl.com/api/new-bundle"));
        assertThat(response.getData().getHistoricalBundles()).hasSize(3);
    }

    private void addMandatoryDwpDocuments() {
        final List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);
    }

    private void addMandatoryDwpEvidenceDocuments() {
        final List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);
    }

    private void addMandatoryNonEditedDwpDocuments() {
        final List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);
    }

    private void addMandatoryNonEditedDwpEvidenceDocuments() {
        final List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);
    }
}
