package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

class TribunalCommunicationMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private TribunalCommunicationMidEventHandler handler;
    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

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
    void givenInvalidEvent_thenReturnFalse() {
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
    void givenFeatureToggleDisabled_thenReturnUnchangedResponse() {
        handler = new TribunalCommunicationMidEventHandler(false);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(sscsCaseData, response.getData());
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
            .tribunalCommunications(requests)
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
            .tribunalCommunications(requests)
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
