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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
class AbstractHmcHearingsEventTopicListenerTest {

    public static final String SERVICE_CODE = "BBA3";

    private HmcHearingsEventTopicListener hmcHearingsEventTopicListener;

    protected ProcessHmcMessageServiceFactory processHmcMessageServiceFactory;

    private ProcessHmcMessageService processHmcMessageService;

    @Mock ProcessHmcMessageServiceV1 processHmcMessageServiceV1;

    @Mock ProcessHmcMessageServiceV2 processHmcMessageServiceV2;

    @Mock
    private JmsBytesMessage bytesMessage;

    @Mock
    private ObjectMapper mockObjectMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeEach
    void setup() throws JMSException {
        given(processHmcMessageServiceV1.isProcessEventMessageV2Enabled()).willReturn(Boolean.FALSE);
        given(processHmcMessageServiceV2.isProcessEventMessageV2Enabled()).willReturn(Boolean.TRUE);
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

    @DisplayName("Messages should not be processed if their service code does not match the service.")
    @ParameterizedTest
    @CsvSource({"true", "false"})
    void testOnMessage_serviceCodeNotApplicable(boolean isProcessHmcMessageServiceV2Enabled) throws Exception {
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "processEventMessageV2Enabled", isProcessHmcMessageServiceV2Enabled);
        HmcMessage hmcMessage = createHmcMessage("BBA4");

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);

        given(bytesMessage.getBodyLength()).willReturn((long) messageBytes.length);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        hmcHearingsEventTopicListener.onMessage(bytesMessage);

        verifyNoProcessEventMessageCall();
    }

    @DisplayName("Messages should not be processed if their deployment ID does not match ours.")
    @ParameterizedTest
    @CsvSource({"true", "false"})
    void testOnMessage_deploymentNotApplicable(boolean isProcessHmcMessageServiceV2Enabled) throws Exception {
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "processEventMessageV2Enabled", isProcessHmcMessageServiceV2Enabled);
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "hmctsDeploymentId", "test2");
        HmcMessage hmcMessage = createHmcMessage("BBA3");

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);

        hmcHearingsEventTopicListener.onMessage(bytesMessage);

        verifyNoProcessEventMessageCall();
    }

    @DisplayName("Message should be processed if message deployment ID matches ours.")
    @ParameterizedTest
    @CsvSource({"true", "false"})
    void testOnMessage_deploymentApplicable(boolean isProcessHmcMessageServiceV2Enabled) throws Exception {
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "processEventMessageV2Enabled", isProcessHmcMessageServiceV2Enabled);
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "hmctsDeploymentId", "test");
        HmcMessage hmcMessage = createHmcMessage("BBA3");

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        hmcHearingsEventTopicListener.onMessage(bytesMessage);

        verifyProcessEventMessageCall(processHmcMessageService);
    }


    @DisplayName("Messages should be processed if no deployment id is provided on message and service.")
    @ParameterizedTest
    @CsvSource({"true", "false"})
    void testOnMessage_noDeployment(boolean isProcessHmcMessageServiceV2Enabled) throws Exception {
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "processEventMessageV2Enabled", isProcessHmcMessageServiceV2Enabled);
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "hmctsDeploymentId", "");
        given(bytesMessage.getStringProperty("hmctsDeploymentId")).willReturn(null);
        HmcMessage hmcMessage = createHmcMessage("BBA3");

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        hmcHearingsEventTopicListener.onMessage(bytesMessage);

        verifyProcessEventMessageCall(processHmcMessageService);
    }

    @DisplayName("Messages should not be processed if deployment id is provided on message but not on service.")
    @ParameterizedTest
    @CsvSource({"true", "false"})
    void testOnMessage_noDeploymentInService(boolean isProcessHmcMessageServiceV2Enabled) throws Exception {
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "processEventMessageV2Enabled", isProcessHmcMessageServiceV2Enabled);
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "hmctsDeploymentId", "");
        HmcMessage hmcMessage = createHmcMessage("BBA3");

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);

        hmcHearingsEventTopicListener.onMessage(bytesMessage);

        verifyNoProcessEventMessageCall();
    }

    @DisplayName("Messages should be processed if their service code matches the service.")
    @ParameterizedTest
    @CsvSource({"true", "false"})
    void testOnMessage_serviceCodeApplicable(boolean isProcessHmcMessageServiceV2Enabled) throws Exception {
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "processEventMessageV2Enabled", isProcessHmcMessageServiceV2Enabled);
        HmcMessage hmcMessage = createHmcMessage(SERVICE_CODE);

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);

        given(bytesMessage.getBodyLength()).willReturn((long) messageBytes.length);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        hmcHearingsEventTopicListener.onMessage(bytesMessage);

        verifyProcessEventMessageCall(processHmcMessageService);
    }

    @DisplayName("A HmcEventProcessingException should be thrown if a JsonProcessing exception is encountered.")
    @ParameterizedTest
    @CsvSource({"true", "false"})
    void testOnMessage_JsonProcessingException(boolean isProcessHmcMessageServiceV2Enabled) throws JsonProcessingException, JMSException {
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "processEventMessageV2Enabled", isProcessHmcMessageServiceV2Enabled);
        HmcMessage hmcMessage = createHmcMessage(SERVICE_CODE);

        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);

        given(bytesMessage.getBodyLength()).willReturn((long) messageBytes.length);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class)))
            .willThrow(JsonProcessingException.class);

        assertThatExceptionOfType(HmcEventProcessingException.class)
            .isThrownBy(() -> hmcHearingsEventTopicListener.onMessage(bytesMessage))
            .withCauseInstanceOf(JsonProcessingException.class);
    }

    @DisplayName("A HmcEventProcessingException exception should be thrown an exception is encountered.")
    @ParameterizedTest
    @CsvSource({"true", "false"})
    void testOnMessage_HmcEventProcessingException(boolean isProcessHmcMessageServiceV2Enabled) throws Exception {
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "processEventMessageV2Enabled", isProcessHmcMessageServiceV2Enabled);
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
