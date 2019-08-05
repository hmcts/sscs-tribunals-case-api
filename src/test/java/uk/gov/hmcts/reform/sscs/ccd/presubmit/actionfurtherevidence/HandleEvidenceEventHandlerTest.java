package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;

import java.time.LocalDateTime;
import java.util.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

@RunWith(JUnitParamsRunner.class)
public class HandleEvidenceEventHandlerTest {

    private HandleEvidenceEventHandler handleEvidenceEventHandler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private EvidenceManagementService evidenceManagementService;

    private SscsCaseData sscsCaseData;

    private List<ScannedDocument> scannedDocumentList = new ArrayList<>();

    @Before
    public void setUp() {
        initMocks(this);
        handleEvidenceEventHandler = new HandleEvidenceEventHandler(evidenceManagementService);

        when(callback.getEvent()).thenReturn(EventType.ACTION_FURTHER_EVIDENCE);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                .fileName("bla.pdf")
                .subtype("sscs1")
                .url(DocumentLink.builder().documentUrl("www.test.com").build())
                .scannedDate("2019-06-12T00:00:00.000")
                .controlNumber("123")
                .build()).build();

        ScannedDocument scannedDocument2 = ScannedDocument.builder()
            .value(ScannedDocumentDetails.builder()
                .fileName("bla2.pdf")
                .subtype("sscs2")
                .url(DocumentLink.builder().documentUrl("www.test2.com").build())
                .scannedDate("2019-06-12T00:00:00.000")
                .controlNumber("124")
                .build())
            .build();

        scannedDocumentList.add(scannedDocument);
        scannedDocumentList.add(scannedDocument2);
        DynamicList furtherEvidenceActionList = buildFurtherEvidenceActionItemListForGivenOption("otherDocumentManual",
            "Other document type - action manually");

        DynamicListItem value = new DynamicListItem("appellant", "Appellant (or Appointee)");
        DynamicList originalSender = new DynamicList(value, Collections.singletonList(value));

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1234")
            .scannedDocuments(scannedDocumentList)
            .furtherEvidenceAction(furtherEvidenceActionList)
            .originalSender(originalSender)
            .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAHandleEvidenceEvent_thenReturnTrue() {
        assertTrue(handleEvidenceEventHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handleEvidenceEventHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters(method = "generateFurtherEvidenceActionListScenarios")
    public void givenACaseWithScannedDocuments_shouldMoveToSscsDocuments(@Nullable DynamicList furtherEvidenceActionList,
                                                                         @Nullable DynamicList originalSender,
                                                                         @Nullable String evidenceHandle,
                                                                         String expectedDocumentType) {
        sscsCaseData.setFurtherEvidenceAction(furtherEvidenceActionList);
        sscsCaseData.setOriginalSender(originalSender);
        sscsCaseData.setEvidenceHandled(evidenceHandle);

        PreSubmitCallbackResponse<SscsCaseData> response = null;
        try {
            response = handleEvidenceEventHandler.handle(ABOUT_TO_SUBMIT, callback);
        } catch (IllegalStateException e) {
            assertTrue(furtherEvidenceActionList == null || originalSender == null);
        }
        if (null != furtherEvidenceActionList && null != originalSender) {
            assertHappyPaths(expectedDocumentType, response);
        }
    }

    @Test
    public void givenACaseWithScannedDocumentOfTypeCoversheet_shouldNotMoveToSscsDocuments() {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                .type("coversheet")
                .fileName("bla.pdf")
                .subtype("sscs1")
                .url(DocumentLink.builder().documentUrl("www.test.com").build())
                .scannedDate("2019-06-12T00:00:00.000")
                .controlNumber("123")
                .build()).build();
        scannedDocumentList = new ArrayList<>();
        scannedDocumentList.add(scannedDocument);
        sscsCaseData.setScannedDocuments(scannedDocumentList);
        sscsCaseData.setFurtherEvidenceAction(buildFurtherEvidenceActionItemListForGivenOption(APPELLANT_EVIDENCE.getValue(),
            "\"Appellant (or Appointee)"));
        sscsCaseData.setOriginalSender(buildOriginalSenderItemListForGivenOption("appellant",
            "Appellant (or Appointee)"));
        sscsCaseData.setEvidenceHandled("No");

        PreSubmitCallbackResponse<SscsCaseData> response = handleEvidenceEventHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertTrue(CollectionUtils.isEmpty(response.getData().getSscsDocument()));
        assertEquals("Yes", response.getData().getEvidenceHandled());
    }

    private void assertHappyPaths(String expectedDocumentType,
                                  PreSubmitCallbackResponse<SscsCaseData> response) {
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(0).getValue();
        assertEquals("bla.pdf", sscsDocumentDetail.getDocumentFileName());
        assertEquals(expectedDocumentType, sscsDocumentDetail.getDocumentType());
        assertEquals("www.test.com", sscsDocumentDetail.getDocumentLink().getDocumentUrl());
        assertEquals("2019-06-12", sscsDocumentDetail.getDocumentDateAdded());
        assertEquals("123", sscsDocumentDetail.getControlNumber());
        assertEquals("No", response.getData().getSscsDocument().get(0).getValue().getEvidenceIssued());
        assertNull(response.getData().getScannedDocuments());
        assertEquals("Yes", response.getData().getEvidenceHandled());
    }

    private Object[] generateFurtherEvidenceActionListScenarios() {
        DynamicList furtherEvidenceActionListOtherDocuments =
            buildFurtherEvidenceActionItemListForGivenOption("otherDocumentManual",
                "Other document type - action manually");

        DynamicList furtherEvidenceActionListInterloc =
            buildFurtherEvidenceActionItemListForGivenOption("informationReceivedForInterloc",
                "Information received for interlocutory review");

        DynamicList furtherEvidenceActionListIssueParties =
            buildFurtherEvidenceActionItemListForGivenOption("issueFurtherEvidence",
                "Issue further evidence to all parties");

        DynamicList appellantOriginalSender = buildOriginalSenderItemListForGivenOption("appellant",
            "Appellant (or Appointee)");
        DynamicList representativeOriginalSender = buildOriginalSenderItemListForGivenOption("representative",
            "Representative");

        return new Object[]{
            //other options scenarios
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, null, "Other document"},
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, "No", "Other document"},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, "No", "Other document"},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, null, "Other document"},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, "Yes", "Other document"},
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, "Yes", "Other document"},
            //issue parties scenarios
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, null, "appellantEvidence"},
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, "No", "appellantEvidence"},
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, "Yes", "appellantEvidence"},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, "No", "representativeEvidence"},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, "Yes", "representativeEvidence"},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, null, "representativeEvidence"},
            //interloc scenarios
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, null, "appellantEvidence"},
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, "No", "appellantEvidence"},
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, "Yes", "appellantEvidence"},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, null, "representativeEvidence"},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, "No", "representativeEvidence"},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, "Yes", "representativeEvidence"},
            //edge cases scenarios
            new Object[]{null, representativeOriginalSender, "", ""}, //edge case: furtherEvidenceActionOption is null
            new Object[]{furtherEvidenceActionListIssueParties, null, null, ""} //edge case: originalSender is null
        };
    }

    private DynamicList buildOriginalSenderItemListForGivenOption(String code, String label) {
        DynamicListItem value = new DynamicListItem(code, label);
        return new DynamicList(value, Collections.singletonList(value));
    }

    private DynamicList buildFurtherEvidenceActionItemListForGivenOption(String code, String label) {
        DynamicListItem selectedOption = new DynamicListItem(code, label);
        return new DynamicList(selectedOption,
            Collections.singletonList(selectedOption));
    }

    @Test
    public void givenACaseWithScannedDocumentsAndSscsCaseDocuments_thenAppendNewDocumentsToSscsDocumentsList() {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        SscsDocument doc = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentFileName("exist.pdf")
                .build())
            .build();
        sscsDocuments.add(doc);

        sscsCaseData.setScannedDocuments(scannedDocumentList);
        sscsCaseData.setSscsDocument(sscsDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handleEvidenceEventHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals("exist.pdf", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("bla.pdf", response.getData().getSscsDocument().get(1).getValue().getDocumentFileName());
        assertNull(response.getData().getScannedDocuments());
    }

    @Test
    public void givenACaseWithScannedDocumentWithEmptyValues_thenHandleTheDocument() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .url(DocumentLink.builder().documentUrl("www.test.com").build())
                        .build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = handleEvidenceEventHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals("www.test.com", response.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
    }

    @Test
    public void givenACaseWithNoScannedDocuments_thenAddAnErrorToResponse() {
        sscsCaseData.setScannedDocuments(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handleEvidenceEventHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("No further evidence to process", error);
        }
    }

    private Callback<SscsCaseData> buildCallback(String dynamicListItemCode) {
        DynamicList dynamicList = new DynamicList(new DynamicListItem(dynamicListItemCode, "label"),
                Collections.singletonList(new DynamicListItem(dynamicListItemCode, "label")));
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .originalSender(dynamicList)
                .furtherEvidenceAction(dynamicList)
                .scannedDocuments(Collections.singletonList(ScannedDocument.builder().build()))
                .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), EventType.ACTION_FURTHER_EVIDENCE);
    }

    @Test
    public void givenIssueFurtherEvidence_shouldUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callback = buildCallback(ISSUE_FURTHER_EVIDENCE.code);

        PreSubmitCallbackResponse<SscsCaseData> updated = handleEvidenceEventHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals("furtherEvidenceReceived", updated.getData().getDwpFurtherEvidenceStates());
    }

    @Test
    public void givenOtherDocument_shouldNotUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callback = buildCallback(OTHER_DOCUMENT_MANUAL.code);

        PreSubmitCallbackResponse<SscsCaseData> updated = handleEvidenceEventHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertNull(updated.getData().getDwpFurtherEvidenceStates());
    }

    @Test
    public void givenNullFurtherEvidenceAction_shouldNotUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callback = buildCallback(null);

        PreSubmitCallbackResponse<SscsCaseData> updated = handleEvidenceEventHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertNull(updated.getData().getDwpFurtherEvidenceStates());
    }

    @Test
    public void givenADocumentWithNoUrl_thenAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("Testing.jpg").build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = handleEvidenceEventHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("No document URL so could not process", error);
        }
    }

    @Test
    public void givenADocumentWithNoDocFileName_thenAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = handleEvidenceEventHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("No document file name so could not process", error);
        }
    }

    @Test
    @Parameters({"", "A", "B", "C", "D", "X", "Y"})
    public void canWorkOutTheNextAppendixValue(String currentAppendix) {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        if (!currentAppendix.equals("")) {
            SscsDocument theDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().appendix(currentAppendix).build()).build();
            sscsDocuments.add(theDocument);

            if (currentAppendix.toCharArray()[0] > 'A') {
                SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().appendix("A").build()).build();
                sscsDocuments.add(document);
            }
            if (currentAppendix.toCharArray()[0] > 'B') {
                SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().appendix("B").build()).build();
                sscsDocuments.add(document);
            }
            if (currentAppendix.toCharArray()[0] > 'C') {
                SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().appendix("C").build()).build();
                sscsDocuments.add(document);
            }
        }

        String actual = handleEvidenceEventHandler.getNextBundleAddition(sscsDocuments);

        String expected = currentAppendix.equals("") ? "A" : String.valueOf((char)(currentAppendix.charAt(0) +  1));
        assertEquals(expected, actual);
    }

    @Test
    @Parameters({"Z", "Z1", "Z9", "Z85", "Z100"})
    public void canWorkOutTheNextAppendixValueAfterZ(String currentAppendix) {
        SscsDocument theDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().appendix(currentAppendix).build()).build();
        SscsDocument documentA = SscsDocument.builder().value(SscsDocumentDetails.builder().appendix("A").build()).build();
        SscsDocument documentB = SscsDocument.builder().value(SscsDocumentDetails.builder().appendix("B").build()).build();
        SscsDocument documentC = SscsDocument.builder().value(SscsDocumentDetails.builder().appendix("Y").build()).build();
        List<SscsDocument> sscsDocuments = new ArrayList<>(Arrays.asList(theDocument, documentA, documentB, documentC));

        int index = currentAppendix.length() == 1 ? 0 : (Integer.valueOf(currentAppendix.substring(1)));

        if (index > 0) {
            SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().appendix("Z").build()).build();
            sscsDocuments.add(document);
        }
        if (index > 8) {
            SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().appendix("Z7").build()).build();
            sscsDocuments.add(document);
        }
        if (index > 30) {
            SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().appendix("Z28").build()).build();
            sscsDocuments.add(document);
        }
        if (index > 80) {
            SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().appendix("Z79").build()).build();
            sscsDocuments.add(document);
        }

        String expected = index == 0 ? "Z1" : "Z" + (index + 1);
        String actual = handleEvidenceEventHandler.getNextBundleAddition(sscsDocuments);
        assertEquals(expected, actual);
    }

    @Test
    @Parameters({"Z!", "Z3$", "ZN"})
    public void nextAppendixCanHandleInvalidDataThatAreNotNumbersAfterZ(String currentAppendix) {
        SscsDocument theDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().appendix(currentAppendix).build()).build();
        String actual = handleEvidenceEventHandler.getNextBundleAddition(Collections.singletonList(theDocument));
        assertEquals("[", actual);
    }

}