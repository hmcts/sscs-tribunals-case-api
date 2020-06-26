package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fehandledoffline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.FURTHER_EVIDENCE_HANDLED_OFFLINE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import lombok.extern.slf4j.Slf4j;
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

    private final String authToken = "auth token";
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private final FeHandledOfflineHandler feHandledOfflineHandler = new FeHandledOfflineHandler();
    private final SscsDocument noIssuedDoc = SscsDocument.builder()
        .value(SscsDocumentDetails.builder()
            .documentType("Appellant evidence")
            .evidenceIssued("No")
            .build())
        .build();
    private final SscsDocument issuedDoc = SscsDocument.builder()
        .value(SscsDocumentDetails.builder()
            .documentType("Representative evidence")
            .evidenceIssued("Yes")
            .build())
        .build();
    private final SscsDocument dl16Doc = SscsDocument.builder()
        .value(SscsDocumentDetails.builder()
            .documentType("DL16")
            .build())
        .build();

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

        SscsCaseData sscsCaseDataWithEmptyDocs = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(SscsDocument.builder().build()))
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
            new Object[]{FURTHER_EVIDENCE_HANDLED_OFFLINE, ABOUT_TO_SUBMIT, sscsCaseDataWithEmptyDocs, false},
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
    public void givenCaseWithIssuedAndNoIssuedAndDl16Docs_shouldHandleIt() {
        SscsCaseData sscsCaseDataWithIssuedAndNoIssuedAndDl16Docs = SscsCaseData.builder()
            .hmctsDwpState("failedSendingFurtherEvidence")
            .sscsDocument(Arrays.asList(noIssuedDoc, issuedDoc, dl16Doc))
            .build();
        mockCallback(FURTHER_EVIDENCE_HANDLED_OFFLINE, sscsCaseDataWithIssuedAndNoIssuedAndDl16Docs);

        PreSubmitCallbackResponse<SscsCaseData> currentCallback = feHandledOfflineHandler.handle(ABOUT_TO_SUBMIT,
            callback, authToken);

        assertNull(currentCallback.getData().getHmctsDwpState());
        verifyEvidenceIssuedIsYesAndDl6WasNotModified(currentCallback.getData().getSscsDocument());
    }

    @Test
    public void givenCaseWithIssuedAndDl16Docs_shouldHandleIt() {
        SscsCaseData sscsCaseDataWithIssuedAndDl16Docs = SscsCaseData.builder()
            .hmctsDwpState("failedSendingFurtherEvidence")
            .sscsDocument(Arrays.asList(issuedDoc, dl16Doc))
            .build();
        mockCallback(FURTHER_EVIDENCE_HANDLED_OFFLINE, sscsCaseDataWithIssuedAndDl16Docs);

        PreSubmitCallbackResponse<SscsCaseData> currentCallback = feHandledOfflineHandler.handle(ABOUT_TO_SUBMIT,
            callback, authToken);

        assertNull(currentCallback.getData().getHmctsDwpState());

        List<SscsDocument> sscsDocuments = currentCallback.getData().getSscsDocument();

        verifyEvidenceDocs(sscsDocuments);
        verifyDl16(sscsDocuments);
    }

    @Test
    public void givenCaseWithNoDocs_shouldHandleIt() {
        SscsCaseData sscsCaseDataWithIssuedAndDl16Docs = SscsCaseData.builder()
            .hmctsDwpState("failedSendingFurtherEvidence")
            .sscsDocument(null)
            .build();
        mockCallback(FURTHER_EVIDENCE_HANDLED_OFFLINE, sscsCaseDataWithIssuedAndDl16Docs);

        PreSubmitCallbackResponse<SscsCaseData> currentCallback = feHandledOfflineHandler.handle(ABOUT_TO_SUBMIT,
            callback, authToken);

        assertNull(currentCallback.getData().getHmctsDwpState());
    }

    private void verifyEvidenceDocs(List<SscsDocument> sscsDocuments) {
        List<SscsDocument> evidenceDocs = sscsDocuments.stream()
            .filter(doc -> "Yes".equals(doc.getValue().getEvidenceIssued()))
            .collect(Collectors.toList());

        assertThat(evidenceDocs, hasItems(issuedDoc));
    }

    private void verifyEvidenceIssuedIsYesAndDl6WasNotModified(List<SscsDocument> sscsDocuments) {
        List<SscsDocument> evidenceDocs = sscsDocuments.stream()
            .filter(doc -> "Yes".equals(doc.getValue().getEvidenceIssued()))
            .collect(Collectors.toList());

        SscsDocument expectedDoc = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType("Appellant evidence")
                .evidenceIssued("Yes")
                .build())
            .build();
        assertThat(evidenceDocs, hasItems(expectedDoc, issuedDoc));

        verifyDl16(sscsDocuments);
    }

    private void verifyDl16(List<SscsDocument> sscsDocuments) {
        List<SscsDocument> noEvidenceDocs = sscsDocuments.stream()
            .filter(doc -> null == doc.getValue().getEvidenceIssued())
            .collect(Collectors.toList());
        assertThat(noEvidenceDocs, hasItem(dl16Doc));
    }

    @Test(expected = IllegalStateException.class)
    public void givenEventIsHandled_shouldThrowExceptionIfCannotBeHandled() {
        feHandledOfflineHandler.handle(ABOUT_TO_START, callback, authToken);
    }
}
