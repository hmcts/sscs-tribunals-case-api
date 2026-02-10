package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCommRequest;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCommRequestNotActionedResponseDateOffset;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCommRequestWithId;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCommRequestWithNoActionReply;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCommRequestWithReply;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCustomCommRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.ftacommunication.FtaCommunicationAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestTopic;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicMixedChoiceList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.service.BusinessDaysCalculatorService;

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

    @Mock
    private BusinessDaysCalculatorService businessDaysCalculatorService;


    @BeforeEach
    void setUp() {
        openMocks(this);

        handler = new FtaCommunicationAboutToSubmitHandler(idamService, businessDaysCalculatorService);

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
    void givenValidFtaRequest_shouldAddNewCommunicationToList() throws IOException {
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
        when(businessDaysCalculatorService.getBusinessDay(any(LocalDate.class), anyInt()))
            .thenReturn(LocalDate.now().plusDays(2));
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
        LocalDate date = LocalDate.now().plusDays(2);
        assertEquals(date, addedCom.getRequestResponseDueDate());
        assertEquals(date, response.getData().getCommunicationFields().getFtaResponseDueDate());
    }

    @Test
    void givenValidFtaRequest_shouldAddNewCommunicationToPopulatedList() throws IOException {
        CommunicationRequestTopic expectedTopic = CommunicationRequestTopic.APPEAL_TYPE;
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        CommunicationRequest ftaCommunicationPast = buildCustomCommRequest("Past existing Question",
            "Past existing user", -2, -1);
        CommunicationRequest ftaCommunicationFuture = buildCustomCommRequest("Future existing Question",
            "Future existing user", 1, 2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunicationFuture, ftaCommunicationPast));
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .commRequestTopic(expectedTopic)
            .commRequestQuestion(expectedQuestion)
            .ftaCommunications(existingComs)
            .ftaRequestType(FtaRequestType.NEW_REQUEST)
            .build();

        sscsCaseData.setCommunicationFields(fields);

        UserDetails userDetails = UserDetails.builder()
            .name(expectedUserName)
            .roles(List.of(UserRole.IBCA.getValue()))
            .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
        when(businessDaysCalculatorService.getBusinessDay(any(LocalDate.class), anyInt()))
            .thenReturn(LocalDate.now().plusDays(2));

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getFtaCommunications();

        assertEquals(3, resultComs.size());
        assertEquals(ftaCommunicationFuture, resultComs.getFirst());
        CommunicationRequestDetails addedCom = resultComs.get(1).getValue();
        assertEquals(expectedTopic, addedCom.getRequestTopic());
        assertEquals(expectedQuestion, addedCom.getRequestMessage());
        assertEquals(expectedUserName, addedCom.getRequestUserName());
        assertEquals(UserRole.IBCA.getLabel(), addedCom.getRequestUserRole());
        assertNotNull(addedCom.getRequestDateTime());
        assertEquals(ftaCommunicationPast, resultComs.getLast());
        assertEquals(LocalDate.now().plusDays(2), addedCom.getRequestResponseDueDate());
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
        String replyText = "Reply text";
        CommunicationRequest communicationRequest = buildCommRequest();
        DynamicListItem chosenFtaRequest = new DynamicListItem(communicationRequest.getId(), "item");
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
        assertEquals(YesNo.NO, request.getRequestReply().getReplyHasBeenActionedByFta());
        assertNull(request.getRequestResponseDueDate());
        assertEquals(LocalDate.now(), response.getData().getCommunicationFields().getTribunalResponseProvidedDate());
    }

    @Test
    void shouldUpdateTribunalResponseProvidedDateOnHandleReplyToFtaQueryIfExistingInFuture() {
        String replyText = "Reply text";

        CommunicationRequest communicationRequest = buildCommRequest();
        CommunicationRequest communicationRequest2 = buildCommRequestNotActionedResponseDateOffset(1, true);
        DynamicListItem chosenFtaRequest = new DynamicListItem(communicationRequest.getId(), "item");
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
        assertEquals(YesNo.NO, request.getRequestReply().getReplyHasBeenActionedByFta());
        assertNull(request.getRequestResponseDueDate());
        assertEquals(LocalDate.now(), response.getData().getCommunicationFields().getTribunalResponseProvidedDate());
    }

    @Test
    void shouldNotUpdateFtaResponseProvidedDateOnHandleReplyToFtaQueryIfExistingInPast() {
        String replyText = "Reply text";

        CommunicationRequest communicationRequest = buildCommRequest();
        CommunicationRequest communicationRequest2 = buildCommRequestNotActionedResponseDateOffset(-1, true);
        DynamicListItem chosenFtaRequest = new DynamicListItem(communicationRequest.getId(), "item");
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
        assertEquals(YesNo.NO, request.getRequestReply().getReplyHasBeenActionedByFta());
        assertNull(request.getRequestResponseDueDate());
        assertEquals(LocalDate.now().minusYears(1), response.getData().getCommunicationFields().getTribunalResponseProvidedDate());
    }

    @Test
    void shouldHandleReplyToFtaQueryWithNoActionRequired() {
        CommunicationRequest communicationRequest = buildCommRequestWithNoActionReply();

        DynamicListItem chosenFtaRequest = new DynamicListItem(communicationRequest.getId(), "item");
        DynamicList ftaRequestsDl = new DynamicList(chosenFtaRequest, Collections.singletonList(chosenFtaRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .ftaRequestsDl(ftaRequestsDl)
            .tribunalCommunications(List.of(communicationRequest))
            .commRequestResponseNoAction(Collections.singletonList("No reply required"))
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
        assertEquals("No reply required", request.getRequestReply().getReplyMessage());
        assertEquals(userName, request.getRequestReply().getReplyUserName());
        assertEquals(UserRole.TCW.getLabel(), request.getRequestReply().getReplyUserRole());
        assertNotNull(request.getRequestReply().getReplyDateTime());
        assertNull(communicationRequest.getValue().getRequestReply().getReplyHasBeenActionedByTribunal());
        assertNull(communicationRequest.getValue().getRequestResponseDueDate());
        assertNull(response.getData().getCommunicationFields().getTribunalResponseDueDate());
        assertNull(response.getData().getCommunicationFields().getFtaResponseProvidedDate());
    }

    @Test
    void shouldNotWipeFiltersAfterHandleReplyToFtaQueryWhenRequestNoReplyExists() {
        CommunicationRequest communicationRequest = buildCustomCommRequest("", "", 0, 0);
        CommunicationRequest communicationRequest2 = buildCustomCommRequest("", "", 0, 1);
        CommunicationRequest communicationRequest3 = buildCustomCommRequest("", "", 0, 2);
        DynamicListItem chosenFtaRequest = new DynamicListItem(communicationRequest.getId(), "item");
        DynamicList ftaRequestsDl = new DynamicList(chosenFtaRequest, List.of(chosenFtaRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .ftaRequestsDl(ftaRequestsDl)
            .tribunalCommunications(List.of(communicationRequest, communicationRequest3, communicationRequest2))
            .commRequestResponseNoAction(Collections.singletonList("No reply required"))
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
        assertEquals("No reply required", request.getRequestReply().getReplyMessage());
        assertEquals(userName, request.getRequestReply().getReplyUserName());
        assertNotNull(request.getRequestReply().getReplyDateTime());
        assertNull(communicationRequest.getValue().getRequestResponseDueDate());
        assertEquals(LocalDate.now().plusYears(1), response.getData().getCommunicationFields().getTribunalResponseDueDate());
    }


    @Test
    void shouldThrowExceptionWhenNoCommunicationRequestFoundReply() {
        String chosenFtaRequestId = "1";

        DynamicListItem chosenFtaRequest = new DynamicListItem(chosenFtaRequestId, "item");
        DynamicList ftaRequestsDl = new DynamicList(chosenFtaRequest, Collections.singletonList(chosenFtaRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .ftaRequestsDl(ftaRequestsDl)
            .tribunalCommunications(Collections.emptyList())
            .commRequestResponseNoAction(Collections.singletonList("No reply required"))
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
        CommunicationRequest communicationRequest = buildCommRequestWithId("1");
        CommunicationRequest communicationRequest2 = buildCommRequestWithId("2");
        CommunicationRequest communicationRequest3 = buildCommRequestWithId("3");
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
            .tribunalRequestsToReviewDl(new DynamicMixedChoiceList(null, null))
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
        assertNull(ftaCommunicationFields.getTribunalRequestsToReviewDl());
    }

    @Test
    void givenReviewFtaReply_thenUpdateRequestAsActioned() {
        CommunicationRequest request = buildCommRequestWithReply();
        List<DynamicListItem> selectedItems = Collections.singletonList(new DynamicListItem(request.getId(), "Request 1"));
        DynamicMixedChoiceList dynamicList = new DynamicMixedChoiceList(selectedItems, selectedItems);
        
        sscsCaseData.setCommunicationFields(FtaCommunicationFields.builder()
            .ftaRequestType(FtaRequestType.REVIEW_FTA_REPLY)
            .tribunalRequestsToReviewDl(dynamicList)
            .ftaCommunications(Collections.singletonList(request))
            .build());
        
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder().name("Judge").build());
        when(callback.getEvent()).thenReturn(EventType.FTA_COMMUNICATION);
        
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        
        assertNotNull(response);
        assertEquals(YesNo.YES, response.getData().getCommunicationFields().getFtaCommunications().getFirst().getValue().getRequestReply().getReplyHasBeenActionedByTribunal());
        
        assertNull(response.getData().getCommunicationFields().getCommRequestQuestion());
        assertNull(response.getData().getCommunicationFields().getCommRequestTopic());
        assertNull(response.getData().getCommunicationFields().getFtaRequestType());
        assertNull(response.getData().getCommunicationFields().getTribunalRequestsToReviewDl());
    }

    @Test
    void givenReviewFtaReplyMultipleChoice_thenUpdateRequestAsActioned() {
        CommunicationRequest req1 = buildCommRequestWithReply();
        CommunicationRequest req2 = buildCommRequestWithReply();
        CommunicationRequest req3 = buildCommRequestWithReply();
        DynamicListItem dlItem1 = new DynamicListItem(req1.getId(), "Request 1");
        DynamicListItem dlItem2 = new DynamicListItem(req2.getId(), "Request 2");
        DynamicListItem dlItem3 = new DynamicListItem(req3.getId(), "Request 3");
        List<DynamicListItem> dlList = List.of(dlItem1, dlItem2, dlItem3);
        List<DynamicListItem> selectedItems = List.of(dlItem1, dlItem3);
        DynamicMixedChoiceList dynamicList = new DynamicMixedChoiceList(selectedItems, dlList);

        sscsCaseData.setCommunicationFields(FtaCommunicationFields.builder()
            .ftaRequestType(FtaRequestType.REVIEW_FTA_REPLY)
            .tribunalRequestsToReviewDl(dynamicList)
            .ftaCommunications(List.of(req1, req2, req3))
            .build());

        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder().name("Judge").build());
        when(callback.getEvent()).thenReturn(EventType.FTA_COMMUNICATION);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        List<CommunicationRequestDetails> communicationRequestDetailsList = response.getData().getCommunicationFields().getFtaCommunications()
            .stream()
            .map(CommunicationRequest::getValue)
            .toList();
        assertEquals(3, communicationRequestDetailsList.size());
        assertEquals(YesNo.YES, communicationRequestDetailsList.getFirst().getRequestReply().getReplyHasBeenActionedByTribunal());
        assertEquals(YesNo.NO, communicationRequestDetailsList.get(1).getRequestReply().getReplyHasBeenActionedByTribunal());
        assertEquals(YesNo.YES, communicationRequestDetailsList.getLast().getRequestReply().getReplyHasBeenActionedByTribunal());

        assertNull(response.getData().getCommunicationFields().getFtaRequestType());
        assertNull(response.getData().getCommunicationFields().getTribunalRequestsToReviewDl());
    }
}
