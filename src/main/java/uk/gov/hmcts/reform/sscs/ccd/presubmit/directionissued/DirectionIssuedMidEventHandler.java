package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;

@Component
@Slf4j
public class DirectionIssuedMidEventHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final GenerateFile generateFile;
    private final DocumentConfiguration documentConfiguration;

    @Autowired
    public DirectionIssuedMidEventHandler(GenerateFile generateFile,
                                          DocumentConfiguration documentConfiguration) {
        this.generateFile = generateFile;
        this.documentConfiguration = documentConfiguration;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
                && callback.getEvent() == EventType.DIRECTION_ISSUED
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData())
                && isYes(callback.getCaseDetails().getCaseData().getDocumentGeneration().getGenerateNotice());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        if (caseData.getDirectionTypeDl() == null) {
            PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(caseData);
            errorResponse.addError("Direction Type cannot be empty");
            return errorResponse;
        }

        if (DirectionType.PROVIDE_INFORMATION.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())
                && StringUtils.isBlank(caseData.getDirectionDueDate())) {
            final PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(caseData);
            errorResponse.addError("Please populate the direction due date");
            return errorResponse;
        }

        String templateId = documentConfiguration.getDocuments().get(caseData.getLanguagePreference()).get(EventType.DIRECTION_ISSUED);

        log.info("Direction Type is {} and templateId is {}", caseData.getDirectionTypeDl().getValue(), templateId);
        return issueDocument(callback, DocumentType.DIRECTION_NOTICE, templateId, generateFile, userAuthorisation);
    }

}
