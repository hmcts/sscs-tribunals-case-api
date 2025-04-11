package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;

class TribunalCommunicationAboutToSubmitHandlerTest {

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
    void setUp() {
        openMocks(this);

        handler = new TribunalCommunicationAboutToSubmitHandler(idamService, true);

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
    void givenValidTribunalRequest_shouldAddNewCommunicationToList() {
        // Setup Tribunal communication fields
        CommunicationRequestTopic expectedTopic = CommunicationRequestTopic.APPEAL_TYPE;
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
        assertNotNull(addedCom.getRequestResponseDueDate());
        LocalDate date = calculateDueDateWorkingDays(LocalDate.now(), 2);
        assertEquals(date, addedCom.getRequestResponseDueDate());
        assertEquals(date, response.getData().getCommunicationFields().getTribunalResponseDueDate());
        // Verify the enum values are correctly set
        assertEquals(YesNo.YES, response.getData().getCommunicationFields().getInfoRequestFromFta());
        assertNull(response.getData().getCommunicationFields().getAwaitingInfoFromFta());
        assertNull(response.getData().getCommunicationFields().getInfoProvidedByFta());
        assertNull(response.getData().getCommunicationFields().getProvideInfoToTribunal());
        assertEquals(YesNo.YES, response.getData().getCommunicationFields().getAwaitingInfoFromTribunal());
        assertNull(response.getData().getCommunicationFields().getInfoProvidedFromTribunal());
    }

    @Test
    void givenFlagOff_shouldDoNothing() {
        List<CommunicationRequest> existingComs = new ArrayList<>();
        FtaCommunicationFields details = FtaCommunicationFields.builder()
            .tribunalRequestTopic(CommunicationRequestTopic.APPEAL_TYPE)
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
        assertNull(response.getData().getCommunicationFields().getInfoRequestFromFta());
        assertNull(response.getData().getCommunicationFields().getAwaitingInfoFromFta());
        assertNull(response.getData().getCommunicationFields().getInfoProvidedByFta());
        assertNull(response.getData().getCommunicationFields().getProvideInfoToTribunal());
        assertNull(response.getData().getCommunicationFields().getAwaitingInfoFromTribunal());
        assertNull(response.getData().getCommunicationFields().getInfoProvidedFromTribunal());
    }

    @Test
    void givenNoRequestType_shouldDoNothing() {
        List<CommunicationRequest> existingComs = new ArrayList<>();
        FtaCommunicationFields details = FtaCommunicationFields.builder()
            .tribunalRequestTopic(CommunicationRequestTopic.APPEAL_TYPE)
            .tribunalRequestQuestion("someQuestion")
            .tribunalCommunications(existingComs)
            .build();

        sscsCaseData.setCommunicationFields(details);
        handler = new TribunalCommunicationAboutToSubmitHandler(idamService, false);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        List<CommunicationRequest> resultComs = response.getData().getCommunicationFields().getTribunalCommunications();
        assertEquals(0, resultComs.size());
        assertNull(response.getData().getCommunicationFields().getInfoRequestFromFta());
        assertNull(response.getData().getCommunicationFields().getAwaitingInfoFromFta());
        assertNull(response.getData().getCommunicationFields().getInfoProvidedByFta());
        assertNull(response.getData().getCommunicationFields().getProvideInfoToTribunal());
        assertNull(response.getData().getCommunicationFields().getAwaitingInfoFromTribunal());
        assertNull(response.getData().getCommunicationFields().getInfoProvidedFromTribunal());
    }

    @Test
    void givenValidTribunalRequest_shouldAddNewCommunicationToPopulatedList() {
        // Setup Tribunal communication fields
        CommunicationRequestTopic expectedTopic = CommunicationRequestTopic.APPEAL_TYPE;
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        // Create list of existing communications
        CommunicationRequest tribunalCommunicationPast = CommunicationRequest.builder().value(
                CommunicationRequestDetails.builder()
                        .requestTopic(CommunicationRequestTopic.OTHER_PARTY_PERSONAL_INFORMATION)
                        .requestMessage("Past existing Question")
                        .requestDateTime(LocalDateTime.now().minusYears(2))
                        .requestUserName("Past existing user")
                        .requestResponseDueDate(LocalDate.now().minusYears(1))
                        .build()
                ).build();
        CommunicationRequest tribunalCommunicationFuture = CommunicationRequest.builder().value(
                CommunicationRequestDetails.builder()
                        .requestTopic(CommunicationRequestTopic.APPOINTEE_PERSONAL_INFORMATION)
                        .requestMessage("Future existing Question")
                        .requestDateTime(LocalDateTime.now().plusYears(1))
                        .requestUserName("Future existing user")
                        .requestResponseDueDate(LocalDate.now().plusYears(2))
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
        assertEquals(YesNo.YES, response.getData().getCommunicationFields().getInfoRequestFromFta());
        assertNull(response.getData().getCommunicationFields().getAwaitingInfoFromFta());
        assertNull(response.getData().getCommunicationFields().getInfoProvidedByFta());
        assertNull(response.getData().getCommunicationFields().getProvideInfoToTribunal());
        assertEquals(YesNo.YES, response.getData().getCommunicationFields().getAwaitingInfoFromTribunal());
        assertNull(response.getData().getCommunicationFields().getInfoProvidedFromTribunal());
    }

    @Test
    void givenNullCommunicationsList_shouldHandleGracefully() {
        // Setup Tribunal communication fields with null communications list
        CommunicationRequestTopic expectedTopic = CommunicationRequestTopic.APPEAL_TYPE;
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
        assertEquals(YesNo.YES, response.getData().getCommunicationFields().getInfoRequestFromFta());
        assertNull(response.getData().getCommunicationFields().getAwaitingInfoFromFta());
        assertNull(response.getData().getCommunicationFields().getInfoProvidedByFta());
        assertNull(response.getData().getCommunicationFields().getProvideInfoToTribunal());
        assertEquals(YesNo.YES, response.getData().getCommunicationFields().getAwaitingInfoFromTribunal());
        assertNull(response.getData().getCommunicationFields().getInfoProvidedFromTribunal());
    }
}