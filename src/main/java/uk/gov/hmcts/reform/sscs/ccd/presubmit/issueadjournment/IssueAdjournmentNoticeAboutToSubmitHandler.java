package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.ADJOURNMENT_NOTICE_ISSUED;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Component
@Slf4j
public class IssueAdjournmentNoticeAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;
    private final Validator validator;

    @Autowired
    public IssueAdjournmentNoticeAboutToSubmitHandler(FooterService footerService, Validator validator) {
        this.footerService = footerService;
        this.validator = validator;
    }

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

            sscsCaseData.setDwpState(ADJOURNMENT_NOTICE_ISSUED.getId());

            calculateDueDate(sscsCaseData);

            if (sscsCaseData.getAdjournCasePreviewDocument() != null) {

                if (!preSubmitCallbackResponse.getErrors().isEmpty()) {
                    return preSubmitCallbackResponse;
                }

                createAdjournmentNoticeFromPreviewDraft(preSubmitCallbackResponse);
            } else {
                preSubmitCallbackResponse.addError("There is no Draft Adjournment Notice on the case so adjournment cannot be issued");
            }

            if ("yes".equalsIgnoreCase(sscsCaseData.getAdjournCaseAreDirectionsBeingMadeToParties())) {
                sscsCaseData.setState(State.NOT_LISTABLE);
            } else {
                sscsCaseData.setState(State.READY_TO_LIST);
            }

            preSubmitCallbackResponse.getData().getSscsDocument()
                .removeIf(doc -> doc.getValue().getDocumentType().equals(DRAFT_ADJOURNMENT_NOTICE.getValue()));
        }

        return preSubmitCallbackResponse;
    }

    private void calculateDueDate(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getAdjournCaseDirectionsDueDate() != null && !"".equalsIgnoreCase(sscsCaseData.getAdjournCaseDirectionsDueDate())) {
            sscsCaseData.setDirectionDueDate(sscsCaseData.getAdjournCaseDirectionsDueDate());
        } else if (sscsCaseData.getAdjournCaseDirectionsDueDateDaysOffset() != null && !"".equalsIgnoreCase(sscsCaseData.getAdjournCaseDirectionsDueDateDaysOffset())) {
            sscsCaseData.setDirectionDueDate(LocalDate.now().plusDays(Integer.valueOf(sscsCaseData.getAdjournCaseDirectionsDueDateDaysOffset())).toString());
        }
    }

    private void createAdjournmentNoticeFromPreviewDraft(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        SscsCaseData sscsCaseData = preSubmitCallbackResponse.getData();

        DocumentLink docLink = sscsCaseData.getAdjournCasePreviewDocument();

        DocumentLink documentLink = DocumentLink.builder()
            .documentUrl(docLink.getDocumentUrl())
            .documentFilename(docLink.getDocumentFilename())
            .documentBinaryUrl(docLink.getDocumentBinaryUrl())
            .build();

        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"));

        footerService.createFooterAndAddDocToCase(documentLink, sscsCaseData, DocumentType.ADJOURNMENT_NOTICE, now,
                null, null);
    }

}
