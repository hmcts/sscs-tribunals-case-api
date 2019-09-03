package uk.gov.hmcts.reform.sscs.ccd.presubmit.withdrawnappeals;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
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
        boolean actualResult = handler.canHandle(callbackType, buildTestCallback(eventType,
            "adminAppealWithdrawnCallback.json"));
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void handle() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualResult = handler.handle(
            CallbackType.ABOUT_TO_SUBMIT, buildTestCallback(EventType.ADMIN_APPEAL_WITHDRAWN,
                "adminAppealWithdrawnCallback.json"));

        String expectedCaseData = fetchData("callback/withdrawnappeals/adminAppealWithdrawnExpectedCaseData.json");
        assertEquals("withdrawalReceived", actualResult.getData().getDwpState());
        assertThatJson(actualResult.getData()).isEqualTo(expectedCaseData);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({
        "ABOUT_TO_START,ADMIN_APPEAL_WITHDRAWN",
        "ABOUT_TO_START,null",
        "null,ADMIN_APPEAL_WITHDRAWN"
    })
    public void handleCornerCaseScenarios(@Nullable CallbackType callbackType, @Nullable EventType eventType)
        throws IOException {
        handler.handle(callbackType, buildTestCallback(eventType, "adminAppealWithdrawnCallback.json"));
    }

}