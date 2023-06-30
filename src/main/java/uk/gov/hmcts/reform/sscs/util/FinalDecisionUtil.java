package uk.gov.hmcts.reform.sscs.util;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.CORRECTED_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.FinalDecisionUtil.FinalDecisionType.INITIAL;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

@Slf4j
public class FinalDecisionUtil {

    private FinalDecisionUtil() {
        //
    }

    public enum FinalDecisionType {
        INITIAL,
        CORRECTED
    }

    public static void writePreviewFinalDecisionNotice(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, PreviewDocumentService previewDocumentService, DecisionNoticeService decisionNoticeService) {
        State state = sscsCaseData.getState();
        String benefitType = getBenefitType(sscsCaseData);
        if (benefitType == null) {
            preSubmitCallbackResponse.addError("Unexpected error - benefit type is null");
        } else {
            DecisionNoticeOutcomeService outcomeService =  decisionNoticeService.getOutcomeService(benefitType);
            outcomeService.validate(preSubmitCallbackResponse, sscsCaseData);
            if (!(State.READY_TO_LIST.equals(state) || State.WITH_DWP.equals(state))) {
                sscsCaseData.setPreviousState(state);
            }
            previewDocumentService.writePreviewDocumentToSscsDocument(sscsCaseData, DRAFT_DECISION_NOTICE, sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        }
    }

    public static void processDraftFinalDecisionNotice(Callback<SscsCaseData> callback, String userAuthorisation, SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response, FinalDecisionType finalDecisionType, DecisionNoticeService decisionNoticeService) {
        if (sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument() != null) {

            String benefitType = FinalDecisionUtil.getBenefitType(sscsCaseData);

            if (benefitType == null) {
                response.addError("Unexpected error - benefit type is null");
                return;
            }
            WriteFinalDecisionPreviewDecisionServiceBase previewDecisionService = decisionNoticeService.getPreviewService(benefitType);
            DocumentType documentType = INITIAL.equals(finalDecisionType)
                ? FINAL_DECISION_NOTICE
                : CORRECTED_DECISION_NOTICE;
            previewDecisionService.preview(callback, documentType, userAuthorisation, true);
        } else {
            response.addError("No draft final decision notice found on case. Please use 'Write final decision' event before trying to issue.");
        }
    }

    public static void issueFinalDecisionNoticeFromPreviewDraft(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, FinalDecisionType finalDecisionType, FooterService footerService) {

        SscsCaseData sscsCaseData = preSubmitCallbackResponse.getData();

        DocumentType documentType = INITIAL.equals(finalDecisionType)
            ? FINAL_DECISION_NOTICE
            : DocumentType.CORRECTED_DECISION_NOTICE;

        DocumentLink documentLink = sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument();
        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        final SscsDocumentTranslationStatus documentTranslationStatus = sscsCaseData.isLanguagePreferenceWelsh() ? TRANSLATION_REQUIRED : null;
        footerService.createFooterAndAddDocToCase(documentLink, sscsCaseData, documentType, now, null, null, documentTranslationStatus);
        if (documentTranslationStatus != null) {
            sscsCaseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION);
            log.info("Set the InterlocReviewState to {},  for case id : {}", sscsCaseData.getInterlocReviewState(), sscsCaseData.getCcdCaseId());
            sscsCaseData.setTranslationWorkOutstanding(YES.getValue());
        }
    }

    public static String getBenefitType(SscsCaseData sscsCaseData) {
        if (sscsCaseData == null || sscsCaseData.getAppeal() == null || sscsCaseData.getAppeal().getBenefitType() == null) {
            return null;
        }

        String benefitType = sscsCaseData.getAppeal().getBenefitType().getCode();

        Set<String> nonGenBenefitTypes = Set.of("PIP", "ESA", "UC");
        return nonGenBenefitTypes.contains(benefitType) ? benefitType : "GEN";
    }

}
