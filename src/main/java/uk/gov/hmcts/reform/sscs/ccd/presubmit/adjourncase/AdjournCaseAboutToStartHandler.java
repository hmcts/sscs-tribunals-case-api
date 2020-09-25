package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

@Component
@Slf4j
public class AdjournCaseAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private PreviewDocumentService previewDocumentService;

    @Autowired
    public AdjournCaseAboutToStartHandler(PreviewDocumentService previewDocumentService) {
        this.previewDocumentService = previewDocumentService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_START
            && callback.getEvent() == EventType.ADJOURN_CASE
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

        clearTransientFields(preSubmitCallbackResponse);

        return preSubmitCallbackResponse;
    }

    private void clearTransientFields(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        if (preSubmitCallbackResponse.getData().getSscsDocument() != null && !preSubmitCallbackResponse.getData().getSscsDocument().stream()
            .anyMatch(doc -> doc.getValue().getDocumentType().equals(DRAFT_ADJOURNMENT_NOTICE.getValue()))) {

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
            sscsCaseData.setAdjournCaseTime(null);
            sscsCaseData.setAdjournCaseReasons(null);
            sscsCaseData.setAdjournCaseAdditionalDirections(null);
        }
    }
}

