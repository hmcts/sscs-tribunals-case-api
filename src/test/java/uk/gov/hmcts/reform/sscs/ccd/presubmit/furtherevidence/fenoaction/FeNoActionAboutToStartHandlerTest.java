package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fenoaction;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.BaseHandlerTest;

@RunWith(JUnitParamsRunner.class)
public class FeNoActionAboutToStartHandlerTest extends BaseHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private FeNoActionAboutToStartHandler handler = new FeNoActionAboutToStartHandler();
    private static final String FE_NO_ACTION_CALLBACK_JSON = "fenoaction/feNoActionAboutToStartCallback.json";

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
        boolean actualResult = handler.canHandle(callbackType, buildTestCallbackGivenData(eventType, state,
            "appellantEvidence", FE_NO_ACTION_CALLBACK_JSON));

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void handle() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(CallbackType.ABOUT_TO_START,
            buildTestCallbackGivenData(EventType.FE_NO_ACTION, State.WITH_DWP.getId(),
                "appellantEvidence", FE_NO_ACTION_CALLBACK_JSON), USER_AUTHORISATION);

        String expectedCaseData = fetchData("fenoaction/expectedFeNoActionAboutToStartCallbackResponse.json");
        assertThatJson(actualCaseData).isEqualTo(expectedCaseData);
        assertEquals("feReceived", actualCaseData.getData().getDwpState());

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(DwpState.FE_ACTIONED_NR.getId(), DwpState.FE_ACTIONED_NR.getLabel()));
        listOptions.add(new DynamicListItem(DwpState.FE_ACTIONED_NA.getId(), DwpState.FE_ACTIONED_NA.getLabel()));
        assertEquals(listOptions, actualCaseData.getData().getDwpStateFeNoAction().getListItems());
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({
        "ABOUT_TO_SUBMIT,FE_NO_ACTION,withDwp",
        "ABOUT_TO_START,UPLOAD_DOCUMENT,appealCreated",
        "ABOUT_TO_SUBMIT,null,withDwp",
        "null,UPLOAD_DOCUMENT,withDwp"
    })
    public void handleCornerCaseScenarios(@Nullable CallbackType callbackType, @Nullable EventType eventType,
                                          @Nullable String state)
        throws IOException {
        handler.handle(callbackType, buildTestCallbackGivenData(eventType, state, "appellantEvidence",
            FE_NO_ACTION_CALLBACK_JSON), USER_AUTHORISATION);
    }

}
