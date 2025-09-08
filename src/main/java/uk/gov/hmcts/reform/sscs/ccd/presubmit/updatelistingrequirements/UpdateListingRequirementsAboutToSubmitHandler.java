package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.DISTRICT_TRIBUNAL_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getHmcHearingType;

import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        if ("NoMedicalMemberRequired"
                .equals(sscsCaseData.getPanelMemberComposition().getPanelCompositionMemberMedical1())) {
            sscsCaseData.getPanelMemberComposition().clearMedicalMembers();
        }
        resetReservedJudgeFields(sscsCaseData);
        syncConfirmPanelComposition(callbackResponse.getData());

        OverrideFields overrideFields = sscsCaseData.getSchedulingAndListingFields().getOverrideFields();

        if (nonNull(overrideFields) && !overrideFields.isAllNull()) {
            HearingChannel hearingChannel = overrideFields.getAppellantHearingChannel();
            if (nonNull(hearingChannel)) {
                SscsUtil.updateHearingChannel(sscsCaseData, hearingChannel);
            }
            boolean updateDuration = updateHearingDuration(sscsCaseData, callback.getCaseDetailsBefore());
            HearingInterpreter appellantInterpreter = overrideFields.getAppellantInterpreter();
            if (nonNull(appellantInterpreter)) {
                SscsUtil.updateHearingInterpreter(sscsCaseData, callbackResponse, appellantInterpreter);
            }
            if (updateDuration) {
                overrideFields.setDuration(hearingDurationsService.getHearingDurationBenefitIssueCodes(sscsCaseData));
            }
        }
      
        sscsCaseData.getAppeal()
            .setHearingOptions(ofNullable(sscsCaseData.getAppeal().getHearingOptions())
                .map(HearingOptions::toBuilder)
                .orElseGet(HearingOptions::builder)
                .hmcHearingType(getHmcHearingType(sscsCaseData))
                .build());
        return callbackResponse;
    }

    private void resetReservedJudgeFields(SscsCaseData caseData) {
        YesNo reservedDtj = ofNullable(caseData.getSchedulingAndListingFields().getReserveTo())
                .orElse(new ReserveTo()).getReservedDistrictTribunalJudge();

        if (isYes(reservedDtj)) {
            caseData.getSchedulingAndListingFields().getReserveTo().setReservedJudge(null);
            caseData.getPanelMemberComposition().setPanelCompositionJudge(null);
            caseData.getPanelMemberComposition()
                    .setDistrictTribunalJudge(DISTRICT_TRIBUNAL_JUDGE.getReference());
        } else {
            caseData.getPanelMemberComposition().setDistrictTribunalJudge(null);
        }
    }

    private void syncConfirmPanelComposition(SscsCaseData sscsCaseData) {
        PanelMemberComposition panelMemberComposition = sscsCaseData.getPanelMemberComposition();

        sscsCaseData.setIsFqpmRequired(panelMemberComposition.hasFqpm() ? YES : NO);

        if (sscsCaseData.isIbcCase()) {
            sscsCaseData.setIsMedicalMemberRequired(panelMemberComposition.hasMedicalMember() ? YES : NO);
        }
    }

    private boolean updateHearingDuration(SscsCaseData sscsCaseData,
                                          Optional<CaseDetails<SscsCaseData>> caseDetailsBeforeOpt) {
        var snlFields = sscsCaseData.getSchedulingAndListingFields();
        if (nonNull(snlFields.getOverrideFields().getDuration()) || isNull(snlFields.getDefaultListingValues())) {
            return false;
        }
        return isChannelUpdated(caseDetailsBeforeOpt, snlFields) || isInterpreterUpdated(sscsCaseData);
    }

    private static boolean isChannelUpdated(Optional<CaseDetails<SscsCaseData>> caseDetailsBeforeOpt,
                                            SchedulingAndListingFields snlFields) {
        var caseDetailsBefore = caseDetailsBeforeOpt
                .orElseThrow(() -> new RuntimeException("CaseDeatailsBefore cannot be empty"));
        OverrideFields overrideFieldsBefore =
                caseDetailsBefore.getCaseData().getSchedulingAndListingFields().getOverrideFields();
        HearingChannel channelBefore =
                nonNull(overrideFieldsBefore) ? overrideFieldsBefore.getAppellantHearingChannel() : null;
        HearingChannel channelCurrent = snlFields.getOverrideFields().getAppellantHearingChannel();
        return !Objects.equals(channelBefore, channelCurrent);
    }

    private static boolean isInterpreterUpdated(SscsCaseData sscsCaseData) {
        HearingInterpreter appellantInterpreter =
                sscsCaseData.getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter();
        if (nonNull(appellantInterpreter.getIsInterpreterWanted())) {
            Optional<HearingOptions> hearingOptions = ofNullable(sscsCaseData.getAppeal().getHearingOptions());
            var caseInterpreter = hearingOptions.isPresent() && nonNull(hearingOptions.get().getLanguageInterpreter())
                    ? hearingOptions.get().getLanguageInterpreter() : "No";
            return isYes(caseInterpreter) != isYes(appellantInterpreter.getIsInterpreterWanted());
        }
        return false;
    }
}
