package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFilter;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.TribunalCommunicationFilter;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;

public class FtaCommunicationAboutToSubmitHandlerTest {

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
    public void setUp() {
        openMocks(this);

        handler = new FtaCommunicationAboutToSubmitHandler(idamService, true);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(callback.getEvent()).thenReturn(EventType.FTA_COMMUNICATION);
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
    public void givenValidFtaRequest_shouldAddNewCommunicationToList() {
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
    public void givenFlagOff_shouldDoNothing() {
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
    public void givenValidFtaRequest_shouldAddNewCommunicationToPopulatedList() {
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
    public void givenNullCommunicationsList_shouldHandleGracefully() {
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

    @ParameterizedTest
    @MethodSource(value = {"dueDateParameters"})
    public void calculateDueDate_shouldAdjustForWeekends(LocalDate inputDate, LocalDate expectedDueDate) {

        LocalDate actualDueDate = FtaCommunicationAboutToSubmitHandler.calculateDueDate(inputDate);

        assertEquals(expectedDueDate, actualDueDate);
    }

    @SuppressWarnings("unused")
    private static Object[] dueDateParameters() {
        return new Object[] {
            // Normal weekday -> Weekday (2 days later)
            new Object[] {
                LocalDate.of(2023, 6, 5), // Monday
                LocalDate.of(2023, 6, 7)  // Wednesday (2 days later)
            },
            // Normal weekday -> Weekday (2 days later)
            new Object[] {
                LocalDate.of(2023, 6, 6), // Tuesday
                LocalDate.of(2023, 6, 8)  // Thursday (2 days later)
            },
            // Normal weekday -> Weekday (2 days later)
            new Object[] {
                LocalDate.of(2023, 6, 7), // Wednesday
                LocalDate.of(2023, 6, 9)  // Friday (2 days later)
            },
            // Thursday -> Add 2 days = Saturday, should be moved to Monday (4 days later)
            new Object[] {
                LocalDate.of(2023, 6, 8), // Thursday
                LocalDate.of(2023, 6, 12) // Monday (4 days later)
            },
            // Friday -> Add 2 days = Sunday, should be moved to Monday (3 days later)
            new Object[] {
                LocalDate.of(2023, 6, 9), // Friday
                LocalDate.of(2023, 6, 12) // Monday (3 days later)
            },
            // Saturday -> Add 2 days = Monday (normal)
            new Object[] {
                LocalDate.of(2023, 6, 10), // Saturday
                LocalDate.of(2023, 6, 12)  // Monday (2 days later)
            },
            // Sunday -> Add 2 days = Tuesday (normal)
            new Object[] {
                LocalDate.of(2023, 6, 11), // Sunday
                LocalDate.of(2023, 6, 13)  // Tuesday (2 days later)
            }
        };
    }
}