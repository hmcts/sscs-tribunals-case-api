package uk.gov.hmcts.reform.sscs.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.sscs.callback.controllers.EvidenceNotifyCallbackController;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.servicebus.EvidenceNotifyCallbackProcessor;

public class EvidenceNotifyCallbackControllerTest {

    private EvidenceNotifyCallbackController controller;

    @Mock
    private EvidenceNotifyCallbackProcessor callbackHandler;

    @Mock
    private SscsCaseCallbackDeserializer deserializer;

    @Before
    public void setUp() {
        openMocks(this);
        controller = new EvidenceNotifyCallbackController(callbackHandler, deserializer);
    }

    @Test
    public void shouldCreateAndSendNotificationForSscsCaseData() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().build();
        var time = LocalDateTime.now();
        CaseDetails<SscsCaseData> details = new CaseDetails<>(1L, "jurisdiction", null, sscsCaseData, time, "Benefit");
        EventType eventType = EventType.APPEAL_RECEIVED;
        when(deserializer.deserialize(anyString())).thenReturn(new Callback<>(details, Optional.empty(), eventType, false));
        ResponseEntity<String> responseEntity = controller.send("");
        verify(callbackHandler).handle(any());
        assertEquals(200, responseEntity.getStatusCode().value());
        assertEquals("{}", responseEntity.getBody());
    }

}
