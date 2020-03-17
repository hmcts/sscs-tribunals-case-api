package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.ActionFurtherEvidenceAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@RunWith(JUnitParamsRunner.class)
public class ActionFurtherEvidenceAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private ActionFurtherEvidenceAboutToSubmitHandler actionFurtherEvidenceAboutToSubmitHandler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private FooterService footerService;

    private SscsCaseData sscsCaseData;

    private List<ScannedDocument> scannedDocumentList = new ArrayList<>();

    @Before
    public void setUp() {
        initMocks(this);
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService);

        when(callback.getEvent()).thenReturn(EventType.ACTION_FURTHER_EVIDENCE);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                .fileName("bla.pdf")
                .subtype("sscs1")
                .url(DocumentLink.builder().documentUrl("www.test.com").build())
                .scannedDate("2019-06-13T00:00:00.000")
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
        assertTrue(actionFurtherEvidenceAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(actionFurtherEvidenceAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters(method = "generateFurtherEvidenceActionListScenarios")
    public void givenACaseWithScannedDocuments_shouldMoveToSscsDocuments(@Nullable DynamicList furtherEvidenceActionList,
                                                                         @Nullable DynamicList originalSender,
                                                                         @Nullable String evidenceHandle,
                                                                         DocumentType expectedDocumentType) {
        sscsCaseData.setFurtherEvidenceAction(furtherEvidenceActionList);
        sscsCaseData.setOriginalSender(originalSender);
        sscsCaseData.setEvidenceHandled(evidenceHandle);

        PreSubmitCallbackResponse<SscsCaseData> response = null;
        try {
            response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        } catch (IllegalStateException e) {
            assertTrue(furtherEvidenceActionList == null || originalSender == null);
        }
        if (null != furtherEvidenceActionList && null != originalSender) {
            assertHappyPaths(expectedDocumentType, response);
        }
    }

    @Test
    @Parameters({"true", "false"})
    public void givenACaseWithScannedDocumentOfTypeCoversheet_shouldNotMoveToSscsDocumentsAndWarningShouldBeReturned(boolean ignoreWarnings) {
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

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(CollectionUtils.isEmpty(response.getData().getSscsDocument()));
        assertEquals("Yes", response.getData().getEvidenceHandled());

        if (ignoreWarnings) {
            assertEquals(0, response.getWarnings().size());
        } else {
            String warning = response.getWarnings().stream().findFirst().orElse("");
            assertEquals("Coversheet will be ignored, are you happy to proceed?", warning);
        }
    }

    private void assertHappyPaths(DocumentType expectedDocumentType,
                                  PreSubmitCallbackResponse<SscsCaseData> response) {

        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(1).getValue();
        assertEquals((expectedDocumentType.getLabel() != null ? expectedDocumentType.getLabel() : expectedDocumentType.getValue()) + " received on 13-06-2019", sscsDocumentDetail.getDocumentFileName());
        assertEquals(expectedDocumentType.getValue(), sscsDocumentDetail.getDocumentType());
        assertEquals("www.test.com", sscsDocumentDetail.getDocumentLink().getDocumentUrl());
        assertEquals("2019-06-13", sscsDocumentDetail.getDocumentDateAdded());
        assertEquals("123", sscsDocumentDetail.getControlNumber());
        assertEquals("No", response.getData().getSscsDocument().get(1).getValue().getEvidenceIssued());
        assertNull(response.getData().getScannedDocuments());
        assertEquals("Yes", response.getData().getEvidenceHandled());
    }

    private Object[] generateFurtherEvidenceActionListScenarios() {
        DynamicList furtherEvidenceActionListOtherDocuments =
            buildFurtherEvidenceActionItemListForGivenOption("otherDocumentManual",
                "Other document type - action manually");

        DynamicList furtherEvidenceActionListInterloc =
            buildFurtherEvidenceActionItemListForGivenOption("informationReceivedForInterlocJudge",
                "Information received for interlocutory review");

        DynamicList furtherEvidenceActionListIssueParties =
            buildFurtherEvidenceActionItemListForGivenOption("issueFurtherEvidence",
                "Issue further evidence to all parties");

        DynamicList appellantOriginalSender = buildOriginalSenderItemListForGivenOption("appellant",
            "Appellant (or Appointee)");
        DynamicList representativeOriginalSender = buildOriginalSenderItemListForGivenOption("representative",
            "Representative");
        DynamicList dwpOriginalSender = buildOriginalSenderItemListForGivenOption("dwp",
                "Dwp");

        return new Object[]{
            //other options scenarios
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, null, OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, "No", OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, "Yes", OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, "No", OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, null, OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, "Yes", OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, dwpOriginalSender, "No", OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, dwpOriginalSender, null, OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, dwpOriginalSender, "Yes", OTHER_DOCUMENT},
            //issue parties scenarios
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, null, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, "No", APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, "Yes", APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, "No", REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, "Yes", REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, null, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, dwpOriginalSender, "No", DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, dwpOriginalSender, "Yes", DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, dwpOriginalSender, null, DWP_EVIDENCE},
            //interloc scenarios
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, null, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, "No", APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, "Yes", APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, null, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, "No", REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, "Yes", REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, dwpOriginalSender, null, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, dwpOriginalSender, "No", DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, dwpOriginalSender, "Yes", DWP_EVIDENCE},
            //edge cases scenarios
            new Object[]{null, representativeOriginalSender, "", null}, //edge case: furtherEvidenceActionOption is null
            new Object[]{furtherEvidenceActionListIssueParties, null, null, null} //edge case: originalSender is null
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

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Other document received on 12-06-2019", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("2019-06-12", response.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Other document received on 13-06-2019", response.getData().getSscsDocument().get(1).getValue().getDocumentFileName());
        assertEquals("2019-06-13", response.getData().getSscsDocument().get(1).getValue().getDocumentDateAdded());
        assertEquals("exist.pdf", response.getData().getSscsDocument().get(2).getValue().getDocumentFileName());
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

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("www.test.com", response.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
    }

    @Test
    public void givenACaseWithNoScannedDocuments_thenAddAnErrorToResponse() {
        sscsCaseData.setScannedDocuments(null);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

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
        return new Callback<>(caseDetails, Optional.empty(), EventType.ACTION_FURTHER_EVIDENCE, false);
    }

    @Test
    public void givenIssueFurtherEvidence_shouldUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callback = buildCallback(ISSUE_FURTHER_EVIDENCE.getCode());

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("furtherEvidenceReceived", updated.getData().getDwpFurtherEvidenceStates());
    }

    @Test
    public void givenOtherDocument_shouldNotUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callback = buildCallback(OTHER_DOCUMENT_MANUAL.getCode());

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(updated.getData().getDwpFurtherEvidenceStates());
    }

    @Test
    public void givenNullFurtherEvidenceAction_shouldNotUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callback = buildCallback(null);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(updated.getData().getDwpFurtherEvidenceStates());
    }

    @Test
    public void givenADocumentWithNoUrl_thenAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("Testing.jpg").build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("No document URL so could not process", error);
        }
    }

    @Test
    @Parameters({"null", " ", "    "})
    public void givenADocumentWithNoDocFileName_thenAddAnErrorToResponse(@Nullable String filename) {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName(filename).url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("No document file name so could not process", error);
        }
    }
}