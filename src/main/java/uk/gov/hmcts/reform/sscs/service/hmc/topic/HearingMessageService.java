package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareMessagingService;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareRequest;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * This class replaces an ASB queue implementation that used to exist between the tribunals API
 * and the SSCS Hearings API. It is now a direct call inside the Tribunals API.
 *
 * A future improvement would be to remove the json serialization and deserialization and directly
 * use the HearingRequest object.
 */
public class HearingMessageService implements SessionAwareMessagingService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final HearingMessageServiceListener hearingMessageServiceListener;

    @Override
    public boolean sendMessage(SessionAwareRequest message, SscsCaseData sscsCaseData) {
        try {
            var json = objectMapper.writeValueAsString(message);
            var hearingRequest = objectMapper.readValue(json, HearingRequest.class);

            hearingMessageServiceListener.handleIncomingMessage(hearingRequest, sscsCaseData);
            log.info("***************************HMS: {}", sscsCaseData.getSchedulingAndListingFields());
            return true;
        } catch (Exception ex) {
            log.error("Unable to send message {}. Cause: {}", message, ex.getMessage(), ex);
            return false;
        }
    }
}
