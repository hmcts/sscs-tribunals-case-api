package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

        handler = new TribunalCommunicationAboutToSubmitHandler(idamService);

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
        List<TribunalCommunication> existingComs = new ArrayList<>();
        TribunalCommunicationDetails details = TribunalCommunicationDetails.builder()
                .tribunalRequestTopic(expectedTopic)
                .tribunalRequestQuestion(expectedQuestion)
                .tribunalCommunications(existingComs)
                .tribunalRequestType(TribunalRequestType.NEW_REQUEST)
                .build();

        sscsCaseData.setTribunalCommunicationsDetails(details);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
                .name(expectedUserName)
                .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        // Verify a new Tribunal communication was added
        List<TribunalCommunication> resultComs = response.getData().getTribunalCommunicationsDetails().getTribunalCommunications();

        assertEquals(2, resultComs.size());

        TribunalCommunicationFields addedCom = resultComs.get(0).getValue();
        assertEquals(expectedTopic, addedCom.getRequestTopic());
        assertEquals(expectedQuestion, addedCom.getRequestMessage());
        assertEquals(expectedUserName, addedCom.getRequestUserName());
        assertNotNull(addedCom.getRequestDateTime());

        // Verify the enum values are correctly set
        assertEquals(TribunalCommunicationFilter.INFO_REQUEST_FROM_FTA, response.getData().getTribunalCommunicationsDetails().getTribunalCommunicationFilter());
        assertEquals(FtaCommunicationFilter.AWAITING_INFO_FROM_TRIBUNAL, response.getData().getTribunalCommunicationsDetails().getFtaCommunicationFilter());
    }

    @Test
    public void givenValidTribunalRequest_shouldAddNewCommunicationToPopulatedList() {
        // Setup Tribunal communication fields
        String expectedTopic = "Test Topic";
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        // Create list of existing communications
        TribunalCommunication tribunalCommunicationPast = TribunalCommunication.builder().value(
                TribunalCommunicationFields.builder()
                        .requestTopic("Past existing Topic")
                        .requestMessage("Past existing Question")
                        .requestDateTime(LocalDateTime.now().minusYears(2))
                        .requestUserName("Past existing user")
                        .requestResponseDue(LocalDateTime.now().minusYears(1))
                        .build()
                ).build();
        TribunalCommunication tribunalCommunicationFuture = TribunalCommunication.builder().value(
                TribunalCommunicationFields.builder()
                        .requestTopic("Future existing Topic")
                        .requestMessage("Future existing Question")
                        .requestDateTime(LocalDateTime.now().plusYears(1))
                        .requestUserName("Future existing user")
                        .requestResponseDue(LocalDateTime.now().plusYears(2))
                        .build()
                ).build();
        List<TribunalCommunication> existingComs = new ArrayList<>(List.of(tribunalCommunicationFuture, tribunalCommunicationPast));
        TribunalCommunicationDetails details = TribunalCommunicationDetails.builder()
                .tribunalRequestTopic(expectedTopic)
                .tribunalRequestQuestion(expectedQuestion)
                .tribunalRequestType(TribunalRequestType.NEW_REQUEST)
                .tribunalCommunications(existingComs)
                .build();

        sscsCaseData.setTribunalCommunicationsDetails(details);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
                .name(expectedUserName)
                .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        // Verify a new Tribunal communication was added
        List<TribunalCommunication> resultComs = response.getData().getTribunalCommunicationsDetails().getTribunalCommunications();

        assertEquals(6, resultComs.size());

        assertEquals(tribunalCommunicationFuture, resultComs.getFirst());
        TribunalCommunicationFields addedCom = resultComs.get(2).getValue();
        assertEquals(expectedTopic, addedCom.getRequestTopic());
        assertEquals(expectedQuestion, addedCom.getRequestMessage());
        assertEquals(expectedUserName, addedCom.getRequestUserName());
        assertNotNull(addedCom.getRequestDateTime());
        assertEquals(tribunalCommunicationPast, resultComs.getLast());

        // Verify the enum values are correctly set
        assertEquals(TribunalCommunicationFilter.INFO_REQUEST_FROM_FTA, response.getData().getTribunalCommunicationsDetails().getTribunalCommunicationFilter());
        assertEquals(FtaCommunicationFilter.AWAITING_INFO_FROM_TRIBUNAL, response.getData().getTribunalCommunicationsDetails().getFtaCommunicationFilter());
    }

    @Test
    public void givenNullCommunicationsList_shouldHandleGracefully() {
        // Setup Tribunal communication fields with null communications list
        String expectedTopic = "Test Topic";
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        TribunalCommunicationDetails details = TribunalCommunicationDetails.builder()
                .tribunalRequestTopic(expectedTopic)
                .tribunalRequestQuestion(expectedQuestion)
                .tribunalRequestType(TribunalRequestType.NEW_REQUEST)
                .tribunalCommunications(null) // Explicitly set to null
                .build();

        sscsCaseData.setTribunalCommunicationsDetails(details);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
                .name(expectedUserName)
                .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        // Verify a new Tribunal communication was added
        List<TribunalCommunication> resultComs = response.getData().getTribunalCommunicationsDetails().getTribunalCommunications();

        assertNotNull(resultComs);

        // Verify the enum values are correctly set
        assertEquals(TribunalCommunicationFilter.INFO_REQUEST_FROM_FTA, response.getData().getTribunalCommunicationsDetails().getTribunalCommunicationFilter());
        assertEquals(FtaCommunicationFilter.AWAITING_INFO_FROM_TRIBUNAL, response.getData().getTribunalCommunicationsDetails().getFtaCommunicationFilter());
    }

    @ParameterizedTest
    @MethodSource(value = {"dueDateParameters"})
    public void calculateDueDate_shouldAdjustForWeekends(LocalDateTime inputDate, LocalDateTime expectedDueDate) {
        LocalDateTime actualDueDate = handler.dueDate(inputDate);

        assertEquals(expectedDueDate, actualDueDate);
    }

    @SuppressWarnings("unused")
    private static Object[] dueDateParameters() {
        return new Object[] {
            // Normal weekday -> Weekday (2 days later)
            new Object[] {
                    LocalDateTime.of(2023, 6, 5, 10, 0), // Monday
                    LocalDateTime.of(2023, 6, 7, 10, 0)  // Wednesday (2 days later)
            },
            // Normal weekday -> Weekday (2 days later)
            new Object[] {
                    LocalDateTime.of(2023, 6, 6, 10, 0), // Tuesday
                    LocalDateTime.of(2023, 6, 8, 10, 0)  // Thursday (2 days later)
            },
            // Normal weekday -> Weekday (2 days later)
            new Object[] {
                    LocalDateTime.of(2023, 6, 7, 10, 0), // Wednesday
                    LocalDateTime.of(2023, 6, 9, 10, 0)  // Friday (2 days later)
            },
            // Thursday -> Add 2 days = Saturday, should be moved to Monday (4 days later)
            new Object[] {
                    LocalDateTime.of(2023, 6, 8, 10, 0), // Thursday
                    LocalDateTime.of(2023, 6, 12, 10, 0) // Monday (4 days later)
            },
            // Friday -> Add 2 days = Sunday, should be moved to Monday (3 days later)
            new Object[] {
                    LocalDateTime.of(2023, 6, 9, 10, 0), // Friday
                    LocalDateTime.of(2023, 6, 13, 10, 0) // Tuesday (4 days later)
            },
            // Saturday -> Add 2 days = Monday (normal)
            new Object[] {
                    LocalDateTime.of(2023, 6, 10, 10, 0), // Saturday
                    LocalDateTime.of(2023, 6, 12, 10, 0)  // Monday (2 days later)
            },
            // Sunday -> Add 2 days = Tuesday (normal)
            new Object[] {
                    LocalDateTime.of(2023, 6, 11, 10, 0), // Sunday
                    LocalDateTime.of(2023, 6, 13, 10, 0)  // Tuesday (2 days later)
            }
        };
    }
}