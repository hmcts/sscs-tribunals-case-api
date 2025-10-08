package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReserveTo;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateListingRequirementsAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final PanelCompositionService panelCompositionService;
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

        String caseId = sscsCaseData.getCcdCaseId();

        log.info("Handling override fields update listing requirements event for caseId {}", caseId);

        SchedulingAndListingFields schedulingAndListingFields = sscsCaseData.getSchedulingAndListingFields();
        OverrideFields overrideFields = schedulingAndListingFields.getOverrideFields();

        if (isNull(overrideFields) || isNull(overrideFields.getAppellantInterpreter())) {
            overrideFields = initialiseOverrideFields();
            schedulingAndListingFields.setOverrideFields(overrideFields);
        }

        HearingInterpreter appellantInterpreter = overrideFields.getAppellantInterpreter();
        DynamicList interpreterLanguages =
                utils.generateInterpreterLanguageFields(appellantInterpreter.getInterpreterLanguage());
        appellantInterpreter.setInterpreterLanguage(interpreterLanguages);

        log.info("{} Languages in DynamicList for caseId {}", interpreterLanguages.getListItems().size(), caseId);
        if (isNull(overrideFields.getHmcHearingType())) {
            overrideFields.setHmcHearingType(sscsCaseData.getHmcHearingType());
        }

        if (isNull(sscsCaseData.getPanelMemberComposition())
                || sscsCaseData.getPanelMemberComposition().isEmpty()) {
            var johTiers = panelCompositionService.getDefaultPanelComposition(sscsCaseData).getJohTiers();
            sscsCaseData.setPanelMemberComposition(new PanelMemberComposition(johTiers));
            log.info("Setting default JOH tiers ({}) on case ({})", johTiers, caseId);
        }

        if (nonNull(sscsCaseData.getPanelMemberComposition().getPanelCompositionJudge())) {
            if (isNull(schedulingAndListingFields.getReserveTo())) {
                schedulingAndListingFields.setReserveTo(ReserveTo.builder().build());
            }
            schedulingAndListingFields.getReserveTo().setReservedDistrictTribunalJudge(YesNo.NO);
        }
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private OverrideFields initialiseOverrideFields() {
        return OverrideFields.builder()
                .appellantInterpreter(HearingInterpreter.builder().build())
                .build();
    }
}
