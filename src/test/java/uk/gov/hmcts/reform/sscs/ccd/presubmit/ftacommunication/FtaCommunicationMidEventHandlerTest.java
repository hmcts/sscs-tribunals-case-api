package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCommRequestWithActionedReply;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCommRequestWithReply;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCommRequestWithoutReply;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCustomCommRequest;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.dlItemFromCommRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.ftacommunication.FtaCommunicationMidEventHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestReply;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicMixedChoiceList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

class FtaCommunicationMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String NO_REQUESTS_ERROR_MESSAGE = "There are no requests to reply to. Please select a different communication type.";
    private static final String NO_REQUESTS_DELETE_ERROR_MESSAGE = "There are no requests to delete. Please select a different communication type.";
    private static final String PROVIDE_RESPONSE_ERROR_MESSAGE = "Please provide a response to the FTA query or select No reply required.";
    private static final String NO_REPLIES_ERROR_MESSAGE = "There are no replies to review. Please select a different communication type.";

    FtaCommunicationMidEventHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;


    @BeforeEach
    void setUp() {
        openMocks(this);
        handler = new FtaCommunicationMidEventHandler();

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(callback.getEvent()).thenReturn(EventType.FTA_COMMUNICATION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenAValidEventMidEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    void givenAnInvalidEventMidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    void givenAValidEventAboutToStart_thenReturnFalse() {
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    void throwsExceptionIfItCannotHandle() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertThrows(IllegalStateException.class, () -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION));
    }

    @Test
    void shouldDoNothing_whenNoPageId() {
        when(callback.getPageId()).thenReturn("none");
        CommunicationRequest ftaCommunication1 = buildCustomCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCustomCommRequest("a message", "a user", 1, 2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
        assertEquals(fields, sscsCaseData.getCommunicationFields());
    }

    @Test
    void shouldPopulateDl_whenReviewFtaReplyChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCommRequestWithReply();
        CommunicationRequest ftaCommunication2 = buildCommRequestWithReply();
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaCommunications(existingComs)
            .ftaRequestType(FtaRequestType.REVIEW_FTA_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertNull(response.getData().getCommunicationFields().getFtaRequestsDl());
        DynamicMixedChoiceList dl = response.getData().getCommunicationFields().getTribunalRequestsToReviewDl();
        assertNotNull(dl);
        assertNull(dl.getValue());
        assertEquals(2, dl.getListItems().size());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldIgnoreReviewedInDl_whenReviewFtaReplyChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCommRequestWithReply();
        CommunicationRequest ftaCommunication2 = buildCommRequestWithActionedReply(true);
        CommunicationRequest ftaCommunication3 = buildCommRequestWithoutReply();
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2, ftaCommunication3));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaCommunications(existingComs)
            .ftaRequestType(FtaRequestType.REVIEW_FTA_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicMixedChoiceList dl = response.getData().getCommunicationFields().getTribunalRequestsToReviewDl();
        assertNotNull(dl);
        assertNull(dl.getValue());
        assertEquals(1, dl.getListItems().size());
        assertEquals(ftaCommunication1.getId(), dl.getListItems().getFirst().getCode());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldErrorOnNoReplies_whenReviewFtaReplyChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaCommunications(Collections.emptyList())
            .ftaRequestType(FtaRequestType.REVIEW_FTA_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getTribunalRequestsDl();
        assertNull(dl);
        assertTrue(response.getErrors().contains(NO_REPLIES_ERROR_MESSAGE));
    }


    @Test
    void shouldErrorOnNullRequests_whenReviewFtaReplyChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaRequestType(FtaRequestType.REVIEW_FTA_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getTribunalRequestsDl();
        assertNull(dl);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(NO_REPLIES_ERROR_MESSAGE));
    }

    @Test
    void shouldNotPopulateDl_whenReplyToQueryNotChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCustomCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCustomCommRequest("a message", "a user", 1, 2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .ftaRequestType(FtaRequestType.NEW_REQUEST)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertNull(response.getData().getCommunicationFields().getDeleteCommRequestReadOnly());
        DynamicList dl = response.getData().getCommunicationFields().getFtaRequestsDl();
        assertNull(dl);
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldPopulateDl_whenReplyToQueryChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCustomCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCustomCommRequest("a message", "a user", 1, 2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertNull(response.getData().getCommunicationFields().getDeleteCommRequestReadOnly());
        DynamicList dl = response.getData().getCommunicationFields().getFtaRequestsDl();
        assertNotNull(dl);
        assertNull(dl.getValue());
        assertEquals(2, dl.getListItems().size());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldIgnoreRepliedToInDl_whenReplyToQueryChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCustomCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCustomCommRequest("a message", "a user", 1, 2);
        ftaCommunication2.getValue().setRequestReply(CommunicationRequestReply.builder().build());
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertNull(response.getData().getCommunicationFields().getDeleteCommRequestReadOnly());
        DynamicList dl = response.getData().getCommunicationFields().getFtaRequestsDl();
        assertNotNull(dl);
        assertNull(dl.getValue());
        assertEquals(1, dl.getListItems().size());
        assertEquals(ftaCommunication1.getId(), dl.getListItems().getFirst().getCode());
        assertTrue(dl.getListItems().getFirst().getLabel().contains(ftaCommunication1.getValue().getRequestTopic().getValue()));
        assertTrue(dl.getListItems().getFirst().getLabel().contains(ftaCommunication1.getValue().getRequestUserName()));
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldErrorOnNoRequests_whenReplyToQueryChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(Collections.emptyList())
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertNull(response.getData().getCommunicationFields().getDeleteCommRequestReadOnly());
        DynamicList dl = response.getData().getCommunicationFields().getFtaRequestsDl();
        assertNull(dl);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(NO_REQUESTS_ERROR_MESSAGE));
    }

    @Test
    void shouldErrorOnNullRequests_whenReplyToQueryChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertNull(response.getData().getCommunicationFields().getDeleteCommRequestReadOnly());
        DynamicList dl = response.getData().getCommunicationFields().getFtaRequestsDl();
        assertNull(dl);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(NO_REQUESTS_ERROR_MESSAGE));
    }

    @Test
    void shouldSetQueryForReply_whenSelectFtaRequest() {
        when(callback.getPageId()).thenReturn("selectFtaRequest");

        CommunicationRequest ftaCommunication1 = buildCustomCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCustomCommRequest("a message", "a user", 1, 2);
        DynamicListItem dlItem1 = dlItemFromCommRequest(ftaCommunication1);
        DynamicListItem dlItem2 = dlItemFromCommRequest(ftaCommunication2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));
        DynamicList dynamicList = new DynamicList(dlItem1, List.of(dlItem1, dlItem2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .ftaRequestsDl(dynamicList)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertEquals(ftaCommunication1.getValue().getRequestMessage(), response.getData().getCommunicationFields().getFtaRequestNoResponseQuery());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldThrowIfNoDlItemChosen_whenSelectFtaRequest() {
        when(callback.getPageId()).thenReturn("selectFtaRequest");

        CommunicationRequest ftaCommunication1 = buildCustomCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCustomCommRequest("a message", "a user", 1, 2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));
        DynamicList dynamicList = new DynamicList(null, Collections.emptyList());

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .ftaRequestsDl(dynamicList)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION));
        assertEquals("No chosen FTA request found", exception.getMessage());
    }

    @Test
    void shouldThrowIfDlChosenDoesNotExist_whenSelectFtaRequest() {
        when(callback.getPageId()).thenReturn("selectFtaRequest");

        CommunicationRequest ftaCommunication1 = buildCustomCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCustomCommRequest("a message", "a user", 1, 2);
        DynamicListItem dlItem1 = dlItemFromCommRequest(ftaCommunication1);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication2));
        DynamicList dynamicList = new DynamicList(dlItem1, List.of(dlItem1));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .ftaRequestsDl(dynamicList)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION));
        assertEquals("No communication request found with id: " + ftaCommunication1.getId(), exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("generateResponseActionErrorData")
    void shouldErrorEmptyResponseAndNoActionNotSelected_whenReplyToFtaQuery(String textArea, List<String> noAction) {
        when(callback.getPageId()).thenReturn("replyToFtaQuery");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .commRequestResponseTextArea(textArea)
            .commRequestResponseNoAction(noAction)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(PROVIDE_RESPONSE_ERROR_MESSAGE));
    }

    @Test
    void shouldNotErrorWithResponse_whenReplyToFtaQuery() {
        when(callback.getPageId()).thenReturn("replyToFtaQuery");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .commRequestResponseTextArea("some value")
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldNotErrorWithNoActionSelected_whenReplyToFtaQuery() {
        when(callback.getPageId()).thenReturn("replyToFtaQuery");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .commRequestResponseNoAction(Collections.singletonList("some value"))
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldDoNothing_whenNoRequestType() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");
        CommunicationRequest ftaCommunication1 = buildCustomCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCustomCommRequest("a message", "a user", 1, 2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
        assertEquals(fields, sscsCaseData.getCommunicationFields());
    }

    @Test
    void shouldNotPopulateDeleteDl_whenDeleteRequestReplyNotChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCustomCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCustomCommRequest("a message", "a user", 1, 2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .ftaRequestType(FtaRequestType.NEW_REQUEST)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getDeleteCommRequestRadioDl();
        assertNull(dl);
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldPopulateDeleteDl_whenDeleteRequestReplyChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCustomCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCustomCommRequest("a message", "a user", 1, 2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .ftaRequestType(FtaRequestType.DELETE_REQUEST_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getDeleteCommRequestRadioDl();
        assertNotNull(dl);
        assertNull(dl.getValue());
        assertEquals(2, dl.getListItems().size());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldErrorOnNoRequests_whenDeleteRequestReplyChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(Collections.emptyList())
            .ftaRequestType(FtaRequestType.DELETE_REQUEST_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getDeleteCommRequestRadioDl();
        assertNull(dl);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(NO_REQUESTS_DELETE_ERROR_MESSAGE));
    }

    @Test
    void shouldErrorOnNullRequests_whenDeleteRequestReplyChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaRequestType(FtaRequestType.DELETE_REQUEST_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getDeleteCommRequestRadioDl();
        assertNull(dl);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(NO_REQUESTS_DELETE_ERROR_MESSAGE));
    }


    @Test
    void shouldSetReadOnlyField_whenSelectRequestToDelete() {
        when(callback.getPageId()).thenReturn("selectRequestToDelete");

        CommunicationRequest ftaCommunication1 = buildCustomCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCustomCommRequest("a message", "a user", 1, 2);
        DynamicListItem dlItem1 = dlItemFromCommRequest(ftaCommunication1);
        DynamicListItem dlItem2 = dlItemFromCommRequest(ftaCommunication2);
        DynamicList dynamicList = new DynamicList(dlItem1, List.of(dlItem1, dlItem2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(List.of(ftaCommunication1))
            .ftaCommunications(List.of(ftaCommunication2))
            .deleteCommRequestRadioDl(dynamicList)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertEquals(ftaCommunication1.getValue(), response.getData().getCommunicationFields().getDeleteCommRequestReadOnly());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldThrowIfNoDlItemChosen_whenSelectRequestToDelete() {
        when(callback.getPageId()).thenReturn("selectRequestToDelete");

        CommunicationRequest ftaCommunication1 = buildCustomCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCustomCommRequest("a message", "a user", 1, 2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));
        DynamicList dynamicList = new DynamicList(null, Collections.emptyList());

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .deleteCommRequestRadioDl(dynamicList)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION));
        assertEquals("No chosen request found", exception.getMessage());
    }

    @Test
    void shouldThrowIfDlChosenDoesNotExist_whenSelectRequestToDelete() {
        when(callback.getPageId()).thenReturn("selectRequestToDelete");

        CommunicationRequest ftaCommunication1 = buildCustomCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCustomCommRequest("a message", "a user", 1, 2);
        DynamicListItem dlItem1 = dlItemFromCommRequest(ftaCommunication1);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication2));
        DynamicList dynamicList = new DynamicList(dlItem1, List.of(dlItem1));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaCommunications(existingComs)
            .deleteCommRequestRadioDl(dynamicList)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION));
        assertEquals("No communication request found with id: " + ftaCommunication1.getId(), exception.getMessage());
    }

    static Stream<Arguments> generateResponseActionErrorData() {
        return Stream.of(
            Arguments.of(null, Collections.emptyList()),
            Arguments.of("", Collections.emptyList()),
            Arguments.of(null, null),
            Arguments.of("", null)
        );
    }
}
