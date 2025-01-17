package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.TypeOfHearing;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdjournCaseAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final DynamicListLanguageUtil utils;

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
        if (isDirectionHearing(sscsCaseData)) {
            preSubmitCallbackResponse.addError("In order to run this event the hearing type must be substantive, please update the hearing type to proceed");
            return preSubmitCallbackResponse;
        }
        DynamicList languageList = utils.generateInterpreterLanguageFields(sscsCaseData.getAdjournment().getInterpreterLanguage());
        if (sscsCaseData.getSscsDocument() != null) {
            boolean draftAdjournmentDoc = sscsCaseData.getSscsDocument().stream()
                .anyMatch(sscsDocument -> sscsDocument.getValue().getDocumentType().equals(DRAFT_ADJOURNMENT_NOTICE.getValue()));
            if (!draftAdjournmentDoc) {
                sscsCaseData.setAdjournment(Adjournment.builder().build());
            }
        } else {
            sscsCaseData.setAdjournment(Adjournment.builder().build());
        }
        sscsCaseData.getAdjournment().setInterpreterLanguage(languageList);

        return preSubmitCallbackResponse;
    }

    private boolean isDirectionHearing(SscsCaseData sscsCaseData) {
        return TypeOfHearing.DIRECTION_HEARINGS.equals(sscsCaseData.getSchedulingAndListingFields() != null
            && sscsCaseData.getSchedulingAndListingFields().getOverrideFields() != null
            && sscsCaseData.getSchedulingAndListingFields().getOverrideFields().getTypeOfHearing() != null
            ? sscsCaseData.getSchedulingAndListingFields().getOverrideFields().getTypeOfHearing()
            : sscsCaseData.getTypeOfHearing());
    }
}

