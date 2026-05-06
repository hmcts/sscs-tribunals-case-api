package uk.gov.hmcts.reform.sscs.jms.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static uk.gov.hmcts.reform.sscs.config.CorrelationIdFilter.CORRELATION_ID_MDC_KEY;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSException;
import java.nio.charset.StandardCharsets;
import org.apache.qpid.jms.message.JmsBytesMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.exception.HmcEventProcessingException;
import uk.gov.hmcts.reform.sscs.exception.MessageProcessingException;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HearingUpdate;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HmcMessage;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.ProcessHmcMessageServiceV2;

@ExtendWith(MockitoExtension.class)
class HmcHearingsEventTopicListenerCorrelationIdTest {

    private HmcHearingsEventTopicListener listener;

    @Mock
    private ProcessHmcMessageServiceV2 processHmcMessageServiceV2;

    @Mock
    private JmsBytesMessage bytesMessage;

    @Mock
    private ObjectMapper mockObjectMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() throws JMSException {
        listener = new HmcHearingsEventTopicListener(processHmcMessageServiceV2);
        ReflectionTestUtils.setField(listener, "objectMapper", mockObjectMapper);
        ReflectionTestUtils.setField(listener, "hmctsDeploymentId", "test");
        given(bytesMessage.getStringProperty("hmctsDeploymentId")).willReturn("test");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should set MDC correlationId from JMS correlation ID")
    void shouldSetMdcFromJmsCorrelationId() throws Exception {
        String jmsCorrelationId = "jms-corr-id-abc-123";
        given(bytesMessage.getJMSCorrelationID()).willReturn(jmsCorrelationId);

        HmcMessage hmcMessage = createHmcMessage();
        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);
        given(bytesMessage.getBodyLength()).willReturn((long) messageBytes.length);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        // Capture the MDC value during processing
        final String[] mdcDuringProcessing = {null};
        doAnswer(invocation -> {
            mdcDuringProcessing[0] = MDC.get(CORRELATION_ID_MDC_KEY);
            return null;
        }).when(processHmcMessageServiceV2).processEventMessage(any(HmcMessage.class));

        listener.onMessage(bytesMessage);

        assertThat(mdcDuringProcessing[0]).isEqualTo(jmsCorrelationId);
    }

    @Test
    @DisplayName("Should generate UUID when JMS correlation ID is null")
    void shouldGenerateUuidWhenJmsCorrelationIdIsNull() throws Exception {
        given(bytesMessage.getJMSCorrelationID()).willReturn(null);

        HmcMessage hmcMessage = createHmcMessage();
        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);
        given(bytesMessage.getBodyLength()).willReturn((long) messageBytes.length);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        final String[] mdcDuringProcessing = {null};
        doAnswer(invocation -> {
            mdcDuringProcessing[0] = MDC.get(CORRELATION_ID_MDC_KEY);
            return null;
        }).when(processHmcMessageServiceV2).processEventMessage(any(HmcMessage.class));

        listener.onMessage(bytesMessage);

        assertThat(mdcDuringProcessing[0]).isNotNull().isNotBlank();
        // Validate it looks like a UUID
        assertThat(mdcDuringProcessing[0]).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("Should generate UUID when JMS correlation ID is blank")
    void shouldGenerateUuidWhenJmsCorrelationIdIsBlank() throws Exception {
        given(bytesMessage.getJMSCorrelationID()).willReturn("   ");

        HmcMessage hmcMessage = createHmcMessage();
        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);
        given(bytesMessage.getBodyLength()).willReturn((long) messageBytes.length);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        final String[] mdcDuringProcessing = {null};
        doAnswer(invocation -> {
            mdcDuringProcessing[0] = MDC.get(CORRELATION_ID_MDC_KEY);
            return null;
        }).when(processHmcMessageServiceV2).processEventMessage(any(HmcMessage.class));

        listener.onMessage(bytesMessage);

        assertThat(mdcDuringProcessing[0]).isNotNull().isNotBlank();
        assertThat(mdcDuringProcessing[0]).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("Should clear MDC after successful processing")
    void shouldClearMdcAfterSuccessfulProcessing() throws Exception {
        given(bytesMessage.getJMSCorrelationID()).willReturn("corr-id-success");

        HmcMessage hmcMessage = createHmcMessage();
        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);
        given(bytesMessage.getBodyLength()).willReturn((long) messageBytes.length);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        listener.onMessage(bytesMessage);

        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should clear MDC even when exception is thrown during processing")
    void shouldClearMdcWhenExceptionThrown() throws Exception {
        given(bytesMessage.getJMSCorrelationID()).willReturn("corr-id-error");

        HmcMessage hmcMessage = createHmcMessage();
        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);
        given(bytesMessage.getBodyLength()).willReturn((long) messageBytes.length);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        doThrow(MessageProcessingException.class)
                .when(processHmcMessageServiceV2)
                .processEventMessage(hmcMessage);

        assertThatExceptionOfType(HmcEventProcessingException.class)
                .isThrownBy(() -> listener.onMessage(bytesMessage));

        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should not contaminate MDC of calling thread with pre-existing values")
    void shouldNotContaminateCallingThreadMdc() throws Exception {
        // Simulate a pre-existing MDC value from a previous context (should not happen but defensive check)
        MDC.put(CORRELATION_ID_MDC_KEY, "pre-existing-value");

        given(bytesMessage.getJMSCorrelationID()).willReturn("new-corr-id");

        HmcMessage hmcMessage = createHmcMessage();
        byte[] messageBytes = OBJECT_MAPPER.writeValueAsString(hmcMessage).getBytes(StandardCharsets.UTF_8);
        given(bytesMessage.getBodyLength()).willReturn((long) messageBytes.length);
        given(mockObjectMapper.readValue(any(String.class), eq(HmcMessage.class))).willReturn(hmcMessage);

        listener.onMessage(bytesMessage);

        // After processing, the MDC should be cleared (not restored to pre-existing)
        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
    }

    private HmcMessage createHmcMessage() {
        return HmcMessage.builder()
                .hmctsServiceCode("BBA3")
                .caseId(1234L)
                .hearingId("testId")
                .hearingUpdate(HearingUpdate.builder()
                        .hmcStatus(HmcStatus.ADJOURNED)
                        .build())
                .build();
    }
}
