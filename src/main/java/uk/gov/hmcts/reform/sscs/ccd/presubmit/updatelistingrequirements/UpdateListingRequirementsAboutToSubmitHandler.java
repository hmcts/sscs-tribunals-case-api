package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getHmcHearingType;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateListingRequirementsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.default-panel-comp.enabled}")
    private boolean isDefaultPanelCompEnabled;

    private final HearingDurationsService hearingDurationsService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
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

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        ReserveTo callbackReserveTo = callbackResponse.getData().getSchedulingAndListingFields().getReserveTo();
        SchedulingAndListingFields caseDataSnlFields = sscsCaseData.getSchedulingAndListingFields();

        if (nonNull(callbackReserveTo)) {
            YesNo callbackReservedDtj = callbackReserveTo.getReservedDistrictTribunalJudge();
            ReserveTo caseDataReserveTo = caseDataSnlFields.getReserveTo();
            caseDataReserveTo.setReservedDistrictTribunalJudge(callbackReservedDtj);

            if (isYes(callbackReservedDtj)) {
                caseDataReserveTo.setReservedJudge(null);
                if (isDefaultPanelCompEnabled && callbackResponse.getData().getPanelMemberComposition() != null) {
                    callbackResponse.getData().getPanelMemberComposition().setPanelCompositionJudge(null);
                }
            }
        }

        if (isDefaultPanelCompEnabled && callbackResponse.getData().getPanelMemberComposition() != null
                && "NoMedicalMemberRequired".equals(callbackResponse.getData().getPanelMemberComposition().getPanelCompositionMemberMedical1())) {
            callbackResponse.getData().getPanelMemberComposition().setPanelCompositionMemberMedical1(null);
            callbackResponse.getData().getPanelMemberComposition().setPanelCompositionMemberMedical2(null);
        }

        OverrideFields overrideFields = caseDataSnlFields.getOverrideFields();

        if (nonNull(overrideFields)) {
            HearingChannel hearingChannel = overrideFields.getAppellantHearingChannel();
            if (nonNull(hearingChannel)) {
                SscsUtil.updateHearingChannel(sscsCaseData, hearingChannel);
            }
            HearingInterpreter appellantInterpreter = overrideFields.getAppellantInterpreter();
            Optional<HearingOptions> hearingOptions = Optional.ofNullable(sscsCaseData.getAppeal().getHearingOptions());
            String caseInterpreter = hearingOptions.isPresent() ? hearingOptions.get().getLanguageInterpreter() : null;
            boolean updateDuration = nonNull(caseInterpreter) && nonNull(appellantInterpreter) && YesNo.isYes(caseInterpreter) != YesNo.isYes(appellantInterpreter.getIsInterpreterWanted());
            if (nonNull(appellantInterpreter)) {
                SscsUtil.updateHearingInterpreter(sscsCaseData, callbackResponse, appellantInterpreter);
            }
            if (updateDuration && nonNull(sscsCaseData.getSchedulingAndListingFields().getDefaultListingValues())) {
                sscsCaseData.getSchedulingAndListingFields().getDefaultListingValues().setDuration(
                        hearingDurationsService.getHearingDurationBenefitIssueCodes(sscsCaseData)
                );
            }
        }
      
        sscsCaseData.getAppeal()
            .setHearingOptions(Optional.ofNullable(sscsCaseData.getAppeal().getHearingOptions())
                .map(HearingOptions::toBuilder)
                .orElseGet(HearingOptions::builder)
                .hmcHearingType(getHmcHearingType(sscsCaseData))
                .build());
        return callbackResponse;
    }
}
