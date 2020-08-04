package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.ADJOURNMENT_NOTICE_ISSUED;
import static uk.gov.hmcts.reform.sscs.util.DocumentUtil.isFileAPdf;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Component
@Slf4j
public class IssueAdjournmentNoticeAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;

    @Autowired
    public IssueAdjournmentNoticeAboutToSubmitHandler(FooterService footerService) {
        this.footerService = footerService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.ISSUE_ADJOURNMENT
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

        if (preSubmitCallbackResponse.getErrors().isEmpty()) {

            sscsCaseData.setDwpState(ADJOURNMENT_NOTICE_ISSUED.getId());

            calculateDueDate(sscsCaseData);

            if (sscsCaseData.getAdjournCasePreviewDocument() != null) {

                if (!isFileAPdf(sscsCaseData.getAdjournCasePreviewDocument())) {
                    preSubmitCallbackResponse.addError("You need to upload PDF documents only");
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

            clearTransientFields(preSubmitCallbackResponse);
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

    private void clearTransientFields(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        SscsCaseData sscsCaseData = preSubmitCallbackResponse.getData();

        sscsCaseData.setAdjournCaseGenerateNotice(null);
        sscsCaseData.setAdjournCaseTypeOfHearing(null);
        sscsCaseData.setAdjournCaseCanCaseBeListedRightAway(null);
        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties(null);
        sscsCaseData.setAdjournCaseDirectionsDueDateDaysOffset(null);
        sscsCaseData.setAdjournCaseDirectionsDueDate(null);
        sscsCaseData.setAdjournCaseTypeOfNextHearing(null);
        sscsCaseData.setAdjournCaseNextHearingVenue(null);
        sscsCaseData.setAdjournCaseNextHearingVenueSelected(null);
        sscsCaseData.setAdjournCasePanelMembersExcluded(null);
        sscsCaseData.setAdjournCaseDisabilityQualifiedPanelMemberName(null);
        sscsCaseData.setAdjournCaseMedicallyQualifiedPanelMemberName(null);
        sscsCaseData.setAdjournCaseOtherPanelMemberName(null);
        sscsCaseData.setAdjournCaseNextHearingListingDurationType(null);
        sscsCaseData.setAdjournCaseNextHearingListingDuration(null);
        sscsCaseData.setAdjournCaseNextHearingListingDurationUnits(null);
        sscsCaseData.setAdjournCaseInterpreterRequired(null);
        sscsCaseData.setAdjournCaseInterpreterLanguage(null);
        sscsCaseData.setAdjournCaseNextHearingDateType(null);
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod(null);
        sscsCaseData.setAdjournCaseNextHearingDateOrTime(null);
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate(null);
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterPeriod(null);
        sscsCaseData.setAdjournCaseNextHearingSpecificDate(null);
        sscsCaseData.setAdjournCaseNextHearingSpecificTime(null);
        sscsCaseData.setAdjournCaseReasons(null);
        sscsCaseData.setAdjournCaseAnythingElse(null);

        preSubmitCallbackResponse.getData().getSscsDocument()
                .removeIf(doc -> doc.getValue().getDocumentType().equals(DRAFT_ADJOURNMENT_NOTICE.getValue()));
    }

}
