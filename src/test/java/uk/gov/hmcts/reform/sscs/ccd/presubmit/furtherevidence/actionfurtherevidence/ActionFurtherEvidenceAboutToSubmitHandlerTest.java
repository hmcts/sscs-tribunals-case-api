package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DWP_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.JOINT_PARTY_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.OTHER_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.URGENT_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.OriginalSenderItemList.APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.OriginalSenderItemList.JOINT_PARTY;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DatedRequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
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
    private BundleAdditionFilenameBuilder bundleAdditionFilenameBuilder = new BundleAdditionFilenameBuilder();

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
        DynamicList furtherEvidenceActionList = buildFurtherEvidenceActionItemListForGivenOption("otherDocumentManual",
            "Other document type - action manually");

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

    private void assertHappyPaths(DocumentType expectedDocumentType,
        PreSubmitCallbackResponse<SscsCaseData> response) {

        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(1).getValue();
        assertEquals((expectedDocumentType.getLabel() != null ? expectedDocumentType.getLabel() : expectedDocumentType.getValue()) + " received on 13-06-2019", sscsDocumentDetail.getDocumentFileName());
        assertEquals(expectedDocumentType.getValue(), sscsDocumentDetail.getDocumentType());
        assertEquals("www.test.com", sscsDocumentDetail.getDocumentLink().getDocumentUrl());
        assertEquals("2019-06-13", sscsDocumentDetail.getDocumentDateAdded());
        assertEquals("123", sscsDocumentDetail.getControlNumber());
        assertEquals(NO, response.getData().getSscsDocument().get(1).getValue().getEvidenceIssued());
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
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, null, OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, NO, OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, YES, OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, NO, OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, null, OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, YES, OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, dwpOriginalSender, NO, OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, dwpOriginalSender, null, OTHER_DOCUMENT},
            new Object[]{furtherEvidenceActionListOtherDocuments, dwpOriginalSender, YES, OTHER_DOCUMENT},
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

        assertEquals("Other document received on 12-06-2019", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("2019-06-12", response.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Other document received on 13-06-2019", response.getData().getSscsDocument().get(1).getValue().getDocumentFileName());
        assertEquals("2019-06-13", response.getData().getSscsDocument().get(1).getValue().getDocumentDateAdded());
        assertEquals("exist.pdf", response.getData().getSscsDocument().get(2).getValue().getDocumentFileName());
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

        assertEquals("Other document received on 12-06-2019", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("2019-06-12", response.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED, response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());

        assertEquals("Other document received on 13-06-2019", response.getData().getSscsDocument().get(1).getValue().getDocumentFileName());
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

    private Callback<SscsCaseData> buildCallback(String dynamicListItemCode) {
        DynamicList dynamicList = new DynamicList(new DynamicListItem(dynamicListItemCode, "label"),
            Collections.singletonList(new DynamicListItem(dynamicListItemCode, "label")));
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .originalSender(dynamicList)
            .furtherEvidenceAction(dynamicList)
            .scannedDocuments(Collections.singletonList(ScannedDocument.builder().build()))
            .appeal(Appeal.builder().appellant(Appellant.builder().address(Address.builder().line1("My Road").postcode("TS1 2BA").build()).build()).build())
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
    @Parameters(method = "generateIssueFurtherEvidenceAddressEmptyScenarios")
    public void givenIssueFurtherEvidenceAndEmptyAppellantAddress_shouldReturnAnErrorToUser(Appeal appeal, String... parties) {
        Callback<SscsCaseData> callback = buildCallback(ISSUE_FURTHER_EVIDENCE.getCode());

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
    @Parameters({"SEND_TO_INTERLOC_REVIEW_BY_JUDGE", "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE"})
    public void givenConfidentialRequestWhenJointPartyExistsAndAlreadyHasConfidentialityFromOriginalSenderAppellant_thenUpdateCaseWithConfidentialFieldsAndDisplayWarning(FurtherEvidenceActionDynamicListItems furtherEvidenceActionDynamicListItem) {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(furtherEvidenceActionDynamicListItem.code, furtherEvidenceActionDynamicListItem.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.setJointParty(YES);
        sscsCaseData.setIsProgressingViaGaps(YES);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedRequestOutcome(RequestOutcome.GRANTED));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(1, response.getWarnings().size());

        assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", response.getWarnings().iterator().next());
        assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant().getRequestOutcome());
        assertEquals(createDatedRequestOutcome(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestOutcomeAppellant().getDate());
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
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(OriginalSenderItemList.REPRESENTATIVE.getCode(), OriginalSenderItemList.REPRESENTATIVE.getLabel()));
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

    private DatedRequestOutcome createDatedRequestOutcome(RequestOutcome requestOutcome) {
        return DatedRequestOutcome.builder().date(LocalDate.now().minusDays(1))
            .requestOutcome(requestOutcome).build();
    }

    private Object[][] generateWrappedFurtherEvidenceActionListScenarios() {
        return wrapWithConfidentialityAndWarningCombinationScenarios(generateFurtherEvidenceActionListScenarios());
    }

    private Object[][] wrapWithConfidentialityAndWarningCombinationScenarios(Object[] scenarios) {

        Object[][] results = new Object[scenarios.length * confidentialityAndWarningRequestStateCombinations().length][];
        int index = 0;
        for (Object scenario : scenarios) {
            for (Object[] confidentialityRequestStateCombination : confidentialityAndWarningRequestStateCombinations()) {
                Object[] objects = wrapScenario((Object[]) scenario, confidentialityRequestStateCombination);
                results[index++] = objects;
            }
        }
        return results;
    }

    private Object[] wrapScenario(Object[] scenario, Object[] confidentialityRequestStateCombination) {
        Object[] result = new Object[scenario.length + confidentialityRequestStateCombination.length];
        for (int i = 0; i < confidentialityRequestStateCombination.length; i++) {
            result[i] = confidentialityRequestStateCombination[i];
        }
        for (int i = 0; i < scenario.length; i++) {
            result[confidentialityRequestStateCombination.length + i] = scenario[i];
        }
        return result;
    }

    protected Object[][] confidentialityAndWarningRequestStateCombinations() {
        return new Object[][]{
            new Object[]{null, null, false, false, null},
            new Object[]{null, createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), false, false, null},
            new Object[]{null, createDatedRequestOutcome(RequestOutcome.REFUSED), false, false, null},
            new Object[]{null, createDatedRequestOutcome(RequestOutcome.GRANTED), false, true, YES},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), null, false, false, null},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), false, false, null},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), createDatedRequestOutcome(RequestOutcome.REFUSED), false, false, null},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), createDatedRequestOutcome(RequestOutcome.GRANTED), false, true, YES},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), createDatedRequestOutcome(RequestOutcome.GRANTED), false, false, NO},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), null, false, false, null},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), false, false, null},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), createDatedRequestOutcome(RequestOutcome.REFUSED), false, false, null},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), createDatedRequestOutcome(RequestOutcome.GRANTED), false, true, YES},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), createDatedRequestOutcome(RequestOutcome.GRANTED), false, false, NO},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), null, false, true, YES},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), null, false, false, NO},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), false, true, YES},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), false, false, NO},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.REFUSED), false, true, YES},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.REFUSED), false, false, NO},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.GRANTED), false, true, YES},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.GRANTED), false, false, NO},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.GRANTED), false, false, null},
            new Object[]{null, null, true, false, null},
            new Object[]{null, createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), true, false, null},
            new Object[]{null, createDatedRequestOutcome(RequestOutcome.REFUSED), true, false, null},
            new Object[]{null, createDatedRequestOutcome(RequestOutcome.GRANTED), true, true, YES},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), null, true, false, NO},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), true, false, null},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), createDatedRequestOutcome(RequestOutcome.REFUSED), true, false, null},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), createDatedRequestOutcome(RequestOutcome.GRANTED), true, true, YES},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), createDatedRequestOutcome(RequestOutcome.GRANTED), true, false, NO},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), null, true, false, null},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), true, false, null},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), createDatedRequestOutcome(RequestOutcome.REFUSED), true, false, null},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), createDatedRequestOutcome(RequestOutcome.GRANTED), true, true, YES},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), createDatedRequestOutcome(RequestOutcome.GRANTED), true, false, NO},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), null, true, true, null},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), true, true, YES},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), true, false, NO},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.REFUSED), true, true, YES},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.REFUSED), true, false, NO},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.GRANTED), true, true, YES},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.GRANTED), true, false, null},
        };
    }

    @Test
    @Parameters(method = "generateWrappedFurtherEvidenceActionListScenarios")
    public void givenACaseWithScannedDocuments_shouldMoveToSscsDocumentsAndDisplayAppropriatesConfidentialityWarning(
        @Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
        @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome,
        boolean ignoreWarnings,
        boolean expectConfidentialityWarning,
        @Nullable String isProgressingViaGaps,
        @Nullable DynamicList furtherEvidenceActionList,
        @Nullable DynamicList originalSender,
        @Nullable String evidenceHandle,
        DocumentType expectedDocumentType) {
        sscsCaseData.setFurtherEvidenceAction(furtherEvidenceActionList);
        sscsCaseData.setOriginalSender(originalSender);
        sscsCaseData.setEvidenceHandled(evidenceHandle);
        sscsCaseData.setIsProgressingViaGaps(isProgressingViaGaps);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        PreSubmitCallbackResponse<SscsCaseData> response = null;
        try {
            response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            if (ignoreWarnings) {
                assertEquals(0, response.getWarnings().size());
            } else {
                Iterator<String> iterator = response.getWarnings().iterator();
                if (expectConfidentialityWarning) {
                    String warning1 = iterator.next();
                    assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", warning1);
                }
                String warning2 = iterator.next();
                assertEquals("Document type is empty, are you happy to proceed?", warning2);
            }

        } catch (IllegalStateException e) {
            assertTrue(furtherEvidenceActionList == null || originalSender == null);
        }
        if (null != furtherEvidenceActionList && null != originalSender) {
            assertHappyPaths(expectedDocumentType, response);
        }
    }

}

