package uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.*;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.JUDGE;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.SUPER_USER;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;

@Service
public class ProcessAudioVideoEvidenceAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final IdamService idamService;

    @Autowired
    public ProcessAudioVideoEvidenceAboutToStartHandler(IdamService idamService) {
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.PROCESS_AUDIO_VIDEO;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        if (sscsCaseData.getAudioVideoEvidence() == null || sscsCaseData.getAudioVideoEvidence().isEmpty()) {
            PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
            errorResponse.addError("Before running this event audio and video evidence must be uploaded");
            return errorResponse;
        }
        final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
        final boolean hasJudgeRole = userDetails.hasRole(JUDGE);
        final boolean hasSuperUserRole = userDetails.hasRole(SUPER_USER);
        setProcessAudioVideoActionDropdown(sscsCaseData, hasJudgeRole, hasSuperUserRole);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setProcessAudioVideoActionDropdown(SscsCaseData sscsCaseData, boolean hasJudgeRole, boolean hasSuperUserRole) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        populateListWithItems(listOptions, ISSUE_DIRECTIONS_NOTICE);

        if (hasJudgeRole || hasSuperUserRole) {
            populateListWithItems(listOptions, EXCLUDE_EVIDENCE);
        }

        sscsCaseData.setProcessAudioVideoAction(new DynamicList(listOptions.get(0), listOptions));
    }

    private void populateListWithItems(List<DynamicListItem> listOptions,
                                       ProcessAudioVideoActionDynamicListItems... items) {

        for (ProcessAudioVideoActionDynamicListItems item : items) {
            listOptions.add(new DynamicListItem(item.getCode(), item.getLabel()));
        }
    }
}
