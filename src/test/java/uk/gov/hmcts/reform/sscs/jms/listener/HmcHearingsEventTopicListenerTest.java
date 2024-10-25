package uk.gov.hmcts.reform.sscs.jms.listener;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.ADJOURNED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
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
import uk.gov.hmcts.reform.sscs.service.hmc.topic.ProcessHmcMessageServiceFactory;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.ProcessHmcMessageServiceV1;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.ProcessHmcMessageServiceV2;

@ExtendWith(MockitoExtension.class)
class HmcHearingsEventTopicListenerTest {

    public static final String SERVICE_CODE = "BBA3";

    private HmcHearingsEventTopicListener hmcHearingsEventTopicListener;

    private ProcessHmcMessageServiceFactory processHmcMessageServiceFactory;

    private ProcessHmcMessageService processHmcMessageService;

    @Mock
    private ProcessHmcMessageServiceV1 processHmcMessageServiceV1;

    @Mock
    private ProcessHmcMessageServiceV2 processHmcMessageServiceV2;

    @Mock
    private JmsBytesMessage bytesMessage;

    @Mock
    private ObjectMapper mockObjectMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeEach
    void setup() throws JMSException {
        processHmcMessageServiceFactory = new ProcessHmcMessageServiceFactory(Lists.newArrayList(processHmcMessageServiceV1, processHmcMessageServiceV2));
        hmcHearingsEventTopicListener = new HmcHearingsEventTopicListener(SERVICE_CODE, processHmcMessageServiceFactory);
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "objectMapper", mockObjectMapper);
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "sscsServiceCode", SERVICE_CODE);
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "isByPassHearingServiceEnabled", true);
        given(bytesMessage.getStringProperty("hmctsDeploymentId")).willReturn("test");
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "hmctsDeploymentId", "test");

        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "isDeploymentFilterEnabled", true);
        processHmcMessageService = (ProcessHmcMessageService) ReflectionTestUtils.getField(hmcHearingsEventTopicListener, "processHmcMessageService");

    }

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

        verifyProcessEventMessageCall(processHmcMessageService);
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

        verifyProcessEventMessageCall(processHmcMessageService);
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

        verifyProcessEventMessageCall(processHmcMessageService);
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

        throwMessageProcessingException(processHmcMessageService, hmcMessage);

        assertThatExceptionOfType(HmcEventProcessingException.class)
            .isThrownBy(() -> hmcHearingsEventTopicListener.onMessage(bytesMessage))
            .withCauseInstanceOf(MessageProcessingException.class);
    }

    private void verifyNoProcessEventMessageCall() throws CaseException, MessageProcessingException {
        verify(processHmcMessageService, never()).processEventMessage((any(HmcMessage.class)));
    }

    private void verifyProcessEventMessageCall(ProcessHmcMessageService processHmcMessageService) throws CaseException, MessageProcessingException {
        verify(processHmcMessageService).processEventMessage((any(HmcMessage.class)));
    }

    private void throwMessageProcessingException(ProcessHmcMessageService processHmcMessageService, HmcMessage hmcMessage) throws MessageProcessingException, CaseException {
        doThrow(MessageProcessingException.class)
                .when(processHmcMessageService)
                .processEventMessage(hmcMessage);
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
