package uk.gov.hmcts.reform.sscs.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.managedocuments.UploadDocumentMidEventHandler.getDocumentIdFromUrl;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.FACE_TO_FACE;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.NOT_ATTENDING;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.PAPER;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.TELEPHONE;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.VIDEO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCasePanelMembersExcluded;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitCode;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrectionActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentStaging;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingSubtype;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HmcHearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.InternalCaseDocumentData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Issue;
import uk.gov.hmcts.reform.sscs.ccd.domain.JudicialUserPanel;
import uk.gov.hmcts.reform.sscs.ccd.domain.LibertyToApplyActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberExclusions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PermissionToAppealActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SetAsideActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.StatementOfReasonsActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.UkPortOfEntry;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingDuration;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;
import uk.gov.hmcts.reform.sscs.utility.StringUtils;

@Slf4j
public class SscsUtil {
    public static final String IN_CHAMBERS = "In chambers";

    public static final String INVALID_BENEFIT_ISSUE_CODE = "Incorrect benefit/issue code combination";
    public static final String BENEFIT_CODE_NOT_IN_USE = "The benefit code selected is not in use";

    private static final String ID_FORMAT = "%s_%s";

    public static final String CATEGORY_TYPE_TEMPLATE = "%s-%03d";
    public static final String CATEGORY_SUBTYPE_TEMPLATE = "%s%s";

    private SscsUtil() {
        //
    }

    public static <T> List<T> mutableEmptyListIfNull(List<T> list) {
        return ofNullable(list).orElse(new ArrayList<>());
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

    public static void clearPostponementTransientFields(SscsCaseData sscsCaseData) {
        if (!isNull(sscsCaseData.getPostponement())) {
            sscsCaseData.setPostponement(null);
        }
        if (!isNull(sscsCaseData.getPostponementRequest())) {
            sscsCaseData.setPostponementRequest(null);
        }
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

    public static void addPanelMembersToExclusions(SscsCaseData caseData, boolean arePanelMembersReserved) {
        PanelMemberExclusions panelMemberExclusions = caseData.getSchedulingAndListingFields().getPanelMemberExclusions();

        if (isNull(panelMemberExclusions)) {
            panelMemberExclusions = PanelMemberExclusions.builder().build();
            caseData.getSchedulingAndListingFields().setPanelMemberExclusions(panelMemberExclusions);
        }

        if (caseData.getLatestHearing() != null) {
            JudicialUserPanel panel = caseData.getLatestHearing().getValue().getPanel();

            if (nonNull(panel)) {
                setAdjournmentPanelMembersExclusions(panelMemberExclusions,
                    panel.getAllPanelMembers(),
                    arePanelMembersReserved ? AdjournCasePanelMembersExcluded.RESERVED : AdjournCasePanelMembersExcluded.YES);
            }
        }
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
        addDocumentToDocumentTabAndBundle(footerService, caseData, documentLink, documentType, eventType, false);
    }


    public static void addDocumentToDocumentTabAndBundle(FooterService footerService,
                                                         SscsCaseData caseData,
                                                         DocumentLink documentLink,
                                                         DocumentType documentType,
                                                         EventType eventType,
                                                         boolean shouldBeIssued) {
        if (nonNull(documentLink)) {
            String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            SscsDocumentTranslationStatus documentTranslationStatus = getDocumentTranslationStatus(caseData);

            footerService.createFooterAndAddDocToCase(documentLink, caseData, documentType, now,
                null, null, documentTranslationStatus, eventType, shouldBeIssued);

            updateTranslationStatus(caseData, documentTranslationStatus);
        }
    }

    public static void addDocumentToCaseDataDocuments(SscsCaseData caseData, SscsDocument sscsDocument) {
        List<SscsDocument> documents = new ArrayList<>(emptyIfNull(caseData.getSscsDocument()));
        documents.addFirst(sscsDocument);
        caseData.setSscsDocument(documents);
    }

    public static void removeDocumentFromCaseDataDocuments(SscsCaseData caseData, SscsDocument sscsDocument) {
        List<SscsDocument> caseDocuments = new ArrayList<>(emptyIfNull(caseData.getSscsDocument()));
        caseDocuments = removeDocumentFromDocList(getDocumentIdFromUrl(sscsDocument), caseDocuments);
        caseData.setSscsDocument(caseDocuments);
    }

    public static void addDocumentToCaseDataInternalDocuments(SscsCaseData caseData, SscsDocument sscsDocument) {
        if (!isNull(sscsDocument.getValue().getDocumentFileName()) && sscsDocument.getValue().getDocumentFileName().startsWith("Addition ")) {
            String[] splitFileName = sscsDocument.getValue().getDocumentFileName().split("- ");
            String newFileName = String.join(" ", Arrays.copyOfRange(splitFileName, 1, splitFileName.length));
            sscsDocument.getValue().setDocumentFileName(newFileName);
        }
        sscsDocument.getValue().setBundleAddition(null);
        sscsDocument.getValue().setEvidenceIssued(null);
        InternalCaseDocumentData internalCaseDocumentData = ofNullable(caseData.getInternalCaseDocumentData())
            .orElse(InternalCaseDocumentData.builder().build());
        List<SscsDocument> documents = new ArrayList<>(emptyIfNull(internalCaseDocumentData.getSscsInternalDocument()));
        documents.addFirst(sscsDocument);
        internalCaseDocumentData.setSscsInternalDocument(documents);
        caseData.setInternalCaseDocumentData(internalCaseDocumentData);
    }

    public static void removeDocumentFromCaseDataInternalDocuments(SscsCaseData caseData, SscsDocument sscsDocument) {
        InternalCaseDocumentData internalCaseDocumentData = ofNullable(caseData.getInternalCaseDocumentData())
            .orElse(InternalCaseDocumentData.builder().build());
        List<SscsDocument> caseDocuments = new ArrayList<>(emptyIfNull(internalCaseDocumentData.getSscsInternalDocument()));
        caseDocuments = removeDocumentFromDocList(getDocumentIdFromUrl(sscsDocument), caseDocuments);
        internalCaseDocumentData.setSscsInternalDocument(caseDocuments);
        caseData.setInternalCaseDocumentData(internalCaseDocumentData);
    }

    private static List<SscsDocument> removeDocumentFromDocList(String docId, List<SscsDocument> caseDocuments) {
        return caseDocuments.stream()
            .filter(doc -> !getDocumentIdFromUrl(doc).equalsIgnoreCase(docId)).toList();
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

            if (isYes(caseData.getPostHearing().getCorrection().getIsCorrectionFinalDecisionInProgress())) {
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
            caseDetails.getCaseData().getPostHearing().getCorrection().setIsCorrectionFinalDecisionInProgress(correctionInProgress);
        }
    }

    public static boolean isCorrectionInProgress(SscsCaseData caseData, boolean isPostHearingsEnabled) {
        return isPostHearingsEnabled && isYes(caseData.getPostHearing().getCorrection().getIsCorrectionFinalDecisionInProgress());
    }

    public static boolean isOriginalDecisionNoticeUploaded(SscsCaseData sscsCaseData) {
        return isNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision());
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
        if (isNotBlank(finalDecisionCaseData.getWriteFinalDecisionOtherPanelMemberName())) {
            names.add(finalDecisionCaseData.getWriteFinalDecisionOtherPanelMemberName());
        }
        if (isNotBlank(finalDecisionCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName())) {
            names.add(finalDecisionCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        }
        if (isNotBlank(finalDecisionCaseData.getWriteFinalDecisionFinanciallyQualifiedPanelMemberName())) {
            names.add(finalDecisionCaseData.getWriteFinalDecisionFinanciallyQualifiedPanelMemberName());
        }
        return StringUtils.getGramaticallyJoinedStrings(names);
    }

    public static String buildWriteFinalDecisionHeldAt(SscsCaseData caseData, VenueDataLoader venueDataLoader) {
        if (isNotEmpty(caseData.getHearings())) {
            HearingDetails finalHearing = getLastValidHearing(caseData);
            if (nonNull(finalHearing)) {
                if (nonNull(finalHearing.getVenue())) {
                    return venueDataLoader.getGapVenueName(finalHearing.getVenue(), finalHearing.getVenueId());
                }

                return "";
            }
        }

        return IN_CHAMBERS;
    }

    public static HearingDetails getLastValidHearing(SscsCaseData caseData) {
        List<Hearing> hearings = caseData.getHearings();
        if (nonNull(hearings)) {
            for (Hearing hearing : hearings) {
                if (isHearingValid(hearing)) {
                    return hearing.getValue();
                }
            }
        }

        return null;
    }

    private static boolean isHearingValid(Hearing hearing) {
        if (nonNull(hearing)) {
            HearingDetails hearingDetails = hearing.getValue();
            return nonNull(hearingDetails)
                && isNotBlank(hearingDetails.getHearingDate())
                && nonNull(hearingDetails.getVenue())
                && isNotBlank(hearingDetails.getVenue().getName());
        }

        return false;
    }

    public static DynamicList getPortsOfEntry() {
        List<DynamicListItem> items = Arrays.stream(UkPortOfEntry.values())
            .sorted(Comparator.comparing(UkPortOfEntry::getLabel))
            .map(ukPortOfEntry -> new DynamicListItem(ukPortOfEntry.getLocationCode(), ukPortOfEntry.getLabel()))
            .toList();

        return new DynamicList(null, items);
    }

    public static DynamicListItem getPortOfEntryFromCode(String locationCode) {
        return Arrays.stream(UkPortOfEntry.values())
            .filter(ukPortOfEntry -> ukPortOfEntry.getLocationCode().equals(locationCode))
            .findFirst()
            .map(ukPortOfEntry -> new DynamicListItem(ukPortOfEntry.getLocationCode(), ukPortOfEntry.getLabel()))
            .orElseGet(() -> new DynamicListItem(null, null));
    }

    public static DynamicList getBenefitDescriptions() {
        List<DynamicListItem> items = Arrays.stream(Benefit.values())
            .sorted(Comparator.comparing(Benefit::getDescription))
            .map(SscsUtil::getBenefitDescriptionList)
            .flatMap(List::stream)
            .toList();

        return new DynamicList(null, items);
    }

    private static List<DynamicListItem> getBenefitDescriptionList(Benefit benefit) {
        return benefit.getCaseLoaderKeyId().stream()
            .map(code -> new DynamicListItem(code, benefit.getDescription() + " / " + code))
            .toList();
    }

    public static void handleBenefitType(SscsCaseData caseData) {
        Appeal appeal = caseData.getAppeal();
        if (isNull(appeal)) {
            return;
        }

        BenefitType benefitType = appeal.getBenefitType();
        if (isNull(benefitType)) {
            return;
        }

        DynamicList benefitTypeDescription = benefitType.getDescriptionSelection();
        if (isNull(benefitTypeDescription)) {
            return;
        }

        DynamicListItem selectedBenefitType = benefitTypeDescription.getValue();
        if (isNull(selectedBenefitType)) {
            return;
        }

        String code = selectedBenefitType.getCode();
        Benefit benefit = Benefit.getBenefitFromBenefitCode(code);
        benefitType.setCode(benefit.getShortName());
        benefitType.setDescription(benefit.getDescription());
        benefitType.setDescriptionSelection(null);
        caseData.setBenefitCode(code);
    }

    public static void handleIbcaCase(SscsCaseData caseData) {
        if (caseData.getAppeal().getHearingOptions() != null) {
            caseData.getAppeal().getHearingOptions().setHearingRoute(LIST_ASSIST);
        } else {
            caseData.getAppeal().setHearingOptions(HearingOptions.builder().hearingRoute(LIST_ASSIST).build());
        }
        MrnDetails mrnDetails = ofNullable(caseData.getAppeal().getMrnDetails()).orElse(MrnDetails.builder().build());
        mrnDetails.setDwpIssuingOffice("IBCA");
        caseData.getAppeal().setMrnDetails(mrnDetails);
        if (caseData.getRegionalProcessingCenter() != null) {
            RegionalProcessingCenter listAssistRegionalProcessingCenter = caseData.getRegionalProcessingCenter()
                .toBuilder()
                .hearingRoute(LIST_ASSIST)
                .build();
            caseData.setRegionalProcessingCenter(listAssistRegionalProcessingCenter);
        }
    }

    public static String generateUniqueIbcaId(Appellant appellant) {
        String appellantLastName = appellant.getName().getLastName();
        String ibcaReference = appellant.getIdentity().getIbcaReference();
        return String.format(ID_FORMAT, appellantLastName, ibcaReference);
    }

    public static void updateHearingChannel(SscsCaseData caseData, HearingChannel hearingChannel) {
        String wantsToAttend = YES.toString();
        HearingType hearingType = HearingType.ORAL;

        if (NOT_ATTENDING.equals(hearingChannel) || PAPER.equals(hearingChannel)) {
            wantsToAttend = NO.toString();
            hearingType = HearingType.PAPER;
        }

        log.info("Updating hearing type to {} and wants to attend to {}", hearingType, wantsToAttend);

        Appeal appeal = caseData.getAppeal();

        HearingOptions hearingOptions = appeal.getHearingOptions();
        if (isNull(hearingOptions)) {
            hearingOptions = HearingOptions.builder().build();
            appeal.setHearingOptions(hearingOptions);
        }

        appeal.getHearingOptions().setWantsToAttend(wantsToAttend);
        appeal.setHearingType(hearingType.getValue());

        HearingSubtype hearingSubtype = appeal.getHearingSubtype();
        if (isNull(hearingSubtype)) {
            hearingSubtype = HearingSubtype.builder().build();
            appeal.setHearingSubtype(hearingSubtype);
        }

        hearingSubtype.setWantsHearingTypeFaceToFace(hearingChannelToYesNoString(FACE_TO_FACE, hearingChannel));
        hearingSubtype.setWantsHearingTypeTelephone(hearingChannelToYesNoString(TELEPHONE, hearingChannel));
        hearingSubtype.setWantsHearingTypeVideo(hearingChannelToYesNoString(VIDEO, hearingChannel));

        caseData.getSchedulingAndListingFields().getOverrideFields().setAppellantHearingChannel(hearingChannel);
    }

    public static void updateHearingInterpreter(SscsCaseData caseData,
                                                PreSubmitCallbackResponse<SscsCaseData> response, HearingInterpreter appellantInterpreter) {
        Appeal appeal = caseData.getAppeal();

        if (nonNull(appellantInterpreter.getIsInterpreterWanted())) {

            HearingOptions hearingOptions = appeal.getHearingOptions();
            if (isNull(hearingOptions)) {
                hearingOptions = HearingOptions.builder().build();
                appeal.setHearingOptions(hearingOptions);
            }

            String interpreterWanted = appellantInterpreter.getIsInterpreterWanted().getValue();
            hearingOptions.setLanguageInterpreter(interpreterWanted);

            if (interpreterWanted.equals(YES.toString())) {
                if (nonNull(appellantInterpreter.getInterpreterLanguage()) && nonNull(appellantInterpreter.getInterpreterLanguage().getValue())) {
                    String interpreterLanguage = appellantInterpreter.getInterpreterLanguage().getValue().getLabel();
                    hearingOptions.setLanguages(interpreterLanguage);
                } else {
                    response.addError("Interpreter language must be selected if an interpreter is wanted.");
                }
            } else {
                appellantInterpreter.setInterpreterLanguage(null);
                hearingOptions.setLanguages(null);
            }
        }

        caseData.getSchedulingAndListingFields().getOverrideFields().setAppellantInterpreter(appellantInterpreter);
    }

    private static String hearingChannelToYesNoString(HearingChannel expectedHearingChannel, HearingChannel hearingChannel) {
        return expectedHearingChannel.equals(hearingChannel) ? YES.toString() : NO.toString();
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

    public static void clearPostHearingRequestFormatAndContentFields(SscsCaseData caseData, PostHearingRequestType requestType) {
        PostHearing postHearing = caseData.getPostHearing();
        DocumentGeneration docGen = caseData.getDocumentGeneration();

        switch (requestType) {
            case SET_ASIDE -> {
                postHearing.getSetAside().setRequestFormat(null);
                docGen.setBodyContent(null);
            }
            case CORRECTION -> {
                postHearing.getCorrection().setRequestFormat(null);
                docGen.setCorrectionBodyContent(null);
            }
            case STATEMENT_OF_REASONS -> {
                postHearing.getStatementOfReasons().setRequestFormat(null);
                docGen.setStatementOfReasonsBodyContent(null);
            }
            case LIBERTY_TO_APPLY -> {
                postHearing.getLibertyToApply().setRequestFormat(null);
                docGen.setLibertyToApplyBodyContent(null);
            }
            case PERMISSION_TO_APPEAL -> {
                postHearing.getPermissionToAppeal().setRequestFormat(null);
                docGen.setPermissionToAppealBodyContent(null);
            }
            case null, default -> {
            }
        }
    }

    public static String getSscsType(SscsCaseData caseData) {
        return !isNull(caseData.getBenefitCode())
            ? Benefit.getBenefitFromBenefitCode(caseData.getBenefitCode()).getSscsType().getId().toUpperCase()
            : null;
    }

    public static HmcHearingType getHmcHearingType(SscsCaseData sscsCaseData) {
        return ofNullable(sscsCaseData.getSchedulingAndListingFields())
            .map(SchedulingAndListingFields::getOverrideFields)
            .map(OverrideFields::getHmcHearingType)
            .orElse(sscsCaseData.getHmcHearingType());
    }

    public static void setHearingRouteIfNotSet(SscsCaseData sscsCaseData) {
        SchedulingAndListingFields schedulingAndListingFields =
            ofNullable(sscsCaseData.getSchedulingAndListingFields())
                .orElse(SchedulingAndListingFields.builder().build());
        if (isNull(schedulingAndListingFields.getHearingRoute())) {
            HearingRoute hearingRoute = ofNullable(sscsCaseData.getRegionalProcessingCenter())
                .orElse(RegionalProcessingCenter.builder().build()).getHearingRoute();
            schedulingAndListingFields.setHearingRoute(hearingRoute);
            sscsCaseData.setSchedulingAndListingFields(schedulingAndListingFields);
        }
    }

    public static void setListAssistRoutes(SscsCaseData sscsCaseData) {
        SchedulingAndListingFields schedulingAndListingFields = ofNullable(sscsCaseData.getSchedulingAndListingFields())
            .orElse(SchedulingAndListingFields.builder().build());
        schedulingAndListingFields.setHearingRoute(HearingRoute.LIST_ASSIST);
        sscsCaseData.setSchedulingAndListingFields(schedulingAndListingFields);

        RegionalProcessingCenter rpc = ofNullable(sscsCaseData.getRegionalProcessingCenter())
            .orElse(RegionalProcessingCenter.builder().build());
        sscsCaseData.setRegionalProcessingCenter(rpc.toBuilder().hearingRoute(HearingRoute.LIST_ASSIST).build());

        Appeal appeal = ofNullable(sscsCaseData.getAppeal()).orElse(Appeal.builder().build());
        HearingOptions hearingOptions = ofNullable(appeal.getHearingOptions())
            .orElse(HearingOptions.builder().build());
        hearingOptions.setHearingRoute(HearingRoute.LIST_ASSIST);
        appeal.setHearingOptions(hearingOptions);
        sscsCaseData.setAppeal(appeal);
    }

    public static String getCategoryTypeValue(SscsCaseData sscsCaseData) {
        return String.format(CATEGORY_TYPE_TEMPLATE, "BBA3", BenefitCode.getBenefitCode(sscsCaseData.getBenefitCode()).getCcdReference());
    }

    public static String getCategorySubTypeValue(SscsCaseData sscsCaseData) {
        return String.format(CATEGORY_SUBTYPE_TEMPLATE, getCategoryTypeValue(sscsCaseData), Issue.valueOf(sscsCaseData.getIssueCode()).name());
    }

    public static Integer getDurationForAdjournment(SscsCaseData sscsCaseData, HearingDurationsService hearingDurationsService) {
        HearingDuration hearingDuration = hearingDurationsService.getHearingDuration(sscsCaseData.getBenefitCode(), sscsCaseData.getIssueCode());
        Integer duration = AdjournCaseTypeOfHearing.PAPER.equals(sscsCaseData.getAdjournment().getTypeOfNextHearing())
                ? hearingDuration.getDurationPaper()
                : YesNo.isYes(sscsCaseData.getAdjournment().getInterpreterRequired())
                ? hearingDuration.getDurationInterpreter()
                : hearingDuration.getDurationFaceToFace();
        return duration;
    }

    public static boolean hasChannelChangedForAdjournment(SscsCaseData caseData, String hearingType) {

        if (YesNo.NO.equals(caseData.getAdjournment().getGenerateNotice())) {
            return (HearingType.ORAL.getValue().equals(hearingType) && AdjournCaseTypeOfHearing.PAPER.equals(caseData.getAdjournment().getTypeOfNextHearing()))
                    || (HearingType.PAPER.getValue().equals(hearingType) && !AdjournCaseTypeOfHearing.PAPER.equals(caseData.getAdjournment().getTypeOfNextHearing()));
        }
        return (AdjournCaseTypeOfHearing.PAPER.equals(caseData.getAdjournment().getTypeOfHearing())
                && !AdjournCaseTypeOfHearing.PAPER.equals(caseData.getAdjournment().getTypeOfNextHearing()))
                || (AdjournCaseTypeOfHearing.PAPER.equals(caseData.getAdjournment().getTypeOfNextHearing()) && !AdjournCaseTypeOfHearing.PAPER.equals(caseData.getAdjournment().getTypeOfHearing()));
    }

    public static boolean hasInterpreterChangedForAdjournment(SscsCaseData caseData, HearingOptions hearingOptions) {
        String languageInterpreter = ofNullable(hearingOptions.getLanguageInterpreter()).orElse("NO");
        return nonNull(caseData.getAdjournment().getInterpreterRequired()) && !caseData.getAdjournment().getInterpreterRequired().equals(YesNo.valueOf(languageInterpreter.toUpperCase()));
    }
}

