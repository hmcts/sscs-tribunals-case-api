package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;

import java.util.Objects;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.GRANT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.ISSUE_DIRECTIONS_NOTICE;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.isDateInTheFuture;

@Component
@Slf4j
public class ActionPostponementRequestMidEventHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final GenerateFile generateFile;
    private final DocumentConfiguration documentConfiguration;

    @Autowired
    public ActionPostponementRequestMidEventHandler(GenerateFile generateFile,
                                                    DocumentConfiguration documentConfiguration) {
        this.generateFile = generateFile;
        this.documentConfiguration = documentConfiguration;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
                && callback.getEvent() == EventType.ACTION_POSTPONEMENT_REQUEST
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData())
                && callback.getCaseDetails().getCaseData().isGenerateNotice();
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        validateDueDateIsInFuture(response);

        if (!response.getErrors().isEmpty()) {
            return response;
        }

        if(isGrantPostponement(caseData.getPostponementRequest())) {
            String templateId = documentConfiguration.getDocuments()
                    .get(caseData.getLanguagePreference()).get(EventType.DIRECTION_ISSUED);
            return issueDocument(callback, DocumentType.DECISION_NOTICE, templateId, generateFile, userAuthorisation);
        }

        return response;
    }

    private void validateDueDateIsInFuture(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (nonNull(preSubmitCallbackResponse.getData().getDirectionDueDate()) && !isDateInTheFuture(preSubmitCallbackResponse.getData().getDirectionDueDate())) {
            preSubmitCallbackResponse.addError("Directions due date must be in the future");
        }
    }

    private boolean isGrantPostponement(PostponementRequest postponementRequest) {
        return postponementRequest.getActionPostponementRequestSelected().equals(GRANT.getValue());
    }
}
