package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fehandledoffline;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;

@RunWith(JUnitParamsRunner.class)
public class FeHandledOfflineHandlerTest {

    private final String auth_token = "auth token";
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private final FeHandledOfflineHandler feHandledOfflineHandler = new FeHandledOfflineHandler();

    @Test
    @Parameters({
        "FURTHER_EVIDENCE_HANDLED_OFFLINE, ABOUT_TO_SUBMIT, true",
        "APPEAL_RECEIVED, ABOUT_TO_SUBMIT, false",
        "FURTHER_EVIDENCE_HANDLED_OFFLINE, ABOUT_TO_START, false",
        "null, ABOUT_TO_SUBMIT, false",
        "FURTHER_EVIDENCE_HANDLED_OFFLINE, null, false"
    })
    public void givenEventIsTriggered_thenCanHandle(@Nullable EventType eventType, @Nullable CallbackType callbackType,
                                                    boolean expectation) {
        given(callback.getEvent()).willReturn(eventType);

        boolean actual = feHandledOfflineHandler.canHandle(callbackType, callback);

        assertThat(actual, is(expectation));
    }

    @Test
    public void givenCallbackIsNull_shouldReturnFalse() {
        assertFalse(feHandledOfflineHandler.canHandle(ABOUT_TO_SUBMIT, null));
    }

    @Test
    public void givenEventIsTriggered_shouldHandleIt() {
        mockCallback();

        PreSubmitCallbackResponse<SscsCaseData> currentCallback = feHandledOfflineHandler.handle(ABOUT_TO_SUBMIT,
            callback, auth_token);

        assertNull(currentCallback.getData().getHmctsDwpState());
        verifyEvidenceIssuedIsYes(currentCallback);
    }

    private void verifyEvidenceIssuedIsYes(PreSubmitCallbackResponse<SscsCaseData> currentCallback) {
        List<SscsDocument> sscsDocument = currentCallback.getData().getSscsDocument();
        sscsDocument.forEach(doc -> assertThat(doc.getValue().getEvidenceIssued(), equalTo("Yes")));
    }

    private void mockCallback() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        SscsDocument document = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .evidenceIssued("No")
                .build())
            .build();
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .hmctsDwpState("failedSendingFurtherEvidence")
            .sscsDocument(Arrays.asList(document, document))
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        given(callback.getEvent()).willReturn(EventType.FURTHER_EVIDENCE_HANDLED_OFFLINE);
    }

    @Test(expected = IllegalStateException.class)
    public void givenEventIsHandled_shouldThrowExceptionIfCannotBeHandled() {
        feHandledOfflineHandler.handle(ABOUT_TO_START, callback, auth_token);
    }
}