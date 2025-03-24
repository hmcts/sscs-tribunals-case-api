package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunication;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;

@RunWith(JUnitParamsRunner.class)
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


    @Before
    public void setUp() {
        openMocks(this);

        handler = new FtaCommunicationAboutToSubmitHandler(idamService);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(callback.getEvent()).thenReturn(EventType.FTA_COMMUNICATION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
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
    
        assertThat(resultComs.size(), is(1));
    
        FtaCommunication addedCom = resultComs.get(0);
        assertThat(addedCom.getRequestTopic(), is(expectedTopic));
        assertThat(addedCom.getRequestText(), is(expectedQuestion));
        assertThat(addedCom.getRequestUserName(), is(expectedUserName));
        assertThat(addedCom.getRequestDateTime(), is(notNullValue()));
    
    }

    @Test
    @Parameters(method = "dueDateParameters")
    public void calculateDueDate_shouldAdjustForWeekends(LocalDateTime inputDate, LocalDateTime expectedDueDate) {
        LocalDateTime actualDueDate = handler.calculateDueDate(inputDate);
        
        assertThat(actualDueDate, is(expectedDueDate));
    }

    @SuppressWarnings("unused")
    private Object[] dueDateParameters() {
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
