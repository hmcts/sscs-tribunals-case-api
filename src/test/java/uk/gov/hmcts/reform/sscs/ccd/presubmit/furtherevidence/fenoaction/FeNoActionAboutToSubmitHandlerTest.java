package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fenoaction;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FE_ACTIONED_NR;

import java.io.IOException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.BaseHandlerTest;

@RunWith(JUnitParamsRunner.class)
public class FeNoActionAboutToSubmitHandlerTest extends BaseHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private FeNoActionAboutToSubmitHandler handler = new FeNoActionAboutToSubmitHandler();
    private static final String FE_NO_ACTION_CALLBACK_JSON = "fenoaction/feNoActionAboutToSubmitCallback.json";


    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,FE_NO_ACTION,withDwp,true",
        "ABOUT_TO_START,FE_NO_ACTION,withDwp,false",
        "ABOUT_TO_SUBMIT,FE_NO_ACTION,appealCreated,true",
        "ABOUT_TO_SUBMIT,APPEAL_RECEIVED,withDwp,false",
        "null,FE_NO_ACTION,withDwp,false",
        "ABOUT_TO_SUBMIT,null,withDwp,false"
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType, String state,
                          boolean expectedResult) throws IOException {
        boolean actualResult = handler.canHandle(callbackType, buildTestCallbackGivenData(eventType, state,
            "appellantEvidence", "", FE_NO_ACTION_CALLBACK_JSON));

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void handle() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(CallbackType.ABOUT_TO_SUBMIT,
            buildTestCallbackGivenData(EventType.FE_NO_ACTION, State.WITH_DWP.getId(),
                "dl6", "", FE_NO_ACTION_CALLBACK_JSON), USER_AUTHORISATION);

        String expectedCaseData = fetchData("fenoaction/expectedFeNoActionAboutToSubmitCallbackResponse.json");
        assertThatJson(actualCaseData)
            .whenIgnoringPaths(
                "data.jointPartyId",
                "data.appeal.appellant.appointee.id",
                "data.appeal.appellant.id",
                "data.appeal.rep.id")
            .when(Option.TREATING_NULL_AS_ABSENT)
            .isEqualTo(expectedCaseData);
        assertEquals(FE_ACTIONED_NR, actualCaseData.getData().getDwpState());
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({
        "ABOUT_TO_START,FE_NO_ACTION,withDwp",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,appealCreated",
        "ABOUT_TO_SUBMIT,null,withDwp",
        "null,UPLOAD_DOCUMENT,withDwp"
    })
    public void handleCornerCaseScenarios(@Nullable CallbackType callbackType, @Nullable EventType eventType,
                                          @Nullable String state)
        throws IOException {
        handler.handle(callbackType, buildTestCallbackGivenData(eventType, state, "appellantEvidence",
            "", FE_NO_ACTION_CALLBACK_JSON), USER_AUTHORISATION);
    }

}
