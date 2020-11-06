package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueNoticeHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipActivityType;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody.WriteFinalDecisionTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.utility.StringUtils;

@Component
@Slf4j
public class WriteFinalDecisionPreviewDecisionService extends IssueNoticeHandler {

    private final DecisionNoticeService decisionNoticeService;

    @Autowired
    public WriteFinalDecisionPreviewDecisionService(GenerateFile generateFile, IdamClient idamClient,
        DecisionNoticeService decisionNoticeService,  DocumentConfiguration documentConfiguration) {
        super(generateFile, idamClient, languagePreference -> getTemplateId(documentConfiguration, languagePreference));
        this.decisionNoticeService = decisionNoticeService;
    }

    private static String getTemplateId(final DocumentConfiguration documentConfiguration, final LanguagePreference languagePreference) {
        return documentConfiguration.getDocuments().get(languagePreference).get(EventType.ISSUE_FINAL_DECISION);
    }

    @Override
    protected NoticeIssuedTemplateBody createPayload(SscsCaseData caseData, String documentTypeLabel, LocalDate dateAdded, LocalDate generatedDate, boolean isScottish,
        String userAuthorisation) {
        NoticeIssuedTemplateBody formPayload = super
            .createPayload(caseData, documentTypeLabel, dateAdded, LocalDate.parse(caseData.getWriteFinalDecisionGeneratedDate(), DateTimeFormatter.ISO_DATE), isScottish, userAuthorisation);
        WriteFinalDecisionTemplateBodyBuilder writeFinalDecisionBuilder = WriteFinalDecisionTemplateBody.builder();

        final NoticeIssuedTemplateBodyBuilder builder = formPayload.toBuilder();

        builder.userName(buildSignedInJudgeName(userAuthorisation));

        writeFinalDecisionBuilder.isDescriptorFlow(caseData.isDailyLivingAndOrMobilityDecision());

        writeFinalDecisionBuilder.heldBefore(buildHeldBefore(caseData, userAuthorisation));

        setHearings(writeFinalDecisionBuilder, caseData);
       
        String benefitType = caseData.getAppeal().getBenefitType() == null ? null : caseData.getAppeal().getBenefitType().getCode();

        if (benefitType == null) {
            throw new IllegalStateException("Unable to determine benefit type");
        }

        Outcome outcome = decisionNoticeService.getOutcomeService(benefitType).determineOutcome(caseData);
        if (outcome == null) {
            throw new IllegalStateException("Outcome cannot be empty. Please check case data. If problem continues please contact support");
        } else {
            writeFinalDecisionBuilder.isAllowed(Outcome.DECISION_IN_FAVOUR_OF_APPELLANT.equals(outcome));
        }

        if ("yes".equalsIgnoreCase((caseData.getWriteFinalDecisionIsDescriptorFlow()))
            && "yes".equalsIgnoreCase(caseData.getWriteFinalDecisionGenerateNotice())) {

            boolean isSetAside = getConsideredComparisonsWithDwp(caseData).stream().anyMatch(comparission -> !"same".equalsIgnoreCase(comparission));

            writeFinalDecisionBuilder.isSetAside(isSetAside);

        } else {
            writeFinalDecisionBuilder.isSetAside(Outcome.DECISION_IN_FAVOUR_OF_APPELLANT.equals(outcome));
        }

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

        if ("PIP".equals(benefitType)) {
            setPipEntitlements(writeFinalDecisionBuilder, caseData);
            setPipDescriptorsAndPoints(writeFinalDecisionBuilder, caseData);
        }

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

        return builder.build();

    }

    private List<String> getConsideredComparisonsWithDwp(SscsCaseData caseData) {
        List<String> consideredComparissons = new ArrayList<>();
        if (!AwardType.NOT_CONSIDERED.getKey().equalsIgnoreCase(caseData.getPipWriteFinalDecisionDailyLivingQuestion())) {
            consideredComparissons.add(caseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        }
        if (!AwardType.NOT_CONSIDERED.getKey().equalsIgnoreCase(caseData.getPipWriteFinalDecisionMobilityQuestion())) {
            consideredComparissons.add(caseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        }
        return consideredComparissons;
    }

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

    private void setPipEntitlements(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData) {

        String dailyLivingAwardType = caseData.getPipWriteFinalDecisionDailyLivingQuestion();
        String mobilityAwardType = caseData.getPipWriteFinalDecisionMobilityQuestion();

        if (dailyLivingAwardType != null) {
            builder.dailyLivingAwardRate(join(
                splitByCharacterTypeCamelCase(dailyLivingAwardType), ' ').toLowerCase());
        } else {
            builder.dailyLivingAwardRate(null);
        }

        if (AwardType.ENHANCED_RATE.getKey().equals(dailyLivingAwardType)) {
            builder.dailyLivingIsEntited(true);
            builder.dailyLivingIsSeverelyLimited(true);
        } else if (AwardType.STANDARD_RATE.getKey().equals(dailyLivingAwardType)) {
            builder.dailyLivingIsEntited(true);
            builder.dailyLivingIsSeverelyLimited(false);
        } else {
            builder.dailyLivingIsEntited(false);
            builder.dailyLivingIsSeverelyLimited(false);
        }

        if (mobilityAwardType != null) {
            builder.mobilityAwardRate(join(
                splitByCharacterTypeCamelCase(mobilityAwardType), ' ').toLowerCase());
        } else {
            builder.mobilityAwardRate(null);
        }

        if (AwardType.ENHANCED_RATE.getKey().equals(mobilityAwardType)) {
            builder.mobilityIsEntited(true);
            builder.mobilityIsSeverelyLimited(true);
        } else if (AwardType.STANDARD_RATE.getKey().equals(mobilityAwardType)) {
            builder.mobilityIsEntited(true);
            builder.mobilityIsSeverelyLimited(false);
        } else {
            builder.mobilityIsEntited(false);
            builder.mobilityIsSeverelyLimited(false);
        }
    }

    protected void setPipDescriptorsAndPoints(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData) {
        List<String> dailyLivingAnswers = PipActivityType.DAILY_LIVING.getAnswersExtractor().apply(caseData);
        if (dailyLivingAnswers != null && !AwardType.NOT_CONSIDERED.getKey().equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion())) {

            List<Descriptor> dailyLivingDescriptors = getPipDescriptorsFromQuestionKeys(caseData, dailyLivingAnswers);

            builder.dailyLivingNumberOfPoints(dailyLivingDescriptors.stream().mapToInt(Descriptor::getActivityAnswerPoints).sum());

            builder.dailyLivingDescriptors(dailyLivingDescriptors);
        } else {
            builder.dailyLivingDescriptors(null);
            builder.dailyLivingNumberOfPoints(null);
        }

        List<String> mobilityAnswers = PipActivityType.MOBILITY.getAnswersExtractor().apply(caseData);
        if (mobilityAnswers != null && !AwardType.NOT_CONSIDERED.getKey().equals(caseData.getPipWriteFinalDecisionMobilityQuestion())) {
            List<Descriptor> mobilityDescriptors = getPipDescriptorsFromQuestionKeys(caseData, mobilityAnswers);

            builder.mobilityDescriptors(mobilityDescriptors);

            builder.mobilityNumberOfPoints(mobilityDescriptors.stream().mapToInt(Descriptor::getActivityAnswerPoints).sum());
        } else {
            builder.mobilityDescriptors(null);
            builder.mobilityNumberOfPoints(null);
        }
    }

    protected List<Descriptor> getPipDescriptorsFromQuestionKeys(SscsCaseData caseData, List<String> questionKeys) {
        return getDescriptorsFromQuestionKeys("PIP", PipActivityQuestion::getByKey, caseData, questionKeys);
    }

    protected List<Descriptor> getDescriptorsFromQuestionKeys(String benefitType, ActivityQuestionLookup activityQuestionlookup, SscsCaseData caseData, List<String> questionKeys) {

        DecisionNoticeQuestionService decisionNoticeQuestionService = decisionNoticeService.getQuestionService(benefitType);

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
