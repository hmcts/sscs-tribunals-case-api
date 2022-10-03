package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import java.time.LocalDate;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

@Component
@Slf4j
@RequiredArgsConstructor
public class AdjournCaseAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private PreviewDocumentService previewDocumentService;

    private final ListAssistHearingMessageHelper hearingMessageHelper;

    @Autowired
    public AdjournCaseAboutToSubmitHandler(PreviewDocumentService previewDocumentService) {
        this.previewDocumentService = previewDocumentService;
    }

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

        if (LIST_ASSIST == sscsCaseData.getSchedulingAndListingFields().getHearingRoute()
            && READY_TO_LIST == sscsCaseData.getState()) {
            hearingMessageHelper.sendListAssistCreateHearingMessage(sscsCaseData.getCcdCaseId());
        }

        // Due to a bug with CCD related to hidden fields, this field not being set
        // on the final submission from CCD, so we need to reset it here
        // See https://tools.hmcts.net/jira/browse/RDM-8200
        // This is a temporary workaround for this issue.
        sscsCaseData.setAdjournCaseGeneratedDate(LocalDate.now().toString());

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        previewDocumentService.writePreviewDocumentToSscsDocument(sscsCaseData, DRAFT_ADJOURNMENT_NOTICE, sscsCaseData.getAdjournCasePreviewDocument());

        if (sscsCaseData.getAdjournCaseInterpreterRequired() != null) {
            HearingOptions hearingOptions = sscsCaseData.getAppeal().getHearingOptions() != null ? sscsCaseData.getAppeal().getHearingOptions() : HearingOptions.builder().build();
            hearingOptions.setLanguages(sscsCaseData.getAdjournCaseInterpreterLanguage());
            hearingOptions.setLanguageInterpreter(sscsCaseData.getAdjournCaseInterpreterRequired());

            sscsCaseData.getAppeal().setHearingOptions(hearingOptions);
        }

        return preSubmitCallbackResponse;
    }
}
