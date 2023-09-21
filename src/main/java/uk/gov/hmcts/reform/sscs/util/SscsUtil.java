package uk.gov.hmcts.reform.sscs.util;

import static io.micrometer.core.instrument.util.StringUtils.isNotBlank;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;
import uk.gov.hmcts.reform.sscs.utility.StringUtils;

@Slf4j
public class SscsUtil {

    public static final String INVALID_BENEFIT_ISSUE_CODE = "Incorrect benefit/issue code combination";
    public static final String BENEFIT_CODE_NOT_IN_USE = "The benefit code selected is not in use";

    private SscsUtil() {
        //
    }

    public static <T> List<T> mutableEmptyListIfNull(List<T> list) {
        return Optional.ofNullable(list).orElse(new ArrayList<>());
    }

    public static boolean isSAndLCase(SscsCaseData sscsCaseData) {
        return LIST_ASSIST == Optional.of(sscsCaseData)
            .map(SscsCaseData::getSchedulingAndListingFields)
            .map(SchedulingAndListingFields::getHearingRoute)
            .orElse(null);
    }

    public static boolean isValidCaseState(State state, List<State> allowedStates) {
        return allowedStates.contains(state);
    }

    public static void clearAdjournmentTransientFields(SscsCaseData caseData) {
        log.info("Clearing transient adjournment case fields for caseId {}", caseData.getCcdCaseId());

        caseData.setAdjournment(Adjournment.builder().build());
    }

    public static void clearPostHearingFields(SscsCaseData caseData, boolean isPostHearingsEnabled) {
        if (isPostHearingsEnabled) {
            caseData.setPostHearing(PostHearing.builder().build());
        }
        clearDocumentTransientFields(caseData);
    }

    public static void clearDocumentTransientFields(SscsCaseData caseData) {
        caseData.setDocumentGeneration(DocumentGeneration.builder().build());
        caseData.setDocumentStaging(DocumentStaging.builder().build());
    }

    public static void setAdjournmentPanelMembersExclusions(PanelMemberExclusions exclusions,
                                           List<JudicialUserBase> adjournmentPanelMembers,
                                           AdjournCasePanelMembersExcluded panelMemberExcluded) {
        if (nonNull(adjournmentPanelMembers)) {
            List<CollectionItem<JudicialUserBase>> panelMembersList = getPanelMembersList(exclusions, panelMemberExcluded);


            if (isNull(panelMembersList)) {
                panelMembersList = new LinkedList<>();
            }

            panelMembersList.addAll(adjournmentPanelMembers.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(panelMember -> new CollectionItem<>(panelMember.getIdamId(), panelMember))
                .filter(not(panelMembersList::contains))
                .toList());

            if (panelMemberExcluded.equals(AdjournCasePanelMembersExcluded.YES)) {
                log.info("Excluding {} panel members with Personal Codes {}", adjournmentPanelMembers.size(),
                    adjournmentPanelMembers.stream()
                            .filter(Objects::nonNull)
                            .map(JudicialUserBase::getPersonalCode)
                            .toList());

                exclusions.setExcludedPanelMembers(panelMembersList);
                exclusions.setArePanelMembersExcluded(YES);
            } else if (panelMemberExcluded.equals(AdjournCasePanelMembersExcluded.RESERVED)) {
                log.info("Reserving {} panel members with Personal Codes {}", adjournmentPanelMembers.size(),
                    adjournmentPanelMembers.stream()
                            .filter(Objects::nonNull)
                            .map(JudicialUserBase::getPersonalCode)
                            .toList());

                exclusions.setReservedPanelMembers(panelMembersList);
                exclusions.setArePanelMembersReserved(YES);
            }
        }
    }

    private static List<CollectionItem<JudicialUserBase>> getPanelMembersList(PanelMemberExclusions exclusions,
                                                                        AdjournCasePanelMembersExcluded panelMemberExcluded) {
        if (panelMemberExcluded.equals(AdjournCasePanelMembersExcluded.YES)) {
            return exclusions.getExcludedPanelMembers();
        }
        if (panelMemberExcluded.equals(AdjournCasePanelMembersExcluded.RESERVED)) {
            return exclusions.getReservedPanelMembers();
        }

        return new LinkedList<>();
    }

    public static void addDocumentToDocumentTabAndBundle(FooterService footerService,
                                                         SscsCaseData caseData,
                                                         DocumentLink documentLink,
                                                         DocumentType documentType) {
        addDocumentToDocumentTabAndBundle(footerService, caseData, documentLink, documentType, null);

    }

    public static void addDocumentToDocumentTabAndBundle(FooterService footerService,
                                                         SscsCaseData caseData,
                                                         DocumentLink documentLink,
                                                         DocumentType documentType,
                                                         EventType eventType) {
        if (nonNull(documentLink)) {
            String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            SscsDocumentTranslationStatus documentTranslationStatus = getDocumentTranslationStatus(caseData);

            footerService.createFooterAndAddDocToCase(documentLink, caseData, documentType, now,
                null, null, documentTranslationStatus, eventType);

            updateTranslationStatus(caseData, documentTranslationStatus);
        }
    }

    public static void addDocumentToCaseDataDocuments(SscsCaseData caseData, SscsDocument sscsDocument) {
        List<SscsDocument> documents = new ArrayList<>();
        documents.add(sscsDocument);

        if (caseData.getSscsDocument() != null) {
            documents.addAll(caseData.getSscsDocument());
        }
        caseData.setSscsDocument(documents);
    }

    public static void addDocumentToBundle(FooterService footerService, SscsCaseData sscsCaseData, SscsDocument sscsDocument) {
        DocumentLink url = sscsDocument.getValue().getDocumentLink();
        DocumentType documentType = DocumentType.fromValue(sscsDocument.getValue().getDocumentType());
        String dateIssued = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        footerService.createFooterAndAddDocToCase(url, sscsCaseData, documentType, dateIssued, null, null, null);
    }

    public static DocumentType getPostHearingReviewDocumentType(PostHearing postHearing, boolean isPostHearingsEnabled) {
        if (isPostHearingsEnabled && nonNull(postHearing.getReviewType())) {
            return getPostHearingReviewDocumentType(postHearing);
        }

        return DocumentType.DECISION_NOTICE;
    }

    private static DocumentType getPostHearingReviewDocumentType(PostHearing postHearing) {
        PostHearingReviewType postHearingReviewType = postHearing.getReviewType();
        switch (postHearingReviewType) {
            case SET_ASIDE -> {
                if (SetAsideActions.REFUSE.equals(postHearing.getSetAside().getAction())) {
                    return DocumentType.SET_ASIDE_REFUSED;
                }

                return DocumentType.SET_ASIDE_GRANTED;
            }
            case CORRECTION -> {
                if (CorrectionActions.REFUSE.equals(postHearing.getCorrection().getAction())) {
                    return DocumentType.CORRECTION_REFUSED;
                }
            }
            case STATEMENT_OF_REASONS -> {
                if (StatementOfReasonsActions.REFUSE.equals(postHearing.getStatementOfReasons().getAction())) {
                    return DocumentType.STATEMENT_OF_REASONS_REFUSED;
                }
                return DocumentType.STATEMENT_OF_REASONS_GRANTED;
            }
            case LIBERTY_TO_APPLY -> {
                if (LibertyToApplyActions.REFUSE.equals(postHearing.getLibertyToApply().getAction())) {
                    return DocumentType.LIBERTY_TO_APPLY_REFUSED;
                }
                return DocumentType.LIBERTY_TO_APPLY_GRANTED;
            }
            case PERMISSION_TO_APPEAL -> {
                if (PermissionToAppealActions.REFUSE.equals(postHearing.getPermissionToAppeal().getAction())) {
                    return DocumentType.PERMISSION_TO_APPEAL_REFUSED;
                } else if (PermissionToAppealActions.REVIEW.equals(postHearing.getPermissionToAppeal().getAction())) {
                    return DocumentType.REVIEW_AND_SET_ASIDE;
                }
                return DocumentType.PERMISSION_TO_APPEAL_GRANTED;
            }
            default -> {
            }
        }

        throw new IllegalArgumentException("getting the document type has an unexpected postHearingReviewType and action");
    }

    private static SscsDocumentTranslationStatus getDocumentTranslationStatus(SscsCaseData caseData) {
        return caseData.isLanguagePreferenceWelsh() ? SscsDocumentTranslationStatus.TRANSLATION_REQUIRED : null;
    }

    private static void updateTranslationStatus(SscsCaseData caseData, SscsDocumentTranslationStatus documentTranslationStatus) {
        if (documentTranslationStatus != null) {
            caseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION);
            log.info("Set the InterlocReviewState to {},  for case id : {}", caseData.getInterlocReviewState(), caseData.getCcdCaseId());
            caseData.setTranslationWorkOutstanding(YES.getValue());
        }
    }

    public static DocumentType getWriteFinalDecisionDocumentType(SscsCaseData caseData, boolean isPostHearingsEnabled) {
        return getWriteFinalDecisionDocumentType(caseData, null, isPostHearingsEnabled);
    }

    public static DocumentType getWriteFinalDecisionDocumentType(SscsCaseData caseData, EventType event, boolean isPostHearingsEnabled) {
        if (isPostHearingsEnabled) {
            if (EventType.ADMIN_ACTION_CORRECTION.equals(event)) {
                return DocumentType.CORRECTED_DECISION_NOTICE;
            }

            if (isYes(caseData.getPostHearing().getCorrection().getCorrectionFinalDecisionInProgress())) {
                return DocumentType.DRAFT_CORRECTED_NOTICE;
            }
        }

        return DocumentType.DRAFT_DECISION_NOTICE;
    }

    public static DocumentType getIssueFinalDecisionDocumentType(SscsCaseData caseData, boolean isPostHearingsEnabled) {
        return getIssueFinalDecisionDocumentType(caseData, null, isPostHearingsEnabled);
    }

    public static DocumentType getIssueFinalDecisionDocumentType(SscsCaseData caseData, EventType event, boolean isPostHearingsEnabled) {
        if (isPostHearingsEnabled) {
            if (EventType.ADMIN_ACTION_CORRECTION.equals(event)) {
                return DocumentType.CORRECTED_DECISION_NOTICE;
            }

            if (isCorrectionInProgress(caseData, true)) {
                return DocumentType.CORRECTION_GRANTED;
            }
        }

        return DocumentType.FINAL_DECISION_NOTICE;
    }

    public static void setCorrectionInProgress(CaseDetails<SscsCaseData> caseDetails, boolean isPostHearingsEnabled) {
        if (isPostHearingsEnabled) {
            YesNo correctionInProgress = State.POST_HEARING.equals(caseDetails.getState()) || State.DORMANT_APPEAL_STATE.equals(caseDetails.getState()) ? YES : NO;
            caseDetails.getCaseData().getPostHearing().getCorrection().setCorrectionFinalDecisionInProgress(correctionInProgress);
        }
    }

    public static boolean isCorrectionInProgress(SscsCaseData caseData, boolean isPostHearingsEnabled) {
        return isPostHearingsEnabled && isYes(caseData.getPostHearing().getCorrection().getCorrectionFinalDecisionInProgress());
    }
      
    public static boolean isGapsCase(SscsCaseData sscsCaseData) {
        return GAPS.equals(sscsCaseData.getSchedulingAndListingFields().getHearingRoute());
    }

    public static void validateBenefitIssueCode(SscsCaseData caseData,
                                                PreSubmitCallbackResponse<SscsCaseData> response,
                                                SessionCategoryMapService categoryMapService) {
        boolean isSecondDoctorPresent = isNotBlank(caseData.getSscsIndustrialInjuriesData().getSecondPanelDoctorSpecialism());
        boolean fqpmRequired = isYes(caseData.getIsFqpmRequired());


        if (isNull(Benefit.getBenefitFromBenefitCode(caseData.getBenefitCode()))) {
            response.addError(BENEFIT_CODE_NOT_IN_USE);
        }


        if (isNull(categoryMapService.getSessionCategory(caseData.getBenefitCode(), caseData.getIssueCode(),
                isSecondDoctorPresent, fqpmRequired))) {
            response.addError(INVALID_BENEFIT_ISSUE_CODE);
        }
    }
  
    public static String buildWriteFinalDecisionHeldBefore(SscsCaseData caseData, @NonNull String signedInJudgeName) {
        List<String> names = new ArrayList<>();
        names.add(signedInJudgeName);
        SscsFinalDecisionCaseData finalDecisionCaseData = caseData.getSscsFinalDecisionCaseData();
        if (isNotBlank(finalDecisionCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName())) {
            names.add(finalDecisionCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        }
        if (isNotBlank(finalDecisionCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName())) {
            names.add(finalDecisionCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        }
        if (isNotBlank(finalDecisionCaseData.getWriteFinalDecisionOtherPanelMemberName())) {
            names.add(finalDecisionCaseData.getWriteFinalDecisionOtherPanelMemberName());
        }
        return StringUtils.getGramaticallyJoinedStrings(names);
    }

    public static String buildWriteFinalDecisionHeldAt(SscsCaseData caseData, VenueDataLoader venueDataLoader) {
        if (CollectionUtils.isNotEmpty(caseData.getHearings())) {
            HearingDetails finalHearing = getLastValidHearing(caseData);
            if (nonNull(finalHearing)) {
                if (nonNull(finalHearing.getVenue())) {
                    return venueDataLoader.getGapVenueName(finalHearing.getVenue(), finalHearing.getVenueId());
                }

                return "";
            }
        }

        return "In chambers";
    }

    public static HearingDetails getLastValidHearing(SscsCaseData caseData) {
        for (Hearing hearing : caseData.getHearings()) {
            if (hearing != null) {
                HearingDetails hearingDetails = hearing.getValue();
                if (hearingDetails != null
                        && org.apache.commons.lang3.StringUtils.isNotBlank(hearingDetails.getHearingDate())
                        && hearingDetails.getVenue() != null
                        && org.apache.commons.lang3.StringUtils.isNotBlank(hearingDetails.getVenue().getName())) {
                    return hearingDetails;
                }
            }
        }
        return null;
    }

    public static void createFinalDecisionNoticeFromPreviewDraft(Callback<SscsCaseData> callback,
                                                                 FooterService footerService,
                                                                 boolean isPostHearingsEnabled) {
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        DocumentLink docLink = sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument();

        DocumentLink documentLink = DocumentLink.builder()
                .documentUrl(docLink.getDocumentUrl())
                .documentFilename(docLink.getDocumentFilename())
                .documentBinaryUrl(docLink.getDocumentBinaryUrl())
                .build();

        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        DocumentType docType = SscsUtil.getIssueFinalDecisionDocumentType(sscsCaseData, callback.getEvent(), isPostHearingsEnabled);

        final SscsDocumentTranslationStatus documentTranslationStatus = sscsCaseData.isLanguagePreferenceWelsh() ? TRANSLATION_REQUIRED : null;
        footerService.createFooterAndAddDocToCase(documentLink, sscsCaseData, docType, now, null, null, documentTranslationStatus);

        if (nonNull(documentTranslationStatus)) {
            sscsCaseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION);
            log.info("Set the InterlocReviewState to {},  for case id : {}", sscsCaseData.getInterlocReviewState(), sscsCaseData.getCcdCaseId());
            sscsCaseData.setTranslationWorkOutstanding(YES.getValue());
        }
    }
}
