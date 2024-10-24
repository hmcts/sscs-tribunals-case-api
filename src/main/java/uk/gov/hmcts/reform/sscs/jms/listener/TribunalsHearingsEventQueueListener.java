package uk.gov.hmcts.reform.sscs.jms.listener;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.LISTING_ERROR;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.exception.GetCaseException;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.exception.TribunalsEventProcessingException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.service.CcdCaseService;
import uk.gov.hmcts.reform.sscs.service.HearingsService;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty({"feature.bypass-hearing-api-service.enabled", "flags.tribunals-to-hearings-api.enabled"})
public class TribunalsHearingsEventQueueListener {

    private final HearingsService hearingsService;

    private final CcdCaseService ccdCaseService;

    @Value("${feature.bypass-hearing-api-service.enabled}")
    private boolean isByPassHearingServiceEnabled;

    @JmsListener(
            destination = "${azure.service-bus.tribunals-to-hearings-api.queueName}",
            containerFactory = "tribunalsHearingsEventQueueContainerFactory"
    )
    public void handleIncomingMessage(HearingRequest message) throws TribunalsEventProcessingException, GetCaseException, UpdateCaseException {

        log.info("isByPassHearingServiceEnabled ------------------------> {}", isByPassHearingServiceEnabled);
        if (!isByPassHearingServiceEnabled) {
            return;
        }

        if (isNull(message)) {
            throw new TribunalsEventProcessingException("An exception occurred as message did not match format");
        }
        String caseId = message.getCcdCaseId();
        HearingState event = message.getHearingState();

        log.info("Attempting to process hearing event {} from hearings event queue for case ID {}",
                 event, caseId);
        try {
            hearingsService.processHearingRequest(message);
            log.info("Hearing event {} for case ID {} successfully processed", event, caseId);
        } catch (ExhaustedRetryException e) {
            handleException(e.getCause(), caseId);
        } catch (Exception e) {
            handleException(e, caseId);
        }
    }

    private void handleException(Throwable throwable, String caseId) throws GetCaseException, UpdateCaseException, TribunalsEventProcessingException {
        if (throwable instanceof ListingException listingException) {
            log.error("Listing exception found, Summary: {}", listingException.getSummary(), listingException);

            SscsCaseData caseData = ccdCaseService.getCaseDetails(caseId).getData();
            ccdCaseService.updateCaseData(
                caseData,
                LISTING_ERROR,
                listingException.getSummary(),
                listingException.getDescription());

            log.info("Listing Error handled. State is now {}.", State.LISTING_ERROR);
        } else if (throwable instanceof Exception exception) {
            throw new TribunalsEventProcessingException("An exception occurred whilst processing hearing event", exception);
        }
    }
}
