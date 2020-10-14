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
import org.apache.commons.lang3.StringUtils;
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
    private final BundleAdditionFilenameBuilder bundleAdditionFilenameBuilder = new BundleAdditionFilenameBuilder();
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
                "Other document type - action manually");

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
    @Parameters(method = "generateWrappedFurtherEvidenceActionListScenarios")
    public void givenACaseWithScannedDocuments_shouldMoveToSscsDocuments(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                         @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome,
                                                                         boolean ignoreWarnings,
                                                                         boolean expectConfidentialityWarning,
                                                                         @Nullable DynamicList furtherEvidenceActionList,
                                                                         @Nullable DynamicList originalSender,
                                                                         @Nullable String evidenceHandle,
                                                                         DocumentType expectedDocumentType) {
        sscsCaseData.setFurtherEvidenceAction(furtherEvidenceActionList);
        sscsCaseData.setOriginalSender(originalSender);
        sscsCaseData.setEvidenceHandled(evidenceHandle);
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

    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void givenACaseWithScannedDocumentOfTypeCoversheet_shouldNotMoveToSscsDocumentsAndWarningShouldBeReturned(
            DatedRequestOutcome appellantConfidentialityRequestOutcome,
            DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings, boolean expectConfidentialityWarning) {
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
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(CollectionUtils.isEmpty(response.getData().getSscsDocument()));
        assertEquals("Yes", response.getData().getEvidenceHandled());

        if (ignoreWarnings) {
            assertEquals(0, response.getWarnings().size());
        } else {
            assertEquals(expectConfidentialityWarning ? 2 : 1, response.getWarnings().size());
            Iterator<String> iterator = response.getWarnings().iterator();
            if (expectConfidentialityWarning) {
                String warning1 = iterator.next();
                assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", warning1);
            }
            String warning2 = iterator.next();
            assertEquals("Coversheet will be ignored, are you happy to proceed?", warning2);

        }
    }

    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void givenACaseWithAnEmptyScannedDocumentType_shouldMoveToSscsDocumentsAndWarningShouldBeReturned(
            DatedRequestOutcome appellantConfidentialityRequestOutcome,
            DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings, boolean expectConfidentialityWarning) {
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
        sscsCaseData.setEvidenceHandled("No");
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);


        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        PreSubmitCallbackResponse<SscsCaseData> response =
                actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(response.getData().getSscsDocument().size(), 1);
        assertEquals("Yes", response.getData().getEvidenceHandled());

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

    private DatedRequestOutcome createDatedRequestOutcome(RequestOutcome requestOutcome) {
        return DatedRequestOutcome.builder().date(LocalDate.now().minusDays(1))
                .requestOutcome(requestOutcome).build();
    }

    protected Object[][] confidentialityAndWarningRequestStateCombinations() {
        return new Object[][]{
            new Object[]{null, null, false, false},
            new Object[]{null, createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), false, false},
            new Object[]{null, createDatedRequestOutcome(RequestOutcome.REFUSED), false, false},
            new Object[]{null, createDatedRequestOutcome(RequestOutcome.GRANTED), false, true},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), null, false, false},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), false, false},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), createDatedRequestOutcome(RequestOutcome.REFUSED), false, false},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), createDatedRequestOutcome(RequestOutcome.GRANTED), false, true},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), null, false, false},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), false, false},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), createDatedRequestOutcome(RequestOutcome.REFUSED), false, false},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), createDatedRequestOutcome(RequestOutcome.GRANTED), false, true},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), null, false, true},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), false, true},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.REFUSED), false, true},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.GRANTED), false, true},
            new Object[]{null, null, true, false},
            new Object[]{null, createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), true, false},
            new Object[]{null, createDatedRequestOutcome(RequestOutcome.REFUSED), true, false},
            new Object[]{null, createDatedRequestOutcome(RequestOutcome.GRANTED), true, true},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), null, true, false},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), true, false},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), createDatedRequestOutcome(RequestOutcome.REFUSED), true, false},
            new Object[]{createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), createDatedRequestOutcome(RequestOutcome.GRANTED), true, true},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), null, true, false},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), true, false},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), createDatedRequestOutcome(RequestOutcome.REFUSED), true, false},
            new Object[]{createDatedRequestOutcome(RequestOutcome.REFUSED), createDatedRequestOutcome(RequestOutcome.GRANTED), true, true},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), null, true, true},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.IN_PROGRESS), true, true},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.REFUSED), true, true},
            new Object[]{createDatedRequestOutcome(RequestOutcome.GRANTED), createDatedRequestOutcome(RequestOutcome.GRANTED), true, true},
        };
    }

    private Object[][] generateWrappedFurtherEvidenceActionListScenarios() {
        return wrapWithConfidentialityAndWarningCombinationScenarios(generateFurtherEvidenceActionListScenarios());
    }

    private Object[][] wrapWithConfidentialityAndWarningCombinationScenarios(Object[][] scenarios) {

        Object[][] results = new Object[scenarios.length * confidentialityAndWarningRequestStateCombinations().length][];
        int index = 0;
        for (Object[] scenario : scenarios) {
            for (Object[] confidentialityRequestStateCombination : confidentialityAndWarningRequestStateCombinations()) {
                results[index++] = wrapScenario(scenario, confidentialityRequestStateCombination);
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

    private Object[][] generateFurtherEvidenceActionListScenarios() {
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
        DynamicList jointPartyOriginalSender = buildOriginalSenderItemListForGivenOption("jointParty",
                "Joint Party");

        return new Object[][]{
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
            new Object[]{furtherEvidenceActionListIssueParties, jointPartyOriginalSender, "No", JOINT_PARTY_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, jointPartyOriginalSender, "Yes", JOINT_PARTY_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, jointPartyOriginalSender, null, JOINT_PARTY_EVIDENCE},
            //interloc scenarios
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, null, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, "No", APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, "Yes", APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, null, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, "No", REPRESENTATIVE_EVIDENCE}, new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, "Yes", REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, dwpOriginalSender, null, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, dwpOriginalSender, "No", DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, dwpOriginalSender, "Yes", DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, jointPartyOriginalSender, null, JOINT_PARTY_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, jointPartyOriginalSender, "No", JOINT_PARTY_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, jointPartyOriginalSender, "Yes", JOINT_PARTY_EVIDENCE},
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
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void givenACaseWithScannedDocumentsAndSscsCaseDocuments_thenAppendNewDocumentsToSscsDocumentsList(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                                                             @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome,
                                                                                                             boolean ignoreWarnings,
                                                                                                             boolean expectConfidentialityWarning) {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        SscsDocument doc = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentType("appellantEvidence")
                        .documentFileName("exist.pdf")
                        .build())
                .build();
        sscsDocuments.add(doc);

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);
        sscsCaseData.setScannedDocuments(scannedDocumentList);
        sscsCaseData.setSscsDocument(sscsDocuments);

        boolean anyDocumentTypeBlank = scannedDocumentList.stream().anyMatch(d -> StringUtils.isBlank(d.getValue().getType()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Other document received on 12-06-2019", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("2019-06-12", response.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Other document received on 13-06-2019", response.getData().getSscsDocument().get(1).getValue().getDocumentFileName());
        assertEquals("2019-06-13", response.getData().getSscsDocument().get(1).getValue().getDocumentDateAdded());
        assertEquals("exist.pdf", response.getData().getSscsDocument().get(2).getValue().getDocumentFileName());
        assertNull(response.getData().getScannedDocuments());

        if (ignoreWarnings || !expectConfidentialityWarning) {
            if (!ignoreWarnings && anyDocumentTypeBlank) {
                assertEquals(1, response.getWarnings().size());
            } else {
                assertEquals(0, response.getWarnings().size());
            }
        } else {
            assertEquals(2, response.getWarnings().size());
            assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", response.getWarnings().iterator().next());
        }
    }

    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void givenAWelshCaseWithScannedDocuments_thenSetTranslationStatusToRequired(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                                       @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                                       boolean expectConfidentialityWarning) {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        SscsDocument doc = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentType("appellantEvidence")
                        .documentFileName("exist.pdf")
                        .build())
                .build();
        sscsDocuments.add(doc);

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);
        sscsCaseData.setScannedDocuments(scannedDocumentList);
        sscsCaseData.setSscsDocument(sscsDocuments);
        sscsCaseData.setLanguagePreferenceWelsh("Yes");

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        boolean anyDocumentTypeBlank = scannedDocumentList.stream().anyMatch(d -> StringUtils.isBlank(d.getValue().getType()));

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
        assertEquals("Yes", response.getData().getTranslationWorkOutstanding());
        if (ignoreWarnings || !expectConfidentialityWarning) {
            if (!ignoreWarnings && anyDocumentTypeBlank) {
                assertEquals(1, response.getWarnings().size());
            } else {
                assertTrue(response.getWarnings().isEmpty());
            }
        } else {
            assertEquals(2, response.getWarnings().size());
            assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", response.getWarnings().iterator().next());
        }
    }

    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void givenACaseWithScannedDocumentWithEmptyValues_thenHandleTheDocument(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                                   @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                                   boolean expectConfidentialityWarning) {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .url(DocumentLink.builder().documentUrl("www.test.com").build())
                        .build()).build();

        docs.add(scannedDocument);

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);

        sscsCaseData.setScannedDocuments(docs);

        boolean anyDocumentTypeBlank = scannedDocumentList.stream().anyMatch(d -> StringUtils.isBlank(d.getValue().getType()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("www.test.com", response.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());

        if (!ignoreWarnings) {
            assertEquals(expectConfidentialityWarning && anyDocumentTypeBlank ? 2 : (expectConfidentialityWarning ? 1 : (anyDocumentTypeBlank ? 1 : 0)), response.getWarnings().size());
            Iterator<String> iterator = response.getWarnings().iterator();
            if (expectConfidentialityWarning) {
                assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", iterator.next());
            }
            if (anyDocumentTypeBlank) {
                assertEquals("Document type is empty, are you happy to proceed?", iterator.next());
            }
        } else {
            assertEquals(0, response.getWarnings().size());
        }
    }

    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void givenACaseWithNoScannedDocuments_thenAddAnErrorToResponse(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                          @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                          boolean expectConfidentialityWarning) {

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        sscsCaseData.setScannedDocuments(null);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("No further evidence to process", error);
        }
        if (ignoreWarnings || !expectConfidentialityWarning) {
            assertTrue(response.getWarnings().isEmpty());
        } else {
            assertEquals(1, response.getWarnings().size());
            assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", response.getWarnings().iterator().next());
        }
    }

    private Callback<SscsCaseData> buildCallback(String dynamicListItemCode, boolean isIgnoreWarnings) {
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
        return new Callback<>(caseDetails, Optional.empty(), EventType.ACTION_FURTHER_EVIDENCE, isIgnoreWarnings);
    }

    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void givenIssueFurtherEvidence_shouldUpdateDwpFurtherEvidenceStates(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                               @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                               boolean expectConfidentialityWarning) {
        Callback<SscsCaseData> callback = buildCallback(ISSUE_FURTHER_EVIDENCE.getCode(), ignoreWarnings);

        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("furtherEvidenceReceived", updated.getData().getDwpFurtherEvidenceStates());

        if (!ignoreWarnings) {
            assertEquals(expectConfidentialityWarning ? 1 : 0, updated.getWarnings().size());
            Iterator<String> iterator = updated.getWarnings().iterator();
            if (expectConfidentialityWarning) {
                assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", iterator.next());
            }
        } else {
            assertEquals(0, updated.getWarnings().size());
        }
    }

    @Test
    @Parameters(method = "generateWrappedIssueFurtherEvidenceAddressEmptyScenarios")
    public void givenIssueFurtherEvidenceAndEmptyAppellantAddress_shouldReturnAnErrorToUser(DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                                            DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings, boolean expectConfidentialityWarning, Appeal appeal, String... parties) {
        Callback<SscsCaseData> callback = buildCallback(ISSUE_FURTHER_EVIDENCE.getCode(), ignoreWarnings);

        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);

        callback.getCaseDetails().getCaseData().setAppeal(appeal);

        PreSubmitCallbackResponse<SscsCaseData> result = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        for (String party : parties) {
            String expectedError = "Address details are missing for the " + party + ", please validate or process manually";
            assertTrue(result.getErrors().contains(expectedError));
        }

        if (!ignoreWarnings && expectConfidentialityWarning) {
            assertEquals(1, result.getWarnings().size());
            assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", result.getWarnings().iterator().next());
        } else {
            assertEquals(0, result.getWarnings().size());
        }
    }

    private Object[][] generateWrappedIssueFurtherEvidenceAddressEmptyScenarios() {
        return wrapWithConfidentialityAndWarningCombinationScenarios(generateIssueFurtherEvidenceAddressEmptyScenarios());
    }

    private Object[][] generateIssueFurtherEvidenceAddressEmptyScenarios() {

        return new Object[][]{
            new Object[]{Appeal.builder().appellant(Appellant.builder().address(Address.builder().build()).build()).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().address(Address.builder().line1("Line1").build()).build()).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().address(Address.builder().postcode("TS1 2BA").build()).build()).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().address(null).build()).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(null).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().isAppointee("Yes").build()).build(), "Appointee"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().isAppointee("Yes").appointee(Appointee.builder().build()).build()).build(), "Appointee"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().isAppointee("Yes").appointee(Appointee.builder().address(Address.builder().build()).build()).build()).build(), "Appointee"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().isAppointee("Yes").appointee(Appointee.builder().address(null).build()).build()).build(), "Appointee"},
            new Object[]{Appeal.builder().rep(Representative.builder().hasRepresentative("Yes").address(Address.builder().build()).build()).appellant(Appellant.builder().address(Address.builder().line1("The road").build()).build()).build(), "Representative"},
            new Object[]{Appeal.builder().rep(Representative.builder().hasRepresentative("Yes").address(null).build()).appellant(Appellant.builder().address(Address.builder().line1("The road").build()).build()).build(), "Representative"},
            new Object[]{Appeal.builder().rep(Representative.builder().hasRepresentative("Yes").address(Address.builder().build()).build()).appellant(Appellant.builder().address(null).build()).build(), "Appellant", "Representative"},
        };
    }

    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void givenOtherDocument_shouldNotUpdateDwpFurtherEvidenceStates(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                           @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                           boolean expectConfidentialityWarning) {
        Callback<SscsCaseData> callback = buildCallback(OTHER_DOCUMENT_MANUAL.getCode(), ignoreWarnings);

        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(updated.getData().getDwpFurtherEvidenceStates());

        if (!ignoreWarnings) {
            assertEquals(expectConfidentialityWarning ? 1 : 0, updated.getWarnings().size());
            Iterator<String> iterator = updated.getWarnings().iterator();
            if (expectConfidentialityWarning) {
                assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", iterator.next());
            }
        } else {
            assertEquals(0, updated.getWarnings().size());
        }
    }

    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void givenNullFurtherEvidenceAction_shouldNotUpdateDwpFurtherEvidenceStates(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                                       @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                                       boolean expectConfidentialityWarning) {
        Callback<SscsCaseData> callback = buildCallback(null, ignoreWarnings);

        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(updated.getData().getDwpFurtherEvidenceStates());
        if (!ignoreWarnings) {
            assertEquals(expectConfidentialityWarning ? 1 : 0, updated.getWarnings().size());
            Iterator<String> iterator = updated.getWarnings().iterator();
            if (expectConfidentialityWarning) {
                assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", iterator.next());
            }
        } else {
            assertEquals(0, updated.getWarnings().size());
        }
    }

    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void givenADocumentWithNoUrl_thenAddAnErrorToResponse(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                 @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                 boolean expectConfidentialityWarning) {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("Testing.jpg").build()).build();

        docs.add(scannedDocument);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);

        sscsCaseData.setScannedDocuments(docs);

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        boolean anyDocumentTypeBlank = scannedDocumentList.stream().anyMatch(d -> StringUtils.isBlank(d.getValue().getType()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("No document URL so could not process", error);
        }
        if (ignoreWarnings || !expectConfidentialityWarning) {
            if (!ignoreWarnings && anyDocumentTypeBlank) {
                assertEquals(1, response.getWarnings().size());
            } else {
                assertTrue(response.getWarnings().isEmpty());
            }
        } else {
            assertEquals(2, response.getWarnings().size());
            assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", response.getWarnings().iterator().next());
        }
    }

    private Object[][] generateFileNameScenarios() {
        return new Object[][]{new Object[]{null}, new Object[]{" "}, new Object[]{"    "}};
    }

    private Object[][] generateWrappedFileNameScenarios() {
        return wrapWithConfidentialityAndWarningCombinationScenarios(generateFileNameScenarios());
    }

    @Test
    @Parameters(method = "generateWrappedFileNameScenarios")
    public void givenADocumentWithNoDocFileName_thenAddAnErrorToResponse(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                         @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                         boolean expectConfidentialityWarning, @Nullable String filename) {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName(filename).url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();

        docs.add(scannedDocument);

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);

        sscsCaseData.setScannedDocuments(docs);

        boolean anyDocumentTypeBlank = scannedDocumentList.stream().anyMatch(d -> StringUtils.isBlank(d.getValue().getType()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("No document file name so could not process", error);
        }
        if (!ignoreWarnings) {
            assertEquals(expectConfidentialityWarning && anyDocumentTypeBlank ? 2 : (expectConfidentialityWarning ? 1 : (anyDocumentTypeBlank ? 1 : 0)), response.getWarnings().size());
            Iterator<String> iterator = response.getWarnings().iterator();
            if (expectConfidentialityWarning) {
                assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", iterator.next());
            }
            if (anyDocumentTypeBlank) {
                assertEquals("Document type is empty, are you happy to proceed?", iterator.next());
            }
        } else {
            assertEquals(0, response.getWarnings().size());
        }
    }

    private Object[][] generateUpdatePreviousStateScenarios() {
        return new Object[][]{
            new Object[]{null},
            new Object[]{"DORMANT_APPEAL_STATE"},
            new Object[]{"VOID_STATE"}
        };
    }

    private Object[][] generateWrappedUpdatePreviousStateScenarios() {
        return wrapWithConfidentialityAndWarningCombinationScenarios(generateUpdatePreviousStateScenarios());
    }

    @Test
    @Parameters(method = "generateWrappedUpdatePreviousStateScenarios")
    public void shouldReviewByJudgeAndUpdatePreviousStateWhenActionManuallyAndHasReinstatementRequestDocument(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                                                              @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                                                              boolean expectConfidentialityWarning, @Nullable State previousState) {
        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder);

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.setPreviousState(previousState);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);

        List<ScannedDocument> docs = new ArrayList<>();

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
        if (ignoreWarnings || !expectConfidentialityWarning) {
            assertTrue(response.getWarnings().isEmpty());
        } else {
            assertEquals(1, response.getWarnings().size());
            assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", response.getWarnings().iterator().next());
        }
    }

    private Object[][] generateNotUpdatePreviousStateScenarios() {
        return new Object[][]{
            new Object[]{"VALID_APPEAL"},
            new Object[]{"READY_TO_LIST"}
        };
    }

    private Object[][] generateWrappedNotUpdatePreviousStateScenarios() {
        return wrapWithConfidentialityAndWarningCombinationScenarios(generateNotUpdatePreviousStateScenarios());
    }

    @Test
    @Parameters(method = "generateWrappedNotUpdatePreviousStateScenarios")
    public void shouldReviewByJudgeButNotUpdatePreviousStateWhenActionManuallyAndHasReinstatementRequestDocument(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                                                                 @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings, boolean expectConfidentialityWarning, @Nullable State previousState) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder);

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.setPreviousState(previousState);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);


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
        if (ignoreWarnings || !expectConfidentialityWarning) {
            assertTrue(response.getWarnings().isEmpty());
        } else {
            assertEquals(1, response.getWarnings().size());
            assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", response.getWarnings().iterator().next());
        }
    }

    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void shouldNotUpdateInterlocReviewStateWhenActionManuallyAndHasNoReinstatementRequestDocument(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                                                         @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                                                         boolean expectConfidentialityWarning) {

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);
        sscsCaseData.setPreviousState(null);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));

        boolean anyDocumentTypeBlank = scannedDocumentList.stream().anyMatch(d -> StringUtils.isBlank(d.getValue().getType()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertNull(sscsCaseData.getPreviousState());
        assertNull(sscsCaseData.getReinstatementOutcome());
        assertNull(sscsCaseData.getReinstatementRegistered());
        assertNull(sscsCaseData.getInterlocReviewState());

        if (!ignoreWarnings) {
            assertEquals(expectConfidentialityWarning && anyDocumentTypeBlank ? 2 : (expectConfidentialityWarning ? 1 : (anyDocumentTypeBlank ? 1 : 0)), response.getWarnings().size());
            Iterator<String> iterator = response.getWarnings().iterator();
            if (expectConfidentialityWarning) {
                assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", iterator.next());
            }
            if (anyDocumentTypeBlank) {
                assertEquals("Document type is empty, are you happy to proceed?", iterator.next());
            }
        } else {
            assertEquals(0, response.getWarnings().size());
        }
    }

    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void shouldNotUpdateInterlocReviewStateWhenNotActionManuallyAndHasReinstatementRequestDocument(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                                                          @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                                                          boolean expectConfidentialityWarning) {
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);
        sscsCaseData.setPreviousState(null);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

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
        if (ignoreWarnings || !expectConfidentialityWarning) {
            assertTrue(response.getWarnings().isEmpty());
        } else {
            assertEquals(1, response.getWarnings().size());
            assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", response.getWarnings().iterator().next());
        }
    }

    private Object[][] generateSendToInterlocReviewByJudgeOrInformationReceivedForInterlocJudgeScenarios() {
        return new Object[][]{
            new Object[]{"SEND_TO_INTERLOC_REVIEW_BY_JUDGE"},
            new Object[]{"INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE"}
        };
    }

    private Object[][] generateWrappedSendToInterlocReviewByJudgeOrInformationReceivedForInterlocJudgeScenarios() {
        return wrapWithConfidentialityAndWarningCombinationScenarios(generateSendToInterlocReviewByJudgeOrInformationReceivedForInterlocJudgeScenarios());
    }

    @Test
    @Parameters(method = "generateWrappedSendToInterlocReviewByJudgeOrInformationReceivedForInterlocJudgeScenarios")
    public void givenConfidentialRequestWhenJointPartyDoesNotExistFromOriginalSenderAppellant_thenUpdateCaseWithConfidentialFields(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                                                                                   @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                                                                                   boolean expectConfidentialityWarning, FurtherEvidenceActionDynamicListItems furtherEvidenceActionDynamicListItem) {
        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);
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
        for (String error : response.getErrors()) {
            assertEquals("Document type \"Confidentiality Request\" is invalid as there is no joint party on the case", error);
        }
        if (ignoreWarnings || !expectConfidentialityWarning) {
            assertTrue(response.getWarnings().isEmpty());
        } else {
            assertEquals(1, response.getWarnings().size());
            assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", response.getWarnings().iterator().next());
        }
    }

    @Test
    @Parameters(method = "generateWrappedSendToInterlocReviewByJudgeOrInformationReceivedForInterlocJudgeScenarios")
    public void givenConfidentialRequestWhenJointPartyExistsFromOriginalSenderAppellant_thenUpdateCaseWithConfidentialFields(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                                                                             @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                                                                             boolean expectConfidentialityWarning, FurtherEvidenceActionDynamicListItems furtherEvidenceActionDynamicListItem) {
        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(furtherEvidenceActionDynamicListItem.code, furtherEvidenceActionDynamicListItem.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.setJointParty("Yes");

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                        .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant().getRequestOutcome());
        assertEquals(jointPartyConfidentialityRequestOutcome, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestOutcomeAppellant().getDate());
        if (ignoreWarnings || !expectConfidentialityWarning) {
            assertTrue(response.getWarnings().isEmpty());
        } else {
            assertEquals(1, response.getWarnings().size());
            assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", response.getWarnings().iterator().next());
        }
    }

    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void givenConfidentialRequestWhenJointPartyDoesNotExistFromOriginalSenderJointParty_thenUpdateCaseWithConfidentialFields(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                                                                                    @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                                                                                    boolean expectConfidentialityWarning) {

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);
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
        for (String error : response.getErrors()) {
            assertEquals("Document type \"Confidentiality Request\" is invalid as there is no joint party on the case", error);
        }
        if (ignoreWarnings || !expectConfidentialityWarning) {
            assertTrue(response.getWarnings().isEmpty());
        } else {
            assertEquals(1, response.getWarnings().size());
            assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", response.getWarnings().iterator().next());
        }
    }

    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void givenConfidentialRequestWhenJointPartyExistsFromOriginalSenderJointParty_thenUpdateCaseWithConfidentialFields(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                                                                              @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings, boolean expectConfidentialityWarning) {

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));
        sscsCaseData.setJointParty("Yes");

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                        .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty().getRequestOutcome());
        assertEquals(appellantConfidentialityRequestOutcome, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestOutcomeJointParty().getDate());

        if (ignoreWarnings || !expectConfidentialityWarning) {
            assertTrue(response.getWarnings().isEmpty());
        } else {
            assertEquals(1, response.getWarnings().size());
            assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", response.getWarnings().iterator().next());
        }
    }


    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void givenConfidentialRequestFromRep_thenShowAnError(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                boolean expectConfidentialityWarning) {

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(OriginalSenderItemList.REPRESENTATIVE.getCode(), OriginalSenderItemList.REPRESENTATIVE.getLabel()));
        sscsCaseData.setJointParty("Yes");

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
        if (ignoreWarnings || !expectConfidentialityWarning) {
            assertTrue(response.getWarnings().isEmpty());
        } else {
            assertEquals(1, response.getWarnings().size());
            assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", response.getWarnings().iterator().next());
        }
    }

    @Test
    @Parameters(method = "confidentialityAndWarningRequestStateCombinations")
    public void givenConfidentialRequestWithInvalidFurtherEvidenceAction_thenShowAnError(@Nullable DatedRequestOutcome appellantConfidentialityRequestOutcome,
                                                                                         @Nullable DatedRequestOutcome jointPartyConfidentialityRequestOutcome, boolean ignoreWarnings,
                                                                                         boolean expectConfidentialityWarning) {

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantConfidentialityRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyConfidentialityRequestOutcome);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(ISSUE_FURTHER_EVIDENCE.code, ISSUE_FURTHER_EVIDENCE.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.setJointParty("Yes");

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
        if (ignoreWarnings || !expectConfidentialityWarning) {
            assertTrue(response.getWarnings().isEmpty());
        } else {
            assertEquals(1, response.getWarnings().size());
            assertEquals("This case is progressing via GAPS. Please ensure any documents are emailed to the Regional Processing Centre to be attached to the paper file.", response.getWarnings().iterator().next());
        }
    }

}

