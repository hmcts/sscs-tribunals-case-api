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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestTopic;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFilter;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.TribunalCommunicationFilter;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;

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

        handler = new FtaCommunicationAboutToSubmitHandler(idamService, true);

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
            .ftaRequestTopic(expectedTopic)
            .ftaRequestQuestion(expectedQuestion)
            .ftaCommunications(existingComs)
            .ftaRequestType(FtaRequestType.NEW_REQUEST)
            .build();

        sscsCaseData.setCommunicationFields(fields);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
            .name(expectedUserName)
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
        assertNotNull(addedCom.getRequestDateTime());

        // Verify the enum values are correctly set
        assertEquals(FtaCommunicationFilter.PROVIDE_INFO_TO_TRIBUNAL, response.getData().getCommunicationFields().getFtaCommunicationFilter());
        assertEquals(TribunalCommunicationFilter.AWAITING_INFO_FROM_FTA, response.getData().getCommunicationFields().getTribunalCommunicationFilter());
    }

    @Test
    void givenFlagOff_shouldDoNothing() {
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaRequestTopic(CommunicationRequestTopic.APPEAL_TYPE)
            .ftaRequestQuestion("someQuestion")
            .ftaCommunications(Collections.emptyList())
            .ftaRequestType(FtaRequestType.NEW_REQUEST)
            .build();
        sscsCaseData.setCommunicationFields(fields);
        handler = new FtaCommunicationAboutToSubmitHandler(idamService, false);
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getFtaCommunications();
        assertEquals(0, resultComs.size());
        assertNull(response.getData().getCommunicationFields().getFtaCommunicationFilter());
        assertNull(response.getData().getCommunicationFields().getTribunalCommunicationFilter());
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
            .ftaRequestTopic(expectedTopic)
            .ftaRequestQuestion(expectedQuestion)
            .ftaCommunications(existingComs)
            .ftaRequestType(FtaRequestType.NEW_REQUEST)
            .build();

        sscsCaseData.setCommunicationFields(fields);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
            .name(expectedUserName)
            .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        // Verify a new FTA communication was added
        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getFtaCommunications();

        assertEquals(3, resultComs.size());

        assertEquals(ftaCommunicationFuture, resultComs.getFirst());
        CommunicationRequestDetails addedCom = resultComs.get(1).getValue();
        assertEquals(expectedTopic, addedCom.getRequestTopic());
        assertEquals(expectedQuestion, addedCom.getRequestMessage());
        assertEquals(expectedUserName, addedCom.getRequestUserName());
        assertNotNull(addedCom.getRequestDateTime());
        assertEquals(ftaCommunicationPast, resultComs.getLast());

        // Verify the enum values are correctly set
        assertEquals(FtaCommunicationFilter.PROVIDE_INFO_TO_TRIBUNAL, response.getData().getCommunicationFields().getFtaCommunicationFilter());
        assertEquals(TribunalCommunicationFilter.AWAITING_INFO_FROM_FTA, response.getData().getCommunicationFields().getTribunalCommunicationFilter());
    }

    @Test
    void givenNullCommunicationsList_shouldHandleGracefully() {
        // Setup FTA communication fields with null communications list
        CommunicationRequestTopic expectedTopic = CommunicationRequestTopic.APPEAL_TYPE;
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaRequestTopic(expectedTopic)
            .ftaRequestQuestion(expectedQuestion)
            .ftaCommunications(null) // Explicitly set to null
            .ftaRequestType(FtaRequestType.NEW_REQUEST)
            .build();

        sscsCaseData.setCommunicationFields(fields);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
            .name(expectedUserName)
            .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        // Verify a new FTA communication was added
        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getFtaCommunications();

        assertNotNull(resultComs);

        // Verify the enum values are correctly set
        assertEquals(FtaCommunicationFilter.PROVIDE_INFO_TO_TRIBUNAL, response.getData().getCommunicationFields().getFtaCommunicationFilter());
        assertEquals(TribunalCommunicationFilter.AWAITING_INFO_FROM_FTA, response.getData().getCommunicationFields().getTribunalCommunicationFilter());
    }

    @Test
    void shouldHandleReplyToFtaQueryWithReplyText() {
        String chosenFtaRequestId = "1";
        String replyText = "Reply text";
        String userName = "Test User";

        CommunicationRequest communicationRequest = CommunicationRequest.builder()
            .id(chosenFtaRequestId)
            .value(CommunicationRequestDetails.builder().build())
            .build();
        DynamicListItem chosenFtaRequest = new DynamicListItem(chosenFtaRequestId, "item");
        DynamicList ftaRequestNoResponseRadioDl = new DynamicList(chosenFtaRequest, Collections.singletonList(chosenFtaRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .ftaRequestNoResponseRadioDl(ftaRequestNoResponseRadioDl)
            .tribunalCommunications(List.of(communicationRequest))
            .ftaRequestNoResponseTextArea(replyText)
            .ftaRequestNoResponseNoAction(Collections.emptyList())
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder().name(userName).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        FtaCommunicationFields fields = response.getData().getCommunicationFields();

        CommunicationRequestDetails request = fields.getTribunalCommunications().getFirst().getValue();
        assertNotNull(request.getRequestReply());
        assertEquals(replyText, request.getRequestReply().getReplyMessage());
        assertEquals(userName, request.getRequestReply().getReplyUserName());
        assertNotNull(request.getRequestReply().getReplyDateTime());
        assertNull(request.getRequestResponseDueDate());
        assertEquals(FtaCommunicationFilter.INFO_PROVIDED_FROM_TRIBUNAL, fields.getFtaCommunicationFilter());
        assertNull(fields.getTribunalCommunicationFilter());
    }

    @Test
    void shouldHandleReplyToFtaQueryWithNoActionRequired() {
        String chosenFtaRequestId = "1";
        String userName = "Test User";

        CommunicationRequest communicationRequest = CommunicationRequest.builder()
            .id(chosenFtaRequestId)
            .value(CommunicationRequestDetails.builder().build())
            .build();

        DynamicListItem chosenFtaRequest = new DynamicListItem(chosenFtaRequestId, "item");
        DynamicList ftaRequestNoResponseRadioDl = new DynamicList(chosenFtaRequest, Collections.singletonList(chosenFtaRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .ftaRequestNoResponseRadioDl(ftaRequestNoResponseRadioDl)
            .tribunalCommunications(List.of(communicationRequest))
            .ftaRequestNoResponseNoAction(Collections.singletonList("No action required"))
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);

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
        assertNull(fields.getFtaCommunicationFilter());
        assertNull(fields.getTribunalCommunicationFilter());
    }

    @Test
    void shouldThrowExceptionWhenNoCommunicationRequestFound() {
        String chosenFtaRequestId = "1";

        DynamicListItem chosenFtaRequest = new DynamicListItem(chosenFtaRequestId, "item");
        DynamicList ftaRequestNoResponseRadioDl = new DynamicList(chosenFtaRequest, Collections.singletonList(chosenFtaRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .ftaRequestNoResponseRadioDl(ftaRequestNoResponseRadioDl)
            .tribunalCommunications(Collections.emptyList())
            .ftaRequestNoResponseNoAction(Collections.singletonList("No action required"))
            .ftaRequestType(FtaRequestType.REPLY_TO_FTA_QUERY)
            .build();
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION)
        );

        assertEquals("No communication request found with id: " + chosenFtaRequestId, exception.getMessage());
    }

    @Test
    void shouldClearFieldsAtEndOfEvent() {
        DynamicListItem dynamicListItem = new DynamicListItem("1", "item");
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaRequestQuestion("question")
            .ftaRequestTopic(CommunicationRequestTopic.MRN_REVIEW_DECISION_NOTICE_DETAILS)
            .ftaRequestNoResponseQuery("query")
            .ftaRequestNoResponseTextArea("textarea")
            .ftaRequestNoResponseRadioDl(new DynamicList(dynamicListItem, Collections.singletonList(dynamicListItem)))
            .ftaRequestNoResponseNoAction(Collections.singletonList("something"))
            .build();
        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        FtaCommunicationFields ftaCommunicationFields = response.getData().getCommunicationFields();
        assertNull(ftaCommunicationFields.getFtaRequestQuestion());
        assertNull(ftaCommunicationFields.getFtaRequestTopic());
        assertNull(ftaCommunicationFields.getFtaRequestType());
        assertNull(ftaCommunicationFields.getFtaRequestNoResponseQuery());
        assertNull(ftaCommunicationFields.getFtaRequestNoResponseTextArea());
        assertNull(ftaCommunicationFields.getFtaRequestNoResponseRadioDl());
        assertNull(ftaCommunicationFields.getFtaRequestNoResponseNoAction());
    }
}