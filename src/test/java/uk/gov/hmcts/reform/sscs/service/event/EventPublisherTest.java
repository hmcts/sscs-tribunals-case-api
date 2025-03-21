package uk.gov.hmcts.reform.sscs.service.event;

import static java.lang.Long.parseLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.service.servicebus.TopicPublisher;

public class EventPublisherTest {

    private EventPublisher eventPublisher;

    @Mock
    private TopicPublisher topicPublisher;


    private SscsCaseData sscsCaseData;

    private static final String JURISDICTION = "Benefit";

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        eventPublisher = new EventPublisher(topicPublisher);
        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1")
            .state(State.WITH_DWP)
            .appeal(Appeal.builder().build()).build();
    }

    @Test
    public void testPublishEvent() {

        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(parseLong(sscsCaseData.getCcdCaseId()), JURISDICTION,
            sscsCaseData.getState(), sscsCaseData, LocalDateTime.now(), "Benefit");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(),
            EventType.DWP_SUPPLEMENTARY_RESPONSE, false);

        eventPublisher.publishEvent(callback);

        verify(topicPublisher).sendMessage(eq(callback));
    }

}
