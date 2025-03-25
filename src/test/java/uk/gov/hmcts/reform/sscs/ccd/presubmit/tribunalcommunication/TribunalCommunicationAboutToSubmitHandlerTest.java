package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.TribunalCommunication;
import uk.gov.hmcts.reform.sscs.ccd.domain.TribunalCommunicationFields;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;

@RunWith(JUnitParamsRunner.class)
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

    @Before
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

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenValidTribunalRequest_shouldAddNewCommunicationToList() {
        // Setup Tribunal communication fields
        String expectedTopic = "Test Topic";
        String expectedQuestion = "Test Question";
        String expectedUserName = "Test User";

        // Create empty list of communications
        List<TribunalCommunicationFields> existingComs = new ArrayList<>();
        TribunalCommunication fields = TribunalCommunication.builder()
                .tribunalRequestTopic(expectedTopic)
                .tribunalRequestQuestion(expectedQuestion)
                .tribunalCommunicationFields(existingComs)
                .build();

        sscsCaseData.setTribunalCommunications(fields);

        // Mock user details
        UserDetails userDetails = UserDetails.builder()
                .name(expectedUserName)
                .build();
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

        // Execute the function
        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        // Verify a new Tribunal communication was added
        List<TribunalCommunicationFields> resultComs = response.getData().getTribunalCommunications().getTribunalCommunicationFields();

        assertThat(resultComs.size(), is(1));

        TribunalCommunicationFields addedCom = resultComs.get(0);
        assertThat(addedCom.getRequestTopic(), is(expectedTopic));
        assertThat(addedCom.getRequestMessage(), is(expectedQuestion));
        assertThat(addedCom.getRequestUserName(), is(expectedUserName));
        assertThat(addedCom.getRequestDateTime(), is(notNullValue()));
    }
    /*
    @Test
    @Parameters(method = "dueDateParameters")
    public void calculateDueDate_shouldAdjustForWeekends(LocalDateTime inputDate, LocalDateTime expectedDueDate) {
        LocalDateTime actualDueDate = handler.calculateDueDate(inputDate);

        assertThat(actualDueDate, is(expectedDueDate));
    }*/
}