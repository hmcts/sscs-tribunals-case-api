package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class CaseUpdatedAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final DynamicListLanguageUtil utils;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && (callback.getEvent() == EventType.CASE_UPDATED);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        String caseId = sscsCaseData.getCcdCaseId();

        HearingOptions hearingOptions = sscsCaseData.getAppeal().getHearingOptions();
        DynamicList interpreterLanguages = utils.generateInterpreterLanguageFields(hearingOptions.getLanguages());
        hearingOptions.setLanguages(interpreterLanguages);

        log.info("Populated {} Languages in DynamicList for caseId {} for update to case data event",
                interpreterLanguages.getListItems().size(), caseId);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
