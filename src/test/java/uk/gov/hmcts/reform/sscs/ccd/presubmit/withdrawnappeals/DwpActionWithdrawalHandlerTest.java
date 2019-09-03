package uk.gov.hmcts.reform.sscs.ccd.presubmit.withdrawnappeals;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

@RunWith(JUnitParamsRunner.class)
public class DwpActionWithdrawalHandlerTest extends AdminAppealWithdrawnBase {

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,DWP_ACTION_WITHDRAWAL,true",
//        "ABOUT_TO_START,DWP_ACTION_WITHDRAWAL,false",
//        "SUBMITTED,DWP_ACTION_WITHDRAWAL,false",
//        "MID_EVENT,ADMIN_APPEAL_WITHDRAWN,false",
//        "ABOUT_TO_SUBMIT,ISSUE_FURTHER_EVIDENCE,false",
//        "null,DWP_ACTION_WITHDRAWAL,false",
//        "ABOUT_TO_SUBMIT,null,false",
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType, boolean expectedResult)
        throws IOException {
        DwpActionWithdrawalHandler handler = new DwpActionWithdrawalHandler();
        boolean actualResult = handler.canHandle(CallbackType.ABOUT_TO_SUBMIT,
            buildTestCallback(EventType.DWP_ACTION_WITHDRAWAL,
                "dwpActionWithdrawalCallback.json"));
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void handle() {
    }
}