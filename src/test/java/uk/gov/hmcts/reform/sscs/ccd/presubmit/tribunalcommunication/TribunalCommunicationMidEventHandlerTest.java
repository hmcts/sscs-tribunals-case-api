package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.TribunalRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

class TribunalCommunicationMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String NO_REPLIES_ERROR_MESSAGE = "There are no replies to review. Please select a different communication type.";
    private static final String PROVIDE_ACTION_ERROR_MESSAGE = "Please only select Yes if all actions to the response have been completed.";

    private TribunalCommunicationMidEventHandler handler;
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
        handler = new TribunalCommunicationMidEventHandler(true);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(callback.getEvent()).thenReturn(EventType.TRIBUNAL_COMMUNICATION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenValidMidEventCallback_thenReturnTrue() {
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    void givenAnInvalidEventMidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    void givenAboutToStartCallbackType_thenReturnFalse() {
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    void throwsExceptionIfItCannotHandle() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertThrows(IllegalStateException.class, () -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION));
    }

    @Test
    void shouldNotPopulateDl_whenReviewTribunalReplyNotChosen() {
        when(callback.getPageId()).thenReturn("selectTribunalCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCommRequestWithReply();
        CommunicationRequest ftaCommunication2 = buildCommRequestWithReply();
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .tribunalRequestType(TribunalRequestType.NEW_REQUEST)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getTribunalRequestRespondedDl();
        assertNull(dl);
        assertTrue(response.getErrors().isEmpty());
    }
  
    @Test
    void givenFeatureToggleDisabled_thenReturnUnchangedResponse() {
        handler = new TribunalCommunicationMidEventHandler(false);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(sscsCaseData, response.getData());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldPopulateDl_whenReviewTribunalReplyChosen() {
        when(callback.getPageId()).thenReturn("selectTribunalCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCommRequestWithReply();
        CommunicationRequest ftaCommunication2 = buildCommRequestWithReply();
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .tribunalRequestType(TribunalRequestType.REVIEW_TRIBUNAL_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getTribunalRequestRespondedDl();
        assertNotNull(dl);
        assertNull(dl.getValue());
        assertEquals(2, dl.getListItems().size());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldDoNothing_whenFlagOff() {
        when(callback.getPageId()).thenReturn("selectTribunalCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCommRequestWithReply();
        CommunicationRequest ftaCommunication2 = buildCommRequestWithReply();
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .tribunalRequestType(TribunalRequestType.REVIEW_TRIBUNAL_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        handler = new TribunalCommunicationMidEventHandler(false);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getTribunalRequestRespondedDl();
        assertNull(dl);
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldIgnoreReviewedInDl_whenReviewTribunalReplyChosen() {
        when(callback.getPageId()).thenReturn("selectTribunalCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCommRequestWithReply();
        CommunicationRequest ftaCommunication2 = buildCommRequestWithActionedReply();
        CommunicationRequest ftaCommunication3 = buildCommRequestWithoutReply();
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2, ftaCommunication3));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .tribunalRequestType(TribunalRequestType.REVIEW_TRIBUNAL_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getTribunalRequestRespondedDl();
        assertNotNull(dl);
        assertNull(dl.getValue());
        assertEquals(1, dl.getListItems().size());
        assertEquals(ftaCommunication1.getId(), dl.getListItems().getFirst().getCode());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldErrorOnNoReplies_whenReviewTribunalReplyChosen() {
        when(callback.getPageId()).thenReturn("selectTribunalCommunicationAction");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(Collections.emptyList())
            .tribunalRequestType(TribunalRequestType.REVIEW_TRIBUNAL_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getTribunalRequestRespondedDl();
        assertNull(dl);
        assertTrue(response.getErrors().contains(NO_REPLIES_ERROR_MESSAGE));
    }


    @Test
    void shouldErrorOnNullRequests_whenReviewTribunalReplyChosen() {
        when(callback.getPageId()).thenReturn("selectTribunalCommunicationAction");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalRequestType(TribunalRequestType.REVIEW_TRIBUNAL_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicList dl = response.getData().getCommunicationFields().getTribunalRequestRespondedDl();
        assertNull(dl);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(NO_REPLIES_ERROR_MESSAGE));
    }

    @Test
    void shouldSetQueryReplyForReviewPage_whenSelectTribunalReply() {
        when(callback.getPageId()).thenReturn("selectTribunalReply");

        CommunicationRequest ftaCommunication1 = buildCommRequestWithReply();
        CommunicationRequest ftaCommunication2 = buildCommRequestWithReply();
        DynamicListItem dlItem1 = dlItemFromCommRequest(ftaCommunication1);
        DynamicListItem dlItem2 = dlItemFromCommRequest(ftaCommunication2);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));
        DynamicList dynamicList = new DynamicList(dlItem1, List.of(dlItem1, dlItem2));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .tribunalRequestRespondedDl(dynamicList)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        assertEquals(ftaCommunication1.getValue().getRequestMessage(), response.getData().getCommunicationFields().getTribunalRequestRespondedQuery());
        assertEquals(ftaCommunication1.getValue().getRequestReply().getReplyMessage(), response.getData().getCommunicationFields().getTribunalRequestRespondedReply());
        assertTrue(response.getErrors().isEmpty());
    }
  
    @Test
    void givenSelectTribunalCommunicationActionPage_andReplyToQueryWithNoCommunications_thenShowError() {
        when(callback.getPageId()).thenReturn("selectTribunalCommunicationAction");
        
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .tribunalCommunications(Collections.emptyList())
            .build();
        
        sscsCaseData.setCommunicationFields(fields);
        
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        
        assertFalse(response.getErrors().isEmpty());
        assertEquals("There are no requests to reply to. Please select a different communication type.", 
                    response.getErrors().iterator().next());
    }

    @Test
    void givenSelectTribunalCommunicationActionPage_andValidCommunications_thenCreateDynamicList() {
        when(callback.getPageId()).thenReturn("selectTribunalCommunicationAction");
        
        List<CommunicationRequest> requests = new ArrayList<>();
        CommunicationRequestDetails requestDetails = CommunicationRequestDetails.builder()
            .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
            .requestMessage("Test message")
            .requestDateTime(LocalDateTime.now())
            .requestUserName("Test User")
            .build();
        
        requests.add(CommunicationRequest.builder()
            .id("1")
            .value(requestDetails)
            .build());
        
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .ftaCommunications(requests)
            .build();
        
        sscsCaseData.setCommunicationFields(fields);
        
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        
        assertTrue(response.getErrors().isEmpty());
        assertNotNull(response.getData().getCommunicationFields().getTribunalRequestNoResponseRadioDl());
        assertFalse(response.getData().getCommunicationFields().getTribunalRequestNoResponseRadioDl().getListItems().isEmpty());
    }

    @Test
    void givenSelectTribunalRequestPage_thenPopulateQueryForReply() {
        when(callback.getPageId()).thenReturn("selectTribunalRequest");
        
        // Create a communication request
        CommunicationRequestDetails requestDetails = CommunicationRequestDetails.builder()
            .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
            .requestMessage("Test message to retrieve")
            .requestDateTime(LocalDateTime.now())
            .requestUserName("Test User")
            .build();
        
        CommunicationRequest request = CommunicationRequest.builder()
            .id("1")
            .value(requestDetails)
            .build();
        
        List<CommunicationRequest> requests = Collections.singletonList(request);
        
        // Create dynamic list with selected item
        DynamicListItem selectedItem = new DynamicListItem("1", "Test Topic");
        DynamicList dynamicList = new DynamicList(selectedItem, Collections.singletonList(selectedItem));
        
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .ftaCommunications(requests)
            .tribunalRequestNoResponseRadioDl(dynamicList)
            .build();
        
        sscsCaseData.setCommunicationFields(fields);
        
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        
        assertTrue(response.getErrors().isEmpty());
        assertEquals("Test message to retrieve", response.getData().getCommunicationFields().getTribunalRequestNoResponseQuery());
    }

    @Test
    void givenReplyToTribunalQueryPage_withEmptyResponseAndNoAction_thenShowError() {
        when(callback.getPageId()).thenReturn("replyToTribunalQuery");
        
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .tribunalRequestNoResponseTextArea("")
            .build();
        
        sscsCaseData.setCommunicationFields(fields);
        
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        
        assertFalse(response.getErrors().isEmpty());
        assertEquals("Please provide a response to the Tribunal query or select No action required.", 
                    response.getErrors().iterator().next());
    }

    @Test
    void givenReplyToTribunalQueryPage_withValidResponse_thenNoError() {
        when(callback.getPageId()).thenReturn("replyToTribunalQuery");
        
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .tribunalRequestNoResponseTextArea("This is a response")
            .build();
        
        sscsCaseData.setCommunicationFields(fields);
        
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void givenReplyToTribunalQueryPage_withNoActionRequired_thenNoError() {
        when(callback.getPageId()).thenReturn("replyToTribunalQuery");
        
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .tribunalRequestNoResponseTextArea("")
            .tribunalRequestNoResponseNoAction(Collections.singletonList("No action required"))
            .build();
        
        sscsCaseData.setCommunicationFields(fields);
        
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldThrowIfNoDlItemChosen_whenSelectTribunalReply() {
        when(callback.getPageId()).thenReturn("selectTribunalReply");

        CommunicationRequest ftaCommunication1 = buildCommRequestWithReply();
        CommunicationRequest ftaCommunication2 = buildCommRequestWithReply();
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2));
        DynamicList dynamicList = new DynamicList(null, Collections.emptyList());

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .tribunalRequestRespondedDl(dynamicList)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION));
        assertEquals("No chosen FTA request found", exception.getMessage());
    }

    @Test
    void shouldThrowIfDlChosenDoesNotExist_whenSelectTribunalReply() {
        when(callback.getPageId()).thenReturn("selectTribunalReply");

        CommunicationRequest ftaCommunication1 = buildCommRequestWithReply();
        CommunicationRequest ftaCommunication2 = buildCommRequestWithReply();
        DynamicListItem dlItem1 = dlItemFromCommRequest(ftaCommunication1);
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication2));
        DynamicList dynamicList = new DynamicList(dlItem1, List.of(dlItem1));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .tribunalRequestRespondedDl(dynamicList)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION));
        assertEquals("No communication request found with id: " + ftaCommunication1.getId(), exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource(value = {"NO", "null"}, nullValues = "null")
    void shouldErrorIfNoOrNullAction_whenReviewTribunalReply(YesNo yesNo) {
        when(callback.getPageId()).thenReturn("reviewTribunalReply");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalRequestRespondedActioned(yesNo)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(1, preSubmitCallbackResponse.getErrors().size());
        assertTrue(preSubmitCallbackResponse.getErrors().contains(PROVIDE_ACTION_ERROR_MESSAGE));
    }

    @Test
    void shouldNotErrorIfYesAction_whenReviewTribunalReply() {
        when(callback.getPageId()).thenReturn("reviewTribunalReply");

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalRequestRespondedActioned(YesNo.YES)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertTrue(preSubmitCallbackResponse.getErrors().isEmpty());
    }

    @Test
    void givenUnknownPageId_thenNoErrorAndNoChanges() {
        when(callback.getPageId()).thenReturn("unknownPage");
        
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .build();
        
        sscsCaseData.setCommunicationFields(fields);
        
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        
        assertTrue(response.getErrors().isEmpty());
        assertEquals(sscsCaseData, response.getData());
    }

    private CommunicationRequest buildCommRequestWithoutReply() {
        return CommunicationRequest.builder().value(
            CommunicationRequestDetails.builder()
                .requestUserName("some user")
                .requestDateTime(LocalDateTime.now())
                .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
                .build())
            .build();
    }

    private CommunicationRequest buildCommRequestWithReply() {
        return CommunicationRequest.builder().value(
            CommunicationRequestDetails.builder()
                .requestUserName("some user")
                .requestDateTime(LocalDateTime.now())
                .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
                .requestMessage("some request message")
                .requestReply(CommunicationRequestReply.builder()
                    .replyDateTime(LocalDateTime.now())
                    .replyMessage("some reply message")
                    .replyHasBeenActioned(YesNo.NO)
                    .build())
                .build())
            .build();
    }

    private CommunicationRequest buildCommRequestWithActionedReply() {
        return CommunicationRequest.builder().value(
                CommunicationRequestDetails.builder()
                    .requestUserName("some user")
                    .requestDateTime(LocalDateTime.now())
                    .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
                    .requestReply(CommunicationRequestReply.builder()
                        .replyDateTime(LocalDateTime.now())
                        .replyHasBeenActioned(YesNo.YES)
                        .build())
                    .build())
            .build();
    }

    private DynamicListItem dlItemFromCommRequest(CommunicationRequest ftaCommunication) {
        return new DynamicListItem(ftaCommunication.getId(),
            ftaCommunication.getValue().getRequestTopic().getValue() + " - "
                + ftaCommunication.getValue().getRequestDateTime()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm")) + " - "
                + ftaCommunication.getValue().getRequestUserName());
    }
}
