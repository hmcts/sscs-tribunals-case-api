package uk.gov.hmcts.reform.sscs.ccd.presubmit.fenoaction;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.BaseHandlerTest;

@RunWith(JUnitParamsRunner.class)
public class FeNoActionHandlerTest extends BaseHandlerTest {

    private FeNoActionHandler handler = new FeNoActionHandler();
    private static final String UPLOAD_DOCUMENT_CALLBACK_JSON = "uploaddocument/uploadDocumentCallback.json";

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,FE_NO_ACTION,withDwp,true",
        "ABOUT_TO_START,FE_NO_ACTION,withDwp,false",
        "ABOUT_TO_SUBMIT,FE_NO_ACTION,appealCreated,false",
        "ABOUT_TO_SUBMIT,APPEAL_RECEIVED,withDwp,false",
        "null,FE_NO_ACTION,withDwp,false",
        "ABOUT_TO_SUBMIT,null,withDwp,false"
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType, String state,
                          boolean expectedResult) throws IOException {
        boolean actualResult = handler.canHandle(callbackType, buildTestCallbackGivenEvent(eventType, state,
            UPLOAD_DOCUMENT_CALLBACK_JSON));
        assertEquals(expectedResult, actualResult);
    }
}
