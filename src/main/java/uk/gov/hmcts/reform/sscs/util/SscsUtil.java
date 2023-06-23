package uk.gov.hmcts.reform.sscs.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Slf4j
public class SscsUtil {

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

    public static void clearPostHearingFields(SscsCaseData caseData) {
        caseData.setPostHearing(null);
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
                .collect(Collectors.toList()));

            if (panelMemberExcluded.equals(AdjournCasePanelMembersExcluded.YES)) {
                log.info("Excluding {} panel members with Personal Codes {}", adjournmentPanelMembers.size(),
                    adjournmentPanelMembers.stream().map(JudicialUserBase::getPersonalCode).collect(Collectors.toList()));

                exclusions.setExcludedPanelMembers(panelMembersList);
                exclusions.setArePanelMembersExcluded(YES);
            } else if (panelMemberExcluded.equals(AdjournCasePanelMembersExcluded.RESERVED)) {
                log.info("Reserving {} panel members with Personal Codes {}", adjournmentPanelMembers.size(),
                    adjournmentPanelMembers.stream().map(JudicialUserBase::getPersonalCode).collect(Collectors.toList()));

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

    public static void addDocumentToDocumentTabAndBundle(FooterService footerService, SscsCaseData caseData, DocumentType documentType) {
        DocumentLink url = caseData.getDocumentStaging().getPreviewDocument();

        if (nonNull(url)) {
            String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            SscsDocumentTranslationStatus documentTranslationStatus = getDocumentTranslationStatus(caseData);

            footerService.createFooterAndAddDocToCase(url, caseData, documentType, now,
                null, null, documentTranslationStatus);

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
        PostHearingReviewType postHearingReviewType = postHearing.getReviewType();
        if (isPostHearingsEnabled && nonNull(postHearingReviewType)) {
            switch (postHearingReviewType) {
                case SET_ASIDE:
                    if (SetAsideActions.REFUSE.equals(postHearing.getSetAside().getAction())) {
                        return DocumentType.SET_ASIDE_REFUSED;
                    }
                    break;
                case CORRECTION:
                    if (CorrectionActions.REFUSE.equals(postHearing.getCorrection().getAction())) {
                        return DocumentType.CORRECTION_REFUSED;
                    }
                    break;
                case STATEMENT_OF_REASONS:
                    if (StatementOfReasonsActions.REFUSE.equals(postHearing.getStatementOfReasons().getAction())) {
                        return DocumentType.STATEMENT_OF_REASONS_REFUSED;
                    }

                    return DocumentType.STATEMENT_OF_REASONS_GRANTED;
                case LIBERTY_TO_APPLY:
                case PERMISSION_TO_APPEAL:
                default:
                    break;
            }
        } else {
            return DocumentType.DECISION_NOTICE;
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
            caseData.setTranslationWorkOutstanding(YesNo.YES.getValue());
        }
    }

    public static boolean isGapsCase(SscsCaseData sscsCaseData) {
        return GAPS.equals(sscsCaseData.getSchedulingAndListingFields().getHearingRoute());
    }
}
