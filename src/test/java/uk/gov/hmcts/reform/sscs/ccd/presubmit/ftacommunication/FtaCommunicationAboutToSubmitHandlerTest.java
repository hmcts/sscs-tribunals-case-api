package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

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
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunication;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationDetails;
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
        ReflectionTestUtils.setField(handler, "isFtaCommuncationEnabled", true);

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
        String expectedTopic = "Test Topic";
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";
    
        // Create empty list of communications
        List<FtaCommunication> existingComs = new ArrayList<>();
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaRequestTopic(expectedTopic)
            .ftaRequestQuestion(expectedQuestion)
            .ftaCommunications(existingComs)
            .ftaRequestType(FtaRequestType.NEW_REQUEST)
            .build();
        
        sscsCaseData.setFtaCommunicationFields(fields);
    
        // Mock user details
        UserDetails userDetails = UserDetails.builder()
            .name(expectedUserName)
            .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    
        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response = 
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    
        // Verify a new FTA communication was added
        List<FtaCommunication> resultComs = response.getData().getFtaCommunicationFields().getFtaCommunications();
    
        assertEquals(1, resultComs.size());

        FtaCommunicationDetails addedCom = resultComs.getFirst().getValue();
        assertEquals(expectedTopic, addedCom.getRequestTopic());
        assertEquals(expectedQuestion, addedCom.getRequestText());
        assertEquals(expectedUserName, addedCom.getRequestUserName());
        assertNotNull(addedCom.getRequestDateTime());
        
        // Verify the enum values are correctly set
        assertEquals(FtaCommunicationFilter.AWAITING_INFO_FROM_FTA, response.getData().getFtaCommunicationFields().getFtaCommunicationFilter());
        assertEquals(TribunalCommunicationFilter.NEW_FTA_FILTER, response.getData().getFtaCommunicationFields().getTribunalCommunicationFilter());
    }

    @Test
    public void givenValidFtaRequest_shouldAddNewCommunicationToPopulatedList() {
        // Setup FTA communication fields
        String expectedTopic = "Test Topic";
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        // Create list of existing communications
        FtaCommunication ftaCommunicationPast = FtaCommunication.builder().value(
            FtaCommunicationDetails.builder()
                .requestTopic("Past existing Topic")
                .requestText("Past existing Question")
                .requestDateTime(LocalDateTime.now().minusYears(2))
                .requestUserName("Past existing user")
                .requestDueDate(LocalDateTime.now().minusYears(1))
                .build()
        ).build();
        FtaCommunication ftaCommunicationFuture = FtaCommunication.builder().value(
            FtaCommunicationDetails.builder()
                .requestTopic("Future existing Topic")
                .requestText("Future existing Question")
                .requestDateTime(LocalDateTime.now().plusYears(1))
                .requestUserName("Future existing user")
                .requestDueDate(LocalDateTime.now().plusYears(2))
                .build()
        ).build();
        List<FtaCommunication> existingComs = new ArrayList<>(List.of(ftaCommunicationFuture, ftaCommunicationPast));
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaRequestTopic(expectedTopic)
            .ftaRequestQuestion(expectedQuestion)
            .ftaCommunications(existingComs)
            .ftaRequestType(FtaRequestType.NEW_REQUEST)
            .build();

        sscsCaseData.setFtaCommunicationFields(fields);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
            .name(expectedUserName)
            .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        // Verify a new FTA communication was added
        List<FtaCommunication> resultComs = response.getData().getFtaCommunicationFields().getFtaCommunications();

        assertEquals(3, resultComs.size());

        assertEquals(ftaCommunicationFuture, resultComs.getFirst());
        FtaCommunicationDetails addedCom = resultComs.get(1).getValue();
        assertEquals(expectedTopic, addedCom.getRequestTopic());
        assertEquals(expectedQuestion, addedCom.getRequestText());
        assertEquals(expectedUserName, addedCom.getRequestUserName());
        assertNotNull(addedCom.getRequestDateTime());
        assertEquals(ftaCommunicationPast, resultComs.getLast());
        
        // Verify the enum values are correctly set
        assertEquals(FtaCommunicationFilter.AWAITING_INFO_FROM_FTA, response.getData().getFtaCommunicationFields().getFtaCommunicationFilter());
        assertEquals(TribunalCommunicationFilter.NEW_FTA_FILTER, response.getData().getFtaCommunicationFields().getTribunalCommunicationFilter());
    }
    
    @Test
    public void givenNullCommunicationsList_shouldHandleGracefully() {
        // Setup FTA communication fields with null communications list
        String expectedTopic = "Test Topic";
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";
    
        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .ftaRequestTopic(expectedTopic)
            .ftaRequestQuestion(expectedQuestion)
            .ftaCommunications(null) // Explicitly set to null
            .ftaRequestType(FtaRequestType.NEW_REQUEST)
            .build();
        
        sscsCaseData.setFtaCommunicationFields(fields);
    
        // Mock user details
        UserDetails userDetails = UserDetails.builder()
            .name(expectedUserName)
            .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    
        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response = 
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    
        // Verify a new FTA communication was added
        List<FtaCommunication> resultComs = response.getData().getFtaCommunicationFields().getFtaCommunications();
    
        assertNotNull(resultComs);
        
        // Verify the enum values are correctly set
        assertEquals(FtaCommunicationFilter.AWAITING_INFO_FROM_FTA, response.getData().getFtaCommunicationFields().getFtaCommunicationFilter());
        assertEquals(TribunalCommunicationFilter.NEW_FTA_FILTER, response.getData().getFtaCommunicationFields().getTribunalCommunicationFilter());
    }

    @ParameterizedTest
    @MethodSource(value = {"dueDateParameters"})
    public void calculateDueDate_shouldAdjustForWeekends(LocalDateTime inputDate, LocalDateTime expectedDueDate) {
       
        LocalDateTime actualDueDate = handler.calculateDueDate(inputDate);
        
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
                LocalDateTime.of(2023, 6, 12, 10, 0) // Monday (3 days later)
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