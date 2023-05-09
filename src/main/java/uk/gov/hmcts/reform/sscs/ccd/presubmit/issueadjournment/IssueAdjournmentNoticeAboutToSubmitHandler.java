package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.ADJOURNMENT_NOTICE_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.adjourncase.AdjournCaseService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
@AllArgsConstructor
public class IssueAdjournmentNoticeAboutToSubmitHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;
    private final Validator validator;

    private final ListAssistHearingMessageHelper hearingMessageHelper;

    @Value("${feature.snl.adjournment.enabled}")
    private boolean isAdjournmentEnabled; // TODO SSCS-10951

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.ISSUE_ADJOURNMENT_NOTICE
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        Set<ConstraintViolation<SscsCaseData>> violations = validator.validate(sscsCaseData);
        for (ConstraintViolation<SscsCaseData> violation : violations) {
            preSubmitCallbackResponse.addError(violation.getMessage());
        }

        if (preSubmitCallbackResponse.getErrors().isEmpty()) {
            SscsDocumentTranslationStatus documentTranslationStatus = sscsCaseData.isLanguagePreferenceWelsh() ? SscsDocumentTranslationStatus.TRANSLATION_REQUIRED : null;

            calculateDueDate(sscsCaseData);

            if (sscsCaseData.getAdjournment().getPreviewDocument() != null) {
                processResponse(sscsCaseData, preSubmitCallbackResponse, documentTranslationStatus);
            } else {
                preSubmitCallbackResponse.addError("There is no Draft Adjournment Notice on the case so adjournment cannot be issued");
            }
        }

        return preSubmitCallbackResponse;
    }

    private void processResponse(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse,
        SscsDocumentTranslationStatus documentTranslationStatus) {
        createAdjournmentNoticeFromPreviewDraft(preSubmitCallbackResponse, documentTranslationStatus);

        if (!SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(documentTranslationStatus)) {
            sscsCaseData.setDwpState(ADJOURNMENT_NOTICE_ISSUED);
            if (isYes(sscsCaseData.getAdjournment().getAreDirectionsBeingMadeToParties())) {
                sscsCaseData.setState(State.NOT_LISTABLE);
            } else {
                sscsCaseData.setState(State.READY_TO_LIST);
            }
        } else {
            log.info("Case is a Welsh case so Adjournment Notice requires translation for case id : {}", sscsCaseData.getCcdCaseId());
            sscsCaseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION);
            log.info("Set the InterlocReviewState to {},  for case id : {}", sscsCaseData.getInterlocReviewState(), sscsCaseData.getCcdCaseId());
            sscsCaseData.setTranslationWorkOutstanding("Yes");
        }

        Adjournment adjournment = sscsCaseData.getAdjournment();

        if (SscsUtil.isSAndLCase(sscsCaseData)
            && isAdjournmentEnabled // TODO SSCS-10951
            && (isYes(adjournment.getCanCaseBeListedRightAway())
            || isNoOrNull(adjournment.getAreDirectionsBeingMadeToParties()))) {
            adjournment.setAdjournmentInProgress(YES);
            hearingMessageHelper.sendListAssistCreateHearingMessage(sscsCaseData.getCcdCaseId());
        }

        clearBasicTransientFields(sscsCaseData);
        AdjournCaseService.clearTransientFields(sscsCaseData, isAdjournmentEnabled);

        preSubmitCallbackResponse.getData().getSscsDocument()
                .removeIf(doc -> doc.getValue().getDocumentType().equals(DRAFT_ADJOURNMENT_NOTICE.getValue()));
    }

    private void calculateDueDate(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getAdjournment().getDirectionsDueDate() != null) {
            sscsCaseData.setDirectionDueDate(sscsCaseData.getAdjournment().getDirectionsDueDate().toString());
        } else if (sscsCaseData.getAdjournment().getDirectionsDueDateDaysOffset() != null) {
            sscsCaseData.setDirectionDueDate(LocalDate.now()
                .plusDays(sscsCaseData.getAdjournment().getDirectionsDueDateDaysOffset().getCcdDefinition())
                .toString());
        }
    }

    private void createAdjournmentNoticeFromPreviewDraft(
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse,
        SscsDocumentTranslationStatus documentTranslationStatus) {

        SscsCaseData sscsCaseData = preSubmitCallbackResponse.getData();

        DocumentLink docLink = sscsCaseData.getAdjournment().getPreviewDocument();

        DocumentLink documentLink = DocumentLink.builder()
            .documentUrl(docLink.getDocumentUrl())
            .documentFilename(docLink.getDocumentFilename())
            .documentBinaryUrl(docLink.getDocumentBinaryUrl())
            .build();

        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        footerService.createFooterAndAddDocToCase(documentLink, sscsCaseData, DocumentType.ADJOURNMENT_NOTICE, now,
                null, null, documentTranslationStatus);
    }

}
