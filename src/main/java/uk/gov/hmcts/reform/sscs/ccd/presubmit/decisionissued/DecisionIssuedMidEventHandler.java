package uk.gov.hmcts.reform.sscs.ccd.presubmit.decisionissued;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@Component
@Slf4j
public class DecisionIssuedMidEventHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final GenerateFile generateFile;
    protected final UserDetailsService userDetailsService;
    private final DocumentConfiguration documentConfiguration;


    @Autowired
    public DecisionIssuedMidEventHandler(GenerateFile generateFile, UserDetailsService userDetailsService, DocumentConfiguration documentConfiguration) {
        this.generateFile = generateFile;
        this.userDetailsService = userDetailsService;
        this.documentConfiguration = documentConfiguration;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
                && callback.getEvent() == EventType.DECISION_ISSUED
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData())
                && isYes(callback.getCaseDetails().getCaseData().getDocumentGeneration().getGenerateNotice());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        String templateId = documentConfiguration.getDocuments()
                .get(callback.getCaseDetails().getCaseData().getLanguagePreference()).get(EventType.DIRECTION_ISSUED);
        callback.getCaseDetails().getCaseData().getDocumentGeneration().setSignedRole("Tribunal Judge");
        callback.getCaseDetails().getCaseData().getDocumentGeneration().setSignedBy(userDetailsService.buildLoggedInUserSurname(userAuthorisation));
        return issueDocument(callback, DocumentType.DECISION_NOTICE, templateId, generateFile, userAuthorisation);

    }
}
