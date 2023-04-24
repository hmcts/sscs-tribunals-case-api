package uk.gov.hmcts.reform.sscs.util;

import static java.util.Objects.isNull;
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
import org.jetbrains.annotations.Nullable;
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

    public static void clearDocumentTransientFields(SscsCaseData caseData) {
        caseData.setDocumentGeneration(DocumentGeneration.builder().build());
        caseData.setDocumentStaging(DocumentStaging.builder().build());
    }

    public static void addDocumentToDocumentTab(FooterService footerService, SscsCaseData caseData, DocumentType documentType) {
        DocumentLink url = caseData.getDocumentStaging().getPreviewDocument();
        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        SscsDocumentTranslationStatus documentTranslationStatus = null;
        if (caseData.isLanguagePreferenceWelsh()) {
            documentTranslationStatus = SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
        }
        footerService.createFooterAndAddDocToCase(url, caseData, documentType, now,
            null, null, documentTranslationStatus);
        if (documentTranslationStatus != null) {
            caseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION);
            log.info("Set the InterlocReviewState to {},  for case id : {}", caseData.getInterlocReviewState(), caseData.getCcdCaseId());
            caseData.setTranslationWorkOutstanding(YesNo.YES.getValue());
        }
    }

    public static void addDocumentToBundle(FooterService footerService, SscsCaseData sscsCaseData, SscsDocument sscsDocument, String overrideFileName) {
        DocumentLink url = sscsDocument.getValue().getDocumentLink();
        DocumentType documentType = DocumentType.fromValue(sscsDocument.getValue().getDocumentType());
        String dateIssued = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        footerService.createFooterAndAddDocToCase(url, sscsCaseData, documentType, dateIssued, null, overrideFileName, null);
    }

    @Nullable
    public static CcdCallbackMap getCcdCallbackMap(PostHearing postHearing, PostHearingReviewType reviewType) {
        if (isNull(reviewType)) {
            return null;
        }
        switch (reviewType) {
            case SET_ASIDE:
                CcdCallbackMap action = postHearing.getSetAside().getAction();
                if (action == SetAsideActions.REFUSE
                        && isYes(postHearing.getSetAside().getRequestStatementOfReasons())) {
                    action = SetAsideActions.REFUSE_SOR;
                }
                return action;
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
}
