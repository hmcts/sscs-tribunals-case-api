package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.RoboticsService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@Slf4j
@Service
public class RoboticsCallbackHandler implements CallbackHandler<SscsCaseData> {

    private final RoboticsService roboticsService;

    private final DispatchPriority dispatchPriority;

    private final CcdService ccdService;

    private final IdamService idamService;

    private final RegionalProcessingCenterService regionalProcessingCenterService;

    @Value("${feature.gaps-switchover.enabled}")
    private boolean gapsSwitchOverFeature;

    @Autowired
    public RoboticsCallbackHandler(RoboticsService roboticsService,
                                   CcdService ccdService,
                                   IdamService idamService,
                                   RegionalProcessingCenterService regionalProcessingCenterService
    ) {
        this.roboticsService = roboticsService;
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.dispatchPriority = DispatchPriority.EARLIEST;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && ((callback.getEvent() == VALID_APPEAL_CREATED
            || callback.getEvent() == APPEAL_TO_PROCEED
            || callback.getEvent() == EventType.READY_TO_LIST
            || callback.getEvent() == EventType.VALID_APPEAL
            || callback.getEvent() == INTERLOC_VALID_APPEAL
            || callback.getEvent() == EventType.SEND_TO_DWP
            || callback.getEvent() == EventType.DWP_RAISE_EXCEPTION)
            && !callback.getCaseDetails().getCaseData().isTranslationWorkOutstanding())
            || callback.getEvent() == RESEND_CASE_TO_GAPS2;
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        log.info("Processing robotics for case id {} in evidence share service", callback.getCaseDetails().getId());

        SscsCaseDetails latestCase = ccdService.getByCaseId(callback.getCaseDetails().getId(), idamService.getIdamTokens());

        if (gapsSwitchOverFeature && HearingRoute.LIST_ASSIST == callback.getCaseDetails().getCaseData()
            .getSchedulingAndListingFields().getHearingRoute()) {
            log.info("Hearing route is: {}. Case {} will not be sent to robotics.",
                HearingRoute.LIST_ASSIST, callback.getCaseDetails().getId());

            updateCaseDataIfEventApplicable(callback, "Case sent to List Assist", "Updated case with sent to List Assist");

            return;
        }

        try {
            boolean isCaseValidToSendToRobotics = checkCaseValidToSendToRobotics(callback, latestCase);
            log.info("Is case valid to send to robotics {} for case id {}", isCaseValidToSendToRobotics, callback.getCaseDetails().getId());

            if (isCaseValidToSendToRobotics) {
                updateRpc(callback);
                roboticsService.sendCaseToRobotics(callback.getCaseDetails());

                // As part of ticket SSCS-6869, a new field was required to let the caseworker know when a case had been sent to GAPS2 via Robotics. However, if this was done as part of this handler it would need to be updated
                // as part of a separate event. Some of the events handled in this class are handled in the SendToBulkPrintHandler, which also triggers an update to CCD. If both these events occur at the same time then we would
                // end up with a concurrent case modification error from CCD. Therefore, this is an attempt to safely update the case. For events not handled in SendToBulkPrintHandler then trigger a separate updateCase event.
                // For events that are handled in the SendToBulkPrintHandler then just update the case data here, as the case would be saved to CCD further down the chain as part of the sentToDwp event in SendToBulkPrintHandler.

                callback.getCaseDetails().getCaseData().setDateCaseSentToGaps(LocalDate.now().toString());
                callback.getCaseDetails().getCaseData().setDateTimeCaseSentToGaps(LocalDateTime.now().toString());

                updateCaseDataIfEventApplicable(callback, "Case sent to robotics", "Updated case with date sent to robotics");

            }
        } catch (Exception e) {
            log.error("Error when sending to robotics: {}", callback.getCaseDetails().getId(), e);
        }
    }

    private void updateCaseDataIfEventApplicable(Callback<SscsCaseData> callback, String summary,
                                                 String description) {
        String ccdEventType = null;
        if (callback.getEvent() == DWP_RAISE_EXCEPTION) {
            ccdEventType = NOT_LISTABLE.getCcdType();
        } else if (callback.getEvent() == EventType.READY_TO_LIST
            || callback.getEvent() == RESEND_CASE_TO_GAPS2) {
            ccdEventType = CASE_UPDATED.getCcdType();
        }

        if (ccdEventType != null) {
            ccdService.updateCase(callback.getCaseDetails().getCaseData(),
                Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId()),
                ccdEventType, summary, description,
                idamService.getIdamTokens());
        }
    }

    private void updateRpc(final Callback<SscsCaseData> callback) {
        // Updating the RPC also done on CASE_UPDATED in tribunals-api.
        // We should update the case details before sending robotics.
        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        if (sscsCaseData.getAppeal().getAppellant() != null && sscsCaseData.getAppeal().getAppellant().getAddress() != null && sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode() != null) {
            RegionalProcessingCenter rpc = regionalProcessingCenterService.getByPostcode(sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode());
            sscsCaseData.setRegionalProcessingCenter(rpc);

            if (rpc != null) {
                sscsCaseData.setRegion(rpc.getName());
            }
        }
    }

    private boolean checkCaseValidToSendToRobotics(Callback<SscsCaseData> callback, SscsCaseDetails latestCaseDetails) {

        log.info("The callback event is {} and the createdInGapsFrom field is {} for case id {}", callback.getEvent(), callback.getCaseDetails().getCaseData().getCreatedInGapsFrom(), callback.getCaseDetails().getId());

        if (callback.getEvent() == RESEND_CASE_TO_GAPS2
            || (callback.getEvent() == DWP_RAISE_EXCEPTION)) {
            return true;
        }


        if (!notSentToRoboticsWithinThreshold(latestCaseDetails)) {
            log.info("Case {} already sent to robotics within last 24 hours. Skipping sending to robotics", callback.getCaseDetails().getId());
            return false;

        }

        return (callback.getCaseDetails().getCaseData().getCreatedInGapsFrom() == null
            || equalsIgnoreCase(callback.getCaseDetails().getCaseData().getCreatedInGapsFrom(), callback.getCaseDetails().getState().getId()));
    }

    private boolean notSentToRoboticsWithinThreshold(SscsCaseDetails latestCaseDetails) {

        boolean canSend = true;

        if (latestCaseDetails != null && latestCaseDetails.getData() != null) {
            Optional<LocalDateTime> sentToGapsO = latestCaseDetails.getData().getDateTimeSentToGaps();

            canSend = sentToGapsO.map(sentToGaps -> {
                LocalDateTime maxCutOff = LocalDateTime.now().minusHours(24);
                return sentToGaps.isBefore(maxCutOff);

            }).orElse(true);

        }
        return canSend;
    }

    @Override
    public DispatchPriority getPriority() {
        return this.dispatchPriority;
    }
}
