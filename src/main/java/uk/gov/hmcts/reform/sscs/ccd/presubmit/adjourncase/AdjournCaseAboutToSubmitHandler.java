package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.PAPER;

import java.time.LocalDate;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCasePanelMembersExcluded;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberExclusions;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdjournCaseAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final PreviewDocumentService previewDocumentService;

    private final AirLookupService airLookupService;

    private final RegionalProcessingCenterService regionalProcessingCenterService;

    private final ListAssistHearingMessageHelper hearingMessageHelper;

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

        if (SscsUtil.isSAndLCase(sscsCaseData)
            && isAdjournmentEnabled // TODO SSCS-10951
            && (isYes(adjournment.getCanCaseBeListedRightAway())
            || isNoOrNull(adjournment.getAreDirectionsBeingMadeToParties()))
        ) {
            adjournment.setAdjournmentInProgress(YES);
            hearingMessageHelper.sendListAssistCreateHearingMessage(sscsCaseData.getCcdCaseId());
        }

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

        if (adjournment.getGeneratedDate() == null) {
            adjournment.setGeneratedDate(LocalDate.now());
        }

        updateHearingChannel(sscsCaseData);

        updateExcludedPanelMembers(sscsCaseData);

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

    private static void updateExcludedPanelMembers(SscsCaseData caseData) {
        Adjournment adjournment = caseData.getAdjournment();
        AdjournCasePanelMembersExcluded panelMembersExcluded = adjournment.getPanelMembersExcluded();
        if (nonNull(panelMembersExcluded)) {
            PanelMemberExclusions panelMemberExclusions = caseData.getSchedulingAndListingFields()
                .getPanelMemberExclusions();

            if (panelMembersExcluded.equals(AdjournCasePanelMembersExcluded.YES)) {
                SscsUtil.excludePanelMembers(panelMemberExclusions, adjournment.getPanelMembers());
            } else if (panelMembersExcluded.equals(AdjournCasePanelMembersExcluded.RESERVED)) {
                panelMemberExclusions.setArePanelMembersReserved(YES);
            }
        }
    }
}
