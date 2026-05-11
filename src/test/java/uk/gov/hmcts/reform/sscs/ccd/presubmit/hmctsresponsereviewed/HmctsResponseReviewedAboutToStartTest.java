package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.REVIEW_BY_TCW;
import static uk.gov.hmcts.reform.sscs.service.HearingsService.EXISTING_HEARING_ERROR;
import static uk.gov.hmcts.reform.sscs.service.HearingsService.REQUEST_FAILURE_WARNING;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.HearingsService;

class HmctsResponseReviewedAboutToStartTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private HmctsResponseReviewedAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private HearingsService hearingsService;

    @Mock
    private DwpAddressLookupService dwpAddressLookupService;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        openMocks(this);
        dwpAddressLookupService = new DwpAddressLookupService();
        handler = new HmctsResponseReviewedAboutToStartHandler(dwpAddressLookupService, hearingsService, false);

        when(callback.getEvent()).thenReturn(EventType.HMCTS_RESPONSE_REVIEWED);

        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.PIP.getShortName()).build()).mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenAValidEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.HMCTS_RESPONSE_REVIEWED);

        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isTrue();
    }

    @Test
    void givenANonResponseReviewedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    void populateOriginatingAndPresentingOfficeDropdownsWhenHandlerFires_withCorrectSelectedOffice() {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("PIP").build());
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpOriginatingOffice().getValue().getCode()).isEqualTo("DWP PIP (3)");
        assertThat(response.getData().getDwpPresentingOffice().getValue().getCode()).isEqualTo("DWP PIP (3)");
    }

    @Test
    void givenMrnIsNull_populateOriginatingAndPresentingOfficeDropdownsWhenHandlerFires_withNoDefaultedSelectedOffice() {
        callback.getCaseDetails().getCaseData().getAppeal().setMrnDetails(null);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpOriginatingOffice().getValue().getCode()).isNull();
        assertThat(response.getData().getDwpPresentingOffice().getValue().getCode()).isNull();
    }

    @Test
    void givenIbcaCase_populateBenefitAndIssueCodes() {
        sscsCaseData.setBenefitCode("093");
        sscsCaseData.setIssueCode("RA");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getBenefitCodeIbcaOnly()).isEqualTo("093");
        assertThat(response.getData().getIssueCodeIbcaOnly()).isEqualTo("RA");
    }

    @Test
    void givenOriginatingAndPresentingOfficeHavePreviouslyBeenSet_thenDefaultToTheseOfficesAndNotTheOneSetInMrn() {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("PIP").build());

        final DynamicListItem value = new DynamicListItem("DWP PIP (4)", "DWP PIP (4)");
        final DynamicList originatingOfficeList = new DynamicList(value, Collections.singletonList(value));

        final DynamicListItem value2 = new DynamicListItem("DWP PIP (5)", "DWP PIP (5)");
        final DynamicList presentingOfficeList = new DynamicList(value2, Collections.singletonList(value2));

        callback.getCaseDetails().getCaseData().setDwpOriginatingOffice(originatingOfficeList);
        callback.getCaseDetails().getCaseData().setDwpPresentingOffice(presentingOfficeList);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpOriginatingOffice().getValue().getCode()).isEqualTo("DWP PIP (4)");
        assertThat(response.getData().getDwpPresentingOffice().getValue().getCode()).isEqualTo("DWP PIP (5)");
    }

    @Test
    void defaultTheDwpOptionsToNo() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpIsOfficerAttending()).isEqualTo("No");
        assertThat(response.getData().getDwpUcb()).isEqualTo("No");
        assertThat(response.getData().getDwpPhme()).isEqualTo("No");
        assertThat(response.getData().getDwpComplexAppeal()).isEqualTo("No");
    }

    @Test
    void givenHmctsResponseReviewedEventIsTriggeredNonDigitalCase_thenDisplayError() {
        callback.getCaseDetails().getCaseData().setCreatedInGapsFrom("validAppeal");
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().toArray()[0]).isEqualTo("This event cannot be run for cases created in GAPS at valid appeal");
    }

    @Test
    void givenAListAssistCaseIfAHearingIsListedThenReturnError() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);

        when(hearingsService.validationCheckForListedOrExceptionHearings(any(), any()))
                .thenAnswer(invocation -> {
                    final PreSubmitCallbackResponse<SscsCaseData> response = invocation.getArgument(1);
                    response.addError(EXISTING_HEARING_ERROR);
                    return null;
                });

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1).contains(EXISTING_HEARING_ERROR);
    }

    @Test
    void giveWarningIfHearingInExceptionState() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);

        when(hearingsService.validationCheckForListedOrExceptionHearings(any(), any()))
                .thenAnswer(invocation -> {
                    final PreSubmitCallbackResponse<SscsCaseData> response = invocation.getArgument(1);
                    response.addWarning(REQUEST_FAILURE_WARNING);
                    return null;
                });

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings()).hasSize(1).contains(REQUEST_FAILURE_WARNING);
    }

    @Test
    void givenResponseEventWithDwpDocumentsInCollection_thenPopulateLegacyFields() {
        final DwpDocument dwpResponseDocument = DwpDocument.builder().value(DwpDocumentDetails.builder().documentLink(DocumentLink.builder().documentFilename("dwpresponse").build()).documentType(DwpDocumentType.DWP_RESPONSE.getValue()).build()).build();
        final DwpDocument dwpAt38Document = DwpDocument.builder().value(DwpDocumentDetails.builder().documentLink(DocumentLink.builder().documentFilename("at38").build()).documentType(DwpDocumentType.AT_38.getValue()).build()).build();
        final DwpDocument dwpEvidenceDocument = DwpDocument.builder().value(DwpDocumentDetails.builder().documentLink(DocumentLink.builder().documentFilename("dwpevidence").build()).documentType(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue()).build()).build();
        final DwpDocument dwpAppendix12 = DwpDocument.builder().value(DwpDocumentDetails.builder().documentLink(DocumentLink.builder().documentFilename("dwpappendix12").build()).documentType(DwpDocumentType.APPENDIX_12.getValue()).build()).build();
        final DwpDocument dwpUcbDocument = DwpDocument.builder().value(DwpDocumentDetails.builder().documentLink(DocumentLink.builder().documentFilename("dwpucb").build()).documentType(DwpDocumentType.UCB.getValue()).build()).build();
        final List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(dwpResponseDocument);
        dwpDocuments.add(dwpAt38Document);
        dwpDocuments.add(dwpEvidenceDocument);
        dwpDocuments.add(dwpAppendix12);
        dwpDocuments.add(dwpUcbDocument);

        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpResponseDocument().getDocumentLink().getDocumentFilename()).isEqualTo("dwpresponse");
        assertThat(response.getData().getDwpAT38Document().getDocumentLink().getDocumentFilename()).isEqualTo("at38");
        assertThat(response.getData().getDwpEvidenceBundleDocument().getDocumentLink().getDocumentFilename()).isEqualTo("dwpevidence");
        assertThat(response.getData().getAppendix12Doc().getDocumentLink().getDocumentFilename()).isEqualTo("dwpappendix12");
        assertThat(response.getData().getDwpUcbEvidenceDocument().getDocumentFilename()).isEqualTo("dwpucb");
    }


    @Test
    void givenResponseEventWithDwpDocumentsAndEditedInCollection_thenPopulateLegacyFields() {
        final DwpDocument dwpResponseDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("dwpresponse").documentBinaryUrl("/responsebinaryurl").documentUrl("/responseurl").build())
                .dwpEditedEvidenceReason("phme")
                .editedDocumentLink(DocumentLink.builder().documentFilename("editedresponse").documentBinaryUrl("/responseeditedbinaryurl").documentUrl("/responseeditedurl").build()).documentType(DwpDocumentType.DWP_RESPONSE.getValue()).build()).build();
        final DwpDocument dwpAt38Document = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("at38").documentBinaryUrl("/binaryurl").documentUrl("/url").build()).documentType(DwpDocumentType.AT_38.getValue()).build()).build();
        final DwpDocument dwpEvidenceDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("dwpevidence").documentBinaryUrl("/evidencebinaryurl").documentUrl("/evidenceurl").build())
                .dwpEditedEvidenceReason("phme")
                .editedDocumentLink(DocumentLink.builder().documentFilename("editedevidence").documentBinaryUrl("/evidenceeditedbinaryurl").documentUrl("/evidenceeditedurl").build()).documentType(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue()).build()).build();
        final DwpDocument dwpAppendix12 = DwpDocument.builder().value(DwpDocumentDetails.builder().documentLink(DocumentLink.builder().documentFilename("dwpappendix12").build()).documentType(DwpDocumentType.APPENDIX_12.getValue()).build()).build();
        final DwpDocument dwpUcbDocument = DwpDocument.builder().value(DwpDocumentDetails.builder().documentLink(DocumentLink.builder().documentFilename("dwpucb").build()).documentType(DwpDocumentType.UCB.getValue()).build()).build();

        final List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(dwpResponseDocument);
        dwpDocuments.add(dwpAt38Document);
        dwpDocuments.add(dwpEvidenceDocument);
        dwpDocuments.add(dwpAppendix12);
        dwpDocuments.add(dwpUcbDocument);

        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpResponseDocument().getDocumentLink().getDocumentFilename()).isEqualTo("dwpresponse");
        assertThat(response.getData().getDwpAT38Document().getDocumentLink().getDocumentFilename()).isEqualTo("at38");
        assertThat(response.getData().getDwpEvidenceBundleDocument().getDocumentLink().getDocumentFilename()).isEqualTo("dwpevidence");
        assertThat(response.getData().getDwpEditedResponseDocument().getDocumentLink().getDocumentFilename()).isEqualTo("editedresponse");
        assertThat(response.getData().getDwpEditedEvidenceBundleDocument().getDocumentLink().getDocumentFilename()).isEqualTo("editedevidence");
        assertThat(response.getData().getAppendix12Doc().getDocumentLink().getDocumentFilename()).isEqualTo("dwpappendix12");
        assertThat(response.getData().getDwpUcbEvidenceDocument().getDocumentFilename()).isEqualTo("dwpucb");
    }

    private DwpDocument buildDocument(DwpDocumentType documentType, String filename, LocalDate dateAdded) {
        return DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename(filename).documentBinaryUrl("/binaryurl")
                        .documentFilename(filename).documentUrl("/url").build()).documentType(documentType.getValue())
                .documentDateAdded(dateAdded.toString()).build()).build();
    }

    private DwpDocument buildDocument(DwpDocumentType documentType, String filename, LocalDateTime dateTimeAdded) {
        return DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename(filename).documentBinaryUrl("/binaryurl")
                        .documentFilename(filename).documentUrl("/url").build()).documentType(documentType.getValue())
                .documentDateTimeAdded(dateTimeAdded).build()).build();
    }

    @ParameterizedTest
    @EnumSource(value = DwpDocumentType.class, names = {"AT_38", "APPENDIX_12", "DWP_EVIDENCE_BUNDLE", "DWP_RESPONSE", "UCB"})
    void givenResponseEventWithDwpDocumentsWithMultipleDates_thenPopulateLegacyFields(DwpDocumentType documentType) {

        final DwpDocument docAtStartOfToday = buildDocument(documentType, "file-startOfToday", LocalDate.now());
        final DwpDocument latestAtDoc = buildDocument(documentType, "file-latest", LocalDateTime.now());
        final DwpDocument doc10MinutesAgo = buildDocument(documentType, "file-latest", LocalDateTime.now().minusMinutes(10));
        final DwpDocument doc2DaysAgo = buildDocument(documentType, "file-2DaysAgo", LocalDate.now().minusDays(2));
        final DwpDocument docYesterday = buildDocument(documentType, "file-yesterday", LocalDateTime.now().minusDays(1));

        final List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(docAtStartOfToday);
        dwpDocuments.add(latestAtDoc);
        dwpDocuments.add(doc10MinutesAgo);
        dwpDocuments.add(doc2DaysAgo);
        dwpDocuments.add(docYesterday);

        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);
        callback.getCaseDetails().getCaseData().sortCollections();

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DocumentLink documentLink = null;
        if (documentType == DwpDocumentType.AT_38) {
            documentLink = response.getData().getDwpAT38Document().getDocumentLink();
        } else if (documentType == DwpDocumentType.APPENDIX_12) {
            documentLink = response.getData().getAppendix12Doc().getDocumentLink();
        } else if (documentType == DwpDocumentType.DWP_RESPONSE) {
            documentLink = response.getData().getDwpResponseDocument().getDocumentLink();
        } else if (documentType == DwpDocumentType.DWP_EVIDENCE_BUNDLE) {
            documentLink = response.getData().getDwpEvidenceBundleDocument().getDocumentLink();
        } else if (documentType == DwpDocumentType.UCB) {
            documentLink = response.getData().getDwpUcbEvidenceDocument();
        }
        assertThat(documentLink).isNotNull();
        assertThat(documentLink.getDocumentFilename()).isEqualTo("file-2DaysAgo");
    }

    @Test
    void populatesSelectWhoReviewsCaseDropDown() {

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        final List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()));
        listOptions.add(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()));
        final DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertThat(response.getData().getSelectWhoReviewsCase()).isEqualTo(expected);
    }

    @Test
    void givenFlagEnabledAndChildSupport_thenSelectedConfidentialityPartyHasNoDefaultSelection() {
        handler = new HmctsResponseReviewedAboutToStartHandler(dwpAddressLookupService, hearingsService, true);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getExtendedSscsCaseData().getSelectedConfidentialityParty().getValue().getCode()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "false, childSupport",
        "true, PIP"
    })
    void givenVariousFlagAndBenefitCombinations_whenNotChildSupportWithFlagEnabled_thenSelectedConfidentialityPartyIsNotSet(
        boolean featureFlag, String benefitCode) {
        handler = new HmctsResponseReviewedAboutToStartHandler(dwpAddressLookupService, hearingsService, featureFlag);

        String codeToUse = benefitCode.equals("PIP") ? Benefit.PIP.getShortName() : benefitCode;
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(codeToUse).build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getExtendedSscsCaseData().getSelectedConfidentialityParty()).isNull();
    }

}
