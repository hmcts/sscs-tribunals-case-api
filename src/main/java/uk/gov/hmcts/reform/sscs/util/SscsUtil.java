package uk.gov.hmcts.reform.sscs.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.GRANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.REFUSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
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

    public static void clearPostHearingFields(SscsCaseData caseData) {
        caseData.setPostHearing(null);
        clearDocumentTransientFields(caseData);
    }

    public static void clearDocumentTransientFields(SscsCaseData caseData) {
        caseData.setDocumentGeneration(DocumentGeneration.builder().build());
        caseData.setDocumentStaging(DocumentStaging.builder().build());
    }

    public static void addDocumentToDocumentTabAndBundle(FooterService footerService,
                                                         SscsCaseData caseData, DocumentType documentType) {
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

    public static void addDocumentToBundle(FooterService footerService, SscsCaseData sscsCaseData, SscsDocument sscsDocument, String overrideFileName) {
        DocumentLink url = sscsDocument.getValue().getDocumentLink();
        DocumentType documentType = DocumentType.fromValue(sscsDocument.getValue().getDocumentType());
        String dateIssued = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        footerService.createFooterAndAddDocToCase(url, sscsCaseData, documentType, dateIssued, null, overrideFileName, null);
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
            case SET_ASIDE:
                if (SetAsideActions.GRANT.equals(postHearing.getSetAside().getAction())) {
                    return DocumentType.SET_ASIDE_GRANTED;
                }
                if (SetAsideActions.REFUSE.equals(postHearing.getSetAside().getAction())) {
                    return DocumentType.SET_ASIDE_REFUSED;
                }
                break;
            case CORRECTION:
                if (CorrectionActions.GRANT.equals(postHearing.getCorrection().getAction())) {
                    return DocumentType.CORRECTION_GRANTED;
                }
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
              if (LibertyToApplyActions.REFUSE.equals(postHearing.getLibertyToApply().getAction())) {
                return DocumentType.LIBERTY_TO_APPLY_REFUSED;
              }

              return DocumentType.LIBERTY_TO_APPLY_GRANTED;
            case PERMISSION_TO_APPEAL:
            default:
                break;
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

    @Nullable
    public static DocumentType getDocumentTypeFromReviewType(PostHearingReviewType reviewType) {
        if (isNull(reviewType)) {
            return null;
        }
        switch (reviewType) {
            case SET_ASIDE:
                return DocumentType.SET_ASIDE_APPLICATION;
            case CORRECTION:
                return DocumentType.CORRECTION_APPLICATION;
            case STATEMENT_OF_REASONS:
            case PERMISSION_TO_APPEAL:
            case LIBERTY_TO_APPLY:
            default:
                return null;
        }
    }

    @Nullable
    public static EventType getEventTypeFromDocumentReviewTypeAndAction(PostHearingReviewType reviewType, String actionName) {
        if (isNull(reviewType) || isNull(actionName)) {
            return null;
        }
        boolean isGrant = GRANT.getValue().equals(actionName);
        boolean isRefuse = REFUSE.getValue().equals(actionName);

        switch (reviewType) {
            case SET_ASIDE:
                if (isGrant) {
                    return EventType.SET_ASIDE_GRANTED;
                } else if (isRefuse) {
                    return EventType.SET_ASIDE_REFUSED;
                }
                break;
            case CORRECTION:
                if (isGrant) {
                    return EventType.CORRECTION_GRANTED;
                } else if (isRefuse) {
                    return EventType.CORRECTION_REFUSED;
                }
                break;
            case STATEMENT_OF_REASONS:
            case PERMISSION_TO_APPEAL:
            case LIBERTY_TO_APPLY:
            default:
                // do nothing
        }
        return null;
    }

    @org.jetbrains.annotations.Nullable
    public static CcdCallbackMap getCcdCallbackMap(PostHearing postHearing,
                                                    PostHearingReviewType typeSelected) {
        if (isNull(typeSelected)) {
            return null;
        }

        switch (typeSelected) {
            case SET_ASIDE:
                SetAside setAside = postHearing.getSetAside();

                if (isSetAsideRefusedSor(setAside)) {
                    return SetAsideActions.REFUSE_SOR;
                } else {
                    return setAside.getAction();
                }
            case CORRECTION:
                return postHearing.getCorrection().getAction();
            case STATEMENT_OF_REASONS:
                return postHearing.getStatementOfReasons().getAction();
            case PERMISSION_TO_APPEAL:
                return postHearing.getPermissionToAppeal().getAction();
            case LIBERTY_TO_APPLY:
                return postHearing.getLibertyToApply().getAction();
            default:
                return null;
        }
    }

    public static boolean isSetAsideRefusedSor(SetAside setAside) {
        return SetAsideActions.REFUSE.equals(setAside.getAction()) && isYes(setAside.getRequestStatementOfReasons());
    }
}
