package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fehandledoffline;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.FURTHER_EVIDENCE_HANDLED_OFFLINE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
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
@Slf4j
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
    @Parameters(method = "generateCanHandleScenarios")
    public void givenEventIsTriggered_thenCanHandle(@Nullable EventType eventType, @Nullable CallbackType callbackType,
                                                    @Nullable SscsCaseData sscsCaseData, boolean expectation) {
        mockCallback(eventType, sscsCaseData);

        boolean actual = feHandledOfflineHandler.canHandle(callbackType, callback);

        assertThat(actual, is(expectation));
    }

    private void mockCallback(EventType eventType, SscsCaseData sscsCaseData) {
        given(callback.getEvent()).willReturn(eventType);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
    }

    private Object[] generateCanHandleScenarios() {
        SscsDocument noIssuedDoc = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .evidenceIssued("No")
                .build())
            .build();

        SscsDocument issuedDoc = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .evidenceIssued("Yes")
                .build())
            .build();

        SscsCaseData sscsCaseDataWitNoIssuedDocsAndHmctsDwpStateFlagToClear = SscsCaseData.builder()
            .hmctsDwpState("failedSendingFurtherEvidence")
            .sscsDocument(Collections.singletonList(noIssuedDoc))
            .build();

        SscsCaseData sscsCaseDataWitNoIssuedDocsAndHmctsDwpStateFlagEmpty = SscsCaseData.builder()
            .hmctsDwpState("")
            .sscsDocument(Collections.singletonList(noIssuedDoc))
            .build();

        SscsCaseData sscsCaseDataWithNullDocs = SscsCaseData.builder()
            .sscsDocument(null)
            .build();

        SscsCaseData sscsCaseDataWithIssuedDocs = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(issuedDoc))
            .build();

        SscsCaseData sscsCaseDataWithIssuedDocsAndHmctsDwpStateToClear = SscsCaseData.builder()
            .hmctsDwpState("failedSendingFurtherEvidence")
            .sscsDocument(Collections.singletonList(issuedDoc))
            .build();

        SscsCaseData sscsCaseDataWithIssuedAndNoIssuedDocs = SscsCaseData.builder()
            .hmctsDwpState("failedSendingFurtherEvidence")
            .sscsDocument(Arrays.asList(issuedDoc, noIssuedDoc))
            .build();

        return new Object[]{
            new Object[]{FURTHER_EVIDENCE_HANDLED_OFFLINE, ABOUT_TO_SUBMIT, sscsCaseDataWitNoIssuedDocsAndHmctsDwpStateFlagToClear, true},
            new Object[]{FURTHER_EVIDENCE_HANDLED_OFFLINE, ABOUT_TO_SUBMIT, sscsCaseDataWitNoIssuedDocsAndHmctsDwpStateFlagEmpty, true},
            new Object[]{FURTHER_EVIDENCE_HANDLED_OFFLINE, ABOUT_TO_SUBMIT, sscsCaseDataWithIssuedAndNoIssuedDocs, true},
            new Object[]{FURTHER_EVIDENCE_HANDLED_OFFLINE, ABOUT_TO_SUBMIT, sscsCaseDataWithNullDocs, false},
            new Object[]{FURTHER_EVIDENCE_HANDLED_OFFLINE, ABOUT_TO_SUBMIT, sscsCaseDataWithIssuedDocs, false},
            new Object[]{FURTHER_EVIDENCE_HANDLED_OFFLINE, ABOUT_TO_SUBMIT, sscsCaseDataWithIssuedDocsAndHmctsDwpStateToClear, true},
            new Object[]{APPEAL_RECEIVED, ABOUT_TO_SUBMIT, sscsCaseDataWitNoIssuedDocsAndHmctsDwpStateFlagToClear, false},
            new Object[]{FURTHER_EVIDENCE_HANDLED_OFFLINE, ABOUT_TO_START, sscsCaseDataWitNoIssuedDocsAndHmctsDwpStateFlagToClear, false},
            new Object[]{null, ABOUT_TO_SUBMIT, sscsCaseDataWitNoIssuedDocsAndHmctsDwpStateFlagToClear, false},
            new Object[]{FURTHER_EVIDENCE_HANDLED_OFFLINE, null, sscsCaseDataWitNoIssuedDocsAndHmctsDwpStateFlagToClear, false},
        };
    }

    @Test
    public void givenCallbackIsNull_shouldReturnFalse() {
        assertFalse(feHandledOfflineHandler.canHandle(ABOUT_TO_SUBMIT, null));
    }

    @Test
    @Parameters(method = "generateSscsCaseDataScenariosToHandleEvent")
    @Ignore
    public void givenEventIsTriggered_shouldHandleIt(String testScenarioDesc, SscsCaseData sscsCaseData) {
        log.info(testScenarioDesc);
        mockCallback(FURTHER_EVIDENCE_HANDLED_OFFLINE, sscsCaseData);
        given(callback.getEvent()).willReturn(FURTHER_EVIDENCE_HANDLED_OFFLINE);

        PreSubmitCallbackResponse<SscsCaseData> currentCallback = feHandledOfflineHandler.handle(ABOUT_TO_SUBMIT,
            callback, auth_token);

        assertNull(currentCallback.getData().getHmctsDwpState());
        verifyEvidenceIssuedIsYes(currentCallback);
    }

    private void verifyEvidenceIssuedIsYes(PreSubmitCallbackResponse<SscsCaseData> currentCallback) {
        List<SscsDocument> sscsDocument = currentCallback.getData().getSscsDocument();
        sscsDocument.forEach(doc -> assertThat(doc.getValue().getEvidenceIssued(), equalTo("Yes")));
    }

    private Object[] generateSscsCaseDataScenariosToHandleEvent() {
        SscsDocument noIssuedDoc = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .evidenceIssued("No")
                .build())
            .build();
        SscsDocument issuedDoc = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .evidenceIssued("Yes")
                .build())
            .build();

        SscsCaseData sscsCaseDataWithNoIssuedDocs = SscsCaseData.builder()
            .hmctsDwpState("failedSendingFurtherEvidence")
            .sscsDocument(Arrays.asList(noIssuedDoc, noIssuedDoc))
            .build();

        SscsCaseData sscsCaseDataWithIssuedAndNoIssuedDocs = SscsCaseData.builder()
            .hmctsDwpState("failedSendingFurtherEvidence")
            .sscsDocument(Arrays.asList(noIssuedDoc, issuedDoc))
            .build();

        SscsCaseData sscsCaseDataWithNullDocs = SscsCaseData.builder()
            .hmctsDwpState("failedSendingFurtherEvidence")
            .sscsDocument(null)
            .build();

        return new Object[]{
            new Object[]{"caseData with issued and no issued evidence", sscsCaseDataWithIssuedAndNoIssuedDocs},
            new Object[]{"caseData with null sscsDocument object", sscsCaseDataWithNullDocs},
            new Object[]{"caseData with no issued evidence", sscsCaseDataWithNoIssuedDocs},
        };
    }

    @Test(expected = IllegalStateException.class)
    public void givenEventIsHandled_shouldThrowExceptionIfCannotBeHandled() {
        feHandledOfflineHandler.handle(ABOUT_TO_START, callback, auth_token);
    }
}