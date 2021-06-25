package uk.gov.hmcts.reform.sscs.ccd.presubmit.withdrawnappeals;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class AdminAppealWithdrawnHandlerTest extends AdminAppealWithdrawnBase {
    private static final String USER_AUTHORISATION = "Bearer token";
    public static final String ADMIN_APPEAL_WITHDRAWN_CALLBACK_JSON = "adminAppealWithdrawnCallback.json";
    private final AdminAppealWithdrawnHandler handler = new AdminAppealWithdrawnHandler();

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,ADMIN_APPEAL_WITHDRAWN,true",
        "ABOUT_TO_START,ADMIN_APPEAL_WITHDRAWN,false",
        "SUBMITTED,ADMIN_APPEAL_WITHDRAWN,false",
        "MID_EVENT,ADMIN_APPEAL_WITHDRAWN,false",
        "ABOUT_TO_SUBMIT,ISSUE_FURTHER_EVIDENCE,false",
        "null,ADMIN_APPEAL_WITHDRAWN,false",
        "ABOUT_TO_SUBMIT,null,false",
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType, boolean expectedResult)
        throws IOException {
        boolean actualResult = handler.canHandle(callbackType, buildTestCallbackGivenEvent(eventType,
            "adminAppealWithdrawnCallback.json"));
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void handle() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualResult = handler.handle(
            CallbackType.ABOUT_TO_SUBMIT, buildTestCallbackGivenEvent(EventType.ADMIN_APPEAL_WITHDRAWN,
                ADMIN_APPEAL_WITHDRAWN_CALLBACK_JSON), USER_AUTHORISATION);

        String expectedCaseData = fetchData("callback/withdrawnappeals/adminAppealWithdrawnExpectedCaseData.json");
        assertEquals("withdrawalReceived", actualResult.getData().getDwpState());
        assertThatJson(actualResult.getData()).isEqualTo(expectedCaseData);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({
        "ABOUT_TO_START,ADMIN_APPEAL_WITHDRAWN",
        "ABOUT_TO_SUBMIT,null",
        "null,ADMIN_APPEAL_WITHDRAWN"
    })
    public void handleCornerCaseScenarios(@Nullable CallbackType callbackType, @Nullable EventType eventType)
        throws IOException {
        handler.handle(callbackType, buildTestCallbackGivenEvent(eventType, ADMIN_APPEAL_WITHDRAWN_CALLBACK_JSON), USER_AUTHORISATION);
    }

    @Test
    public void movesWithdrawalDocumentToSscsDocumentsCollection() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualResult = handler.handle(
                CallbackType.ABOUT_TO_SUBMIT, buildTestCallbackGivenEvent(EventType.ADMIN_APPEAL_WITHDRAWN,
                        "adminAppealWithdrawnCallbackWithdrawalDocument.json"), USER_AUTHORISATION);

        assertEquals("withdrawalReceived", actualResult.getData().getDwpState());
        assertThatJson(actualResult.getData().getSscsDocument().size()).isEqualTo(1);
        assertEquals(LocalDate.now().toString(), actualResult.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
    }
}