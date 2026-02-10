package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.ScannedDocumentType.REINSTATEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.ActionFurtherEvidenceAboutToSubmitHandlerTest.buildOriginalSenderItemListForGivenOption;
import static uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.furtherevidence.actionfurtherevidence.ActionFurtherEvidenceMidEventHandler.FURTHER_ACTION_INVALID_INTERNAL_ERROR;
import static uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.furtherevidence.actionfurtherevidence.ActionFurtherEvidenceMidEventHandler.INCLUDE_BUNDLE_AND_INTERNAL_ERROR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.furtherevidence.actionfurtherevidence.ActionFurtherEvidenceMidEventHandler;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.FooterService;

class ActionFurtherEvidenceMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private final List<ScannedDocument> scannedDocumentList = new ArrayList<>();
    private ActionFurtherEvidenceMidEventHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private FooterService footerService;
    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        openMocks(this);
        handler = new ActionFurtherEvidenceMidEventHandler(footerService, false, false, false);

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

    @ParameterizedTest
    @CsvSource({"sendToInterlocReviewByTcw, Send to Interloc - Review by Tcw", "sendToInterlocReviewByJudge, Send to Interloc - Review by Judge"})
    void givenAPostponementRequestInInterlocTcwOrJudgeActionAndCaseInHearing_thenAddNoError(String furtherEvidenceActionCode, String furtherEvidenceActionLabel) {

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
    void givenAPostponementRequestWithoutDetails_thenAddNoError() {
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
    void givenAPostponementRequestInOtherThanInterlocTcwOrJudgeAction_thenAddAnError() {
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
    void givenAPostponementRequestWithGaps_thenAddAnError() {
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
    void givenAPostponementRequestWithListAssist_thenNoError() {
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
    void givenAPostponementRequestInOtherThanHearingState_thenAddAnError() {
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
    void givenMoreThanOnePostponementRequest_thenAddAnError() {
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
    void givenAHandleEvidenceEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(MID_EVENT, callback));
    }


    @Test
    void givenACaseWithScannedDocumentsAndSscsCaseDocuments_thenAppendNewDocumentsToSscsDocumentsList() {
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
    void givenACaseWithScannedDocumentWithNoFileName_showAnError() {
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
    void givenACaseWithScannedDocumentWithNoDocumentType_showAWarning() {
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
    void givenACaseWithNoScannedDocuments_thenAddAnErrorToResponse() {
        sscsCaseData.setScannedDocuments(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("Please add a scanned document", response.getErrors().iterator().next());
    }

    @Test
    void givenADocumentWithNoUrl_thenAddAnErrorToResponse() {
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
    void givenANonConfidentialCaseAndEditedDocumentPopulated_thenAddAnErrorToResponse() {
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

    @ParameterizedTest
    @ValueSource(strings = {"null", " ", "    "})
    void givenADocumentWithNoDocFileName_thenAddAnErrorToResponse(String filename) {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName(filename.equals("null") ? null : filename)
                .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("No document file name so could not process", response.getErrors().iterator().next());
    }

    @Test
    void givenACaseWithUnreadableScannedDocument_showAnError() {
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
    void givenACaseWithPasswordEncryptedScannedDocument_showAnError() {
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

    @ParameterizedTest
    @EnumSource(value = PartyItemList.class, names = {"OTHER_PARTY", "OTHER_PARTY_REPRESENTATIVE"})
    void givenARequestWithOtherPartySelectedAsOriginalSenderAndOtherPartyHearingPreferencesDocumentSelected_thenNoErrorShown(PartyItemList partyItemList) {
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
    void givenARequestWithOtherPartyNotSelectedAsOriginalSenderAndOtherPartyHearingPreferencesDocumentSelected_thenErrorShown() {
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

    @ParameterizedTest
    @ValueSource(strings = {"setAsideApplication", "correctionApplication", "statementOfReasonsApplication",
        "libertyToApplyApplication", "permissionToAppealApplication"})
    void givenAGapsCaseAndPostponementRequest_thenAddAnErrorToResponse(String doctype) {
        handler = new ActionFurtherEvidenceMidEventHandler(footerService, true, true, false);
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
    void givenAGapsCaseAndNotPostponementRequest_thenDontAddErrorToResponse() {
        handler = new ActionFurtherEvidenceMidEventHandler(footerService, true, true, false);
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

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThrows(IllegalStateException.class, () -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION));
    }

    @ParameterizedTest
    @EnumSource(value = FurtherEvidenceActionDynamicListItems.class, mode = EnumSource.Mode.EXCLUDE, names = {"ADMIN_ACTION_CORRECTION", "ISSUE_FURTHER_EVIDENCE"})
    void givenInternalDocumentFlagOnAndValidActionNoBundle_thenDoNotAddErrorToResponse(FurtherEvidenceActionDynamicListItems action) {
        handler = new ActionFurtherEvidenceMidEventHandler(footerService, false, false, true);
        DynamicListItem issueEvidenceAction = new DynamicListItem(action.getCode(), action.getLabel());
        sscsCaseData.getFurtherEvidenceAction().setValue(issueEvidenceAction);

        ScannedDocument scannedDocument = ScannedDocument.builder()
            .value(ScannedDocumentDetails.builder()
                .fileName("Test.pdf")
                .url(DocumentLink.builder().documentUrl("test.com").build())
                .documentTabChoice(DocumentTabChoice.INTERNAL)
                .includeInBundle("no")
                .build())
            .build();
        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
    }

    @ParameterizedTest
    @EnumSource(value = FurtherEvidenceActionDynamicListItems.class, names = {"ADMIN_ACTION_CORRECTION", "ISSUE_FURTHER_EVIDENCE"})
    void givenInternalDocumentFlagOnAndInvalidActionNoBundle_thenAddErrorToResponse(FurtherEvidenceActionDynamicListItems action) {
        handler = new ActionFurtherEvidenceMidEventHandler(footerService, false, false, true);
        DynamicListItem issueEvidenceAction = new DynamicListItem(action.getCode(), action.getLabel());
        sscsCaseData.getFurtherEvidenceAction().setValue(issueEvidenceAction);

        ScannedDocument scannedDocument = ScannedDocument.builder()
            .value(ScannedDocumentDetails.builder()
                .fileName("Test.pdf")
                .url(DocumentLink.builder().documentUrl("test.com").build())
                .documentTabChoice(DocumentTabChoice.INTERNAL)
                .includeInBundle("no")
                .build())
            .build();
        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(FURTHER_ACTION_INVALID_INTERNAL_ERROR));
    }

    @ParameterizedTest
    @EnumSource(value = FurtherEvidenceActionDynamicListItems.class, mode = EnumSource.Mode.EXCLUDE, names = {"ADMIN_ACTION_CORRECTION", "ISSUE_FURTHER_EVIDENCE"})
    void givenInternalDocumentFlagOnAndValidActionIncludeInBundle_thenAddErrorToResponse(FurtherEvidenceActionDynamicListItems action) {
        handler = new ActionFurtherEvidenceMidEventHandler(footerService, false, false, true);
        DynamicListItem issueEvidenceAction = new DynamicListItem(action.getCode(), action.getLabel());
        sscsCaseData.getFurtherEvidenceAction().setValue(issueEvidenceAction);

        ScannedDocument scannedDocument = ScannedDocument.builder()
            .value(ScannedDocumentDetails.builder()
                .fileName("Test.pdf")
                .url(DocumentLink.builder().documentUrl("test.com").build())
                .documentTabChoice(DocumentTabChoice.INTERNAL)
                .includeInBundle("yes")
                .build())
            .build();
        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(INCLUDE_BUNDLE_AND_INTERNAL_ERROR));
    }

    @ParameterizedTest
    @EnumSource(value = FurtherEvidenceActionDynamicListItems.class, names = {"ADMIN_ACTION_CORRECTION", "ISSUE_FURTHER_EVIDENCE"})
    void givenInternalDocumentFlagOnAndInvalidActionIncludeInBundle_thenAddOneErrorToResponse(FurtherEvidenceActionDynamicListItems action) {
        handler = new ActionFurtherEvidenceMidEventHandler(footerService, false, false, true);
        DynamicListItem issueEvidenceAction = new DynamicListItem(action.getCode(), action.getLabel());
        sscsCaseData.getFurtherEvidenceAction().setValue(issueEvidenceAction);

        ScannedDocument scannedDocument = ScannedDocument.builder()
            .value(ScannedDocumentDetails.builder()
                .fileName("Test.pdf")
                .url(DocumentLink.builder().documentUrl("test.com").build())
                .documentTabChoice(DocumentTabChoice.INTERNAL)
                .includeInBundle("yes")
                .build())
            .build();
        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(FURTHER_ACTION_INVALID_INTERNAL_ERROR));
    }
}
