package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityrequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import junitparams.Parameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DatedRequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class ReviewConfidentialityRequestAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private ReviewConfidentialityRequestAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    public void setUp()  {
        openMocks(this);
        handler = new ReviewConfidentialityRequestAboutToStartHandler();

        when(callback.getEvent()).thenReturn(EventType.REVIEW_CONFIDENTIALITY_REQUEST);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .dwpState(DwpState.FE_ACTIONED_NR)
            .interlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE)
            .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @ParameterizedTest
    public void givenANonReviewConfidentialityRequestEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }


    @SuppressWarnings("unused")
    private static Object[] noConfidentialityRequestsAreInProgress() {
        return new Object[]{
            new RequestOutcome[]{null, null},
            new RequestOutcome[]{null, RequestOutcome.GRANTED},
            new RequestOutcome[]{null, RequestOutcome.REFUSED},
            new RequestOutcome[]{RequestOutcome.GRANTED, null},
            new RequestOutcome[]{RequestOutcome.GRANTED, RequestOutcome.GRANTED},
            new RequestOutcome[]{RequestOutcome.GRANTED, RequestOutcome.REFUSED},
            new RequestOutcome[]{RequestOutcome.REFUSED, null},
            new RequestOutcome[]{RequestOutcome.REFUSED, RequestOutcome.GRANTED},
            new RequestOutcome[]{RequestOutcome.REFUSED, RequestOutcome.REFUSED},
        };
    }


    @SuppressWarnings("unused")
    private static Object[] noRequestIsInProgress() {
        return new Object[]{
            new RequestOutcome[]{null},
            new RequestOutcome[]{RequestOutcome.GRANTED},
            new RequestOutcome[]{RequestOutcome.REFUSED},
        };
    }


    @SuppressWarnings("unused")
    private static Object[] populatedRequestReviewValues() {
        return new Object[]{
            new String[]{"something"},
            new String[]{"grantConfidentialityRequest"},
            new String[]{"refuseConfidentialityRequest"}
        };
    }


    @SuppressWarnings("unused")
    private static Object[] noRequestIsInProgressAndPopulatedRequestReviewValues() {
        return new Object[]{
            new Object[]{null, "something"},
            new Object[]{null, "grantConfidentialityRequest"},
            new Object[]{null, "refuseConfidentialityRequest"},
            new Object[]{RequestOutcome.GRANTED, "something"},
            new Object[]{RequestOutcome.GRANTED, "grantConfidentialityRequest"},
            new Object[]{RequestOutcome.GRANTED, "refuseConfidentialityRequest"},
            new Object[]{RequestOutcome.REFUSED, "something"},
            new Object[]{RequestOutcome.REFUSED, "grantConfidentialityRequest"},
            new Object[]{RequestOutcome.REFUSED, "refuseConfidentialityRequest"}
        };
    }

    private DatedRequestOutcome createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome outcome) {
        return outcome == null ? null :
            DatedRequestOutcome.builder().date(LocalDate.now().minusDays(1)).requestOutcome(outcome).build();
    }

    @MethodSource("noConfidentialityRequestsAreInProgress")
    @ParameterizedTest
    public void givenNoConfidentialityRequestsAreInProgressShouldDisplayAnErrorWhenJointPartyWhenJointPartyExists(RequestOutcome appellantOutcome, RequestOutcome jointPartyOutcome) {

        sscsCaseData.getJointParty().setHasJointParty(YES);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assertions.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no confidentiality request to review", error);

        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
    }

    @MethodSource("noRequestIsInProgress")
    @ParameterizedTest
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorWhenJointPartyExists(RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.getJointParty().setHasJointParty(YES);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assertions.assertEquals(0, response.getErrors().size());

        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }


    @MethodSource("noRequestIsInProgress")
    @ParameterizedTest
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorWhenJointPartyExists(RequestOutcome appellantRequestOutcome) {

        sscsCaseData.getJointParty().setHasJointParty(YES);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assertions.assertEquals(0, response.getErrors().size());

        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @ParameterizedTest
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorWhenJointPartyExists() {

        sscsCaseData.getJointParty().setHasJointParty(YES);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assertions.assertEquals(0, response.getErrors().size());

        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }


    @MethodSource("noConfidentialityRequestsAreInProgress")
    @ParameterizedTest
    public void givenNoConfidentialityRequestsAreInProgressShouldDisplayAnErrorWhenJointPartyWhenJointPartyDoesNotExist(RequestOutcome appellantOutcome, RequestOutcome jointPartyOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assertions.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no confidentiality request to review", error);

        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
    }

    @MethodSource("noRequestIsInProgress")
    @ParameterizedTest
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorWhenJointPartyDoesNotExist(RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assertions.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no confidentiality request to review", error);

        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }


    @MethodSource("noRequestIsInProgress")
    @ParameterizedTest
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorWhenJointPartyDoesNotExists(RequestOutcome appellantRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assertions.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no confidentiality request to review", error);

        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @ParameterizedTest
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorWhenJointPartyDoesNotExist() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assertions.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no confidentiality request to review", error);

        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assertions.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
    }


    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @ParameterizedTest
    public void throwsExceptionIfItCannotHandleTheRequest() {
        assertThrows(IllegalStateException.class, () -> {
            when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        });
    }
}
