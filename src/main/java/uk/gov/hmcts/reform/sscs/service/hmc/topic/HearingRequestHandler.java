package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.LISTING_ERROR;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.exception.GetCaseException;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.exception.TribunalsEventProcessingException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.service.HearingsService;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * This class replaces an ASB queue implementation that used to listen for messages from the Tribunals API
 * sent to the SSCS Hearings API. It is now a direct call inside the Tribunals API from the HearingMessageService.
 */
public class HearingRequestHandler {

    private final HearingsService hearingsService;

    private final UpdateCcdCaseService updateCcdCaseService;

    private final IdamService idamService;

    @Async
    public void handleHearingRequest(HearingRequest message) throws TribunalsEventProcessingException, GetCaseException, UpdateCaseException {
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

            log.info("Pre calling trigger Case Event V2 for handling Listing Error for case id: {}", caseId);
            updateCcdCaseService.triggerCaseEventV2(
                    Long.parseLong(caseId),
                    LISTING_ERROR.getCcdType(),
                    listingException.getSummary(),
                    listingException.getDescription(),
                    idamService.getIdamTokens());
            log.info("Triggered case event V2. Listing Error handled. State is now {}.", State.LISTING_ERROR);
        } else if (throwable instanceof Exception exception) {
            throw new TribunalsEventProcessingException("An exception occurred whilst processing hearing event", exception);
        }
    }
}
