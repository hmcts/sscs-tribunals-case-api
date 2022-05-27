package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

@RunWith(JUnitParamsRunner.class)
public class HmctsResponseReviewedAboutToStartTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private HmctsResponseReviewedAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private DwpAddressLookupService dwpAddressLookupService;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        dwpAddressLookupService = new DwpAddressLookupService();
        handler = new HmctsResponseReviewedAboutToStartHandler(dwpAddressLookupService);

        when(callback.getEvent()).thenReturn(EventType.HMCTS_RESPONSE_REVIEWED);

        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.PIP.getShortName()).build()).mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"HMCTS_RESPONSE_REVIEWED"})
    public void givenAValidEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenANonResponseReviewedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void populateOriginatingAndPresentingOfficeDropdownsWhenHandlerFires_withCorrectSelectedOffice() {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("PIP").build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("DWP PIP (3)", response.getData().getDwpOriginatingOffice().getValue().getCode());
        assertEquals("DWP PIP (3)", response.getData().getDwpPresentingOffice().getValue().getCode());
    }

    @Test
    public void givenMrnIsNull_populateOriginatingAndPresentingOfficeDropdownsWhenHandlerFires_withNoDefaultedSelectedOffice() {
        callback.getCaseDetails().getCaseData().getAppeal().setMrnDetails(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertNull(response.getData().getDwpOriginatingOffice().getValue().getCode());
        assertNull(response.getData().getDwpPresentingOffice().getValue().getCode());
    }

    @Test
    public void givenOriginatingAndPresentingOfficeHavePreviouslyBeenSet_thenDefaultToTheseOfficesAndNotTheOneSetInMrn() {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("PIP").build());

        DynamicListItem value = new DynamicListItem("DWP PIP (4)", "DWP PIP (4)");
        DynamicList originatingOfficeList = new DynamicList(value, Collections.singletonList(value));

        DynamicListItem value2 = new DynamicListItem("DWP PIP (5)", "DWP PIP (5)");
        DynamicList presentingOfficeList = new DynamicList(value2, Collections.singletonList(value2));

        callback.getCaseDetails().getCaseData().setDwpOriginatingOffice(originatingOfficeList);
        callback.getCaseDetails().getCaseData().setDwpPresentingOffice(presentingOfficeList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("DWP PIP (4)", response.getData().getDwpOriginatingOffice().getValue().getCode());
        assertEquals("DWP PIP (5)", response.getData().getDwpPresentingOffice().getValue().getCode());
    }

    @Test
    public void defaultTheDwpOptionsToNo() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("No", response.getData().getDwpIsOfficerAttending());
        assertEquals("No", response.getData().getDwpUcb());
        assertEquals("No", response.getData().getDwpPhme());
        assertEquals("No", response.getData().getDwpComplexAppeal());
    }

    @Test
    public void givenHmctsResponseReviewedEventIsTriggeredNonDigitalCase_thenDisplayError() {
        callback.getCaseDetails().getCaseData().setCreatedInGapsFrom("validAppeal");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("This event cannot be run for cases created in GAPS at valid appeal", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenResponseEventWithDwpDocumentsInCollection_thenPopulateLegacyFields() {
        DwpDocument dwpResponseDocument = DwpDocument.builder().value(DwpDocumentDetails.builder().documentLink(DocumentLink.builder().documentFilename("dwpresponse").build()).documentType(DwpDocumentType.DWP_RESPONSE.getValue()).build()).build();
        DwpDocument dwpAt38Document = DwpDocument.builder().value(DwpDocumentDetails.builder().documentLink(DocumentLink.builder().documentFilename("at38").build()).documentType(DwpDocumentType.AT_38.getValue()).build()).build();
        DwpDocument dwpEvidenceDocument = DwpDocument.builder().value(DwpDocumentDetails.builder().documentLink(DocumentLink.builder().documentFilename("dwpevidence").build()).documentType(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue()).build()).build();
        DwpDocument dwpAppendix12 = DwpDocument.builder().value(DwpDocumentDetails.builder().documentLink(DocumentLink.builder().documentFilename("dwpappendix12").build()).documentType(DwpDocumentType.APPENDIX_12.getValue()).build()).build();
        DwpDocument dwpUcbDocument = DwpDocument.builder().value(DwpDocumentDetails.builder().documentLink(DocumentLink.builder().documentFilename("dwpucb").build()).documentType(DwpDocumentType.UCB.getValue()).build()).build();
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(dwpResponseDocument);
        dwpDocuments.add(dwpAt38Document);
        dwpDocuments.add(dwpEvidenceDocument);
        dwpDocuments.add(dwpAppendix12);
        dwpDocuments.add(dwpUcbDocument);

        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("dwpresponse", response.getData().getDwpResponseDocument().getDocumentLink().getDocumentFilename());
        assertEquals("at38", response.getData().getDwpAT38Document().getDocumentLink().getDocumentFilename());
        assertEquals("dwpevidence", response.getData().getDwpEvidenceBundleDocument().getDocumentLink().getDocumentFilename());
        assertEquals("dwpappendix12", response.getData().getAppendix12Doc().getDocumentLink().getDocumentFilename());
        assertEquals("dwpucb", response.getData().getDwpUcbEvidenceDocument().getDocumentFilename());
    }


    @Test
    public void givenResponseEventWithDwpDocumentsAndEditedInCollection_thenPopulateLegacyFields() {
        DwpDocument dwpResponseDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("dwpresponse").documentBinaryUrl("/responsebinaryurl").documentUrl("/responseurl").build())
                .dwpEditedEvidenceReason("phme")
                .editedDocumentLink(DocumentLink.builder().documentFilename("editedresponse").documentBinaryUrl("/responseeditedbinaryurl").documentUrl("/responseeditedurl").build()).documentType(DwpDocumentType.DWP_RESPONSE.getValue()).build()).build();
        DwpDocument dwpAt38Document = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("at38").documentBinaryUrl("/binaryurl").documentUrl("/url").build()).documentType(DwpDocumentType.AT_38.getValue()).build()).build();
        DwpDocument dwpEvidenceDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("dwpevidence").documentBinaryUrl("/evidencebinaryurl").documentUrl("/evidenceurl").build())
                .dwpEditedEvidenceReason("phme")
                .editedDocumentLink(DocumentLink.builder().documentFilename("editedevidence").documentBinaryUrl("/evidenceeditedbinaryurl").documentUrl("/evidenceeditedurl").build()).documentType(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue()).build()).build();
        DwpDocument dwpAppendix12 = DwpDocument.builder().value(DwpDocumentDetails.builder().documentLink(DocumentLink.builder().documentFilename("dwpappendix12").build()).documentType(DwpDocumentType.APPENDIX_12.getValue()).build()).build();
        DwpDocument dwpUcbDocument = DwpDocument.builder().value(DwpDocumentDetails.builder().documentLink(DocumentLink.builder().documentFilename("dwpucb").build()).documentType(DwpDocumentType.UCB.getValue()).build()).build();

        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(dwpResponseDocument);
        dwpDocuments.add(dwpAt38Document);
        dwpDocuments.add(dwpEvidenceDocument);
        dwpDocuments.add(dwpAppendix12);
        dwpDocuments.add(dwpUcbDocument);

        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("dwpresponse", response.getData().getDwpResponseDocument().getDocumentLink().getDocumentFilename());
        assertEquals("at38", response.getData().getDwpAT38Document().getDocumentLink().getDocumentFilename());
        assertEquals("dwpevidence", response.getData().getDwpEvidenceBundleDocument().getDocumentLink().getDocumentFilename());
        assertEquals("editedresponse", response.getData().getDwpEditedResponseDocument().getDocumentLink().getDocumentFilename());
        assertEquals("editedevidence", response.getData().getDwpEditedEvidenceBundleDocument().getDocumentLink().getDocumentFilename());
        assertEquals("dwpappendix12", response.getData().getAppendix12Doc().getDocumentLink().getDocumentFilename());
        assertEquals("dwpucb", response.getData().getDwpUcbEvidenceDocument().getDocumentFilename());
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

    @Test
    @Parameters({"AT_38", "APPENDIX_12", "DWP_EVIDENCE_BUNDLE", "DWP_RESPONSE", "UCB"})
    public void givenResponseEventWithDwpDocumentsWithMultipleDates_thenPopulateLegacyFields(DwpDocumentType documentType) {

        DwpDocument docAtStartOfToday = buildDocument(documentType, "file-startOfToday", LocalDate.now());
        DwpDocument latestAtDoc = buildDocument(documentType, "file-latest", LocalDateTime.now());
        DwpDocument doc10MinutesAgo = buildDocument(documentType, "file-latest", LocalDateTime.now().minusMinutes(10));
        DwpDocument doc2DaysAgo = buildDocument(documentType, "file-2DaysAgo", LocalDate.now().minusDays(2));
        DwpDocument docYesterday = buildDocument(documentType, "file-yesterday", LocalDateTime.now().minusDays(1));

        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(docAtStartOfToday);
        dwpDocuments.add(latestAtDoc);
        dwpDocuments.add(doc10MinutesAgo);
        dwpDocuments.add(doc2DaysAgo);
        dwpDocuments.add(docYesterday);

        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);
        callback.getCaseDetails().getCaseData().sortCollections();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

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
        assertNotNull(documentLink);
        assertEquals("file-2DaysAgo", documentLink.getDocumentFilename());
    }

    @Test
    public void populatesSelectWhoReviewsCaseDropDown() {

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()));
        listOptions.add(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()));
        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getSelectWhoReviewsCase());
    }

}
