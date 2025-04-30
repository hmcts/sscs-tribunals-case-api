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
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.calculateDueDateWorkingDays;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;

class FtaCommunicationAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    FtaCommunicationAboutToSubmitHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamService idamService;


    @BeforeEach
    void setUp() {
        openMocks(this);

        handler = new FtaCommunicationAboutToSubmitHandler(idamService);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(callback.getEvent()).thenReturn(EventType.FTA_COMMUNICATION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    void givenAnInvalidAboutToSubmitEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    void givenAValidAboutToStartEvent_thenReturnFalse() {
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    void throwsExceptionIfItCannotHandleEvent() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    void throwsExceptionIfItCannotHandleCallbackType() {
        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION));
    }

    @Test
    void givenValidFtaRequest_shouldAddNewCommunicationToList() {
        // Setup FTA communication fields
        CommunicationRequestTopic expectedTopic = CommunicationRequestTopic.APPEAL_TYPE;
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        // Create empty list of communications
        List<CommunicationRequest> existingComs = new ArrayList<>();
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .commRequestTopic(expectedTopic)
            .commRequestQuestion(expectedQuestion)
            .ftaCommunications(existingComs)
            .ftaRequestType(FtaRequestType.NEW_REQUEST)
            .build();

        sscsCaseData.setCommunicationFields(fields);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
            .name(expectedUserName)
            .roles(List.of(UserRole.CTSC_CLERK.getValue()))
            .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        // Verify a new FTA communication was added
        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getFtaCommunications();

        assertEquals(1, resultComs.size());

        CommunicationRequestDetails addedCom = resultComs.getFirst().getValue();
        assertEquals(expectedTopic, addedCom.getRequestTopic());
        assertEquals(expectedQuestion, addedCom.getRequestMessage());
        assertEquals(expectedUserName, addedCom.getRequestUserName());
        assertEquals(UserRole.CTSC_CLERK.getLabel(), addedCom.getRequestUserRole());
        assertNotNull(addedCom.getRequestDateTime());
        LocalDate date = calculateDueDateWorkingDays(LocalDate.now(), 2);
        assertEquals(date, addedCom.getRequestResponseDueDate());
        assertEquals(date, response.getData().getCommunicationFields().getFtaResponseDueDate());
    }

    @Test
    void givenValidFtaRequest_shouldAddNewCommunicationToPopulatedList() {
        // Setup FTA communication fields
        CommunicationRequestTopic expectedTopic = CommunicationRequestTopic.APPEAL_TYPE;
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        // Create list of existing communications
        CommunicationRequest ftaCommunicationPast = CommunicationRequest.builder().value(
            CommunicationRequestDetails.builder()
                .requestTopic(CommunicationRequestTopic.ISSUING_OFFICE)
                .requestMessage("Past existing Question")
                .requestDateTime(LocalDateTime.now().minusYears(2))
                .requestUserName("Past existing user")
                .requestResponseDueDate(LocalDate.now().minusYears(1))
                .build()
        ).build();
        CommunicationRequest ftaCommunicationFuture = CommunicationRequest.builder().value(
            CommunicationRequestDetails.builder()
                .requestTopic(CommunicationRequestTopic.OTHER_PARTY_PERSONAL_INFORMATION)
                .requestMessage("Future existing Question")
                .requestDateTime(LocalDateTime.now().plusYears(1))
                .requestUserName("Future existing user")
                .requestResponseDueDate(LocalDate.now().plusYears(2))
                .build()
        ).build();
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunicationFuture, ftaCommunicationPast));
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .commRequestTopic(expectedTopic)
            .commRequestQuestion(expectedQuestion)
            .ftaCommunications(existingComs)
            .ftaRequestType(FtaRequestType.NEW_REQUEST)
            .build();

        sscsCaseData.setCommunicationFields(fields);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
            .name(expectedUserName)
            .roles(List.of(UserRole.IBCA.getValue()))
            .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        // Verify a new FTA communication was added
        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getFtaCommunications();

        assertEquals(3, resultComs.size());

        assertEquals(ftaCommunicationPast, resultComs.getFirst());
        CommunicationRequestDetails addedCom = resultComs.get(1).getValue();
        assertEquals(expectedTopic, addedCom.getRequestTopic());
        assertEquals(expectedQuestion, addedCom.getRequestMessage());
        assertEquals(expectedUserName, addedCom.getRequestUserName());
        assertEquals(UserRole.IBCA.getLabel(), addedCom.getRequestUserRole());
        assertNotNull(addedCom.getRequestDateTime());
        assertEquals(ftaCommunicationFuture, resultComs.getLast());
        assertEquals(calculateDueDateWorkingDays(LocalDate.now(), 2), addedCom.getRequestResponseDueDate());
    }

    @Test
    void givenNullCommunicationsList_shouldHandleGracefully() {
        // Setup FTA communication fields with null communications list
        CommunicationRequestTopic expectedTopic = CommunicationRequestTopic.APPEAL_TYPE;
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .commRequestTopic(expectedTopic)
            .commRequestQuestion(expectedQuestion)
            .ftaCommunications(null) // Explicitly set to null
            .ftaRequestType(FtaRequestType.NEW_REQUEST)
            .build();

        sscsCaseData.setCommunicationFields(fields);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
            .name(expectedUserName)
            .roles(List.of(UserRole.SUPER_USER.getValue()))
            .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        // Verify a new FTA communication was added
        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getFtaCommunications();

        assertNotNull(resultComs);
        assertEquals(UserRole.SUPER_USER.getLabel(), resultComs.getFirst().getValue().getRequestUserRole());
    }

    @Test
    void shouldHandleReplyToFtaQueryWithReplyText() {
        String chosenFtaRequestId = "1";
        String replyText = "Reply text";
        CommunicationRequest communicationRequest = CommunicationRequest.builder()
            .id(chosenFtaRequestId)
            .value(CommunicationRequestDetails.builder().build())
            .build();
        DynamicListItem chosenFtaRequest = new DynamicListItem(chosenFtaRequestId, "item");
        DynamicList ftaRequestsDl = new DynamicList(chosenFtaRequest, Collections.singletonList(chosenFtaRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .ftaRequestsDl(ftaRequestsDl)
            .tribunalCommunications(List.of(communicationRequest))
            .commRequestResponseTextArea(replyText)
            .commRequestResponseNoAction(Collections.emptyList())
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
        String userName = "Test User";
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
            .name(userName).roles(List.of(UserRole.IBCA.getValue())).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        FtaCommunicationFields fields = response.getData().getCommunicationFields();

        CommunicationRequestDetails request = fields.getTribunalCommunications().getFirst().getValue();
        assertNotNull(request.getRequestReply());
        assertEquals(replyText, request.getRequestReply().getReplyMessage());
        assertEquals(userName, request.getRequestReply().getReplyUserName());
        assertEquals(UserRole.IBCA.getLabel(), request.getRequestReply().getReplyUserRole());
        assertNotNull(request.getRequestReply().getReplyDateTime());
        assertEquals(YesNo.NO, request.getRequestReply().getReplyHasBeenActioned());
        assertNull(request.getRequestResponseDueDate());
        assertEquals(LocalDate.now(), response.getData().getCommunicationFields().getFtaResponseProvidedDate());
    }

    @Test
    void shouldUpdateFtaResponseProvidedDateOnHandleReplyToFtaQueryIfExistingInFuture() {
        String chosenFtaRequestId = "1";
        String replyText = "Reply text";

        CommunicationRequest communicationRequest = CommunicationRequest.builder()
            .id(chosenFtaRequestId)
            .value(CommunicationRequestDetails.builder().build())
            .build();
        CommunicationRequest communicationRequest2 = CommunicationRequest.builder()
            .id(chosenFtaRequestId)
            .value(CommunicationRequestDetails.builder()
                .requestReply(CommunicationRequestReply.builder()
                    .replyHasBeenActioned(YesNo.NO)
                    .replyDateTime(LocalDateTime.now().plusYears(1))
                    .build())
                .build())
            .build();
        DynamicListItem chosenFtaRequest = new DynamicListItem(chosenFtaRequestId, "item");
        DynamicList ftaRequestsDl = new DynamicList(chosenFtaRequest, Collections.singletonList(chosenFtaRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .ftaRequestsDl(ftaRequestsDl)
            .tribunalCommunications(List.of(communicationRequest, communicationRequest2))
            .commRequestResponseTextArea(replyText)
            .commRequestResponseNoAction(Collections.emptyList())
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
        String userName = "Test User";
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
            .name(userName).roles(List.of(UserRole.CTSC_CLERK.getValue())).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        FtaCommunicationFields fields = response.getData().getCommunicationFields();

        CommunicationRequestDetails request = fields.getTribunalCommunications().getFirst().getValue();
        assertNotNull(request.getRequestReply());
        assertEquals(replyText, request.getRequestReply().getReplyMessage());
        assertEquals(userName, request.getRequestReply().getReplyUserName());
        assertEquals(UserRole.CTSC_CLERK.getLabel(), request.getRequestReply().getReplyUserRole());
        assertNotNull(request.getRequestReply().getReplyDateTime());
        assertEquals(YesNo.NO, request.getRequestReply().getReplyHasBeenActioned());
        assertNull(request.getRequestResponseDueDate());
        assertEquals(LocalDate.now(), response.getData().getCommunicationFields().getFtaResponseProvidedDate());
    }

    @Test
    void shouldNotUpdateFtaResponseProvidedDateOnHandleReplyToFtaQueryIfExistingInPast() {
        String chosenFtaRequestId = "1";
        String replyText = "Reply text";

        CommunicationRequest communicationRequest = CommunicationRequest.builder()
            .id(chosenFtaRequestId)
            .value(CommunicationRequestDetails.builder().build())
            .build();
        CommunicationRequest communicationRequest2 = CommunicationRequest.builder()
            .id(chosenFtaRequestId)
            .value(CommunicationRequestDetails.builder()
                .requestReply(CommunicationRequestReply.builder()
                    .replyHasBeenActioned(YesNo.NO)
                    .replyDateTime(LocalDateTime.now().minusYears(1))
                    .build())
                .build())
            .build();
        DynamicListItem chosenFtaRequest = new DynamicListItem(chosenFtaRequestId, "item");
        DynamicList ftaRequestsDl = new DynamicList(chosenFtaRequest, Collections.singletonList(chosenFtaRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .ftaRequestsDl(ftaRequestsDl)
            .tribunalCommunications(List.of(communicationRequest, communicationRequest2))
            .commRequestResponseTextArea(replyText)
            .commRequestResponseNoAction(Collections.emptyList())
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
        String userName = "Test User";
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
            .name(userName).roles(List.of(UserRole.SUPER_USER.getValue())).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        FtaCommunicationFields fields = response.getData().getCommunicationFields();

        CommunicationRequestDetails request = fields.getTribunalCommunications().getFirst().getValue();
        assertNotNull(request.getRequestReply());
        assertEquals(replyText, request.getRequestReply().getReplyMessage());
        assertEquals(userName, request.getRequestReply().getReplyUserName());
        assertEquals(UserRole.SUPER_USER.getLabel(), request.getRequestReply().getReplyUserRole());
        assertNotNull(request.getRequestReply().getReplyDateTime());
        assertEquals(YesNo.NO, request.getRequestReply().getReplyHasBeenActioned());
        assertNull(request.getRequestResponseDueDate());
        assertEquals(LocalDate.now().minusYears(1), response.getData().getCommunicationFields().getFtaResponseProvidedDate());
    }

    @Test
    void shouldHandleReplyToFtaQueryWithNoActionRequired() {
        String chosenFtaRequestId = "1";

        CommunicationRequest communicationRequest = CommunicationRequest.builder()
            .id(chosenFtaRequestId)
            .value(CommunicationRequestDetails.builder().build())
            .build();

        DynamicListItem chosenFtaRequest = new DynamicListItem(chosenFtaRequestId, "item");
        DynamicList ftaRequestsDl = new DynamicList(chosenFtaRequest, Collections.singletonList(chosenFtaRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .ftaRequestsDl(ftaRequestsDl)
            .tribunalCommunications(List.of(communicationRequest))
            .commRequestResponseNoAction(Collections.singletonList("No action required"))
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();
        ftaCommunicationFields.setTribunalResponseDueDate(LocalDate.of(1, 1, 1));
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
        String userName = "Test User";
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
            .name(userName).roles(List.of(UserRole.TCW.getValue())).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        FtaCommunicationFields fields = response.getData().getCommunicationFields();

        CommunicationRequestDetails request = fields.getTribunalCommunications().getFirst().getValue();
        assertNotNull(request.getRequestReply());
        assertEquals("No action required", request.getRequestReply().getReplyMessage());
        assertEquals(userName, request.getRequestReply().getReplyUserName());
        assertEquals(UserRole.TCW.getLabel(), request.getRequestReply().getReplyUserRole());
        assertNotNull(request.getRequestReply().getReplyDateTime());
        assertNull(communicationRequest.getValue().getRequestReply().getReplyHasBeenActioned());
        assertNull(communicationRequest.getValue().getRequestResponseDueDate());
        assertNull(response.getData().getCommunicationFields().getTribunalResponseDueDate());
        assertNull(response.getData().getCommunicationFields().getFtaResponseProvidedDate());
    }

    @Test
    void shouldNotWipeFiltersAfterHandleReplyToFtaQueryWhenRequestNoReplyExists() {
        String chosenFtaRequestId = "1";
        String chosenFtaRequestId2 = "2";

        CommunicationRequest communicationRequest = CommunicationRequest.builder()
            .id(chosenFtaRequestId)
            .value(CommunicationRequestDetails.builder()
                .requestResponseDueDate(LocalDate.of(2, 2, 2))
                .build())
            .build();
        CommunicationRequest communicationRequest2 = CommunicationRequest.builder()
            .id(chosenFtaRequestId2)
            .value(CommunicationRequestDetails.builder()
                .requestResponseDueDate(LocalDate.of(3, 3, 3))
                .build())
            .build();
        CommunicationRequest communicationRequest3 = CommunicationRequest.builder()
            .id(chosenFtaRequestId2)
            .value(CommunicationRequestDetails.builder()
                .requestResponseDueDate(LocalDate.of(4, 4, 4))
                .build())
            .build();

        DynamicListItem chosenFtaRequest = new DynamicListItem(chosenFtaRequestId, "item");
        DynamicList ftaRequestsDl = new DynamicList(chosenFtaRequest, List.of(chosenFtaRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .ftaRequestsDl(ftaRequestsDl)
            .tribunalCommunications(List.of(communicationRequest, communicationRequest3, communicationRequest2))
            .commRequestResponseNoAction(Collections.singletonList("No action required"))
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();
        ftaCommunicationFields.setTribunalResponseDueDate(LocalDate.of(1, 1, 1));
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
        String userName = "Test User";
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder().name(userName).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        FtaCommunicationFields fields = response.getData().getCommunicationFields();

        CommunicationRequestDetails request = fields.getTribunalCommunications().getFirst().getValue();
        assertNotNull(request.getRequestReply());
        assertEquals("No action required", request.getRequestReply().getReplyMessage());
        assertEquals(userName, request.getRequestReply().getReplyUserName());
        assertNotNull(request.getRequestReply().getReplyDateTime());
        assertNull(communicationRequest.getValue().getRequestResponseDueDate());
        assertEquals(LocalDate.of(3, 3, 3), response.getData().getCommunicationFields().getTribunalResponseDueDate());
    }


    @Test
    void shouldThrowExceptionWhenNoCommunicationRequestFoundReply() {
        String chosenFtaRequestId = "1";

        DynamicListItem chosenFtaRequest = new DynamicListItem(chosenFtaRequestId, "item");
        DynamicList ftaRequestsDl = new DynamicList(chosenFtaRequest, Collections.singletonList(chosenFtaRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .ftaRequestsDl(ftaRequestsDl)
            .tribunalCommunications(Collections.emptyList())
            .commRequestResponseNoAction(Collections.singletonList("No action required"))
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION)
        );

        assertEquals("No communication request found with id: " + chosenFtaRequestId, exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"1, 1, 1", "2, 1, 1", "3, 2, 0"})
    void shouldHandleDeleteRequestReply(String chosenId, int tribunalCommsSize, int ftaCommsSize) {
        CommunicationRequest communicationRequest = CommunicationRequest.builder()
            .id("1")
            .value(CommunicationRequestDetails.builder().build())
            .build();
        CommunicationRequest communicationRequest2 = CommunicationRequest.builder()
            .id("2")
            .value(CommunicationRequestDetails.builder().build())
            .build();
        CommunicationRequest communicationRequest3 = CommunicationRequest.builder()
            .id("3")
            .value(CommunicationRequestDetails.builder().build())
            .build();
        DynamicListItem chosenDl = new DynamicListItem(chosenId, "item");
        DynamicList ftaRequestsDl = new DynamicList(chosenDl, null);
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .deleteCommRequestRadioDl(ftaRequestsDl)
            .tribunalCommunications(List.of(communicationRequest, communicationRequest2))
            .ftaCommunications(List.of(communicationRequest3))
            .deleteCommRequestReadOnly(communicationRequest3.getValue())
            .deleteCommRequestTextArea("delete reason " + chosenId)
            .ftaRequestType(FtaRequestType.DELETE_REQUEST_REPLY)
            .build();
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
        String userName = "Test User";
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
            .name(userName).roles(List.of(UserRole.TCW.getValue())).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        FtaCommunicationFields fields = response.getData().getCommunicationFields();

        List<CommunicationRequest> tribunalComms = fields.getTribunalCommunications();
        List<CommunicationRequest> ftaComms = fields.getFtaCommunications();
        assertEquals(tribunalCommsSize, tribunalComms.size());
        assertEquals(ftaCommsSize, ftaComms.size());
        assertFalse(tribunalComms.stream().map(CommunicationRequest::getId).toList().contains(chosenId));
        assertFalse(ftaComms.stream().map(CommunicationRequest::getId).toList().contains(chosenId));
        assertEquals(communicationRequest3.getValue(), fields.getDeleteCommRequestReadOnlyStored());
        assertEquals("delete reason " + chosenId, fields.getDeleteCommRequestTextAreaStored());
        assertNull(fields.getDeleteCommRequestReadOnly());
        assertNull(fields.getDeleteCommRequestTextArea());
    }

    @Test
    void shouldClearFieldsAtEndOfEvent() {
        DynamicListItem dynamicListItem = new DynamicListItem("1", "item");
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .commRequestQuestion("question")
            .commRequestTopic(CommunicationRequestTopic.MRN_REVIEW_DECISION_NOTICE_DETAILS)
            .ftaRequestNoResponseQuery("query")
            .commRequestResponseTextArea("textarea")
            .ftaRequestsDl(new DynamicList(dynamicListItem, Collections.singletonList(dynamicListItem)))
            .commRequestResponseNoAction(Collections.singletonList("something"))
            .build();
        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        FtaCommunicationFields ftaCommunicationFields = response.getData().getCommunicationFields();
        assertNull(ftaCommunicationFields.getCommRequestQuestion());
        assertNull(ftaCommunicationFields.getCommRequestTopic());
        assertNull(ftaCommunicationFields.getFtaRequestType());
        assertNull(ftaCommunicationFields.getFtaRequestNoResponseQuery());
        assertNull(ftaCommunicationFields.getCommRequestResponseTextArea());
        assertNull(ftaCommunicationFields.getFtaRequestsDl());
        assertNull(ftaCommunicationFields.getCommRequestResponseNoAction());
    }

    @Test
    void givenReviewFtaReplyWithActionedYesResponse_thenUpdateRequestAsActioned() {
        // Given
        DynamicListItem selectedItem = new DynamicListItem("1", "Request 1");
        List<DynamicListItem> listItems = Collections.singletonList(selectedItem);
        DynamicList dynamicList = new DynamicList(selectedItem, listItems);
        
        sscsCaseData.setCommunicationFields(FtaCommunicationFields.builder()
            .ftaRequestType(FtaRequestType.REVIEW_FTA_REPLY)
            .commRequestActioned(YesNo.YES)
            .tribunalRequestsDl(dynamicList)
            .ftaCommunications(List.of(
                CommunicationRequest.builder()
                    .id("1")
                    .value(CommunicationRequestDetails.builder()
                        .requestMessage("Some message")
                        .requestTopic(CommunicationRequestTopic.APPEAL_TYPE) // Using an existing enum value
                        .requestDateTime(LocalDateTime.now())
                        .requestUserName("Judge")
                        .requestResponseDueDate(LocalDate.now().plusDays(2))
                        .requestReply(CommunicationRequestReply.builder()
                            .replyMessage("Reply message")
                            .replyDateTime(LocalDateTime.now())
                            .replyUserName("Tribunal")
                            .build())
                        .build())
                    .build()
            ))
            .build());
        
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder().name("Judge").build());
        when(callback.getEvent()).thenReturn(EventType.FTA_COMMUNICATION);
        
        // When
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        
        // Then
        assertNotNull(response);
        assertEquals(YesNo.YES, response.getData().getCommunicationFields().getFtaCommunications().getFirst().getValue().getRequestReply().getReplyHasBeenActioned());
        
        // Verify fields are cleared
        assertNull(response.getData().getCommunicationFields().getCommRequestQuestion());
        assertNull(response.getData().getCommunicationFields().getCommRequestTopic());
        assertNull(response.getData().getCommunicationFields().getFtaRequestType());
        assertNull(response.getData().getCommunicationFields().getCommRequestActioned());
        assertNull(response.getData().getCommunicationFields().getTribunalRequestsDl());
    }
}