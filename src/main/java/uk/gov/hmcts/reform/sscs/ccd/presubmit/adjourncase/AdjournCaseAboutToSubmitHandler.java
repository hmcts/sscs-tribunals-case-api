package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.PAPER;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdjournCaseAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final PreviewDocumentService previewDocumentService;

    private final AirLookupService airLookupService;

    private final RegionalProcessingCenterService regionalProcessingCenterService;

    private final VenueDataLoader venueDataLoader;

    @Value("${feature.snl.adjournment.enabled}")
    private boolean isAdjournmentEnabled; // TODO SSCS-10951

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.ADJOURN_CASE
            && nonNull(callback.getCaseDetails())
            && nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        Adjournment adjournment = sscsCaseData.getAdjournment();

        previewDocumentService.writePreviewDocumentToSscsDocument(
            sscsCaseData,
            DRAFT_ADJOURNMENT_NOTICE,
            adjournment.getPreviewDocument());

        if (adjournment.getInterpreterRequired() != null) {
            HearingOptions hearingOptions = HearingOptions.builder().build();
            if (sscsCaseData.getAppeal().getHearingOptions() != null) {
                hearingOptions = sscsCaseData.getAppeal().getHearingOptions();
            }
            DynamicList interpreterLanguage = adjournment.getInterpreterLanguage();
            hearingOptions.setLanguages(nonNull(interpreterLanguage.getValue()) ? interpreterLanguage.getValue().getLabel() : "");
            hearingOptions.setLanguageInterpreter(adjournment.getInterpreterRequired().getValue());

            sscsCaseData.getAppeal().setHearingOptions(hearingOptions);
        }

        if (nonNull(adjournment.getNextHearingVenueSelected())) {
            String venueId = adjournment.getNextHearingVenueSelected().getValue().getCode();

            RegionalProcessingCenter rpc = regionalProcessingCenterService.getByVenueId(venueId);

            sscsCaseData.setRegionalProcessingCenter(rpc);

            if (nonNull(rpc)) {
                sscsCaseData.setRegion(rpc.getName());

                String processingVenue = airLookupService.lookupAirVenueNameByPostCode(
                    rpc.getPostcode(),
                    sscsCaseData.getAppeal().getBenefitType());

                sscsCaseData.setProcessingVenue(processingVenue);
            }
        }

        adjournment.setGeneratedDate(LocalDate.now());

        if (isAdjournmentEnabled) {
            updatePanelMembers(sscsCaseData);
            updateHearingChannel(sscsCaseData);
            updateOverrideFields(sscsCaseData);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    public static void updateHearingChannel(SscsCaseData sscsCaseData) {

        if (sscsCaseData.getAdjournment().getTypeOfNextHearing() != null) {
            log.info(String.format("Update the hearing channel %s", sscsCaseData.getAdjournment().getTypeOfNextHearing()));
            final Hearing latestHearing = sscsCaseData.getLatestHearing();
            if (latestHearing != null && latestHearing.getValue() != null) {
                final HearingChannel hearingChannel = getNextHearingChannel(sscsCaseData);
                latestHearing.getValue().setHearingChannel(hearingChannel);
                if (hearingChannel.getValueTribunals().equalsIgnoreCase(PAPER.getValueTribunals())) {
                    sscsCaseData.getAppeal().setHearingType(PAPER.getValueTribunals());
                } else {
                    sscsCaseData.getAppeal().setHearingType("oral");
                }
            }
        }
    }

    private static HearingChannel getNextHearingChannel(SscsCaseData caseData) {
        return Arrays.stream(HearingChannel.values())
                .filter(hearingChannel -> caseData.getAdjournment().getTypeOfNextHearing().getHearingChannel().getValueTribunals().equalsIgnoreCase(
                        hearingChannel.getValueTribunals()))
                .findFirst().orElse(HearingChannel.PAPER);
    }

    private static void updatePanelMembers(SscsCaseData caseData) {
        Adjournment adjournment = caseData.getAdjournment();
        AdjournCasePanelMembersExcluded panelMemberExcluded = adjournment.getPanelMembersExcluded();

        if (nonNull(panelMemberExcluded)) {
            PanelMemberExclusions panelMemberExclusions = caseData.getSchedulingAndListingFields().getPanelMemberExclusions();
            SscsUtil.setAdjournmentPanelMembersExclusions(panelMemberExclusions, adjournment.getPanelMembers(), panelMemberExcluded);
        }
    }

    private void updateOverrideFields(SscsCaseData caseData) {
        OverrideFields fields = caseData.getSchedulingAndListingFields().getOverrideFields();
        Adjournment adjournment = caseData.getAdjournment();

        if (isNull(fields)) {
            fields = OverrideFields.builder().build();
            caseData.getSchedulingAndListingFields().setOverrideFields(fields);
        }

        if (nonNull(adjournment.getTypeOfHearing())) {
            fields.setAppellantHearingChannel(adjournment.getTypeOfHearing().getHearingChannel());
        }

        var nextHearingVenueSelected = adjournment.getNextHearingVenueSelected();

        if (nonNull(nextHearingVenueSelected)) {
            var venueDetails = venueDataLoader.getVenueDetailsMap().get(nextHearingVenueSelected.getValue().getCode());

            if (nonNull(venueDetails)) {
                CcdValue<String> venueDetailsValue = new CcdValue<String>(venueDetails.getEpimsId());
                CcdValue<CcdValue<String>> ccdValue = new CcdValue<>(venueDetailsValue);
                fields.setHearingVenueEpimsIds(List.of(ccdValue));
            }
        }

        Integer duration = caseData.getAdjournment().getNextHearingListingDuration();
        if (duration != null && caseData.getAdjournment().getNextHearingListingDurationType() == AdjournCaseNextHearingDurationType.NON_STANDARD) {
            fields.setDuration(handleNonStandardDuration(caseData, duration));
        }

        if (isYes(adjournment.getInterpreterRequired())) {
            HearingInterpreter interpreter = HearingInterpreter.builder()
                .interpreterLanguage(adjournment.getInterpreterLanguage())
                .isInterpreterWanted(adjournment.getInterpreterRequired())
                .build();
            fields.setAppellantInterpreter(interpreter);
        }

        handleHearingWindow(caseData, fields);
    }

    public static final int DURATION_SESSIONS_MULTIPLIER = 165;
    public static final int DURATION_DEFAULT = 30;
    public static final int MIN_HEARING_DURATION = 30;
    public static final int MIN_HEARING_SESSION_DURATION = 1;

    private static Integer handleNonStandardDuration(SscsCaseData caseData, Integer duration) {
        AdjournCaseNextHearingDurationUnits units = caseData.getAdjournment().getNextHearingListingDurationUnits();
        if (units == AdjournCaseNextHearingDurationUnits.SESSIONS && duration >= MIN_HEARING_SESSION_DURATION) {
            return duration * DURATION_SESSIONS_MULTIPLIER;
        } else if (units == AdjournCaseNextHearingDurationUnits.MINUTES && duration >= MIN_HEARING_DURATION) {
            return duration;
        }
        return DURATION_DEFAULT;
    }

    private void handleHearingWindow(SscsCaseData caseData, OverrideFields overrideFields) {
        HearingWindow hearingWindow = overrideFields.getHearingWindow();
        Adjournment adjournment = caseData.getAdjournment();

        if (hearingWindow == null) {
            hearingWindow = HearingWindow.builder().build();
            overrideFields.setHearingWindow(hearingWindow);
        }

        if (AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER.equals(adjournment.getNextHearingDateType())) {
            if (AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE.equals(adjournment.getNextHearingDateOrPeriod())) {
                hearingWindow.setFirstDateTimeMustBe(adjournment.getNextHearingFirstAvailableDateAfterDate().plusDays(1).atStartOfDay());
            } else if (AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD.equals(adjournment.getNextHearingDateOrPeriod())) {
                long after = Long.valueOf(adjournment.getNextHearingFirstAvailableDateAfterPeriod().toString());
                hearingWindow.setFirstDateTimeMustBe(LocalDate.now().plusDays(after).atStartOfDay());
            }
        }
    }
}
