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
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateListingRequirementsAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.snl.enabled}")
    private boolean isScheduleListingEnabled;

    private final DynamicListLanguageUtil utils;

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
            String caseId = sscsCaseData.getCcdCaseId();

            log.info("Handling override fields update listing requirements event for caseId {}", caseId);

            SchedulingAndListingFields schedulingAndListingFields = sscsCaseData.getSchedulingAndListingFields();
            OverrideFields overrideFields = schedulingAndListingFields.getOverrideFields();

            if (isNull(overrideFields)) {
                overrideFields = initialiseOverrideFields();
                schedulingAndListingFields.setOverrideFields(overrideFields);
            }
            
            HearingInterpreter appellantInterpreter = overrideFields.getAppellantInterpreter();
            DynamicList interpreterLanguages = utils.generateInterpreterLanguageFields(appellantInterpreter.getInterpreterLanguage());
            appellantInterpreter.setInterpreterLanguage(interpreterLanguages);

            log.info("{} Languages in DynamicList for caseId {}", interpreterLanguages.getListItems().size(), caseId);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private OverrideFields initialiseOverrideFields() {
        HearingInterpreter appellantInterpreter = HearingInterpreter.builder().build();

        OverrideFields overrideFields = new OverrideFields();
        overrideFields.setAppellantInterpreter(appellantInterpreter);

        return overrideFields;
    }
}
