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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestReply;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestTopic;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class FtaCommunicationMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String NO_REQUESTS_ERROR_MESSAGE = "There are no requests to reply to. Please select a different communication type.";
    private static final String PROVIDE_RESPONSE_ERROR_MESSAGE = "Please provide a response to the FTA query or select No action required.";

    FtaCommunicationMidEventHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;


    @BeforeEach
    void setUp() {
        openMocks(this);

        handler = new FtaCommunicationMidEventHandler(true);

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
    void shouldNotPopulateDl_whenReplyToQueryNotChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCommRequest("a message", "a user", 1, 2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaCommunications(existingComs)
            .ftaRequestType(FtaRequestType.NEW_REQUEST)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getFtaRequestNoResponseRadioDl();
        assertNull(dl);
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldPopulateDl_whenReplyToQueryChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCommRequest("a message", "a user", 1, 2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaCommunications(existingComs)
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getFtaRequestNoResponseRadioDl();
        assertNotNull(dl);
        assertNull(dl.getValue());
        assertEquals(2, dl.getListItems().size());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldIgnoreRepliedToInDl_whenReplyToQueryChosen() {
        when(callback.getPageId()).thenReturn("selectFtaCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCommRequest("a message", "a user", 1, 2);
        ftaCommunication2.getValue().setRequestReply(CommunicationRequestReply.builder().build());
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaCommunications(existingComs)
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getFtaRequestNoResponseRadioDl();
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
            .ftaCommunications(Collections.emptyList())
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getFtaRequestNoResponseRadioDl();
        assertNotNull(dl);
        assertNull(dl.getValue());
        assertTrue(dl.getListItems().isEmpty());
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
        DynamicList dl = response.getData().getCommunicationFields().getFtaRequestNoResponseRadioDl();
        assertNotNull(dl);
        assertNull(dl.getValue());
        assertTrue(dl.getListItems().isEmpty());
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(NO_REQUESTS_ERROR_MESSAGE));
    }

    @Test
    void shouldSetQueryForReply_whenSelectFtaRequest() {
        when(callback.getPageId()).thenReturn("selectFtaRequest");

        CommunicationRequest ftaCommunication1 = buildCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCommRequest("a message", "a user", 1, 2);
        DynamicListItem dlItem1 = dlItemFromCommRequest(ftaCommunication1);
        DynamicListItem dlItem2 = dlItemFromCommRequest(ftaCommunication2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));
        DynamicList dynamicList = new DynamicList(dlItem1, List.of(dlItem1, dlItem2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaCommunications(existingComs)
            .ftaRequestNoResponseRadioDl(dynamicList)
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

        CommunicationRequest ftaCommunication1 = buildCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCommRequest("a message", "a user", 1, 2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));
        DynamicList dynamicList = new DynamicList(null, Collections.emptyList());

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaCommunications(existingComs)
            .ftaRequestNoResponseRadioDl(dynamicList)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION));
        assertEquals("No chosen FTA request found", exception.getMessage());
    }

    @Test
    void shouldThrowIfDlChosenDoesNotExist_whenSelectFtaRequest() {
        when(callback.getPageId()).thenReturn("selectFtaRequest");

        CommunicationRequest ftaCommunication1 = buildCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCommRequest("a message", "a user", 1, 2);
        DynamicListItem dlItem1 = dlItemFromCommRequest(ftaCommunication1);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication2));
        DynamicList dynamicList = new DynamicList(dlItem1, List.of(dlItem1));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaCommunications(existingComs)
            .ftaRequestNoResponseRadioDl(dynamicList)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION));
        assertEquals("No communication request found with id: " + ftaCommunication1.getId(), exception.getMessage());
    }

    @Test
    void shouldErrorEmptyResponseAndNoActionNotSelected_whenReplyToFtaQuery() {
        when(callback.getPageId()).thenReturn("replyToFtaQuery");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaRequestNoResponseTextArea("")
            .ftaRequestNoResponseNoAction(Collections.emptyList())
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(PROVIDE_RESPONSE_ERROR_MESSAGE));
    }

    @Test
    void shouldErrorNullResponseAndNullNoAction_whenReplyToFtaQuery() {
        when(callback.getPageId()).thenReturn("replyToFtaQuery");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(PROVIDE_RESPONSE_ERROR_MESSAGE));
    }

    @Test
    void shouldNotErrorWithResponseAndOrNoActionSelected_whenReplyToFtaQuery() {
        when(callback.getPageId()).thenReturn("replyToFtaQuery");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaRequestNoResponseTextArea("some value")
            .ftaRequestNoResponseNoAction(Collections.singletonList("some value"))
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldDoNothing_whenFlagOff() {
        handler = new FtaCommunicationMidEventHandler(false);
        CommunicationRequest ftaCommunication1 = buildCommRequest("some message", "some user", -2, -1);
        CommunicationRequest ftaCommunication2 = buildCommRequest("a message", "a user", 1, 2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaCommunications(existingComs)
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getFtaRequestNoResponseRadioDl();
        assertNull(dl);
        assertTrue(response.getErrors().isEmpty());
    }


    private CommunicationRequest buildCommRequest(String message, String username, int requestDateTimeOffset, int responseDueDateOffset) {
        return CommunicationRequest.builder().value(
            CommunicationRequestDetails.builder()
                .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
                .requestMessage(message)
                .requestDateTime(LocalDateTime.now().plusYears(requestDateTimeOffset))
                .requestUserName(username)
                .requestResponseDueDate(LocalDate.now().plusYears(responseDueDateOffset))
                .build()
        ).build();
    }

    private DynamicListItem dlItemFromCommRequest(CommunicationRequest ftaCommunication) {
        return new DynamicListItem(ftaCommunication.getId(),
            ftaCommunication.getValue().getRequestTopic().getValue() + " - "
                + ftaCommunication.getValue().getRequestDateTime()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm")) + " - "
                + ftaCommunication.getValue().getRequestUserName());
    }
}