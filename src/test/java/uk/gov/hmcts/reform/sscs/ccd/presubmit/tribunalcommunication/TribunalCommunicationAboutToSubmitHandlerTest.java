package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;

public class TribunalCommunicationAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    TribunalCommunicationAboutToSubmitHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamService idamService;

    @BeforeEach
    public void setUp() {
        openMocks(this);

        handler = new TribunalCommunicationAboutToSubmitHandler(idamService, true);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(callback.getEvent()).thenReturn(EventType.TRIBUNAL_COMMUNICATION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    public void givenValidTribunalRequest_shouldAddNewCommunicationToList() {
        // Setup Tribunal communication fields
        String expectedTopic = "Test Topic";
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        // Create empty list of communications
        List<CommunicationRequest> existingComs = new ArrayList<>();
        FtaCommunicationFields details = FtaCommunicationFields.builder()
                .tribunalRequestTopic(expectedTopic)
                .tribunalRequestQuestion(expectedQuestion)
                .tribunalCommunications(existingComs)
                .tribunalRequestType(TribunalRequestType.NEW_REQUEST)
                .build();

        sscsCaseData.setCommunicationFields(details);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
                .name(expectedUserName)
                .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

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

        // Verify the enum values are correctly set
        assertEquals(TribunalCommunicationFilter.AWAITING_INFO_FROM_TRIBUNAL, response.getData().getCommunicationFields().getTribunalCommunicationFilter());
        assertEquals(FtaCommunicationFilter.INFO_REQUEST_FROM_FTA, response.getData().getCommunicationFields().getFtaCommunicationFilter());
    }

    @Test
    public void givenFlagOff_shouldDoNothing() {
        List<CommunicationRequest> existingComs = new ArrayList<>();
        FtaCommunicationFields details = FtaCommunicationFields.builder()
            .tribunalRequestTopic("someTopic")
            .tribunalRequestQuestion("someQuestion")
            .tribunalCommunications(existingComs)
            .tribunalRequestType(TribunalRequestType.NEW_REQUEST)
            .build();

        sscsCaseData.setCommunicationFields(details);
        handler = new TribunalCommunicationAboutToSubmitHandler(idamService, false);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getTribunalCommunications();
        assertEquals(0, resultComs.size());
        assertNull(response.getData().getCommunicationFields().getTribunalCommunicationFilter());
        assertNull(response.getData().getCommunicationFields().getFtaCommunicationFilter());
    }

    @Test
    public void givenValidTribunalRequest_shouldAddNewCommunicationToPopulatedList() {
        // Setup Tribunal communication fields
        String expectedTopic = "Test Topic";
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        // Create list of existing communications
        CommunicationRequest tribunalCommunicationPast = CommunicationRequest.builder().value(
                CommunicationRequestDetails.builder()
                        .requestTopic("Past existing Topic")
                        .requestMessage("Past existing Question")
                        .requestDateTime(LocalDateTime.now().minusYears(2))
                        .requestUserName("Past existing user")
                        .requestResponseDueDate(LocalDateTime.now().minusYears(1))
                        .build()
                ).build();
        CommunicationRequest tribunalCommunicationFuture = CommunicationRequest.builder().value(
                CommunicationRequestDetails.builder()
                        .requestTopic("Future existing Topic")
                        .requestMessage("Future existing Question")
                        .requestDateTime(LocalDateTime.now().plusYears(1))
                        .requestUserName("Future existing user")
                        .requestResponseDueDate(LocalDateTime.now().plusYears(2))
                        .build()
                ).build();
        List<CommunicationRequest> existingComs = new ArrayList<>(List.of(tribunalCommunicationFuture, tribunalCommunicationPast));
        FtaCommunicationFields details = FtaCommunicationFields.builder()
                .tribunalRequestTopic(expectedTopic)
                .tribunalRequestQuestion(expectedQuestion)
                .tribunalRequestType(TribunalRequestType.NEW_REQUEST)
                .tribunalCommunications(existingComs)
                .build();

        sscsCaseData.setCommunicationFields(details);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
                .name(expectedUserName)
                .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

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
        assertNotNull(addedCom.getRequestDateTime());
        assertEquals(tribunalCommunicationPast, resultComs.getLast());

        // Verify the enum values are correctly set
        assertEquals(TribunalCommunicationFilter.AWAITING_INFO_FROM_TRIBUNAL, response.getData().getCommunicationFields().getTribunalCommunicationFilter());
        assertEquals(FtaCommunicationFilter.INFO_REQUEST_FROM_FTA, response.getData().getCommunicationFields().getFtaCommunicationFilter());
    }

    @Test
    public void givenNullCommunicationsList_shouldHandleGracefully() {
        // Setup Tribunal communication fields with null communications list
        String expectedTopic = "Test Topic";
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        FtaCommunicationFields details = FtaCommunicationFields.builder()
                .tribunalRequestTopic(expectedTopic)
                .tribunalRequestQuestion(expectedQuestion)
                .tribunalRequestType(TribunalRequestType.NEW_REQUEST)
                .tribunalCommunications(null) // Explicitly set to null
                .build();

        sscsCaseData.setCommunicationFields(details);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
                .name(expectedUserName)
                .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        // Verify a new Tribunal communication was added
        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getTribunalCommunications();

        assertNotNull(resultComs);

        // Verify the enum values are correctly set
        assertEquals(TribunalCommunicationFilter.AWAITING_INFO_FROM_TRIBUNAL, response.getData().getCommunicationFields().getTribunalCommunicationFilter());
        assertEquals(FtaCommunicationFilter.INFO_REQUEST_FROM_FTA, response.getData().getCommunicationFields().getFtaCommunicationFilter());
    }
}