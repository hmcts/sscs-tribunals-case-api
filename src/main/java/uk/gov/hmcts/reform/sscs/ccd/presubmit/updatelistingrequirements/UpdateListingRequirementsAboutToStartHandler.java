package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.DynamicListLangauageUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateListingRequirementsAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.snl.enabled}")
    private boolean isScheduleListingEnabled;

    private final DynamicListLangauageUtil utils;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
            && (callback.getEvent() == EventType.UPDATE_LISTING_REQUIREMENTS);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        if (isScheduleListingEnabled) {
            log.info("Handling override fields update listing requirements event for caseId {}",
                sscsCaseData.getCcdCaseId());

            SchedulingAndListingFields schedulingAndListingFields = sscsCaseData.getSchedulingAndListingFields();

            OverrideFields overrideFields = schedulingAndListingFields.getOverrideFields();

            if (isNull(overrideFields)) {
                overrideFields = new OverrideFields();
                schedulingAndListingFields.setOverrideFields(overrideFields);
            }

            if (isNull(overrideFields.getAppellantInterpreter())
                || isNull(overrideFields.getAppellantInterpreter().getInterpreterLanguage())) {
                overrideFields.setAppellantInterpreter(HearingInterpreter.builder()
                    .interpreterLanguage(new DynamicList(null, null))
                    .build());
            }

            DynamicList languageList = utils.generateInterpreterLanguageFields(overrideFields.getAppellantInterpreter().getInterpreterLanguage());

            overrideFields.getAppellantInterpreter().setInterpreterLanguage(languageList);

            log.info("{} Languages in DynamicList for caseId {}",
                overrideFields.getAppellantInterpreter().getInterpreterLanguage().getListItems().size(),
                sscsCaseData.getCcdCaseId());
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
