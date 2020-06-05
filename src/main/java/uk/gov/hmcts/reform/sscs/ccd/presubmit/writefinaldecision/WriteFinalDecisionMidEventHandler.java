package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.DirectionOrDecisionIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.DirectionOrDecisionIssuedTemplateBody.DirectionOrDecisionIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.util.StringUtils;

@Component
@Slf4j
public class WriteFinalDecisionMidEventHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    // FIXME
    private static final String JUDGE_NAME_PLACEHOLDER = "Judge Name Placeholder";

    private final GenerateFile generateFile;
    private final String templateId;

    @Autowired
    public WriteFinalDecisionMidEventHandler(GenerateFile generateFile, @Value("${doc_assembly.issue_final_decision}") String templateId) {
        this.generateFile = generateFile;
        this.templateId = templateId;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
            && callback.getEvent() == EventType.WRITE_FINAL_DECISION
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (isDecisionNoticeDatesInvalid(sscsCaseData)) {
            preSubmitCallbackResponse.addError("Decision notice end date must be after decision notice start date");
        }
        if (isDecisionNoticeDateOfDecisionInvalid(sscsCaseData)) {
            preSubmitCallbackResponse.addError("Decision notice date of decision must not be in the future");
        }

        if (sscsCaseData.isWriteFinalDecisionGenerateNotice()) {
            try {
                return issueDocument(callback, DocumentType.DRAFT_DECISION_NOTICE, templateId, generateFile, userAuthorisation);
            } catch (IllegalStateException e) {
                preSubmitCallbackResponse.addError(e.getMessage());
            }
        }

        return preSubmitCallbackResponse;
    }

    private boolean isDecisionNoticeDatesInvalid(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getWriteFinalDecisionStartDate() != null && sscsCaseData.getWriteFinalDecisionEndDate() != null) {
            LocalDate decisionNoticeStartDate = LocalDate.parse(sscsCaseData.getWriteFinalDecisionStartDate());
            LocalDate decisionNoticeEndDate = LocalDate.parse(sscsCaseData.getWriteFinalDecisionEndDate());
            return !decisionNoticeStartDate.isBefore(decisionNoticeEndDate);
        }
        return false;
    }

    private boolean isDecisionNoticeDateOfDecisionInvalid(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getWriteFinalDecisionDateOfDecision() != null) {
            LocalDate decisionNoticeDecisionDate = LocalDate.parse(sscsCaseData.getWriteFinalDecisionDateOfDecision());
            LocalDate today = LocalDate.now();
            return decisionNoticeDecisionDate.isAfter(today);
        }
        return false;
    }

    @SuppressWarnings("squid:S1172")
    @Override
    protected DirectionOrDecisionIssuedTemplateBody createPayload(SscsCaseData caseData, String documentTypeLabel, LocalDate dateAdded, boolean isScottish,
        String userAuthorisation) {
        DirectionOrDecisionIssuedTemplateBody formPayload = super.createPayload(caseData, documentTypeLabel, dateAdded, isScottish, userAuthorisation);

        DirectionOrDecisionIssuedTemplateBodyBuilder builder = formPayload.toBuilder();

        builder.heldBefore(buildHeldBefore(caseData, userAuthorisation));

        if (caseData.getHearings() != null && !caseData.getHearings().isEmpty()) {
            Hearing finalHearing = caseData.getHearings().get(caseData.getHearings().size() - 1);
            if (finalHearing != null && finalHearing.getValue() != null) {
                if (finalHearing.getValue().getHearingDate() != null) {
                    builder.heldOn(LocalDate.parse(finalHearing.getValue().getHearingDate()));
                }
                if (finalHearing.getValue().getVenue() != null) {
                    builder.heldAt(finalHearing.getValue().getVenue().getName());
                }
            }
        }

        DirectionOrDecisionIssuedTemplateBody payload = builder.build();
        if (payload.getHeldAt() == null && payload.getHeldOn() == null) {
            throw new IllegalStateException("Unable to determine hearing date or venue");
        }
        else if (payload.getHeldOn() == null) {
            throw new IllegalStateException("Unable to determine hearing date");
        }
        else if (payload.getHeldAt() == null) {
            throw new IllegalStateException("Unable to determine hearing venue");
        }
        return payload;
    }

    @Override
    protected void setDocumentOnCaseData(SscsCaseData caseData, DocumentLink file) {
        caseData.setWriteFinalDecisionPreviewDocument(file);
    }


    @SuppressWarnings("squid:S1172")
    private String buildSignedInJudgeName(String userAuthorisation) {
        // FIXME
        return JUDGE_NAME_PLACEHOLDER;
    }

    private String buildHeldBefore(SscsCaseData caseData, String userAuthorisation) {
        StringBuilder stringBuilder = new StringBuilder();
        List<String> names = new ArrayList<>();
        names.add(buildSignedInJudgeName(userAuthorisation));
        if (caseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName() != null) {
            names.add(caseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        }
        if (caseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName() != null) {
            names.add(caseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        }
        return StringUtils.getGramaticallyJoinedStrings(names);
    }

    private String getGramaticallyJoinedStrings(List<String> strings) {

        StringBuffer result = new StringBuffer();
        if (strings.size() == 1) {
            return strings.get(0);
        } else if (strings.size() > 1) {
            result.append(strings.subList(0, strings.size() - 1)
                .stream().collect(Collectors.joining(", ")));
            result.append(" and ");
            result.append(strings.get(strings.size() - 1));
        }
        return result.toString();

    }
}
