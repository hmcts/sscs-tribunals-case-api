package uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo;

import static java.util.Objects.*;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_INFORMATION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.EXCLUDE_EVIDENCE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Service
public class ProcessAudioVideoEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;

    @Autowired
    public ProcessAudioVideoEvidenceAboutToSubmitHandler(FooterService footerService) {
        this.footerService = footerService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.PROCESS_AUDIO_VIDEO;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData caseData = caseDetails.getCaseData();

        DocumentLink url = null;

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        if (nonNull(caseData.getPreviewDocument())) {
            url = caseData.getPreviewDocument();
        } else {
            response.addError("There is no document notice");
        }

        if (isNull(caseData.getProcessAudioVideoAction()) || isNull(caseData.getProcessAudioVideoAction().getValue())) {
            response.addError("There action to process the audio/video evidence");
        }

        if (!isEmpty(response.getErrors())) {
            return response;
        }

        footerService.createFooterAndAddDocToCase(url, caseData, DocumentType.DIRECTION_NOTICE,
                Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now())
                        .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                caseData.getDateAdded(), null, null);

        caseData.setInterlocReviewState(AWAITING_INFORMATION.getId());
        caseData.setDwpState(DwpState.DIRECTION_ACTION_REQUIRED.getId());

        processIfExcludeEvidence(caseData);

        clearTransientFields(caseData);

        return new PreSubmitCallbackResponse<>(caseData);

    }

    private void processIfExcludeEvidence(SscsCaseData caseData) {
        if (nonNull(caseData.getProcessAudioVideoAction())
                && nonNull(caseData.getProcessAudioVideoAction().getValue())
                && StringUtils.equals(caseData.getProcessAudioVideoAction().getValue().getCode(), EXCLUDE_EVIDENCE.getCode())) {
            caseData.setInterlocReviewState(null);
            caseData.setDwpState(DwpState.DIRECTION_ACTION_REQUIRED.getId());
            caseData.setAudioVideoEvidence(null);
        }
    }

    private void clearTransientFields(SscsCaseData caseData) {
        caseData.setBodyContent(null);
        caseData.setPreviewDocument(null);
        caseData.setGenerateNotice(null);
        caseData.setReservedToJudge(null);
        caseData.setGenerateNotice(null);
        caseData.setSignedBy(null);
        caseData.setSignedRole(null);
        caseData.setDateAdded(null);
    }
}
