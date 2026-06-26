package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

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
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.service.BusinessDaysCalculatorService;

class TribunalCommunicationAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private TribunalCommunicationAboutToSubmitHandler handler;

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

        handler = new TribunalCommunicationAboutToSubmitHandler(idamService, businessDaysCalculatorService);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(callback.getEvent()).thenReturn(EventType.TRIBUNAL_COMMUNICATION);
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
    void throwsExceptionIfItCannotHandle() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    void givenValidTribunalRequest_shouldAddNewCommunicationToList() throws IOException {
        // Setup Tribunal communication fields
        CommunicationRequestTopic expectedTopic = CommunicationRequestTopic.APPEAL_TYPE;
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        // Create empty list of communications
        List<CommunicationRequest> existingComs = new ArrayList<>();
        FtaCommunicationFields details = FtaCommunicationFields.builder()
            .commRequestTopic(expectedTopic)
            .commRequestQuestion(expectedQuestion)
            .tribunalCommunications(existingComs)
            .tribunalRequestType(TribunalRequestType.NEW_REQUEST)
            .build();

        sscsCaseData.setCommunicationFields(details);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
            .name(expectedUserName)
            .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
        when(businessDaysCalculatorService.getBusinessDay(any(LocalDate.class), anyInt()))
            .thenReturn(LocalDate.now().plusDays(2));

        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        // Verify a new Tribunal communication was added
        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getTribunalCommunications();

        assertEquals(1, resultComs.size());

        CommunicationRequestDetails addedCom = resultComs.getFirst().getValue();
        assertEquals(expectedTopic, addedCom.getRequestTopic());
        assertEquals(expectedQuestion, addedCom.getRequestMessage());
        assertEquals(expectedUserName, addedCom.getRequestUserName());
        assertNotNull(addedCom.getRequestDateTime());
        assertNotNull(addedCom.getRequestResponseDueDate());
        LocalDate date = LocalDate.now().plusDays(2);
        assertEquals(date, addedCom.getRequestResponseDueDate());
        assertEquals(date, response.getData().getCommunicationFields().getTribunalResponseDueDate());
    }

    @Test
    void givenNoRequestType_shouldDoNothing() {
        List<CommunicationRequest> existingComs = new ArrayList<>();
        FtaCommunicationFields details = FtaCommunicationFields.builder()
            .commRequestTopic(CommunicationRequestTopic.APPEAL_TYPE)
            .commRequestQuestion("someQuestion")
            .tribunalCommunications(existingComs)
            .build();

        sscsCaseData.setCommunicationFields(details);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getTribunalCommunications();
        assertEquals(0, resultComs.size());
    }

    @Test
    void givenValidTribunalRequest_shouldAddNewCommunicationToPopulatedList() throws IOException {
        // Setup Tribunal communication fields
        CommunicationRequestTopic expectedTopic = CommunicationRequestTopic.APPEAL_TYPE;
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        // Create list of existing communications
        CommunicationRequest tribunalCommunicationPast = buildCustomCommRequest("Past existing Question",
            "Past existing user", -2, -1);
        CommunicationRequest tribunalCommunicationFuture = buildCustomCommRequest("Future existing Question",
            "Future existing user", 1, 2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(tribunalCommunicationFuture, tribunalCommunicationPast));
        FtaCommunicationFields details = FtaCommunicationFields.builder()
            .commRequestTopic(expectedTopic)
            .commRequestQuestion(expectedQuestion)
            .tribunalRequestType(TribunalRequestType.NEW_REQUEST)
            .tribunalCommunications(existingComs)
            .build();

        sscsCaseData.setCommunicationFields(details);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
            .name(expectedUserName)
            .roles(List.of(UserRole.CTSC_CLERK.getValue()))
            .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
        when(businessDaysCalculatorService.getBusinessDay(LocalDate.now(), 2))
            .thenReturn(LocalDate.now().plusDays(2));

        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        // Verify a new Tribunal communication was added
        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getTribunalCommunications();

        assertEquals(3, resultComs.size());

        assertEquals(tribunalCommunicationFuture, resultComs.getFirst());
        CommunicationRequestDetails addedCom = resultComs.get(1).getValue();
        assertEquals(expectedTopic, addedCom.getRequestTopic());
        assertEquals(expectedQuestion, addedCom.getRequestMessage());
        assertEquals(expectedUserName, addedCom.getRequestUserName());
        assertEquals(UserRole.CTSC_CLERK.getLabel(), addedCom.getRequestUserRole());
        assertNotNull(addedCom.getRequestDateTime());
        assertEquals(tribunalCommunicationPast, resultComs.getLast());
        assertEquals(LocalDate.now().plusDays(2), addedCom.getRequestResponseDueDate());
        assertEquals(LocalDate.now().minusYears(1), response.getData().getCommunicationFields().getTribunalResponseDueDate());
    }

    @Test
    void givenNullCommunicationsList_shouldHandleGracefully() {
        // Setup Tribunal communication fields with null communications list
        CommunicationRequestTopic expectedTopic = CommunicationRequestTopic.APPEAL_TYPE;
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        FtaCommunicationFields details = FtaCommunicationFields.builder()
            .commRequestTopic(expectedTopic)
            .commRequestQuestion(expectedQuestion)
            .tribunalRequestType(TribunalRequestType.NEW_REQUEST)
            .tribunalCommunications(null) // Explicitly set to null
            .build();

        sscsCaseData.setCommunicationFields(details);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
            .name(expectedUserName)
            .roles(List.of(UserRole.IBCA.getValue()))
            .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        // Verify a new Tribunal communication was added
        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getTribunalCommunications();

        assertNotNull(resultComs);
        assertEquals(1, resultComs.size());

        CommunicationRequestDetails addedCom = resultComs.getFirst().getValue();
        assertEquals(expectedTopic, addedCom.getRequestTopic());
        assertEquals(expectedQuestion, addedCom.getRequestMessage());
        assertEquals(expectedUserName, addedCom.getRequestUserName());
        assertEquals(UserRole.IBCA.getLabel(), addedCom.getRequestUserRole());
        assertNotNull(addedCom.getRequestDateTime());
    }

    @Test
    void shouldHandleReplyToTribunalQueryWithReplyText() {
        String replyText = "Reply text";

        CommunicationRequest communicationRequest = buildCommRequest();
        DynamicListItem chosenTribunalRequest = new DynamicListItem(communicationRequest.getId(), "item");
        DynamicList tribunalRequestsDl = new DynamicList(chosenTribunalRequest, Collections.singletonList(chosenTribunalRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .tribunalRequestsDl(tribunalRequestsDl)
            .ftaCommunications(List.of(communicationRequest))
            .commRequestResponseTextArea(replyText)
            .commRequestResponseNoAction(Collections.emptyList())
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .build();
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
        String userName = "Test User";
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
            .name(userName).roles(List.of(UserRole.IBCA.getValue())).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        FtaCommunicationFields fields = response.getData().getCommunicationFields();

        CommunicationRequestDetails request = fields.getFtaCommunications().getFirst().getValue();
        assertNotNull(request.getRequestReply());
        assertEquals(replyText, request.getRequestReply().getReplyMessage());
        assertEquals(userName, request.getRequestReply().getReplyUserName());
        assertEquals(UserRole.IBCA.getLabel(), request.getRequestReply().getReplyUserRole());
        assertNotNull(request.getRequestReply().getReplyDateTime());
        assertNull(request.getRequestResponseDueDate());
        assertEquals(LocalDate.now(), response.getData().getCommunicationFields().getFtaResponseProvidedDate());
    }

    @Test
    void shouldUpdateFtaResponseProvidedDateOnHandleReplyToTribunalQueryIfExistingInFuture() {
        String replyText = "Reply text";

        CommunicationRequest communicationRequest = buildCommRequest();
        CommunicationRequest communicationRequest2 = buildCommRequestNotActionedResponseDateOffset(1, true);
        DynamicListItem chosenTribunalRequest = new DynamicListItem(communicationRequest.getId(), "item");
        DynamicList ftaRequestsDl = new DynamicList(chosenTribunalRequest, Collections.singletonList(chosenTribunalRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .tribunalRequestsDl(ftaRequestsDl)
            .ftaCommunications(List.of(communicationRequest, communicationRequest2))
            .commRequestResponseTextArea(replyText)
            .commRequestResponseNoAction(Collections.emptyList())
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .build();
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
        String userName = "Test User";
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
            .name(userName).roles(List.of(UserRole.CTSC_CLERK.getValue())).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        FtaCommunicationFields fields = response.getData().getCommunicationFields();

        CommunicationRequestDetails request = fields.getFtaCommunications().getFirst().getValue();
        assertNotNull(request.getRequestReply());
        assertEquals(replyText, request.getRequestReply().getReplyMessage());
        assertEquals(userName, request.getRequestReply().getReplyUserName());
        assertEquals(UserRole.CTSC_CLERK.getLabel(), request.getRequestReply().getReplyUserRole());
        assertNotNull(request.getRequestReply().getReplyDateTime());
        assertNull(request.getRequestResponseDueDate());
        assertEquals(LocalDate.now(), response.getData().getCommunicationFields().getFtaResponseProvidedDate());
    }

    @Test
    void shouldNotUpdateFtaResponseProvidedDateOnHandleReplyToTribunalQueryIfExistingInPast() {
        String replyText = "Reply text";

        CommunicationRequest communicationRequest = buildCommRequest();
        CommunicationRequest communicationRequest2 = buildCommRequestNotActionedResponseDateOffset(-1, true);
        DynamicListItem chosenTribunalRequest = new DynamicListItem(communicationRequest.getId(), "item");
        DynamicList tribunalRequestsDl = new DynamicList(chosenTribunalRequest, Collections.singletonList(chosenTribunalRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .tribunalRequestsDl(tribunalRequestsDl)
            .ftaCommunications(List.of(communicationRequest, communicationRequest2))
            .commRequestResponseTextArea(replyText)
            .commRequestResponseNoAction(Collections.emptyList())
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .build();
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
        String userName = "Test User";
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
            .name(userName).roles(List.of(UserRole.SUPER_USER.getValue())).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        FtaCommunicationFields fields = response.getData().getCommunicationFields();

        CommunicationRequestDetails request = fields.getFtaCommunications().getFirst().getValue();
        assertNotNull(request.getRequestReply());
        assertEquals(replyText, request.getRequestReply().getReplyMessage());
        assertEquals(userName, request.getRequestReply().getReplyUserName());
        assertEquals(UserRole.SUPER_USER.getLabel(), request.getRequestReply().getReplyUserRole());
        assertNotNull(request.getRequestReply().getReplyDateTime());
        assertNull(request.getRequestResponseDueDate());
        assertEquals(LocalDate.now().minusYears(1), response.getData().getCommunicationFields().getFtaResponseProvidedDate());
    }

    @Test
    void shouldHandleReplyToTribunalQueryWithNoActionRequired() {
        CommunicationRequest communicationRequest = buildCommRequest();

        DynamicListItem chosenTribunalRequest = new DynamicListItem(communicationRequest.getId(), "item");
        DynamicList tribunalRequestsDl = new DynamicList(chosenTribunalRequest, Collections.singletonList(chosenTribunalRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .tribunalRequestsDl(tribunalRequestsDl)
            .ftaCommunications(List.of(communicationRequest))
            .commRequestResponseNoAction(Collections.singletonList("No reply required"))
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .build();
        ftaCommunicationFields.setTribunalResponseDueDate(LocalDate.of(1, 1, 1));
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
        String userName = "Test User";
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
            .name(userName).roles(List.of(UserRole.TCW.getValue())).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        FtaCommunicationFields fields = response.getData().getCommunicationFields();

        CommunicationRequestDetails request = fields.getFtaCommunications().getFirst().getValue();
        assertNotNull(request.getRequestReply());
        assertEquals("No reply required", request.getRequestReply().getReplyMessage());
        assertEquals(userName, request.getRequestReply().getReplyUserName());
        assertEquals(UserRole.TCW.getLabel(), request.getRequestReply().getReplyUserRole());
        assertNotNull(request.getRequestReply().getReplyDateTime());
        assertNull(communicationRequest.getValue().getRequestReply().getReplyHasBeenActionedByFta());
        assertNull(communicationRequest.getValue().getRequestResponseDueDate());
        assertNull(response.getData().getCommunicationFields().getFtaResponseDueDate());
        assertNull(response.getData().getCommunicationFields().getTribunalResponseProvidedDate());
    }

    @Test
    void shouldNotWipeFiltersAfterHandleReplyToTribunalQueryWhenRequestNoReplyExists() {
        CommunicationRequest communicationRequest = buildCustomCommRequest("", "", 0, 0);
        CommunicationRequest communicationRequest2 = buildCustomCommRequest("", "", 0, 1);
        CommunicationRequest communicationRequest3 = buildCustomCommRequest("", "", 0, 2);

        DynamicListItem chosenTribunalRequest = new DynamicListItem(communicationRequest.getId(), "item");
        DynamicList tribunalRequestsDl = new DynamicList(chosenTribunalRequest, List.of(chosenTribunalRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .tribunalRequestsDl(tribunalRequestsDl)
            .ftaCommunications(List.of(communicationRequest, communicationRequest3, communicationRequest2))
            .commRequestResponseNoAction(Collections.singletonList("No reply required"))
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .build();
        ftaCommunicationFields.setTribunalResponseDueDate(LocalDate.of(1, 1, 1));
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
        String userName = "Test User";
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder().name(userName).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        FtaCommunicationFields fields = response.getData().getCommunicationFields();

        CommunicationRequestDetails request = fields.getFtaCommunications().getFirst().getValue();
        assertNotNull(request.getRequestReply());
        assertEquals("No reply required", request.getRequestReply().getReplyMessage());
        assertEquals(userName, request.getRequestReply().getReplyUserName());
        assertNotNull(request.getRequestReply().getReplyDateTime());
        assertNull(communicationRequest.getValue().getRequestResponseDueDate());
        assertEquals(LocalDate.now().plusYears(1), response.getData().getCommunicationFields().getFtaResponseDueDate());
    }

    @Test
    void shouldThrowExceptionWhenNoCommunicationRequestFound() {
        String chosenTribunalRequestId = "1";

        DynamicListItem chosenTribunalRequest = new DynamicListItem(chosenTribunalRequestId, "item");
        DynamicList tribunalRequestsDl = new DynamicList(chosenTribunalRequest, Collections.singletonList(chosenTribunalRequest));
        FtaCommunicationFields ftaCommunicationFields = FtaCommunicationFields.builder()
            .tribunalRequestsDl(tribunalRequestsDl)
            .ftaCommunications(Collections.emptyList())
            .commRequestResponseNoAction(Collections.singletonList("No reply required"))
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .build();
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION)
        );

        assertEquals("No communication request found with id: " + chosenTribunalRequestId, exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource(value = {"NEW_REQUEST", "null"}, nullValues = "null")
    void shouldClearFieldsAtEndOfEvent(TribunalRequestType requestType) {
        FtaCommunicationFields communicationFields = FtaCommunicationFields.builder()
            .commRequestTopic(CommunicationRequestTopic.APPEAL_TYPE)
            .commRequestQuestion("someQuestion")
            .tribunalRequestType(requestType)
            .commRequestResponseNoAction(List.of("something"))
            .tribunalRequestNoResponseQuery("something")
            .ftaRequestsToReviewDl(new DynamicMixedChoiceList(null, Collections.emptyList()))
            .commRequestResponseTextArea("something")
            .ftaRequestsDl(new DynamicList(null, Collections.emptyList()))
            .build();
        sscsCaseData.setCommunicationFields(communicationFields);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getCommunicationFields().getCommRequestTopic());
        assertNull(response.getData().getCommunicationFields().getCommRequestQuestion());
        assertNull(response.getData().getCommunicationFields().getTribunalRequestType());
        assertNull(response.getData().getCommunicationFields().getCommRequestResponseNoAction());
        assertNull(response.getData().getCommunicationFields().getFtaRequestsDl());
        assertNull(response.getData().getCommunicationFields().getTribunalRequestNoResponseQuery());
        assertNull(response.getData().getCommunicationFields().getCommRequestResponseTextArea());
        assertNull(response.getData().getCommunicationFields().getFtaRequestsToReviewDl());
        assertNull(response.getData().getCommunicationFields().getTribunalRequestsDl());
    }

    @Test
    void shouldHandleReviewTribunalReplyWithNoMoreReviewsNeeded() {
        CommunicationRequest communicationRequest = buildCommRequestNotActionedResponseDateOffset(0, true);
        CommunicationRequest communicationRequest2 = buildCommRequestNotActionedResponseDateOffset(1, true);
        DynamicListItem dynamicListItem = new DynamicListItem(communicationRequest.getId(), "item");
        DynamicListItem dynamicListItem2 = new DynamicListItem(communicationRequest2.getId(), "item2");
        DynamicMixedChoiceList dynamicList = new DynamicMixedChoiceList(List.of(dynamicListItem2, dynamicListItem), List.of(dynamicListItem2, dynamicListItem));
        FtaCommunicationFields communicationFields = FtaCommunicationFields.builder()
            .tribunalRequestType(TribunalRequestType.REVIEW_TRIBUNAL_REPLY)
            .ftaRequestsToReviewDl(dynamicList)
            .tribunalCommunications(List.of(communicationRequest, communicationRequest2))
            .ftaResponseProvidedDate(LocalDate.now())
            .build();
        sscsCaseData.setCommunicationFields(communicationFields);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getTribunalCommunications();
        assertNotNull(resultComs);
        assertEquals(2, resultComs.size());
        assertEquals(YesNo.YES, resultComs.getFirst().getValue().getRequestReply().getReplyHasBeenActionedByFta());
        assertEquals(YesNo.YES, resultComs.getLast().getValue().getRequestReply().getReplyHasBeenActionedByFta());
        assertNull(response.getData().getCommunicationFields().getFtaResponseProvidedDate());
    }

    @Test
    void shouldHandleReviewTribunalReplyWithOlderReviewStillNeeded() {
        CommunicationRequest communicationRequest1 = buildCommRequestNotActionedResponseDateOffset(1, true);
        CommunicationRequest communicationRequest2 = buildCommRequestNotActionedResponseDateOffset(0, true);
        CommunicationRequest communicationRequest3 = buildCommRequestNotActionedResponseDateOffset(-1, true);
        DynamicListItem dynamicListItem1 = new DynamicListItem(communicationRequest1.getId(), "item1");
        DynamicListItem dynamicListItem2 = new DynamicListItem(communicationRequest2.getId(), "item2");
        DynamicListItem dynamicListItem3 = new DynamicListItem(communicationRequest3.getId(), "item3");
        DynamicMixedChoiceList dynamicList = new DynamicMixedChoiceList(List.of(dynamicListItem1),
            List.of(dynamicListItem1, dynamicListItem2, dynamicListItem3));
        FtaCommunicationFields communicationFields = FtaCommunicationFields.builder()
            .tribunalRequestType(TribunalRequestType.REVIEW_TRIBUNAL_REPLY)
            .ftaRequestsToReviewDl(dynamicList)
            .tribunalCommunications(List.of(communicationRequest3, communicationRequest2, communicationRequest1))
            .build();
        sscsCaseData.setCommunicationFields(communicationFields);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getTribunalCommunications();
        assertNotNull(resultComs);
        assertEquals(3, resultComs.size());
        assertEquals(YesNo.NO, resultComs.getFirst().getValue().getRequestReply().getReplyHasBeenActionedByFta());
        assertEquals(YesNo.NO, resultComs.get(1).getValue().getRequestReply().getReplyHasBeenActionedByFta());
        assertEquals(YesNo.YES, resultComs.getLast().getValue().getRequestReply().getReplyHasBeenActionedByFta());
        assertEquals(LocalDate.now().minusYears(1), response.getData().getCommunicationFields().getTribunalResponseProvidedDate());
    }

    @Test
    void shouldHandleReviewTribunalReplyWithNewReviewStillNeeded() {
        CommunicationRequest communicationRequest1 = buildCommRequestNotActionedResponseDateOffset(1, true);
        CommunicationRequest communicationRequest2 = buildCommRequestNotActionedResponseDateOffset(0, true);
        CommunicationRequest communicationRequest3 = buildCommRequestNotActionedResponseDateOffset(-1, true);
        DynamicListItem dynamicListItem1 = new DynamicListItem(communicationRequest1.getId(), "item1");
        DynamicListItem dynamicListItem2 = new DynamicListItem(communicationRequest2.getId(), "item2");
        DynamicListItem dynamicListItem3 = new DynamicListItem(communicationRequest3.getId(), "item3");
        DynamicMixedChoiceList dynamicList = new DynamicMixedChoiceList(Collections.singletonList(dynamicListItem3),
            List.of(dynamicListItem1, dynamicListItem2, dynamicListItem3));
        FtaCommunicationFields communicationFields = FtaCommunicationFields.builder()
            .tribunalRequestType(TribunalRequestType.REVIEW_TRIBUNAL_REPLY)
            .ftaRequestsToReviewDl(dynamicList)
            .tribunalCommunications(List.of(communicationRequest1, communicationRequest2, communicationRequest3))
            .build();
        sscsCaseData.setCommunicationFields(communicationFields);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getTribunalCommunications();
        assertNotNull(resultComs);
        assertEquals(3, resultComs.size());
        assertEquals(YesNo.NO, resultComs.getFirst().getValue().getRequestReply().getReplyHasBeenActionedByFta());
        assertEquals(YesNo.NO, resultComs.get(1).getValue().getRequestReply().getReplyHasBeenActionedByFta());
        assertEquals(YesNo.YES, resultComs.getLast().getValue().getRequestReply().getReplyHasBeenActionedByFta());
        assertEquals(LocalDate.now(), response.getData().getCommunicationFields().getTribunalResponseProvidedDate());
    }
}