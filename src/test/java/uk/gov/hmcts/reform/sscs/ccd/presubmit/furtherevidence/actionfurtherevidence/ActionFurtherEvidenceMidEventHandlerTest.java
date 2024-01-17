package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.ScannedDocumentType.REINSTATEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.ActionFurtherEvidenceAboutToSubmitHandlerTest.buildOriginalSenderItemListForGivenOption;

import java.util.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@RunWith(JUnitParamsRunner.class)
public class ActionFurtherEvidenceMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private final List<ScannedDocument> scannedDocumentList = new ArrayList<>();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    private ActionFurtherEvidenceMidEventHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private FooterService footerService;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        handler = new ActionFurtherEvidenceMidEventHandler(footerService, false, false);

        when(callback.getEvent()).thenReturn(EventType.ACTION_FURTHER_EVIDENCE);
        when(footerService.isReadablePdf(any())).thenReturn(PdfState.OK);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .fileName("bla.pdf")
                        .type("type")
                        .subtype("sscs1")
                        .url(DocumentLink.builder().documentUrl("www.test.com").build())
                        .scannedDate("2019-06-13T00:00:00.000")
                        .controlNumber("123")
                        .build()).build();

        scannedDocumentList.add(scannedDocument);
        DynamicList furtherEvidenceActionList = buildFurtherEvidenceActionItemListForGivenOption("otherDocumentManual",
                "Other document type - action manually");

        DynamicListItem value = new DynamicListItem("appellant", "Appellant (or Appointee)");
        DynamicList originalSender = new DynamicList(value, Collections.singletonList(value));

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .scannedDocuments(scannedDocumentList)
                .furtherEvidenceAction(furtherEvidenceActionList)
                .originalSender(originalSender)
                .appeal(Appeal.builder().appellant(
                                Appellant.builder().address(Address.builder().line1("My Road").postcode("TS1 2BA").build()).build())
                        .build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    private DynamicList buildFurtherEvidenceActionItemListForGivenOption(String code, String label) {
        DynamicListItem selectedOption = new DynamicListItem(code, label);
        return new DynamicList(selectedOption,
                Collections.singletonList(selectedOption));
    }

    @Test
    @Parameters({"sendToInterlocReviewByTcw, Send to Interloc - Review by Tcw", "sendToInterlocReviewByJudge, Send to Interloc - Review by Judge"})
    public void givenAPostponementRequestInInterlocTcwOrJudgeActionAndCaseInHearing_thenAddNoError(String furtherEvidenceActionCode, String furtherEvidenceActionLabel) {

        when(caseDetails.getState()).thenReturn(State.HEARING);
        sscsCaseData.getFurtherEvidenceAction().setValue(
                new DynamicListItem(furtherEvidenceActionCode,
                        furtherEvidenceActionLabel));

        ScannedDocument scannedDocument = ScannedDocument.builder()
                .value(ScannedDocumentDetails.builder().type(DocumentType.POSTPONEMENT_REQUEST.getValue())
                        .fileName("Testing.jpg").url(DocumentLink.builder()
                                .documentUrl("test.com").build()).build()).build();
        sscsCaseData.setPostponementRequest(PostponementRequest.builder().postponementRequestDetails("Anything").build());

        sscsCaseData.setScannedDocuments(Arrays.asList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(empty()));
    }

    @Test
    public void givenAPostponementRequestWithoutDetails_thenAddNoError() {
        List<ScannedDocument> docs = new ArrayList<>();
        when(caseDetails.getState()).thenReturn(State.HEARING);
        sscsCaseData.getFurtherEvidenceAction().setValue(
                new DynamicListItem(FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode(),
                        FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getLabel()));

        ScannedDocument scannedDocument = ScannedDocument.builder()
                .value(ScannedDocumentDetails.builder().type(DocumentType.POSTPONEMENT_REQUEST.getValue())
                        .fileName("Testing.jpg").url(DocumentLink.builder()
                                .documentUrl("test.com").build()).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(empty()));
    }

    @Test
    public void givenAPostponementRequestInOtherThanInterlocTcwOrJudgeAction_thenAddAnError() {
        List<ScannedDocument> docs = new ArrayList<>();
        when(caseDetails.getState()).thenReturn(State.HEARING);
        sscsCaseData.getFurtherEvidenceAction().setValue(
                new DynamicListItem(FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE.getCode(),
                        FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE.getLabel()));
        ScannedDocument scannedDocument = ScannedDocument.builder()
                .value(ScannedDocumentDetails.builder().type(DocumentType.POSTPONEMENT_REQUEST.getValue())
                        .fileName("Testing.jpg").url(DocumentLink.builder().documentUrl("test.com").build()).build())
                .build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(not(empty())));
        assertThat(response.getErrors().iterator().next(),
                is(ActionFurtherEvidenceMidEventHandler.POSTPONEMENTS_REVIEWED_BY_TCW_OR_JUDGE));
    }

    @Test
    public void givenAPostponementRequestWithGaps_thenAddAnError() {
        List<ScannedDocument> docs = new ArrayList<>();
        when(caseDetails.getState()).thenReturn(State.HEARING);
        sscsCaseData.getFurtherEvidenceAction().setValue(
                new DynamicListItem(FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode(),
                        FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getLabel()));
        ScannedDocument scannedDocument = ScannedDocument.builder()
                .value(ScannedDocumentDetails.builder().type(DocumentType.POSTPONEMENT_REQUEST.getValue())
                        .fileName("Testing.jpg").url(DocumentLink.builder().documentUrl("test.com").build()).build())
                .build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        sscsCaseData.setSchedulingAndListingFields(
                SchedulingAndListingFields.builder()
                .hearingRoute(HearingRoute.GAPS)
                .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(not(empty())));
        assertThat(response.getErrors().iterator().next(),
                is(ActionFurtherEvidenceMidEventHandler.POSTPONEMENTS_NOT_POSSIBLE_GAPS));
    }

    @Test
    public void givenAPostponementRequestWithListAssist_thenNoError() {
        List<ScannedDocument> docs = new ArrayList<>();
        when(caseDetails.getState()).thenReturn(State.HEARING);
        sscsCaseData.getFurtherEvidenceAction().setValue(
                new DynamicListItem(FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode(),
                        FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getLabel()));
        ScannedDocument scannedDocument = ScannedDocument.builder()
                .value(ScannedDocumentDetails.builder().type(DocumentType.POSTPONEMENT_REQUEST.getValue())
                        .fileName("Testing.jpg").url(DocumentLink.builder().documentUrl("test.com").build()).build())
                .build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        sscsCaseData.setSchedulingAndListingFields(
                SchedulingAndListingFields.builder()
                        .hearingRoute(HearingRoute.LIST_ASSIST)
                        .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(empty()));
    }

    @Test
    public void givenAPostponementRequestInOtherThanHearingState_thenAddAnError() {
        List<ScannedDocument> docs = new ArrayList<>();
        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);
        sscsCaseData.getFurtherEvidenceAction().setValue(
                new DynamicListItem(FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode(),
                        FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getLabel()));
        ScannedDocument scannedDocument = ScannedDocument.builder()
                .value(ScannedDocumentDetails.builder().type(DocumentType.POSTPONEMENT_REQUEST.getValue())
                        .fileName("Testing.jpg").url(DocumentLink.builder().documentUrl("test.com").build()).build())
                .build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(not(empty())));
        assertThat(response.getErrors().iterator().next(),
                is(ActionFurtherEvidenceMidEventHandler.POSTPONEMENT_IN_HEARING_STATE));
    }

    @Test
    public void givenMoreThanOnePostponementRequest_thenAddAnError() {
        List<ScannedDocument> docs = new ArrayList<>();
        when(caseDetails.getState()).thenReturn(State.HEARING);
        sscsCaseData.getFurtherEvidenceAction().setValue(
                new DynamicListItem(FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode(),
                        FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getLabel()));
        ScannedDocument scannedDocument = ScannedDocument.builder()
                .value(ScannedDocumentDetails.builder().type(DocumentType.POSTPONEMENT_REQUEST.getValue())
                        .fileName("Testing.jpg").url(DocumentLink.builder().documentUrl("test.com").build()).build())
                .build();

        ScannedDocument scannedDocument2 = ScannedDocument.builder()
                .value(ScannedDocumentDetails.builder().type(DocumentType.POSTPONEMENT_REQUEST.getValue())
                        .fileName("Testing.jpg").url(DocumentLink.builder().documentUrl("test.com").build()).build())
                .build();

        docs.add(scannedDocument);
        docs.add(scannedDocument2);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(not(empty())));
        assertThat(response.getErrors().iterator().next(),
                is(ActionFurtherEvidenceMidEventHandler.ONLY_ONE_POSTPONEMENT_AT_A_TIME));
    }

    @Test
    public void givenAHandleEvidenceEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(MID_EVENT, callback));
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

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);


        assertTrue(response.getErrors().isEmpty());
        assertTrue(response.getWarnings().isEmpty());
    }

    @Test
    public void givenACaseWithScannedDocumentWithNoFileName_showAnError() {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .type("type")
                        .url(DocumentLink.builder().documentUrl("www.test.com").build())
                        .build()).build();

        scannedDocumentList.add(scannedDocument);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getWarnings().isEmpty());
        assertEquals(1, response.getErrors().size());
        assertEquals("No document file name so could not process", response.getErrors().iterator().next());
    }

    @Test
    public void givenACaseWithScannedDocumentWithNoDocumentType_showAWarning() {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .fileName("type.pdf")
                        .url(DocumentLink.builder().documentUrl("www.test.com").build())
                        .build()).build();

        scannedDocumentList.add(scannedDocument);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
        assertEquals(1, response.getWarnings().size());
        assertEquals("Document type is empty, are you happy to proceed?", response.getWarnings().iterator().next());
    }

    @Test
    public void givenACaseWithNoScannedDocuments_thenAddAnErrorToResponse() {
        sscsCaseData.setScannedDocuments(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("Please add a scanned document", response.getErrors().iterator().next());
    }

    @Test
    public void givenADocumentWithNoUrl_thenAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("Testing.jpg").build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("No document URL so could not process", response.getErrors().iterator().next());
    }

    @Test
    public void givenANonConfidentialCaseAndEditedDocumentPopulated_thenAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName("Testing.jpg")
                        .url(DocumentLink.builder().documentUrl("test.com").build())
                        .editedUrl(DocumentLink.builder().documentUrl("test").build()).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);
        sscsCaseData.setIsConfidentialCase(YesNo.NO);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("Case is not marked as confidential so cannot upload an edited document",
                response.getErrors().iterator().next());
    }

    @Test
    @Parameters({"null", " ", "    "})
    public void givenADocumentWithNoDocFileName_thenAddAnErrorToResponse(@Nullable String filename) {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder().fileName(filename)
                        .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("No document file name so could not process", response.getErrors().iterator().next());
    }

    @Test
    public void givenACaseWithUnreadableScannedDocument_showAnError() {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .fileName("Testing.jpg")
                        .type("type")
                        .url(DocumentLink.builder().documentUrl("www.test.com").build())
                        .build()).build();

        scannedDocumentList.add(scannedDocument);
        when(footerService.isReadablePdf(any())).thenReturn(PdfState.UNREADABLE);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getWarnings().isEmpty());
        assertEquals(3, response.getErrors().size());
        Iterator<String> iterator = response.getErrors().iterator();
        assertEquals("The below PDF document(s) are not readable, please correct this", iterator.next());
        assertEquals("bla.pdf", iterator.next());
        assertEquals("Testing.jpg", iterator.next());
    }

    @Test
    public void givenACaseWithPasswordEncryptedScannedDocument_showAnError() {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .fileName("Testing.jpg")
                        .type("type")
                        .url(DocumentLink.builder().documentUrl("www.test.com").build())
                        .build()).build();

        scannedDocumentList.add(scannedDocument);
        when(footerService.isReadablePdf(any())).thenReturn(PdfState.PASSWORD_ENCRYPTED);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getWarnings().isEmpty());
        assertEquals(3, response.getErrors().size());
        Iterator<String> iterator = response.getErrors().iterator();
        assertEquals("The below PDF document(s) cannot be password protected, please correct this", iterator.next());
        assertEquals("bla.pdf", iterator.next());
        assertEquals("Testing.jpg", iterator.next());
    }

    @Test
    @Parameters({"OTHER_PARTY", "OTHER_PARTY_REPRESENTATIVE"})
    public void givenARequestWithOtherPartySelectedAsOriginalSenderAndOtherPartyHearingPreferencesDocumentSelected_thenNoErrorShown(PartyItemList partyItemList) {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .fileName("Testing.jpg")
                        .type(DocumentType.OTHER_PARTY_HEARING_PREFERENCES.getValue())
                        .url(DocumentLink.builder().documentUrl("www.test.com").build())
                        .build()).build();

        sscsCaseData.setOriginalSender(buildOriginalSenderItemListForGivenOption(partyItemList.getCode(),
                PartyItemList.OTHER_PARTY.getLabel()));

        scannedDocumentList.add(scannedDocument);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getWarnings().isEmpty());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenARequestWithOtherPartyNotSelectedAsOriginalSenderAndOtherPartyHearingPreferencesDocumentSelected_thenErrorShown() {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .fileName("Testing.jpg")
                        .type(DocumentType.OTHER_PARTY_HEARING_PREFERENCES.getValue())
                        .url(DocumentLink.builder().documentUrl("www.test.com").build())
                        .build()).build();

        sscsCaseData.setOriginalSender(buildOriginalSenderItemListForGivenOption(PartyItemList.APPELLANT.getCode(),
                PartyItemList.APPELLANT.getLabel()));

        scannedDocumentList.add(scannedDocument);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getWarnings().isEmpty());
        assertEquals(1, response.getErrors().size());
        Iterator<String> iterator = response.getErrors().iterator();
        assertEquals("You cannot select 'Other party hearing preferences' as a Document Type as an Other party not selected from Original Sender list", iterator.next());
    }

    @Test
    @Parameters({"setAsideApplication", "correctionApplication","statementOfReasonsApplication",
        "libertyToApplyApplication", "permissionToAppealApplication"})
    public void givenAGapsCaseAndPostponementRequest_thenAddAnErrorToResponse(String doctype) {
        handler = new ActionFurtherEvidenceMidEventHandler(footerService, true, true);
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.GAPS);
        DynamicListItem issueEvidenceAction = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(),
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel());

        sscsCaseData.getFurtherEvidenceAction().setValue(issueEvidenceAction);

        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
            .type(doctype)
            .fileName("Test.pdf")
            .url(DocumentLink.builder().documentUrl("test.com").build())
            .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
            .value(scannedDocDetails)
            .build();

        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Cannot upload post hearing requests on GAPS cases", response.getErrors().iterator().next());
    }

    @Test
    public void givenAGapsCaseAndNotPostponementRequest_thenDontAddErrorToResponse() {
        handler = new ActionFurtherEvidenceMidEventHandler(footerService, true, true);
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.GAPS);
        DynamicListItem issueEvidenceAction = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(),
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel());

        sscsCaseData.getFurtherEvidenceAction().setValue(issueEvidenceAction);

        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
            .type(REINSTATEMENT_REQUEST.getValue())
            .fileName("Test.pdf")
            .url(DocumentLink.builder().documentUrl("test.com").build())
            .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
            .value(scannedDocDetails)
            .build();

        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }


    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }

}
