package uk.gov.hmcts.reform.sscs.jms.listener;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.ADJOURNED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import javax.jms.JMSException;
import org.apache.qpid.jms.message.JmsBytesMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.exception.CaseException;
import uk.gov.hmcts.reform.sscs.exception.HmcEventProcessingException;
import uk.gov.hmcts.reform.sscs.exception.MessageProcessingException;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HearingUpdate;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HmcMessage;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.ProcessHmcMessageService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.ProcessHmcMessageServiceV2;


@ExtendWith(MockitoExtension.class)
abstract class AbstractHmcHearingsEventTopicListenerTest {

    public static final String SERVICE_CODE = "BBA3";

    private HmcHearingsEventTopicListener hmcHearingsEventTopicListener;

    @Mock
    private ProcessHmcMessageService processHmcMessageService;

    @Mock
    private ProcessHmcMessageServiceV2 processHmcMessageServiceV2;

    @Mock
    private JmsBytesMessage bytesMessage;

    @Mock
    private ObjectMapper mockObjectMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public abstract Boolean isProcessHmcMessageServiceV2Enabled();

    @BeforeEach
    void setup() throws JMSException {
        hmcHearingsEventTopicListener = new HmcHearingsEventTopicListener(SERVICE_CODE, processHmcMessageService, processHmcMessageServiceV2);
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "objectMapper", mockObjectMapper);
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "sscsServiceCode", SERVICE_CODE);
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "processEventMessageV2Enabled", isProcessHmcMessageServiceV2Enabled());
        given(bytesMessage.getStringProperty("hmctsDeploymentId")).willReturn("test");
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "hmctsDeploymentId", "test");

        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "isDeploymentFilterEnabled", true);

    }

    private void verifyNoProcessEventMessageCall() throws CaseException, MessageProcessingException {
        verify(processHmcMessageService, never()).processEventMessage((any(HmcMessage.class)));
        verify(processHmcMessageServiceV2, never()).processEventMessageV2((any(HmcMessage.class)));
    }

    public abstract void verifyProcessEventMessageCall(ProcessHmcMessageService processHmcMessageService, ProcessHmcMessageServiceV2 processHmcMessageServiceV2) throws CaseException, MessageProcessingException;

    public abstract void throwMessageProcessingException(ProcessHmcMessageService processHmcMessageService, ProcessHmcMessageServiceV2 processHmcMessageServiceV2, HmcMessage hmcMessage) throws MessageProcessingException, CaseException;

    @Test
    @DisplayName("Messages should not be processed if their service code does not match the service.")
    void testOnMessage_serviceCodeNotApplicable() throws Exception {

        HmcMessage hmcMessage = createHmcMessage("BBA4");

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);

        given(bytesMessage.getBodyLength()).willReturn((long) messageBytes.length);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        hmcHearingsEventTopicListener.onMessage(bytesMessage);

        verifyNoProcessEventMessageCall();
    }

    @Test
    @DisplayName("Messages should not be processed if their deployment ID does not match ours.")
    void testOnMessage_deploymentNotApplicable() throws Exception {
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "hmctsDeploymentId", "test2");
        HmcMessage hmcMessage = createHmcMessage("BBA3");

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);

        hmcHearingsEventTopicListener.onMessage(bytesMessage);

        verifyNoProcessEventMessageCall();
    }

    @Test
    @DisplayName("Message should be processed if message deployment ID matches ours.")
    void testOnMessage_deploymentApplicable() throws Exception {
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "hmctsDeploymentId", "test");
        HmcMessage hmcMessage = createHmcMessage("BBA3");

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        hmcHearingsEventTopicListener.onMessage(bytesMessage);

        verifyProcessEventMessageCall(processHmcMessageService, processHmcMessageServiceV2);
    }


    @Test
    @DisplayName("Messages should be processed if no deployment id is provided on message and service.")
    void testOnMessage_noDeployment() throws Exception {
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "hmctsDeploymentId", "");
        given(bytesMessage.getStringProperty("hmctsDeploymentId")).willReturn(null);
        HmcMessage hmcMessage = createHmcMessage("BBA3");

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        hmcHearingsEventTopicListener.onMessage(bytesMessage);

        verifyProcessEventMessageCall(processHmcMessageService, processHmcMessageServiceV2);
    }

    @Test
    @DisplayName("Messages should not be processed if deployment id is provided on message but not on service.")
    void testOnMessage_noDeploymentInService() throws Exception {
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "hmctsDeploymentId", "");
        HmcMessage hmcMessage = createHmcMessage("BBA3");

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);

        hmcHearingsEventTopicListener.onMessage(bytesMessage);

        verifyNoProcessEventMessageCall();
    }



    @Test
    @DisplayName("Messages should be processed if their service code matches the service.")
    void testOnMessage_serviceCodeApplicable() throws Exception {

        HmcMessage hmcMessage = createHmcMessage(SERVICE_CODE);

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);

        given(bytesMessage.getBodyLength()).willReturn((long) messageBytes.length);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        hmcHearingsEventTopicListener.onMessage(bytesMessage);

        verifyProcessEventMessageCall(processHmcMessageService, processHmcMessageServiceV2);
    }

    @Test
    @DisplayName("A HmcEventProcessingException should be thrown if a JsonProcessing exception is encountered.")
    void testOnMessage_JsonProcessingException() throws JsonProcessingException, JMSException {

        HmcMessage hmcMessage = createHmcMessage(SERVICE_CODE);

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);

        given(bytesMessage.getBodyLength()).willReturn((long) messageBytes.length);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class)))
            .willThrow(JsonProcessingException.class);

        assertThatExceptionOfType(HmcEventProcessingException.class)
            .isThrownBy(() -> hmcHearingsEventTopicListener.onMessage(bytesMessage))
            .withCauseInstanceOf(JsonProcessingException.class);
    }

    @Test
    @DisplayName("A HmcEventProcessingException exception should be thrown an exception is encountered.")
    void testOnMessage_HmcEventProcessingException() throws Exception {

        HmcMessage hmcMessage = createHmcMessage(SERVICE_CODE);

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);

        given(bytesMessage.getBodyLength()).willReturn((long) messageBytes.length);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        throwMessageProcessingException(processHmcMessageService, processHmcMessageServiceV2, hmcMessage);

        assertThatExceptionOfType(HmcEventProcessingException.class)
            .isThrownBy(() -> hmcHearingsEventTopicListener.onMessage(bytesMessage))
            .withCauseInstanceOf(MessageProcessingException.class);
    }

    private HmcMessage createHmcMessage(String messageServiceCode) {
        return HmcMessage.builder()
                .hmctsServiceCode(messageServiceCode)
                .caseId(1234L)
                .hearingId("testId")
                .hearingUpdate(HearingUpdate.builder()
                        .hmcStatus(ADJOURNED)
                        .build())
                .build();
    }
}