package uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.ISSUE_DIRECTIONS_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoEvidenceAboutToSubmitHandler.ACTIONS_THAT_REQUIRES_NOTICE;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.*;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.getDocumentType;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.isSelectedEvidence;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.isDateInTheFuture;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;

@Component
@Slf4j
public class ProcessAudioVideoEvidenceMidEventHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final GenerateFile generateFile;
    private final DocumentConfiguration documentConfiguration;
    private final IdamService idamService;

    @Autowired
    public ProcessAudioVideoEvidenceMidEventHandler(GenerateFile generateFile,
                                                    DocumentConfiguration documentConfiguration,
                                                    IdamService idamService) {
        this.generateFile = generateFile;
        this.documentConfiguration = documentConfiguration;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
                && callback.getEvent() == EventType.PROCESS_AUDIO_VIDEO;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        DynamicList processAudioVideoAction = caseData.getProcessAudioVideoAction();

        setActionDropDown(processAudioVideoAction, caseData, userAuthorisation);
        setEvidenceDropdown(caseData);

        AudioVideoEvidenceDetails selectedAudioVideoEvidenceDetails = caseData.getSelectedAudioVideoEvidenceDetails();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        validateDueDateIsInFuture(response);

        if (!response.getErrors().isEmpty()) {
            return response;
        }

        if (nonNull(selectedAudioVideoEvidenceDetails) && nonNull(processAudioVideoAction) && ACTIONS_THAT_REQUIRES_NOTICE.contains(processAudioVideoAction.getValue().getCode())) {
            String templateId = documentConfiguration.getDocuments().get(caseData.getLanguagePreference()).get(EventType.DIRECTION_ISSUED);
            return issueDocument(callback, DocumentType.DIRECTION_NOTICE, templateId, generateFile, userAuthorisation);
        }

        AudioVideoEvidence selectedAudioVideoEvidence = caseData.getAudioVideoEvidence().stream().filter(evidence -> isSelectedEvidence(evidence, caseData)).findFirst().orElse(null);

        if (nonNull(selectedAudioVideoEvidence)) {

            if (nonNull(selectedAudioVideoEvidence.getValue().getRip1Document())) {
                caseData.setShowRip1DocPage(YES);
            } else {
                caseData.setShowRip1DocPage(NO);
            }
            setDocumentType(selectedAudioVideoEvidence.getValue());
            selectedAudioVideoEvidenceDetails = selectedAudioVideoEvidence.getValue();
        }

        caseData.setSelectedAudioVideoEvidenceDetails(selectedAudioVideoEvidenceDetails);

        return response;
    }

    private void setActionDropDown(DynamicList processAudioVideoAction, SscsCaseData caseData, String userAuthorisation) {

        final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
        final boolean hasJudgeRole = userDetails.hasRole(JUDGE);
        final boolean hasTcwRole = userDetails.hasRole(TCW);
        final boolean hasSuperUserRole = userDetails.hasRole(SUPER_USER);

        if (nonNull(processAudioVideoAction) && nonNull(processAudioVideoAction.getValue())) {
            List<DynamicListItem> actionList = ProcessAudioVideoActionHelper.populateListItems(hasJudgeRole, hasTcwRole, hasSuperUserRole);
            DynamicList updatedActions = new DynamicList(processAudioVideoAction.getValue(), actionList);
            caseData.setProcessAudioVideoAction(updatedActions);
        } else {
            ProcessAudioVideoActionHelper.setProcessAudioVideoActionDropdown(caseData, hasJudgeRole, hasTcwRole, hasSuperUserRole);
        }
    }

    private void setEvidenceDropdown(SscsCaseData caseData) {
        final DynamicList evidenceDL = caseData.getSelectedAudioVideoEvidence();

        if (nonNull(evidenceDL) && nonNull(evidenceDL.getValue())) {
            List<DynamicListItem> evidenceList = ProcessAudioVideoActionHelper.populateEvidenceListWithItems(caseData);
            DynamicList updatedEvidences = new DynamicList(evidenceDL.getValue(), evidenceList);
            caseData.setSelectedAudioVideoEvidence(updatedEvidences);
        } else {
            ProcessAudioVideoActionHelper.setSelectedAudioVideoEvidence(caseData);
        }
    }

    private void setDocumentType(AudioVideoEvidenceDetails evidence) {
        DocumentType documentType = getDocumentType(evidence.getDocumentLink().getDocumentFilename());

        String documentTypeLabel = null;

        if (nonNull(documentType)) {
            documentTypeLabel = documentType.getLabel();
        }

        evidence.setDocumentType(documentTypeLabel);
    }

    private void validateDueDateIsInFuture(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (preSubmitCallbackResponse.getData().getProcessAudioVideoAction() != null && preSubmitCallbackResponse.getData().getProcessAudioVideoAction().getValue() != null
            && ISSUE_DIRECTIONS_NOTICE.getCode().equals(preSubmitCallbackResponse.getData().getProcessAudioVideoAction().getValue().getCode())
            && nonNull(preSubmitCallbackResponse.getData().getDirectionDueDate()) && !isDateInTheFuture(preSubmitCallbackResponse.getData().getDirectionDueDate())) {
            preSubmitCallbackResponse.addError("Directions due date must be in the future");
        }
    }
}
