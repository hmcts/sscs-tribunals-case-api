package uk.gov.hmcts.reform.sscs.ccd.presubmit.withdrawnappeals;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.WITHDRAWN;

import java.io.IOException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class DwpActionWithdrawalHandlerTest extends AdminAppealWithdrawnBase {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String DWP_ACTION_WITHDRAWAL_CALLBACK_JSON = "dwpActionWithdrawalCallback.json";
    private final DwpActionWithdrawalHandler handler = new DwpActionWithdrawalHandler();

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,DWP_ACTION_WITHDRAWAL,WITHDRAWAL_RECEIVED,true",
        "ABOUT_TO_SUBMIT,DWP_ACTION_WITHDRAWAL,REGISTERED,false",
        "ABOUT_TO_SUBMIT,DWP_ACTION_WITHDRAWAL,null,false",
        "ABOUT_TO_START,DWP_ACTION_WITHDRAWAL,WITHDRAWAL_RECEIVED,false",
        "SUBMITTED,DWP_ACTION_WITHDRAWAL,WITHDRAWAL_RECEIVED,false",
        "MID_EVENT,ADMIN_APPEAL_WITHDRAWN,WITHDRAWAL_RECEIVED,false",
        "ABOUT_TO_SUBMIT,ISSUE_FURTHER_EVIDENCE,WITHDRAWAL_RECEIVED,false",
        "null,DWP_ACTION_WITHDRAWAL,WITHDRAWAL_RECEIVED,false",
        "ABOUT_TO_SUBMIT,null,WITHDRAWAL_RECEIVED,false",
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType,
                          @Nullable DwpState dwpStateValue, boolean expectedResult) throws IOException {
        boolean actualResult = handler.canHandle(callbackType, buildTestCallbackGivenEventAndDwpState(eventType,
            dwpStateValue));
        assertEquals(expectedResult, actualResult);
    }

    private Callback<SscsCaseData> buildTestCallbackGivenEventAndDwpState(EventType eventType, DwpState dwpStateValue)
        throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenEvent(eventType,
            "dwpActionWithdrawalCallback.json");
        if (callback != null) {
            callback.getCaseDetails().getCaseData().setDwpState(dwpStateValue);
        }
        return callback;
    }

    @Test
    public void handle() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualResult = handler.handle(CallbackType.ABOUT_TO_SUBMIT,
            buildTestCallbackGivenEvent(EventType.DWP_ACTION_WITHDRAWAL, DWP_ACTION_WITHDRAWAL_CALLBACK_JSON), USER_AUTHORISATION);

        String expectedCaseData = fetchData("callback/withdrawnappeals/dwpActionWithdrawalExpectedCaseData.json");
        assertEquals(WITHDRAWN, actualResult.getData().getDwpState());
        assertThatJson(actualResult.getData())
            .whenIgnoringPaths(
                "jointPartyId",
                "appeal.appellant.appointee.id",
                "appeal.appellant.id",
                "appeal.rep.id",
                "appeal.hearingOptions",
                "correction",
                "correctionBodyContent",
                "bodyContent",
                "correctionGenerateNotice",
                "generateNotice",
                "dateAdded",
                "directionNoticeContent",
                "libertyToApply",
                "libertyToApplyBodyContent",
                "libertyToApplyGenerateNotice",
                "permissionToAppeal",
                "postHearingRequestType",
                "postHearingReviewType",
                "previewDocument",
                "setAside",
                "signedBy",
                "signedRole",
                "statementOfReasons",
                "statementOfReasonsBodyContent",
                "statementOfReasonsGenerateNotice")
            .isEqualTo(expectedCaseData);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({
        "ABOUT_TO_START,DWP_ACTION_WITHDRAWAL",
        "ABOUT_TO_SUBMIT,null",
        "null,DWP_ACTION_WITHDRAWAL"
    })
    public void handleCornerCaseScenarios(@Nullable CallbackType callbackType, @Nullable EventType eventType)
        throws IOException {
        handler.handle(callbackType, buildTestCallbackGivenEvent(eventType, DWP_ACTION_WITHDRAWAL_CALLBACK_JSON), USER_AUTHORISATION);
    }
}
