package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.furtherevidence.fenoaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FE_ACTIONED_NR;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.uploaddocuments.BaseHandlerTest;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

public class FeNoActionAboutToSubmitHandlerTest extends BaseHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private FeNoActionAboutToSubmitHandler handler = new FeNoActionAboutToSubmitHandler();
    private static final String FE_NO_ACTION_CALLBACK_JSON = "fenoaction/feNoActionAboutToSubmitCallback.json";
    private final ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @ParameterizedTest
    @CsvSource(value = {
        "ABOUT_TO_SUBMIT,FE_NO_ACTION,withDwp,true",
        "ABOUT_TO_START,FE_NO_ACTION,withDwp,false",
        "ABOUT_TO_SUBMIT,FE_NO_ACTION,appealCreated,true",
        "ABOUT_TO_SUBMIT,APPEAL_RECEIVED,withDwp,false",
        "null,FE_NO_ACTION,withDwp,false",
        "ABOUT_TO_SUBMIT,null,withDwp,false"
    }, nullValues = {"null"})
    public void canHandle(CallbackType callbackType, EventType eventType, String state,
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
        JSONAssert.assertEquals(expectedCaseData, objectWriter.writeValueAsString(actualCaseData),
            new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("data.jointPartyId", (o1, o2) -> true),
                new Customization("data.appeal.appellant.appointee.id", (o1, o2) -> true),
                new Customization("data.appeal.appellant.id", (o1, o2) -> true),
                new Customization("data.appeal.rep.id", (o1, o2) -> true),
                new Customization("data.sscsDocument[0].id", (o1, o2) -> true),
                new Customization("data.sscsDocument[1].id", (o1, o2) -> true)
            ));
        assertEquals(FE_ACTIONED_NR, actualCaseData.getData().getDwpState());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "ABOUT_TO_START,FE_NO_ACTION,withDwp",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,appealCreated",
        "ABOUT_TO_SUBMIT,null,withDwp",
        "null,UPLOAD_DOCUMENT,withDwp"
    }, nullValues = {"null"})
    public void handleCornerCaseScenarios(CallbackType callbackType, EventType eventType,
                                          String state) {
        assertThrows(IllegalStateException.class, () -> handler.handle(callbackType, buildTestCallbackGivenData(eventType, state, "appellantEvidence",
            "", FE_NO_ACTION_CALLBACK_JSON), USER_AUTHORISATION));
    }

}
