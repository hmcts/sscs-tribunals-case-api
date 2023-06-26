package uk.gov.hmcts.reform.sscs.util;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.FinalDecisionUtil.FinalDecisionType.INITIAL;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
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

    public static void createFinalDecisionNoticeFromPreviewDraft(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, FinalDecisionType finalDecisionType, FooterService footerService) {

        SscsCaseData sscsCaseData = preSubmitCallbackResponse.getData();

        DocumentLink docLink;
        DocumentType documentType;

        if (INITIAL.equals(finalDecisionType)) {
            docLink = sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument();
            documentType = DocumentType.FINAL_DECISION_NOTICE;
        } else {
            docLink = sscsCaseData.getDocumentStaging().getPreviewDocument();
            documentType = DocumentType.CORRECTED_DECISION_NOTICE;
        }

        DocumentLink documentLink = DocumentLink.builder()
            .documentUrl(docLink.getDocumentUrl())
            .documentFilename(docLink.getDocumentFilename())
            .documentBinaryUrl(docLink.getDocumentBinaryUrl())
            .build();

        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        final SscsDocumentTranslationStatus documentTranslationStatus = sscsCaseData.isLanguagePreferenceWelsh() ? TRANSLATION_REQUIRED : null;
        footerService.createFooterAndAddDocToCase(documentLink, sscsCaseData, documentType, now, null, null, documentTranslationStatus);
        if (documentTranslationStatus != null) {
            sscsCaseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION);
            log.info("Set the InterlocReviewState to {},  for case id : {}", sscsCaseData.getInterlocReviewState(), sscsCaseData.getCcdCaseId());
            sscsCaseData.setTranslationWorkOutstanding(YES.getValue());
        }
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

    public static String getBenefitType(SscsCaseData sscsCaseData) {
        if (sscsCaseData == null || sscsCaseData.getAppeal() == null || sscsCaseData.getAppeal().getBenefitType() == null) {
            return null;
        }

        String benefitType = sscsCaseData.getAppeal().getBenefitType().getCode();

        Set<String> nonGenBenefitTypes = Set.of("PIP", "ESA", "UC");
        return nonGenBenefitTypes.contains(benefitType) ? benefitType : "GEN";
    }

}
