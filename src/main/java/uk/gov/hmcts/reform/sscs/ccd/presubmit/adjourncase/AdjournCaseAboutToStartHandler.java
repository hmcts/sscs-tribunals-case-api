package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getHmcHearingType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HmcHearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InternalCaseDocumentData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
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
            && nonNull(callback.getCaseDetails())
            && nonNull(callback.getCaseDetails().getCaseData());
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
        InternalCaseDocumentData internalCaseDocumentData = sscsCaseData.getInternalCaseDocumentData();
        if (nonNull(internalCaseDocumentData) &&  nonNull(internalCaseDocumentData.getSscsInternalDocument())) {
            boolean draftAdjournmentDoc = internalCaseDocumentData.getSscsInternalDocument().stream()
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
        return HmcHearingType.DIRECTION_HEARINGS.equals(getHmcHearingType(sscsCaseData));
    }
}

