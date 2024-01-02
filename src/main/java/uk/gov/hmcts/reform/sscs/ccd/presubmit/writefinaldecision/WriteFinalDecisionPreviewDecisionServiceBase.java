package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isOtherPartyPresent;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.IN_CHAMBERS;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getLastValidHearing;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
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
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Slf4j
public abstract class WriteFinalDecisionPreviewDecisionServiceBase extends IssueNoticeHandler {

    protected final DecisionNoticeQuestionService decisionNoticeQuestionService;
    protected final DecisionNoticeOutcomeService decisionNoticeOutcomeService;
    protected final VenueDataLoader venueDataLoader;

    protected WriteFinalDecisionPreviewDecisionServiceBase(GenerateFile generateFile,
                                                           UserDetailsService userDetailsService,
                                                           DecisionNoticeQuestionService decisionNoticeQuestionService,
                                                           DecisionNoticeOutcomeService decisionNoticeOutcomeService,
                                                           DocumentConfiguration documentConfiguration,
                                                           VenueDataLoader venueDataLoader) {
        super(generateFile, userDetailsService, languagePreference -> getTemplateId(documentConfiguration, languagePreference), documentConfiguration);
        this.decisionNoticeQuestionService = decisionNoticeQuestionService;
        this.decisionNoticeOutcomeService = decisionNoticeOutcomeService;
        this.venueDataLoader = venueDataLoader;
    }

    public abstract String getBenefitType();

    private static String getTemplateId(final DocumentConfiguration documentConfiguration, final LanguagePreference languagePreference) {
        Map<EventType, String> eventTypeStringMap = documentConfiguration.getDocuments().get(languagePreference);
        if (eventTypeStringMap == null) {
            throw new IllegalStateException("Unable to obtain benefit specific documents for language:" + languagePreference);
        }
        String templateId = eventTypeStringMap.get(EventType.ISSUE_FINAL_DECISION);
        if (templateId == null) {
            throw new IllegalStateException("Unable to obtain template id for ISSUE_FINAL_DECISION and language:" + languagePreference);
        }
        return templateId;
    }

    @Override
    protected NoticeIssuedTemplateBody createPayload(PreSubmitCallbackResponse<SscsCaseData> response,
                                                     SscsCaseData caseData, String documentTypeLabel,
                                                     LocalDate dateAdded, LocalDate generatedDate,
                                                     boolean isScottish, boolean isPostHearingsEnabled,
                                                     boolean isPostHearingsBEnabled, String userAuthorisation) {

        String benefitType = WriteFinalDecisionBenefitTypeHelper.getBenefitType(caseData);

        if (benefitType == null) {
            throw new IllegalStateException("Unable to determine benefit type");
        }

        decisionNoticeOutcomeService.performPreOutcomeIntegrityAdjustments(caseData);

        NoticeIssuedTemplateBody formPayload = super
            .createPayload(response, caseData, documentTypeLabel, dateAdded, LocalDate.parse(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate(), DateTimeFormatter.ISO_DATE), isScottish, isPostHearingsEnabled, isPostHearingsBEnabled, userAuthorisation);

        final NoticeIssuedTemplateBodyBuilder builder = formPayload.toBuilder();

        SscsFinalDecisionCaseData finalDecisionCaseData = caseData.getSscsFinalDecisionCaseData();

        if (SscsUtil.isCorrectionInProgress(caseData, isPostHearingsEnabled)) {
            builder.dateIssued(finalDecisionCaseData.getFinalDecisionIssuedDate());
            builder.correctedJudgeName(buildSignedInJudgeName(userAuthorisation));
            builder.correctedDateIssued(showIssueDate ? LocalDate.now() : null);
            builder.idamSurname(finalDecisionCaseData.getFinalDecisionIdamSurname());
        } else {
            builder.userName(buildSignedInJudgeName(userAuthorisation));
            builder.idamSurname(buildSignedInJudgeSurname(userAuthorisation));
        }

        WriteFinalDecisionTemplateBodyBuilder writeFinalDecisionBuilder = WriteFinalDecisionTemplateBody.builder();

        setHearings(writeFinalDecisionBuilder, caseData);

        String heldAt = writeFinalDecisionBuilder.build().getHeldAt();
        String heldBefore = buildHeldBefore(caseData, userAuthorisation);

        if (isPostHearingsEnabled && nonNull(finalDecisionCaseData.getFinalDecisionHeldAt())) {
            heldAt = finalDecisionCaseData.getFinalDecisionHeldAt();
        }

        writeFinalDecisionBuilder.heldBefore(heldBefore);
        writeFinalDecisionBuilder.heldAt(heldAt);

        writeFinalDecisionBuilder.summaryOfOutcomeDecision(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDetailsOfDecision());

        Outcome outcome = decisionNoticeOutcomeService.determineOutcome(caseData);
        if (outcome == null) {
            throw new IllegalStateException("Outcome cannot be empty. Please check case data. If problem continues please contact support");
        } else {
            writeFinalDecisionBuilder.isAllowed(Outcome.DECISION_IN_FAVOUR_OF_APPELLANT.equals(outcome));
        }

        writeFinalDecisionBuilder.isSetAside(isSetAside(caseData, outcome));

        writeFinalDecisionBuilder.dateOfDecision(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision());

        writeFinalDecisionBuilder.appellantName(buildName(caseData, false));
        if ("na".equals(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType())) {
            caseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType(null);
        }

        writeFinalDecisionBuilder.appointeeName(buildName(caseData, true));

        writeFinalDecisionBuilder.endDate(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate());
        writeFinalDecisionBuilder.startDate(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate());
        writeFinalDecisionBuilder.isIndefinite(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate() == null);

        setEntitlements(writeFinalDecisionBuilder, caseData);
        setDescriptorsAndPoints(writeFinalDecisionBuilder, caseData);

        writeFinalDecisionBuilder.pageNumber(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference());
        writeFinalDecisionBuilder.detailsOfDecision(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDetailsOfDecision());

        if (caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons() != null && !caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons().isEmpty()) {
            writeFinalDecisionBuilder.reasonsForDecision(
                caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons().stream().map(CollectionItem::getValue).toList());
        } else {
            writeFinalDecisionBuilder.reasonsForDecision(null);
        }

        writeFinalDecisionBuilder.anythingElse(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAnythingElse());

        writeFinalDecisionBuilder.hearingType(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionTypeOfHearing());

        if (caseData.getAppeal().getAppellant().getIsAppointee() != null && caseData.getAppeal().getAppellant().getIsAppointee().equalsIgnoreCase("Yes")) {
            writeFinalDecisionBuilder.appointeeOnCase(true);
        }

        if (caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppointeeAttendedQuestion() != null && caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppointeeAttendedQuestion().equalsIgnoreCase("yes")) {
            writeFinalDecisionBuilder.appointeeAttended(true);
        }

        if (caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion() != null && caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion().equalsIgnoreCase("yes")) {
            writeFinalDecisionBuilder.attendedHearing(true);
        }

        writeFinalDecisionBuilder.presentingOfficerAttended("yes".equalsIgnoreCase(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPresentingOfficerAttendedQuestion()));
        if (isOtherPartyPresent(caseData) && CollectionUtils.isNotEmpty(caseData.getSscsFinalDecisionCaseData().getOtherPartyAttendedQuestions())) {
            writeFinalDecisionBuilder.otherPartyNamesAttendedHearing(caseData.getSscsFinalDecisionCaseData().getOtherPartyAttendedQuestions().stream()
                            .filter(aq -> YesNo.YES.equals(aq.getValue().getAttendedOtherParty()))
                            .map(aq -> aq.getValue().getOtherPartyName())
                            .collect(Collectors.joining(", ")));
        }

        Optional<Benefit> benefit = caseData.getBenefitType();
        if (benefit.isPresent()) {
            if (benefit.get().getSscsType().equals(SscsType.SSCS5)) {
                writeFinalDecisionBuilder.isHmrc(true);
            } else {
                writeFinalDecisionBuilder.isHmrc(false);
            }
        }

        WriteFinalDecisionTemplateBody payload = writeFinalDecisionBuilder.build();

        validateRequiredProperties(payload);

        if (SscsUtil.isCorrectionInProgress(caseData, isPostHearingsEnabled)) {
            builder.dateIssued(caseData.getSscsFinalDecisionCaseData().getFinalDecisionIssuedDate());
        } else if (showIssueDate) {
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
        String heldAt = SscsUtil.buildWriteFinalDecisionHeldAt(caseData, venueDataLoader);

        if (!IN_CHAMBERS.equals(heldAt)) {
            HearingDetails finalHearing = getLastValidHearing(caseData);

            if (nonNull(finalHearing) && nonNull(finalHearing.getHearingDate())) {
                writeFinalDecisionBuilder.heldOn(LocalDate.parse(finalHearing.getHearingDate()));
            }
        } else {
            writeFinalDecisionBuilder.heldOn(LocalDate.now());
        }

        writeFinalDecisionBuilder.heldAt(heldAt);
    }

    protected abstract void setEntitlements(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData);

    protected abstract void setDescriptorsAndPoints(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData);

    protected List<Descriptor> getDescriptorsFromQuestionKeys(ActivityQuestionLookup activityQuestionlookup, SscsCaseData caseData, List<String> questionKeys) {
        return questionKeys.stream()
            .map(questionKey ->
                new ImmutablePair<>(questionKey,
                    decisionNoticeQuestionService.getAnswerForActivityQuestionKey(caseData, questionKey)))
            .filter(pair -> pair.getRight().isPresent())
            .map(pair -> new ImmutablePair<>(pair.getLeft(), pair.getRight().get()))
            .map(pair ->
                buildDescriptorFromActivityAnswer(activityQuestionlookup.getByKey(pair.getLeft()), pair.getRight()))
            .sorted(new DescriptorLexicographicalComparator())
            .toList();
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
        String judgeName = caseData.getSscsFinalDecisionCaseData().getFinalDecisionJudge();

        if (isNull(judgeName)) {
            judgeName = buildSignedInJudgeName(userAuthorisation);
        }

        return SscsUtil.buildWriteFinalDecisionHeldBefore(caseData, judgeName);
    }

    @Override
    protected void setDocumentOnCaseData(SscsCaseData caseData, DocumentLink file) {
        caseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(file);
    }

    @Override
    protected DocumentLink getDocumentFromCaseData(SscsCaseData caseData) {
        return caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument();
    }

    @Override
    protected void setGeneratedDateIfRequired(SscsCaseData sscsCaseData, EventType eventType) {
        // Update the generated date if (and only if) the event type is Adjourn Case
        // ( not for EventType.ISSUE_FINAL_DECISION)
        if (eventType == EventType.WRITE_FINAL_DECISION) {
            sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGeneratedDate(LocalDate.now().toString());
        }
    }
}
