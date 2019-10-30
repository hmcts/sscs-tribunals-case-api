package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;

@Component
@Slf4j
public class DirectionIssuedMidEventHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final GenerateFile generateFile;
    private final String templateId;

    @Autowired
    public DirectionIssuedMidEventHandler(GenerateFile generateFile, @Value("${doc_assembly.direction_issued}") String templateId) {
        this.generateFile = generateFile;
        this.templateId = templateId;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
                && callback.getEvent() == EventType.DIRECTION_ISSUED
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData())
                && callback.getCaseDetails().getCaseData().isGenerateNotice();
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        return issueDocument(callback, DocumentType.DIRECTION_NOTICE, templateId, generateFile, userAuthorisation);
    }

}
