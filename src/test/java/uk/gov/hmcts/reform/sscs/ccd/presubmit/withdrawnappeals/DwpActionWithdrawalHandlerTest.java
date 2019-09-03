package uk.gov.hmcts.reform.sscs.ccd.presubmit.withdrawnappeals;

import java.io.IOException;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

public class DwpActionWithdrawalHandlerTest extends AdminAppealWithdrawnBase {

    @Test
    public void canHandle() throws IOException {
        DwpActionWithdrawalHandler handler = new DwpActionWithdrawalHandler();
        handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, buildTestCallback(EventType.DWP_ACTION_WITHDRAWAL,
            "adminAppealWithdrawnCallback.json"));
    }

    @Test
    public void handle() {
    }
}