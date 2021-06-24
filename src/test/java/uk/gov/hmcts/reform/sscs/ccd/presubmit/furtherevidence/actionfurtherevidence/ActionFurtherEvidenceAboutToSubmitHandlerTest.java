package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DWP_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.JOINT_PARTY_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.URGENT_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.ActionFurtherEvidenceAboutToSubmitHandler.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.ScannedDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.BundleAdditionFilenameBuilder;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@RunWith(JUnitParamsRunner.class)
public class ActionFurtherEvidenceAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String NO = "No";
    private static final String YES = "Yes";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private ActionFurtherEvidenceAboutToSubmitHandler actionFurtherEvidenceAboutToSubmitHandler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private FooterService footerService;

    private SscsCaseData sscsCaseData;

    private List<ScannedDocument> scannedDocumentList = new ArrayList<>();
    private final BundleAdditionFilenameBuilder bundleAdditionFilenameBuilder = new BundleAdditionFilenameBuilder();

    @Before
    public void setUp() {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder);

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
        DynamicList furtherEvidenceActionList = buildFurtherEvidenceActionItemListForGivenOption(ISSUE_FURTHER_EVIDENCE.getCode(),
                ISSUE_FURTHER_EVIDENCE.getLabel());

        DynamicListItem value = new DynamicListItem("appellant", "Appellant (or Appointee)");
        DynamicList originalSender = new DynamicList(value, Collections.singletonList(value));

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1234")
            .scannedDocuments(scannedDocumentList)
            .furtherEvidenceAction(furtherEvidenceActionList)
            .originalSender(originalSender)
            .appeal(Appeal.builder().appellant(Appellant.builder().address(Address.builder().line1("My Road").postcode("TS1 2BA").build()).build()).build())
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
    public void givenAConfidentialCaseWithScannedDocumentsAndEditedDocument_shouldMoveToSscsDocumentsWithEditedDocument() {
        sscsCaseData.setIsConfidentialCase(YesNo.YES);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .fileName("bla3.pdf")
                        .subtype("sscs1")
                        .url(DocumentLink.builder().documentUrl("www.test.com").build())
                        .editedUrl(DocumentLink.builder().documentUrl("www.edited.com").build())
                        .scannedDate("2020-06-13T00:00:00.000")
                        .controlNumber("123")
                        .build()).build();

        scannedDocumentList.add(scannedDocument);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(0).getValue();
        assertEquals("Appellant evidence received on 13-06-2020", sscsDocumentDetail.getDocumentFileName());
        assertEquals("appellantEvidence", sscsDocumentDetail.getDocumentType());
        assertEquals("www.test.com", sscsDocumentDetail.getDocumentLink().getDocumentUrl());
        assertEquals("www.edited.com", sscsDocumentDetail.getEditedDocumentLink().getDocumentUrl());
        assertEquals("2020-06-13", sscsDocumentDetail.getDocumentDateAdded());
        assertEquals("123", sscsDocumentDetail.getControlNumber());
        assertEquals(NO, response.getData().getSscsDocument().get(1).getValue().getEvidenceIssued());
        assertNull(response.getData().getScannedDocuments());
        assertEquals(YES, response.getData().getEvidenceHandled());
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
        sscsCaseData.setEvidenceHandled(NO);

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(CollectionUtils.isEmpty(response.getData().getSscsDocument()));
        assertEquals(YES, response.getData().getEvidenceHandled());

        if (ignoreWarnings) {
            assertEquals(0, response.getWarnings().size());
        } else {
            String warning = response.getWarnings().stream().findFirst().orElse("");
            assertEquals("Coversheet will be ignored, are you happy to proceed?", warning);
        }
    }

    @Test
    @Parameters({"true", "false"})
    public void givenACaseWithAnEmptyScannedDocumentType_shouldMoveToSscsDocumentsAndWarningShouldBeReturned(boolean ignoreWarnings) {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                .type(null)
                .fileName("bla.pdf")
                .subtype("sscs1")
                .url(DocumentLink.builder().documentUrl("www.test.com").build())
                .scannedDate("2019-06-12T00:00:00.000")
                .controlNumber("123")
                .build()).build();
        scannedDocumentList = new ArrayList<>();
        scannedDocumentList.add(scannedDocument);
        sscsCaseData.setScannedDocuments(scannedDocumentList);
        sscsCaseData.setFurtherEvidenceAction(
            buildFurtherEvidenceActionItemListForGivenOption(APPELLANT_EVIDENCE.getValue(),
                "\"Appellant (or Appointee)"));
        sscsCaseData.setOriginalSender(buildOriginalSenderItemListForGivenOption("appellant",
            "Appellant (or Appointee)"));
        sscsCaseData.setEvidenceHandled(NO);

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        PreSubmitCallbackResponse<SscsCaseData> response =
            actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(response.getData().getSscsDocument().size(), 1);
        assertEquals(YES, response.getData().getEvidenceHandled());

        if (ignoreWarnings) {
            assertEquals(0, response.getWarnings().size());
        } else {
            String warning = response.getWarnings().stream().findFirst().orElse("");
            assertEquals("Document type is empty, are you happy to proceed?", warning);
        }
    }

    @Test
    public void edgeCaseWhereEvidenceIsSentToBulkPrintSinceNoFurtherActionIsSelected() {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .type(ScannedDocumentType.REINSTATEMENT_REQUEST.getValue())
                        .fileName("bla.pdf")
                        .subtype("sscs1")
                        .url(DocumentLink.builder().documentUrl("www.test.com").build())
                        .scannedDate("2019-06-12T00:00:00.000")
                        .controlNumber("123")
                        .build()).build();
        scannedDocumentList = new ArrayList<>();
        scannedDocumentList.add(scannedDocument);
        sscsCaseData.setScannedDocuments(scannedDocumentList);
        sscsCaseData.setFurtherEvidenceAction(
                buildFurtherEvidenceActionItemListForGivenOption(null, null));
        sscsCaseData.setOriginalSender(buildOriginalSenderItemListForGivenOption("appellant",
                "Appellant (or Appointee)"));
        sscsCaseData.setEvidenceHandled(NO);

        when(callback.isIgnoreWarnings()).thenReturn(false);

        PreSubmitCallbackResponse<SscsCaseData> response =
                actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(response.getData().getSscsDocument().size(), 1);
        assertEquals(response.getData().getSscsDocument().get(0).getValue().getEvidenceIssued(), NO);
        assertEquals(YES, response.getData().getEvidenceHandled());
        assertEquals(0, response.getWarnings().size());
    }

    private void assertHappyPaths(DocumentType expectedDocumentType,
        PreSubmitCallbackResponse<SscsCaseData> response) {

        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(1).getValue();
        assertEquals((expectedDocumentType.getLabel() != null ? expectedDocumentType.getLabel() : expectedDocumentType.getValue()) + " received on 13-06-2019", sscsDocumentDetail.getDocumentFileName());
        assertEquals(expectedDocumentType.getValue(), sscsDocumentDetail.getDocumentType());
        assertEquals("www.test.com", sscsDocumentDetail.getDocumentLink().getDocumentUrl());
        assertEquals("2019-06-13", sscsDocumentDetail.getDocumentDateAdded());
        assertEquals("123", sscsDocumentDetail.getControlNumber());
        String expectedEvidenceIssued = ACTIONS_THAT_REQUIRES_EVIDENCE_ISSUED_SET_TO_YES_AND_NOT_BULK_PRINTED.contains(response.getData().getFurtherEvidenceAction().getValue().getCode()) ? YES : NO;
        assertEquals(expectedEvidenceIssued, response.getData().getSscsDocument().get(1).getValue().getEvidenceIssued());
        assertNull(response.getData().getScannedDocuments());
        assertEquals(YES, response.getData().getEvidenceHandled());
    }

    private Object[] generateFurtherEvidenceActionListScenarios() {
        DynamicList furtherEvidenceActionListOtherDocuments =
            buildFurtherEvidenceActionItemListForGivenOption("otherDocumentManual",
                "Other document type - action manually");

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
        DynamicList jointPartyOriginalSender = buildOriginalSenderItemListForGivenOption("jointParty",
            "Joint Party");

        return new Object[]{
            //other options scenarios
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, null, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, NO, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, YES, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, NO, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, null, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, YES, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, dwpOriginalSender, NO, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, dwpOriginalSender, null, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, dwpOriginalSender, YES, DWP_EVIDENCE},
            //issue parties scenarios
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, null, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, NO, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, YES, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, NO, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, YES, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, null, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, dwpOriginalSender, NO, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, dwpOriginalSender, YES, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, dwpOriginalSender, null, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, jointPartyOriginalSender, NO, JOINT_PARTY_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, jointPartyOriginalSender, YES, JOINT_PARTY_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, jointPartyOriginalSender, null, JOINT_PARTY_EVIDENCE},
            //interloc scenarios
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, null, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, NO, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, YES, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, null, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, NO, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, YES, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, dwpOriginalSender, null, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, dwpOriginalSender, NO, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, dwpOriginalSender, YES, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, jointPartyOriginalSender, null, JOINT_PARTY_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, jointPartyOriginalSender, NO, JOINT_PARTY_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, jointPartyOriginalSender, YES, JOINT_PARTY_EVIDENCE},
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

        assertEquals("Appellant evidence received on 12-06-2019", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("2019-06-12", response.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Appellant evidence received on 13-06-2019", response.getData().getSscsDocument().get(1).getValue().getDocumentFileName());
        assertEquals("2019-06-13", response.getData().getSscsDocument().get(1).getValue().getDocumentDateAdded());
        assertEquals("exist.pdf", response.getData().getSscsDocument().get(2).getValue().getDocumentFileName());
        assertNotNull(response.getData().getSscsDocument().get(0).getValue().getControlNumber());
        assertNull(response.getData().getScannedDocuments());
    }

    @Test
    public void givenAWelshCaseWithScannedDocuments_thenSetTranslationStatusToRequired() {
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
        sscsCaseData.setLanguagePreferenceWelsh(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Appellant evidence received on 12-06-2019", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("2019-06-12", response.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED, response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());

        assertEquals("Appellant evidence received on 13-06-2019", response.getData().getSscsDocument().get(1).getValue().getDocumentFileName());
        assertEquals("2019-06-13", response.getData().getSscsDocument().get(1).getValue().getDocumentDateAdded());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED, response.getData().getSscsDocument().get(1).getValue().getDocumentTranslationStatus());

        assertEquals("exist.pdf", response.getData().getSscsDocument().get(2).getValue().getDocumentFileName());
        assertNull(response.getData().getSscsDocument().get(2).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getScannedDocuments());
        assertEquals(YES, response.getData().getTranslationWorkOutstanding());
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

    private Callback<SscsCaseData> buildCallback(String dynamicListItemCode, State state) {
        DynamicList dynamicList = new DynamicList(new DynamicListItem(dynamicListItemCode, "label"),
            Collections.singletonList(new DynamicListItem(dynamicListItemCode, "label")));
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .originalSender(dynamicList)
            .furtherEvidenceAction(dynamicList)
            .state(state)
            .scannedDocuments(Collections.singletonList(ScannedDocument.builder().build()))
            .appeal(Appeal.builder().appellant(Appellant.builder().address(Address.builder().line1("My Road").postcode("TS1 2BA").build()).build()).build())
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                state, sscsCaseData, LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), EventType.ACTION_FURTHER_EVIDENCE, false);
    }

    @Test
    public void givenIssueFurtherEvidenceWhenStateNotWithDwp_shouldUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callback = buildCallback(ISSUE_FURTHER_EVIDENCE.getCode(), State.INTERLOCUTORY_REVIEW_STATE);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(FURTHER_EVIDENCE_RECEIVED, updated.getData().getDwpFurtherEvidenceStates());
        assertEquals(DwpState.FE_RECEIVED.getId(), updated.getData().getDwpState());
    }

    @Test
    public void givenIssueFurtherEvidenceAndStateIsWithDwp_shouldNotUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callback = buildCallback(ISSUE_FURTHER_EVIDENCE.getCode(), State.WITH_DWP);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(updated.getData().getDwpFurtherEvidenceStates());
    }

    @Test
    @Parameters(method = "generateIssueFurtherEvidenceAddressEmptyScenarios")
    public void givenIssueFurtherEvidenceAndEmptyAppellantAddress_shouldReturnAnErrorToUser(Appeal appeal, String... parties) {
        Callback<SscsCaseData> callback = buildCallback(ISSUE_FURTHER_EVIDENCE.getCode(), State.INTERLOCUTORY_REVIEW_STATE);

        callback.getCaseDetails().getCaseData().setAppeal(appeal);
        PreSubmitCallbackResponse<SscsCaseData> result = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        for (String party : parties) {
            String expectedError = "Address details are missing for the " + party + ", please validate or process manually";
            assertTrue(result.getErrors().contains(expectedError));
        }
    }

    private Object[] generateIssueFurtherEvidenceAddressEmptyScenarios() {

        return new Object[]{
            new Object[]{Appeal.builder().appellant(Appellant.builder().address(Address.builder().build()).build()).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().address(Address.builder().line1("Line1").build()).build()).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().address(Address.builder().postcode("TS1 2BA").build()).build()).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().address(null).build()).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(null).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().isAppointee(YES).build()).build(), "Appointee"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().isAppointee(YES).appointee(Appointee.builder().build()).build()).build(), "Appointee"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().isAppointee(YES).appointee(Appointee.builder().address(Address.builder().build()).build()).build()).build(), "Appointee"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().isAppointee(YES).appointee(Appointee.builder().address(null).build()).build()).build(), "Appointee"},
            new Object[]{Appeal.builder().rep(Representative.builder().hasRepresentative(YES).address(Address.builder().build()).build()).appellant(Appellant.builder().address(Address.builder().line1("The road").build()).build()).build(), "Representative"},
            new Object[]{Appeal.builder().rep(Representative.builder().hasRepresentative(YES).address(null).build()).appellant(Appellant.builder().address(Address.builder().line1("The road").build()).build()).build(), "Representative"},
            new Object[]{Appeal.builder().rep(Representative.builder().hasRepresentative(YES).address(Address.builder().build()).build()).appellant(Appellant.builder().address(null).build()).build(), "Appellant", "Representative"},
        };
    }

    @Test
    public void givenOtherDocument_shouldNotUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callback = buildCallback(OTHER_DOCUMENT_MANUAL.getCode(), State.INTERLOCUTORY_REVIEW_STATE);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(updated.getData().getDwpFurtherEvidenceStates());
    }

    @Test
    public void givenNullFurtherEvidenceAction_shouldNotUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callback = buildCallback(null, State.INTERLOCUTORY_REVIEW_STATE);

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
    public void givenANonConfidentialCaseAndEditedDocumentPopulated_thenAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("Testing.jpg").url(DocumentLink.builder().documentUrl("test.com").build())
                        .editedUrl(DocumentLink.builder().documentUrl("test").build()).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);
        sscsCaseData.setIsConfidentialCase(YesNo.NO);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Case is not marked as confidential so cannot upload an edited document", error);
        }
    }

    @Test
    public void givenAConfidentialCaseAndEditedDocumentPopulated_thenDoNotAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("Testing.jpg").url(DocumentLink.builder().documentUrl("test.com").build())
                        .editedUrl(DocumentLink.builder().documentUrl("test").build()).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);
        sscsCaseData.setIsConfidentialCase(YesNo.YES);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

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

    @Test
    @Parameters({"null", "DORMANT_APPEAL_STATE", "VOID_STATE"})
    public void shouldReviewByJudgeAndUpdatePreviousStateWhenActionManuallyAndHasReinstatementRequestDocument(@Nullable State previousState) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder);

        List<ScannedDocument> docs = new ArrayList<>();
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.setPreviousState(previousState);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.REINSTATEMENT_REQUEST.getValue())
                .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData.getPreviousState());
        assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getReinstatementOutcome());
        assertEquals(LocalDate.now(), sscsCaseData.getReinstatementRegistered());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    @Parameters({"VALID_APPEAL", "READY_TO_LIST"})
    public void shouldReviewByJudgeButNotUpdatePreviousStateWhenActionManuallyAndHasReinstatementRequestDocument(@Nullable State previousState) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder);


        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.setPreviousState(previousState);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.REINSTATEMENT_REQUEST.getValue())
                .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(previousState, sscsCaseData.getPreviousState());
        assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getReinstatementOutcome());
        assertEquals(LocalDate.now(), sscsCaseData.getReinstatementRegistered());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void skipReinstatementRequestSettingIfWelsh() {

        State previousState = State.VALID_APPEAL;

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder);

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.setPreviousState(previousState);
        sscsCaseData.setLanguagePreferenceWelsh("yes");

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.REINSTATEMENT_REQUEST.getValue())
                        .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(previousState, sscsCaseData.getPreviousState());
        assertNull(sscsCaseData.getReinstatementOutcome());
        assertNull(sscsCaseData.getReinstatementRegistered());
        assertNull(sscsCaseData.getInterlocReviewState());
        assertTrue(sscsCaseData.isTranslationWorkOutstanding());
    }

    @Test
    @Parameters({"VALID_APPEAL", "READY_TO_LIST"})
    public void shouldReviewByJudgeButNotUpdatePreviousStateWhenActionManuallyAndHasUrgentHearingRequestDocument(@Nullable State previousState) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder);


        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.setPreviousState(previousState);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.URGENT_HEARING_REQUEST.getValue())
                        .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(previousState, sscsCaseData.getPreviousState());
        assertEquals(1, response.getData().getSscsDocument().stream().filter(d -> URGENT_HEARING_REQUEST.getValue().equals(d.getValue().getDocumentType())).count());
    }

    @Test
    @Parameters({"READY_TO_LIST,1", "RESPONSE_RECEIVED,1", "DORMANT_APPEAL_STATE,1", "HEARING,1", "WITH_DWP,1", "VALID_APPEAL,0"})
    public void shouldSetBundleAdditionBasedOnPreviousState(@Nullable State state, int occurrs) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder);

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(ISSUE_FURTHER_EVIDENCE.code, ISSUE_FURTHER_EVIDENCE.label));
        when(caseDetails.getState()).thenReturn(state);
        when(footerService.getNextBundleAddition(any())).thenReturn("A");

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.URGENT_HEARING_REQUEST.getValue())
                        .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();

        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(occurrs, response.getData().getSscsDocument().stream().filter(doc -> "A".equals(doc.getValue().getBundleAddition())).count());
    }

    @Test
    @Parameters({"ISSUE_FURTHER_EVIDENCE,No,1", "OTHER_DOCUMENT_MANUAL,Yes,1", "OTHER_DOCUMENT_MANUAL,No,0", "SEND_TO_INTERLOC_REVIEW_BY_JUDGE,Yes,1",
            "SEND_TO_INTERLOC_REVIEW_BY_JUDGE,No,0", "INFORMATION_RECEIVED_FOR_INTERLOC_TCW,Yes,0"})
    public void shouldSetBundleAdditionBasedOnFurtherEvidenceActionType(FurtherEvidenceActionDynamicListItems actionType, String includeInBundle, int occurs) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder);

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(actionType.code, actionType.label));

        when(caseDetails.getState()).thenReturn(READY_TO_LIST);
        when(footerService.getNextBundleAddition(any())).thenReturn("A");

        ScannedDocument scannedDocument = ScannedDocument
                .builder()
                .value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.URGENT_HEARING_REQUEST.getValue())
                        .url(DocumentLink.builder().documentUrl("test.com").build()).includeInBundle(includeInBundle).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();

        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(occurs, response.getData().getSscsDocument().stream().filter(doc -> "A".equals(doc.getValue().getBundleAddition())).count());
    }

    @Test
    public void shouldNotUpdateInterlocReviewStateWhenActionManuallyAndHasNoReinstatementRequestDocument() {
        sscsCaseData.setPreviousState(null);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertNull(sscsCaseData.getPreviousState());
        assertNull(sscsCaseData.getReinstatementOutcome());
        assertNull(sscsCaseData.getReinstatementRegistered());
        assertNull(sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void shouldNotUpdateInterlocReviewStateWhenActionManuallyAndHasNoUrgentHearingRequestDocument() {
        sscsCaseData.setPreviousState(null);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertNull(sscsCaseData.getPreviousState());
        assertNull(sscsCaseData.getInterlocReviewState());
        assertEquals(0, response.getData().getSscsDocument().stream().filter(d -> URGENT_HEARING_REQUEST.getValue().equals(d.getValue().getDocumentType())).count());
    }

    @Test
    public void shouldNotUpdateInterlocReviewStateWhenNotActionManuallyAndHasReinstatementRequestDocument() {
        sscsCaseData.setPreviousState(null);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.REINSTATEMENT_REQUEST.getValue())
                .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertNull(sscsCaseData.getPreviousState());
        assertNull(sscsCaseData.getReinstatementOutcome());
        assertNull(sscsCaseData.getReinstatementRegistered());
        assertNull(sscsCaseData.getInterlocReviewState());
    }

    @Test
    @Parameters({"SEND_TO_INTERLOC_REVIEW_BY_JUDGE", "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE"})
    public void givenConfidentialRequestWhenJointPartyExistsFromOriginalSenderAppellant_thenUpdateCaseWithConfidentialFields(FurtherEvidenceActionDynamicListItems furtherEvidenceActionDynamicListItem) {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(furtherEvidenceActionDynamicListItem.code, furtherEvidenceActionDynamicListItem.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.setJointParty(YES);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());

        assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant().getRequestOutcome());
        assertNull(sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestOutcomeAppellant().getDate());
    }

    @Test
    @Parameters({
            "SEND_TO_INTERLOC_REVIEW_BY_JUDGE, Yes",
            "SEND_TO_INTERLOC_REVIEW_BY_JUDGE, No",
            "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE, Yes",
            "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE, No",
            "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE, null"
    })
    public void givenConfidentialRequestWhenJointPartyExistsAndAlreadyHasConfidentialityFromOriginalSenderAppellant_thenUpdateCaseWithConfidentialFieldsAndDisplayWarning(
            FurtherEvidenceActionDynamicListItems furtherEvidenceActionDynamicListItem,
            @Nullable String isProgressingViaGaps
    ) {
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(furtherEvidenceActionDynamicListItem.code, furtherEvidenceActionDynamicListItem.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.setJointParty(YES);
        sscsCaseData.setIsProgressingViaGaps(isProgressingViaGaps);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedRequestOutcome(RequestOutcome.GRANTED));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());

        assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant().getRequestOutcome());
        assertEquals(createDatedRequestOutcome(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestOutcomeAppellant().getDate());
    }

    @Test
    public void givenAnAppellantHasConfidentialityRequestGrantedAndAppellantSendsFurtherEvidence_thenDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                        .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        assertEquals(1, response.getWarnings().size());
        assertEquals("This case has a confidentiality flag, ensure any evidence from the appellant (or appointee) has confidential information redacted", response.getWarnings().iterator().next());
    }

    @Test
    public void givenAJointPartyHasConfidentialityRequestGrantedAndJointPartySendsFurtherEvidence_thenDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                        .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        assertEquals(1, response.getWarnings().size());
        assertEquals("This case has a confidentiality flag, ensure any evidence from the joint party has confidential information redacted", response.getWarnings().iterator().next());
    }

    @Test
    public void givenAnAppellantHasConfidentialityRequestGrantedAndJointPartySendsFurtherEvidence_thenDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                        .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenAJointPartyHasConfidentialityRequestGrantedAndAppellantSendsFurtherEvidence_thenDoNotDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                        .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    @Parameters({"SEND_TO_INTERLOC_REVIEW_BY_JUDGE", "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE"})
    public void givenConfidentialRequestWhenJointPartyDoesNotExistFromOriginalSenderAppellant_thenShowError(FurtherEvidenceActionDynamicListItems furtherEvidenceActionDynamicListItem) {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(furtherEvidenceActionDynamicListItem.code, furtherEvidenceActionDynamicListItem.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertEquals("Document type \"Confidentiality Request\" is invalid as there is no joint party on the case", response.getErrors().iterator().next());
        assertNull(sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertNull(sscsCaseData.getConfidentialityRequestOutcomeJointParty());
    }

    @Test
    public void givenConfidentialRequestWhenJointPartyExistsFromOriginalSenderJointParty_thenUpdateCaseWithConfidentialFields() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));
        sscsCaseData.setJointParty(YES);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty().getRequestOutcome());
        assertNull(sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestOutcomeJointParty().getDate());
    }

    @Test
    public void givenConfidentialRequestWhenJointPartyDoesNotExistFromOriginalSenderJointParty_thenUpdateCaseWithConfidentialFields() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertEquals("Document type \"Confidentiality Request\" is invalid as there is no joint party on the case", response.getErrors().iterator().next());
        assertNull(sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertNull(sscsCaseData.getConfidentialityRequestOutcomeAppellant());
    }


    @Test
    public void givenConfidentialRequestFromRep_thenShowAnError() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel()));
        sscsCaseData.setJointParty(YES);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        for (String error : response.getErrors()) {
            assertEquals("Original sender must be appellant or joint party for a confidential document", error);
        }
    }

    @Test
    public void givenConfidentialRequestWithInvalidFurtherEvidenceAction_thenShowAnError() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(ISSUE_FURTHER_EVIDENCE.code, ISSUE_FURTHER_EVIDENCE.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.setJointParty(YES);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        for (String error : response.getErrors()) {
            assertEquals("Further evidence action must be 'Send to Interloc - Review by Judge' or 'Information received for Interloc - send to Judge' for a confidential document", error);
        }
    }

    @Test
    @Parameters({"APPELLANT", "REPRESENTATIVE", "DWP", "JOINT_PARTY", "HMCTS"})
    public void shouldIssueToAllParties_willAddFooterTextToDocument(PartyItemList sender) {
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(ISSUE_FURTHER_EVIDENCE.code, ISSUE_FURTHER_EVIDENCE.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(sender.getCode(), sender.getLabel()));
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CHERISHED.getValue())
                        .includeInBundle(YES)
                        .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);

        when(caseDetails.getState()).thenReturn(READY_TO_LIST);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        verify(footerService).addFooter(eq(scannedDocument.getValue().getUrl()), eq(sender.getDocumentFooter()), eq(null));
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
        assertEquals(sender.getDocumentType().getValue(), response.getData().getSscsDocument().get(0).getValue().getDocumentType());
    }

    @Test
    @Parameters({
        "ISSUE_FURTHER_EVIDENCE,Yes,1",
        "ISSUE_FURTHER_EVIDENCE,No,1",
        "OTHER_DOCUMENT_MANUAL,Yes,1",
        "OTHER_DOCUMENT_MANUAL,No,0",
        "SEND_TO_INTERLOC_REVIEW_BY_TCW,Yes,1",
        "SEND_TO_INTERLOC_REVIEW_BY_TCW,No,0"
    })
    public void shouldAddDocumentToBundleBasedOnFurtherEvidenceActionType(FurtherEvidenceActionDynamicListItems actionType, String includeInBundle, int occurs) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder);

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(actionType.code, actionType.label));

        when(caseDetails.getState()).thenReturn(READY_TO_LIST);
        when(footerService.getNextBundleAddition(any())).thenReturn("A");

        ScannedDocument scannedDocument = ScannedDocument
            .builder()
            .value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                    .url(DocumentLink.builder().documentUrl("test.com").build()).includeInBundle(includeInBundle).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(occurs, response.getData().getSscsDocument().stream().filter(doc -> "A".equals(doc.getValue().getBundleAddition())).count());
    }

    private DatedRequestOutcome createDatedRequestOutcome(RequestOutcome requestOutcome) {
        return DatedRequestOutcome.builder().date(LocalDate.now().minusDays(1))
            .requestOutcome(requestOutcome).build();
    }

    @Test
    public void isThreadSafe() throws Exception {

        DynamicList furtherEvidenceActionList =
                buildFurtherEvidenceActionItemListForGivenOption(OTHER_DOCUMENT_MANUAL.getCode(),
                        OTHER_DOCUMENT_MANUAL.getLabel());

        DynamicList originalSender = buildOriginalSenderItemListForGivenOption("appellant",
                "Appellant (or Appointee)");

        String evidenceHandle = null;

        int numberOfTasks = 3;
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        AtomicBoolean noOverwrite = new AtomicBoolean(true);

        try {
            for (int i = 0; i < numberOfTasks; i++) {

                String expectedId1 = String.valueOf(i);

                SscsCaseData thisCaseData = SscsCaseData.builder()
                        .ccdCaseId(expectedId1)
                        .scannedDocuments(scannedDocumentList)
                        .furtherEvidenceAction(furtherEvidenceActionList)
                        .originalSender(originalSender)
                        .appeal(Appeal.builder().appellant(Appellant.builder().address(Address.builder().line1("My Road").postcode("TS1 2BA").build()).build()).build())
                        .build();

                thisCaseData.setFurtherEvidenceAction(furtherEvidenceActionList);
                thisCaseData.setOriginalSender(originalSender);
                thisCaseData.setEvidenceHandled(evidenceHandle);

                Callback<SscsCaseData> mockCallback = mock(Callback.class);
                CaseDetails<SscsCaseData> mockCaseDetails = mock(CaseDetails.class);

                when(mockCallback.getEvent()).thenReturn(EventType.ACTION_FURTHER_EVIDENCE);
                when(mockCallback.getCaseDetails()).thenReturn(mockCaseDetails);
                when(mockCaseDetails.getCaseData()).thenReturn(thisCaseData);

                executor.execute(new ThreadRunnable(i, actionFurtherEvidenceAboutToSubmitHandler, mockCallback, expectedId1, noOverwrite));
            }
        } catch (Exception err) {
            err.printStackTrace();
        }

        Thread.sleep(3000);
        assertTrue(noOverwrite.get());
        executor.shutdown();
    }

}
