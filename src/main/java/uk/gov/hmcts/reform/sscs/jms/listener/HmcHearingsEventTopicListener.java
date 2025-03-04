package uk.gov.hmcts.reform.sscs.jms.listener;

import static uk.gov.hmcts.reform.sscs.service.HmcHearingApi.HMCTS_DEPLOYMENT_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.JMSException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.qpid.jms.message.JmsBytesMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.exception.HearingUpdateException;
import uk.gov.hmcts.reform.sscs.exception.HmcEventProcessingException;
import uk.gov.hmcts.reform.sscs.exception.MessageProcessingException;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HmcMessage;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.ProcessHmcMessageServiceV2;

@Slf4j
@Component
@ConditionalOnProperty("flags.hmc-to-hearings-api.enabled")
public class HmcHearingsEventTopicListener {

    private final ObjectMapper objectMapper;

    private final ProcessHmcMessageServiceV2 processHmcMessageServiceV2;

    @Value("${hmc.deployment-id}")
    private String hmctsDeploymentId;

    public HmcHearingsEventTopicListener(ProcessHmcMessageServiceV2 processHmcMessageServiceV2) {
        this.processHmcMessageServiceV2 = processHmcMessageServiceV2;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @JmsListener(
        destination = "${azure.service-bus.hmc-to-hearings-api.topicName}",
        subscription = "${azure.service-bus.hmc-to-hearings-api.subscriptionName}",
        containerFactory = "hmcHearingsEventTopicContainerFactory"
    )
    public void onMessage(JmsBytesMessage message) throws JMSException, HmcEventProcessingException {
        log.info("message deploymentId , {}", message.getStringProperty(HMCTS_DEPLOYMENT_ID));
        log.info("application deploymentId , {}", hmctsDeploymentId);

        byte[] messageBytes = new byte[(int) message.getBodyLength()];
        message.readBytes(messageBytes);
        String convertedMessage = new String(messageBytes, StandardCharsets.UTF_8);

        try {
            HmcMessage hmcMessage = objectMapper.readValue(convertedMessage, HmcMessage.class);
            Long caseId = hmcMessage.getCaseId();
            String hearingId = hmcMessage.getHearingId();

            log.info(
                "Attempting to process message from HMC hearings topic for event {}, Case ID {}, and Hearing ID {}.",
                hmcMessage.getHearingUpdate().getHmcStatus(),
                caseId,
                hearingId
            );

            processHmcMessageServiceV2.processEventMessage(hmcMessage);
        } catch (JsonProcessingException | MessageProcessingException
                 | HearingUpdateException | ExhaustedRetryException ex) {
            log.error("Unable to successfully deliver HMC message: {}", convertedMessage, ex);
            throw new HmcEventProcessingException(String.format(
                "Unable to successfully deliver HMC message: %s",
                convertedMessage
            ), ex);
        }
    }

}
