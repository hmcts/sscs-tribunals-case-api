package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.resolvePostCode;

import java.time.LocalDate;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.service.VenueService;

@Component
@Slf4j
@AllArgsConstructor
public class AdjournCaseAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final PreviewDocumentService previewDocumentService;
    private final UserDetailsService userDetailsService;
    private final AirLookupService airLookupService;
    private final VenueService venueService;

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

        if (nonNull(adjournment.getNextHearingVenue()) && adjournment.getNextHearingVenue() == AdjournCaseNextHearingVenue.SAME_VENUE) {
            String processingVenue = sscsCaseData.getProcessingVenue();
            String postCode = resolvePostCode(sscsCaseData);
            String newVenueName = airLookupService.lookupAirVenueNameByPostCode(postCode, sscsCaseData.getAppeal().getBenefitType());
            if (!Objects.equals(processingVenue, newVenueName)) {
                String newVenueEpims = venueService.getEpimsIdForVenue(newVenueName);
                VenueDetails newVenueDetails = venueService.getVenueDetailsForActiveVenueByEpimsId(newVenueEpims);
                if (nonNull(newVenueDetails) && Objects.equals(newVenueDetails.getLegacyVenue(), processingVenue)) {
                    sscsCaseData.setProcessingVenue(newVenueName);
                }
            }
        }

        previewDocumentService.writePreviewDocumentToSscsInternalDocument(
                sscsCaseData,
                DRAFT_ADJOURNMENT_NOTICE,
                adjournment.getPreviewDocument());

        try {
            adjournment.setSignedInUser(userDetailsService.getLoggedInUserAsJudicialUser(userAuthorisation));
        } catch (Exception e) {
            log.error(e.getMessage());
        }


        adjournment.setGeneratedDate(LocalDate.now());

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
