package uk.gov.hmcts.reform.sscs.ccd.presubmit.fenoaction;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.BaseHandlerTest;

@RunWith(JUnitParamsRunner.class)
public class FeNoActionAboutToStartHandlerTest extends BaseHandlerTest {

    private FeNoActionAboutToStartHandler handler = new FeNoActionAboutToStartHandler();
    private static final String FE_NO_ACTION_CALLBACK_JSON = "fenoaction/feNoActionCallback.json";

    @Test
    @Parameters({
        "ABOUT_TO_START,FE_NO_ACTION,withDwp,true",
        "ABOUT_TO_SUBMIT,FE_NO_ACTION,withDwp,false",
        "ABOUT_TO_START,FE_NO_ACTION,appealCreated,true",
        "ABOUT_TO_START,APPEAL_RECEIVED,withDwp,false",
        "null,FE_NO_ACTION,withDwp,false",
        "ABOUT_TO_SUBMIT,null,withDwp,false"
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType, String state,
                          boolean expectedResult) throws IOException {
        boolean actualResult = handler.canHandle(callbackType, buildTestCallbackGivenEvent(eventType, state,
            FE_NO_ACTION_CALLBACK_JSON));

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void handle() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(CallbackType.ABOUT_TO_SUBMIT,
            buildTestCallbackGivenEvent(EventType.FE_NO_ACTION, State.WITH_DWP.getId(), FE_NO_ACTION_CALLBACK_JSON));

        String expectedCaseData = fetchData("fenoaction/expectedFeNoActionCallbackResponse.json");
        assertThatJson(actualCaseData).isEqualTo(expectedCaseData);
        assertEquals("feActionedNR", actualCaseData.getData().getDwpState());
    }

}
