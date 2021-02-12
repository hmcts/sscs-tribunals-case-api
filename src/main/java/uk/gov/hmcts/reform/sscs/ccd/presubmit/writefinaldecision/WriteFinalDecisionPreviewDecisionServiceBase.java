package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueNoticeHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody.WriteFinalDecisionTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.utility.StringUtils;

@Slf4j
public abstract class WriteFinalDecisionPreviewDecisionServiceBase extends IssueNoticeHandler {

    protected final DecisionNoticeQuestionService decisionNoticeQuestionService;
    protected final DecisionNoticeOutcomeService decisionNoticeOutcomeService;

    protected WriteFinalDecisionPreviewDecisionServiceBase(GenerateFile generateFile, IdamClient idamClient,
        DecisionNoticeQuestionService decisionNoticeQuestionService,  DecisionNoticeOutcomeService decisionNoticeOutcomeService, DocumentConfiguration documentConfiguration) {
        super(generateFile, idamClient, languagePreferenceAndBenefit -> getTemplateId(documentConfiguration, languagePreferenceAndBenefit.getRight(), languagePreferenceAndBenefit.getLeft()));
        this.decisionNoticeQuestionService = decisionNoticeQuestionService;
        this.decisionNoticeOutcomeService = decisionNoticeOutcomeService;
    }

    public abstract String getBenefitType();

    private static String getTemplateId(final DocumentConfiguration documentConfiguration, final String benefitType, final LanguagePreference languagePreference) {
        if (benefitType == null) {
            throw new IllegalStateException("Benefit type cannot be null");
        }
        Map<LanguagePreference, Map<EventType, String>> benefitSpecificDocuments = documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase());
        if (benefitSpecificDocuments == null) {
            throw new IllegalStateException("Unable to obtain benefit specific documents for benefit type:" + benefitType.toLowerCase() + " and language:" + languagePreference);
        }
        Map<EventType, String> eventTypeStringMap = benefitSpecificDocuments.get(languagePreference);
        if (eventTypeStringMap == null) {
            throw new IllegalStateException("Unable to obtain benefit specific documents for benefit type:" + benefitType.toLowerCase() + " and language:" + languagePreference);
        }
        String templateId = eventTypeStringMap.get(EventType.ISSUE_FINAL_DECISION);
        if (templateId == null) {
            throw new IllegalStateException("Unable to obtain template id for benefit type:" + benefitType + " and language:" + languagePreference);
        }
        return templateId;
    }

    @Override
    protected NoticeIssuedTemplateBody createPayload(PreSubmitCallbackResponse<SscsCaseData> response, SscsCaseData caseData, String documentTypeLabel, LocalDate dateAdded, LocalDate generatedDate, boolean isScottish,
        String userAuthorisation) {

        String benefitType = WriteFinalDecisionBenefitTypeHelper.getBenefitType(caseData);

        if (benefitType == null) {
            throw new IllegalStateException("Unable to determine benefit type");
        }

        decisionNoticeOutcomeService.performPreOutcomeIntegrityAdjustments(caseData);

        NoticeIssuedTemplateBody formPayload = super
            .createPayload(response, caseData, documentTypeLabel, dateAdded, LocalDate.parse(caseData.getWriteFinalDecisionGeneratedDate(), DateTimeFormatter.ISO_DATE), isScottish, userAuthorisation);
        WriteFinalDecisionTemplateBodyBuilder writeFinalDecisionBuilder = WriteFinalDecisionTemplateBody.builder();

        final NoticeIssuedTemplateBodyBuilder builder = formPayload.toBuilder();

        builder.userName(buildSignedInJudgeName(userAuthorisation));

        writeFinalDecisionBuilder.summaryOfOutcomeDecision(caseData.getWriteFinalDecisionDetailsOfDecision());

        writeFinalDecisionBuilder.heldBefore(buildHeldBefore(caseData, userAuthorisation));

        setHearings(writeFinalDecisionBuilder, caseData);

        Outcome outcome = decisionNoticeOutcomeService.determineOutcome(caseData);
        if (outcome == null) {
            throw new IllegalStateException("Outcome cannot be empty. Please check case data. If problem continues please contact support");
        } else {
            writeFinalDecisionBuilder.isAllowed(Outcome.DECISION_IN_FAVOUR_OF_APPELLANT.equals(outcome));
        }

        writeFinalDecisionBuilder.isSetAside(isSetAside(caseData, outcome));

        if (caseData.getWriteFinalDecisionDateOfDecision() != null) {
            writeFinalDecisionBuilder.dateOfDecision(caseData.getWriteFinalDecisionDateOfDecision());
        } else {
            writeFinalDecisionBuilder.dateOfDecision(null);
        }

        writeFinalDecisionBuilder.appellantName(buildName(caseData));
        if ("na".equals(caseData.getWriteFinalDecisionEndDateType())) {
            caseData.setWriteFinalDecisionEndDateType(null);
        }
        writeFinalDecisionBuilder.endDate(caseData.getWriteFinalDecisionEndDate());
        writeFinalDecisionBuilder.startDate(caseData.getWriteFinalDecisionStartDate());
        writeFinalDecisionBuilder.isIndefinite(caseData.getWriteFinalDecisionEndDate() == null);

        setEntitlements(writeFinalDecisionBuilder, caseData);
        setDescriptorsAndPoints(writeFinalDecisionBuilder, caseData);

        writeFinalDecisionBuilder.pageNumber(caseData.getWriteFinalDecisionPageSectionReference());
        writeFinalDecisionBuilder.detailsOfDecision(caseData.getWriteFinalDecisionDetailsOfDecision());

        if (caseData.getWriteFinalDecisionReasons() != null && !caseData.getWriteFinalDecisionReasons().isEmpty()) {
            writeFinalDecisionBuilder.reasonsForDecision(
                caseData.getWriteFinalDecisionReasons().stream().map(CollectionItem::getValue).collect(Collectors.toList()));
        } else {
            writeFinalDecisionBuilder.reasonsForDecision(null);
        }

        writeFinalDecisionBuilder.anythingElse(caseData.getWriteFinalDecisionAnythingElse());

        writeFinalDecisionBuilder.hearingType(caseData.getWriteFinalDecisionTypeOfHearing());
        writeFinalDecisionBuilder.attendedHearing("yes".equalsIgnoreCase(caseData.getWriteFinalDecisionAppellantAttendedQuestion()));
        writeFinalDecisionBuilder.presentingOfficerAttended("yes".equalsIgnoreCase(caseData.getWriteFinalDecisionPresentingOfficerAttendedQuestion()));

        WriteFinalDecisionTemplateBody payload = writeFinalDecisionBuilder.build();

        validateRequiredProperties(payload);

        if (showIssueDate) {
            builder.dateIssued(LocalDate.now());
        } else {
            builder.dateIssued(null);
        }

        builder.writeFinalDecisionTemplateBody(payload);

        setTemplateContent(decisionNoticeOutcomeService, response, builder, caseData, payload);

        return builder.build();

    }

    protected boolean isSetAside(SscsCaseData sscsCaseData, Outcome outcome) {
        return Outcome.DECISION_IN_FAVOUR_OF_APPELLANT.equals(outcome);
    }

    protected abstract void setTemplateContent(DecisionNoticeOutcomeService outcomeService, PreSubmitCallbackResponse<SscsCaseData> response,
        NoticeIssuedTemplateBodyBuilder builder, SscsCaseData caseData, WriteFinalDecisionTemplateBody payload);

    private void setHearings(WriteFinalDecisionTemplateBodyBuilder writeFinalDecisionBuilder, SscsCaseData caseData) {
        if (CollectionUtils.isNotEmpty(caseData.getHearings())) {
            Hearing finalHearing = caseData.getHearings().get(0);
            if (finalHearing != null && finalHearing.getValue() != null) {
                if (finalHearing.getValue().getHearingDate() != null) {
                    writeFinalDecisionBuilder.heldOn(LocalDate.parse(finalHearing.getValue().getHearingDate()));
                }
                if (finalHearing.getValue().getVenue() != null) {
                    writeFinalDecisionBuilder.heldAt(finalHearing.getValue().getVenue().getName());
                }
            }
        } else {
            writeFinalDecisionBuilder.heldOn(LocalDate.now());
            writeFinalDecisionBuilder.heldAt("In chambers");
        }
    }

    protected abstract void setEntitlements(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData);

    protected abstract void setDescriptorsAndPoints(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData);

    protected List<Descriptor> getDescriptorsFromQuestionKeys(ActivityQuestionLookup activityQuestionlookup, SscsCaseData caseData, List<String> questionKeys) {

        List<Descriptor> descriptors = questionKeys
            .stream().map(questionKey -> new ImmutablePair<>(questionKey,
                decisionNoticeQuestionService.getAnswerForActivityQuestionKey(caseData,
                    questionKey))).filter(pair -> pair.getRight().isPresent()).map(pair ->
                new ImmutablePair<>(pair.getLeft(), pair.getRight().get())).map(pair ->
                buildDescriptorFromActivityAnswer(activityQuestionlookup.getByKey(pair.getLeft()),
                    pair.getRight())).collect(Collectors.toList());

        descriptors.sort(new DescriptorLexicographicalComparator());

        return descriptors;
    }

    protected Descriptor buildDescriptorFromActivityAnswer(ActivityQuestion activityQuestion, ActivityAnswer answer) {
        return Descriptor.builder().activityAnswerPoints(answer.getActivityAnswerPoints())
            .activityQuestionNumber(answer.getActivityAnswerNumber())
            .activityAnswerLetter(answer.getActivityAnswerLetter())
            .activityAnswerValue(answer.getActivityAnswerValue())
            .activityQuestionValue(activityQuestion.getValue())
            .build();
    }

    private void validateRequiredProperties(WriteFinalDecisionTemplateBody payload) {
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

    protected String buildHeldBefore(SscsCaseData caseData, String userAuthorisation) {
        List<String> names = new ArrayList<>();
        String signedInJudgeName = buildSignedInJudgeName(userAuthorisation);
        if (signedInJudgeName == null) {
            throw new IllegalStateException("Unable to obtain signed in user name");
        }
        names.add(signedInJudgeName);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(caseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName())) {
            names.add(caseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(caseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName())) {
            names.add(caseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(caseData.getWriteFinalDecisionOtherPanelMemberName())) {
            names.add(caseData.getWriteFinalDecisionOtherPanelMemberName());
        }
        return StringUtils.getGramaticallyJoinedStrings(names);
    }

    @Override
    protected void setDocumentOnCaseData(SscsCaseData caseData, DocumentLink file) {
        caseData.setWriteFinalDecisionPreviewDocument(file);
    }

    @Override
    protected DocumentLink getDocumentFromCaseData(SscsCaseData caseData) {
        return caseData.getWriteFinalDecisionPreviewDocument();
    }

    @Override
    protected void setGeneratedDateIfRequired(SscsCaseData sscsCaseData, EventType eventType) {
        // Update the generated date if (and only if) the event type is Adjourn Case
        // ( not for EventType.ISSUE_FINAL_DECISION)
        if (eventType == EventType.WRITE_FINAL_DECISION) {
            sscsCaseData.setWriteFinalDecisionGeneratedDate(LocalDate.now().toString());
        }
    }
}
