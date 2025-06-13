package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getHmcHearingType;

import java.util.Objects;
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
            boolean updateDuration = updateHearingDuration(sscsCaseData, callback.getCaseDetailsBefore());
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

    private boolean updateHearingDuration(SscsCaseData sscsCaseData, Optional<CaseDetails<SscsCaseData>> sscsCaseDataBefore) {
        if (nonNull(sscsCaseData.getSchedulingAndListingFields().getOverrideFields().getDuration())) {
            return false;
        }
        boolean channelUpdated = false;
        if (sscsCaseDataBefore.isPresent()) {
            OverrideFields overrideFieldsBefore = sscsCaseDataBefore.get().getCaseData().getSchedulingAndListingFields().getOverrideFields();
            if (nonNull(overrideFieldsBefore)) {
                HearingChannel channelBefore = overrideFieldsBefore.getAppellantHearingChannel();
                HearingChannel channelCurrent = sscsCaseData.getSchedulingAndListingFields().getOverrideFields().getAppellantHearingChannel();
                channelUpdated = !Objects.equals(channelBefore, channelCurrent);
            }
        }
        HearingInterpreter appellantInterpreter = sscsCaseData.getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter();
        Optional<HearingOptions> hearingOptions = Optional.ofNullable(sscsCaseData.getAppeal().getHearingOptions());
        String caseInterpreter = hearingOptions.isPresent() && nonNull(hearingOptions.get().getLanguageInterpreter())
                ? hearingOptions.get().getLanguageInterpreter()
                : "No";
        boolean updateDuration = nonNull(appellantInterpreter) && YesNo.isYes(caseInterpreter) != YesNo.isYes(appellantInterpreter.getIsInterpreterWanted());
        return channelUpdated || updateDuration;
    }
}
