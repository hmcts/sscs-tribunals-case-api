package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

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

@RunWith(JUnitParamsRunner.class)
public class UploadDocumentHandlerTest extends BaseHandlerTest {

    private static final String UPLOAD_DOCUMENT_CALLBACK_JSON = "uploaddocument/uploadDocumentCallback.json";
    private UploadDocumentHandler handler = new UploadDocumentHandler();

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,withDwp,true",
        "ABOUT_TO_START,UPLOAD_DOCUMENT,withDwp,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,appealCreated,false",
        "ABOUT_TO_SUBMIT,APPEAL_RECEIVED,withDwp,false",
        "null,UPLOAD_DOCUMENT,withDwp,false",
        "ABOUT_TO_SUBMIT,null,withDwp,false",
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType, String state,
                          boolean expectedResult) throws IOException {
        boolean actualResult = handler.canHandle(callbackType, buildTestCallbackGivenEvent(eventType, state,
            UPLOAD_DOCUMENT_CALLBACK_JSON));

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void handle() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(CallbackType.ABOUT_TO_SUBMIT,
            buildTestCallbackGivenEvent(EventType.UPLOAD_DOCUMENT, State.WITH_DWP.getId(),
                UPLOAD_DOCUMENT_CALLBACK_JSON));

        String expectedCaseData = fetchData("uploaddocument/expectedUploadDocumentCallbackResponse.json");
        assertThatJson(actualCaseData).isEqualTo(expectedCaseData);
        assertEquals("feReceived", actualCaseData.getData().getDwpState());
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({
        "ABOUT_TO_START,UPLOAD_DOCUMENT,withDwp",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,appealCreated",
        "ABOUT_TO_SUBMIT,null,withDwp",
        "null,UPLOAD_DOCUMENT,withDwp"
    })
    public void handleCornerCaseScenarios(@Nullable CallbackType callbackType, @Nullable EventType eventType,
                                          @Nullable String state)
        throws IOException {
        handler.handle(callbackType, buildTestCallbackGivenEvent(eventType, state, UPLOAD_DOCUMENT_CALLBACK_JSON));
    }
}