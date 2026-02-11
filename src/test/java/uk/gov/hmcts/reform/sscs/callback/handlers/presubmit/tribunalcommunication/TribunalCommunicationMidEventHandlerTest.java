package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.tribunalcommunication;

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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.tribunalcommunication.TribunalCommunicationMidEventHandler;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.TribunalRequestType;

class TribunalCommunicationMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String NO_REPLIES_ERROR_MESSAGE = "There are no replies to review. Please select a different communication type.";

    private TribunalCommunicationMidEventHandler handler;
    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    void setUp() {
        openMocks(this);
        handler = new TribunalCommunicationMidEventHandler();
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
        DynamicList dl = response.getData().getCommunicationFields().getTribunalRequestsDl();
        assertNull(dl);
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
        assertNull(response.getData().getCommunicationFields().getTribunalRequestsDl());
        DynamicMixedChoiceList dl = response.getData().getCommunicationFields().getFtaRequestsToReviewDl();
        assertNotNull(dl);
        assertNull(dl.getValue());
        assertEquals(2, dl.getListItems().size());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void shouldIgnoreReviewedInDl_whenReviewTribunalReplyChosen() {
        when(callback.getPageId()).thenReturn("selectTribunalCommunicationAction");

        CommunicationRequest ftaCommunication1 = buildCommRequestWithReply();
        CommunicationRequest ftaCommunication2 = buildCommRequestWithActionedReply(false);
        CommunicationRequest ftaCommunication3 = buildCommRequestWithoutReply();
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(ftaCommunication1, ftaCommunication2, ftaCommunication3));

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(existingComs)
            .tribunalRequestType(TribunalRequestType.REVIEW_TRIBUNAL_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response);
        DynamicMixedChoiceList dl = response.getData().getCommunicationFields().getFtaRequestsToReviewDl();
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
        DynamicList dl = response.getData().getCommunicationFields().getTribunalRequestsDl();
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
        DynamicList dl = response.getData().getCommunicationFields().getTribunalRequestsDl();
        assertNull(dl);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(NO_REPLIES_ERROR_MESSAGE));
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
        assertNotNull(response.getData().getCommunicationFields().getTribunalRequestsDl());
        assertNull(response.getData().getCommunicationFields().getFtaRequestsDl());
        assertFalse(response.getData().getCommunicationFields().getTribunalRequestsDl().getListItems().isEmpty());
    }

    @Test
    void givenSelectTribunalRequestPage_thenPopulateQueryForReply() {
        when(callback.getPageId()).thenReturn("selectTribunalRequest");
        
        CommunicationRequest request = buildCustomCommRequest("Test message to retrieve", "Test User", 0, 0);

        DynamicListItem selectedItem = new DynamicListItem(request.getId(), "Test Topic");
        DynamicList dynamicList = new DynamicList(selectedItem, Collections.singletonList(selectedItem));
        
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .ftaCommunications(Collections.singletonList(request))
            .tribunalRequestsDl(dynamicList)
            .build();
        
        sscsCaseData.setCommunicationFields(fields);
        
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        
        assertTrue(response.getErrors().isEmpty());
        assertEquals("Test message to retrieve", response.getData().getCommunicationFields().getTribunalRequestNoResponseQuery());
    }

    @Test
    void givenSelectTribunalRequestPage_thenThrowIfNoneSelected() {
        when(callback.getPageId()).thenReturn("selectTribunalRequest");

        DynamicList dynamicList = new DynamicList(null, Collections.emptyList());

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalCommunications(Collections.emptyList())
            .tribunalRequestsDl(dynamicList)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION));
        assertEquals("No chosen request found", exception.getMessage());
    }

    @Test
    void givenReplyToTribunalQueryPage_withEmptyResponseAndNoAction_thenShowError() {
        when(callback.getPageId()).thenReturn("replyToTribunalQuery");
        
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .commRequestResponseTextArea("")
            .build();
        
        sscsCaseData.setCommunicationFields(fields);
        
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        
        assertFalse(response.getErrors().isEmpty());
        assertEquals("Please provide a response to the Tribunal query or select No reply required.", 
                    response.getErrors().iterator().next());
    }

    @Test
    void givenReplyToTribunalQueryPage_withValidResponse_thenNoError() {
        when(callback.getPageId()).thenReturn("replyToTribunalQuery");
        
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
            .commRequestResponseTextArea("This is a response")
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
            .commRequestResponseTextArea("")
            .commRequestResponseNoAction(Collections.singletonList("No reply required"))
            .build();
        
        sscsCaseData.setCommunicationFields(fields);
        
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        
        assertTrue(response.getErrors().isEmpty());
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
}
