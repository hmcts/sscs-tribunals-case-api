package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.StringUtils.isBlank;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.DirectionOrDecisionIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.DirectionOrDecisionIssuedTemplateBody.DirectionOrDecisionIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.util.StringUtils;

@Component
@Slf4j
public class WriteFinalDecisionMidEventHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final GenerateFile generateFile;
    private final String templateId;
    private final IdamClient idamClient;
    private final DecisionNoticeOutcomeService decisionNoticeOutcomeService;

    @Autowired
    public WriteFinalDecisionMidEventHandler(GenerateFile generateFile, IdamClient idamClient, DecisionNoticeOutcomeService decisionNoticeOutcomeService, @Value("${doc_assembly.issue_final_decision}") String templateId) {
        this.generateFile = generateFile;
        this.templateId = templateId;
        this.idamClient = idamClient;
        this.decisionNoticeOutcomeService = decisionNoticeOutcomeService;
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
                log.error(e.getMessage() + ". Something has gone wrong for caseId: ", sscsCaseData.getCcdCaseId());
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
        if (!isBlank(sscsCaseData.getWriteFinalDecisionDateOfDecision())) {
            LocalDate decisionNoticeDecisionDate = LocalDate.parse(sscsCaseData.getWriteFinalDecisionDateOfDecision());
            LocalDate today = LocalDate.now();
            return decisionNoticeDecisionDate.isAfter(today);
        }
        return false;
    }

    @Override
    protected DirectionOrDecisionIssuedTemplateBody createPayload(SscsCaseData caseData, String documentTypeLabel, LocalDate dateAdded, boolean isScottish,
        String userAuthorisation) {
        DirectionOrDecisionIssuedTemplateBody formPayload = super.createPayload(caseData, documentTypeLabel, dateAdded, isScottish, userAuthorisation);

        DirectionOrDecisionIssuedTemplateBodyBuilder builder = formPayload.toBuilder();

        builder.heldBefore(buildHeldBefore(caseData, userAuthorisation));

        if (CollectionUtils.isNotEmpty(caseData.getHearings())) {
            Hearing finalHearing = caseData.getHearings().get(0);
            if (finalHearing != null && finalHearing.getValue() != null) {
                if (finalHearing.getValue().getHearingDate() != null) {
                    builder.heldOn(LocalDate.parse(finalHearing.getValue().getHearingDate()));
                }
                if (finalHearing.getValue().getVenue() != null) {
                    builder.heldAt(finalHearing.getValue().getVenue().getName());
                }
            }
        } else {
            builder.heldOn(LocalDate.now());
            builder.heldAt("In-chambers");
        }

        Outcome outcome = decisionNoticeOutcomeService.determineOutcome(caseData);
        if (outcome == null) {
            throw new IllegalStateException("Outcome cannot be empty. Please check case data. If problem continues please contact support");
        } else {
            builder.isAllowed(Outcome.DECISION_IN_FAVOUR_OF_APPELLANT.equals(outcome));
            builder.isSetAside(Outcome.DECISION_IN_FAVOUR_OF_APPELLANT.equals(outcome));
        }

        if (caseData.getWriteFinalDecisionDateOfDecision() != null) {
            builder.dateOfDecision(caseData.getWriteFinalDecisionDateOfDecision());
        }

        DirectionOrDecisionIssuedTemplateBody payload = builder.build();
        validateRequiredProperties(payload);
        return payload;
    }


    private void validateRequiredProperties(DirectionOrDecisionIssuedTemplateBody payload) {
        if (payload.getHeldAt() == null && payload.getHeldOn() == null) {
            throw new IllegalStateException("Unable to determine hearing date or venue");
        } else if (payload.getHeldOn() == null) {
            throw new IllegalStateException("Unable to determine hearing date");
        } else if (payload.getHeldAt() == null) {
            throw new IllegalStateException("Unable to determine hearing venue");
        }
        if (payload.getDateOfDecision() == null) {
            throw new IllegalStateException("Unable to determine date of decision");
        }
    }

    @Override
    protected void setDocumentOnCaseData(SscsCaseData caseData, DocumentLink file) {
        caseData.setWriteFinalDecisionPreviewDocument(file);
    }


    private String buildSignedInJudgeName(String userAuthorisation) {
        UserDetails userDetails = idamClient.getUserDetails(userAuthorisation);
        if (userDetails == null) {
            throw new IllegalStateException("Unable to obtain signed in user details");
        }
        return userDetails.getFullName();
    }

    private String buildHeldBefore(SscsCaseData caseData, String userAuthorisation) {
        List<String> names = new ArrayList<>();
        String signedInJudgeName = buildSignedInJudgeName(userAuthorisation);
        if (signedInJudgeName == null) {
            throw new IllegalStateException("Unable to obtain signed in user name");
        }
        names.add(signedInJudgeName);
        if (caseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName() != null) {
            names.add(caseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        }
        if (caseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName() != null) {
            names.add(caseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        }
        return StringUtils.getGramaticallyJoinedStrings(names);
    }
}
