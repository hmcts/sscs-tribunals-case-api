package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;

import java.time.LocalDate;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdjournCaseAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final PreviewDocumentService previewDocumentService;

    private final ListAssistHearingMessageHelper hearingMessageHelper;

    @Value("${feature.snl.adjournment.enabled}")
    private boolean isAdjournmentEnabled; // TODO SSCS-10951

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
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

        previewDocumentService.writePreviewDocumentToSscsDocument(sscsCaseData, DRAFT_ADJOURNMENT_NOTICE, sscsCaseData.getAdjournCasePreviewDocument());

        if (SscsUtil.isSAndLCase(sscsCaseData)
            && isAdjournmentEnabled // TODO SSCS-10951
            && (sscsCaseData.isAdjournCaseAbleToBeListedRightAway()
            || isNoOrNull(sscsCaseData.getAdjournCaseAreDirectionsBeingMadeToParties()))
        ) {
            hearingMessageHelper.sendListAssistCreateHearingMessage(sscsCaseData.getCcdCaseId());
        } else if (sscsCaseData.getAdjournCaseInterpreterRequired() != null) {
            HearingOptions hearingOptions = HearingOptions.builder().build();
            if (sscsCaseData.getAppeal().getHearingOptions() != null) {
                hearingOptions = sscsCaseData.getAppeal().getHearingOptions();
            }

            hearingOptions.setLanguages(sscsCaseData.getAdjournCaseInterpreterLanguage().getValue().getCode());

            hearingOptions.setLanguageInterpreter(sscsCaseData.getAdjournCaseInterpreterRequired());

            sscsCaseData.getAppeal().setHearingOptions(hearingOptions);
        }

        if (sscsCaseData.getAdjournCaseGeneratedDate() == null) {
            sscsCaseData.setAdjournCaseGeneratedDate(LocalDate.now().toString());
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
