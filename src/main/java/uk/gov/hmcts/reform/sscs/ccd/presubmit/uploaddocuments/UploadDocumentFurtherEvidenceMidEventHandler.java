package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.REQUEST_FOR_HEARING_RECORDING;
import static uk.gov.hmcts.reform.sscs.util.PartiesOnCaseUtil.getPartiesOnCase;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class UploadDocumentFurtherEvidenceMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return (callbackType.equals(CallbackType.MID_EVENT)
                && callback.getEvent() == EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        log.info(String.format("Handling uploadDocumentFurtherEvidence event for caseId %s", sscsCaseData.getCcdCaseId()));

        if (showRequestHearingsPage(callbackResponse)) {
            sscsCaseData.getSscsHearingRecordingCaseData().setShowRequestingPartyPage(YesNo.YES);
            setPartiesToRequestInfoFrom(sscsCaseData);
        }

        return callbackResponse;
    }

    private boolean showRequestHearingsPage(PreSubmitCallbackResponse<SscsCaseData> callbackResponse) {

        long requestHearingCount = countNumberOfHearingRecordingRequests(callbackResponse.getData().getDraftSscsFurtherEvidenceDocument());

        if (requestHearingCount > 1) {
            callbackResponse.addError("Only one request for hearing recording can be submitted at a time");
        }

        return requestHearingCount == 1;
    }

    private long countNumberOfHearingRecordingRequests(List<SscsFurtherEvidenceDoc> sscsFurtherEvidenceDocList) {
        return emptyIfNull(sscsFurtherEvidenceDocList).stream()
                .filter(e -> REQUEST_FOR_HEARING_RECORDING.getId().equals(e.getValue().getDocumentType()))
                .count();
    }

    private void setPartiesToRequestInfoFrom(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = getPartiesOnCase(sscsCaseData);

        sscsCaseData.getSscsHearingRecordingCaseData().setRequestingParty(new DynamicList(listOptions.get(0), listOptions));
    }
}
