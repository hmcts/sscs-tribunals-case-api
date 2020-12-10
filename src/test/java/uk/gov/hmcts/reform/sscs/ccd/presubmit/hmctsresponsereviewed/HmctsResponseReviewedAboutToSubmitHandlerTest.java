package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class HmctsResponseReviewedAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private HmctsResponseReviewedAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new HmctsResponseReviewedAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.HMCTS_RESPONSE_REVIEWED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .selectWhoReviewsCase(new DynamicList(new DynamicListItem("reviewByTcw", "Review by TCW"), null))
                .benefitCode("002")
                .issueCode("CC")
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonHmctsResponseReviewedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAHmctsResponseReviewedEventWithNoDwpResponseDate_thenSetCaseCodeAndDefaultDwpResponseDateToToday() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002CC", response.getData().getCaseCode());
        assertEquals(LocalDate.now().toString(), response.getData().getDwpResponseDate());
    }

    @Test
    public void givenAHmctsResponseReviewedEventWithDwpResponseDate_thenSetCaseCodeAndUseProvidedDwpResponseDate() {
        callback.getCaseDetails().getCaseData().setDwpResponseDate(LocalDate.now().minusDays(1).toString());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002CC", response.getData().getCaseCode());
        assertEquals(LocalDate.now().minusDays(1).toString(), response.getData().getDwpResponseDate());
    }

    @Test
    public void givenAHmctsResponseReviewedWithEmptyBenefitCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setBenefitCode(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Benefit code cannot be empty", error);
        }
    }

    @Test
    public void givenAHmctsResponseReviewedWithEmptyIssueCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Issue code cannot be empty", error);
        }
    }

    @Test
    public void givenAHmctsResponseReviewedWithIssueCodeSetToDD_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode("DD");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Issue code cannot be set to the default value of DD", error);
        }
    }

    @Test
    public void givenAUcCaseWithSingleElementSelected_thenSetCaseCodeToUs() {
        List<String> elementList = new ArrayList<>();
        elementList.add("testElement");
        sscsCaseData.setElementsDisputedList(elementList);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("uc").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("US", response.getData().getIssueCode());
        assertEquals("001", response.getData().getBenefitCode());
        assertEquals("001US", response.getData().getCaseCode());
    }

    @Test
    public void givenAUcCaseWithMultipleElementSelected_thenSetCaseCodeToUm() {
        List<String> elementList = new ArrayList<>();
        elementList.add("testElement");
        elementList.add("testElement2");
        sscsCaseData.setElementsDisputedList(elementList);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("uc").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("UM", response.getData().getIssueCode());
        assertEquals("001", response.getData().getBenefitCode());
        assertEquals("001UM", response.getData().getCaseCode());
    }

    @Test
    public void givenUcbSelectedAndNoUcbDocument_displayAnError() {
        sscsCaseData.setDwpUcb(YES.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Please upload a UCB document"));
    }

    @Test
    public void givenUcbSelectedIsNo_thenTheFieldsAreCleared() {
        sscsCaseData.setDwpUcb(NO.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(DocumentLink.builder().build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(sscsCaseData.getDwpUcb(), is(nullValue()));
        assertThat(sscsCaseData.getDwpUcbEvidenceDocument(), is(nullValue()));
        assertThat(sscsCaseData.getDwpDocuments().size(), is(0));
    }

    @Test
    public void givenUcbSelectedAndUploadedUcbDoc_thenNoErrors() {
        sscsCaseData.setDwpUcb(YES.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(DocumentLink.builder().build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(sscsCaseData.getDwpUcb(), is(YES.getValue()));
        assertThat(sscsCaseData.getDwpUcbEvidenceDocument(), is(nullValue()));
        assertThat(sscsCaseData.getDwpDocuments().size(), is(1));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

}
