package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class UploadDocumentFurtherEvidenceHandlerTest extends BaseHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String UPLOAD_DOCUMENT_FE_CALLBACK_JSON = "uploaddocument/uploadDocumentFECallback.json";
    private UploadDocumentFurtherEvidenceHandler handler = new UploadDocumentFurtherEvidenceHandler();

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,Medical evidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,Other evidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,appellantEvidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,representativeEvidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,sscs1,dl6,true,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,Decision Notice,dl6,true,false",
        "ABOUT_TO_START,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,representativeEvidence,appellantEvidence,false, true",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,appealCreated,Medical evidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,appealCreated,Other evidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,appealCreated,appellantEvidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,appealCreated,representativeEvidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,appealCreated,dl6,sscs1,true,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,appealCreated,DWP response,dl6,true,false",
        "ABOUT_TO_SUBMIT,APPEAL_RECEIVED,withDwp,representativeEvidence,appellantEvidence,false,true",
        "ABOUT_TO_SUBMIT,APPEAL_RECEIVED,withDwp,dl6,appellantEvidence,false, true",
        "null,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,appellantEvidence,appellantEvidence,false, true",
        "ABOUT_TO_SUBMIT,null,withDwp,appellantEvidence,appellantEvidence,false, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,,,true,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,nullSscsDocuments,appellantEvidence,true,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,nullDocumentType,appellantEvidence,true,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,nullSscsDocument,appellantEvidence,true,false"
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType, String state,
                          @Nullable String documentType,@Nullable String documentType2, boolean expectedResult,
                          boolean expectToInitDrafts)
        throws IOException {
        Callback<SscsCaseData> actualCallback = buildTestCallbackGivenData(eventType, state,
            documentType, documentType2, UPLOAD_DOCUMENT_FE_CALLBACK_JSON);
        boolean actualResult = handler.canHandle(callbackType, actualCallback);

        assertEquals(expectedResult, actualResult);

        if (expectToInitDrafts) {
            assertNull(actualCallback.getCaseDetails().getCaseData().getDraftSscsFurtherEvidenceDocument());
        }
    }

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,representativeEvidence,appellantEvidence,true, false"
    })
    public void handle(@Nullable CallbackType callbackType, @Nullable EventType eventType, String state,
                       @Nullable String documentType,@Nullable String documentType2, boolean expectedResult,
                       boolean expectToInitDrafts) throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(eventType,state, documentType, documentType2,
            UPLOAD_DOCUMENT_FE_CALLBACK_JSON);

        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(callbackType, callback,
            USER_AUTHORISATION);

        assertThatJson(actualCaseData).isEqualTo(getExpectedCaseData());
        assertEquals("feReceived", actualCaseData.getData().getDwpState());
        assertNull(actualCaseData.getData().getDraftSscsFurtherEvidenceDocument());
    }

    private String getExpectedCaseData() throws IOException {
        String expectedCaseData = fetchData("uploaddocument/expectedUploadDocumentFECallbackResponse.json");
        return expectedCaseData.replace("DOCUMENT_DATE_ADDED_PLACEHOLDER", LocalDate.now().atStartOfDay()
            .toString());
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({
        "ABOUT_TO_START,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp",
        "ABOUT_TO_SUBMIT,null,withDwp",
        "null,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp"
    })
    public void handleCornerCaseScenarios(@Nullable CallbackType callbackType, @Nullable EventType eventType,
                                          @Nullable String state)
        throws IOException {
        handler.handle(callbackType, buildTestCallbackGivenData(eventType, state,
            "representativeEvidence", "appellantEvidence",
            UPLOAD_DOCUMENT_FE_CALLBACK_JSON), USER_AUTHORISATION);
    }
}