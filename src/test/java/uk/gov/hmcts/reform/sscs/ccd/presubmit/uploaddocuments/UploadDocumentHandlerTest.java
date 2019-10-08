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
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String UPLOAD_DOCUMENT_CALLBACK_JSON = "uploaddocument/uploadDocumentCallback.json";
    private UploadDocumentHandler handler = new UploadDocumentHandler();

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,withDwp,Medical evidence,true",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,withDwp,Other evidence,true",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,withDwp,appellantEvidence,true",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,withDwp,representativeEvidence,true",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,withDwp,sscs1,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,withDwp,Decision Notice,false",
        "ABOUT_TO_START,UPLOAD_DOCUMENT,withDwp,representativeEvidence,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,appealCreated,Medical evidence,true",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,appealCreated,Other evidence,true",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,appealCreated,appellantEvidence,true",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,appealCreated,representativeEvidence,true",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,appealCreated,dl6,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,appealCreated,DWP response,false",
        "ABOUT_TO_SUBMIT,APPEAL_RECEIVED,withDwp,representativeEvidence,false",
        "ABOUT_TO_SUBMIT,APPEAL_RECEIVED,withDwp,dl6,false",
        "null,UPLOAD_DOCUMENT,withDwp,appellantEvidence,false",
        "ABOUT_TO_SUBMIT,null,withDwp,appellantEvidence,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,withDwp,,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,withDwp,nullSscsDocuments,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,withDwp,nullDocumentType,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,withDwp,nullSscsDocument,false"
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType, String state,
                          @Nullable String documentType, boolean expectedResult) throws IOException {
        boolean actualResult = handler.canHandle(callbackType, buildTestCallbackGivenData(eventType, state,
            documentType, UPLOAD_DOCUMENT_CALLBACK_JSON));

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void handle() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(CallbackType.ABOUT_TO_SUBMIT,
            buildTestCallbackGivenData(EventType.UPLOAD_DOCUMENT, State.WITH_DWP.getId(),
                "representativeEvidence", UPLOAD_DOCUMENT_CALLBACK_JSON), USER_AUTHORISATION);

        String expectedCaseData = fetchData("uploaddocument/expectedUploadDocumentCallbackResponse.json");
        assertThatJson(actualCaseData).isEqualTo(expectedCaseData);
        assertEquals("feReceived", actualCaseData.getData().getDwpState());
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({
        "ABOUT_TO_START,UPLOAD_DOCUMENT,withDwp",
        "ABOUT_TO_SUBMIT,null,withDwp",
        "null,UPLOAD_DOCUMENT,withDwp"
    })
    public void handleCornerCaseScenarios(@Nullable CallbackType callbackType, @Nullable EventType eventType,
                                          @Nullable String state)
        throws IOException {
        handler.handle(callbackType, buildTestCallbackGivenData(eventType, state, "representativeEvidence",
            UPLOAD_DOCUMENT_CALLBACK_JSON), USER_AUTHORISATION);
    }
}