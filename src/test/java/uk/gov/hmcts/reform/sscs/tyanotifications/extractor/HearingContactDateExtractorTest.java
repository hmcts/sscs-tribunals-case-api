package uk.gov.hmcts.reform.sscs.tyanotifications.extractor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;

import java.time.ZonedDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.tyanotifications.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;

@RunWith(JUnitParamsRunner.class)
public class HearingContactDateExtractorTest {

    @Mock
    private DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor;

    private final ZonedDateTime dwpResponseReceivedDate =
        ZonedDateTime.parse("2018-01-01T14:01:18Z[Europe/London]");

    private HearingContactDateExtractor hearingContactDateExtractor;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        hearingContactDateExtractor = new HearingContactDateExtractor(
            dwpResponseReceivedDateExtractor,
            60, 3600, 120);
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({
        "DWP_RESPONSE_RECEIVED, oral, 2018-01-01T14:02:18Z[Europe/London]",
        "DWP_RESPONSE_RECEIVED, paper, 2018-01-01T14:03:18Z[Europe/London]",
        "POSTPONEMENT, oral, 2018-01-01T14:02:18Z[Europe/London]"
    })
    public void extractsFirstHearingContactDate(NotificationEventType notificationEventType, String hearingType,
                                                String expectedHearingContactDate) {

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(notificationEventType,
            hearingType);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData()))
            .thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor
            .extract(wrapper.getSscsCaseDataWrapper());

        assertTrue(hearingContactDate.isPresent());
        assertEquals(ZonedDateTime.parse(expectedHearingContactDate), hearingContactDate.get());
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"DWP_RESPONSE_RECEIVED", "DWP_UPLOAD_RESPONSE"})
    public void givenDwpResponseReceivedEvent_thenExtractDateForReferenceEvent(NotificationEventType eventType) {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T14:02:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(eventType);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> dwpResponseReceivedDate = hearingContactDateExtractor.extract(wrapper.getSscsCaseDataWrapper());

        assertTrue(dwpResponseReceivedDate.isPresent());
        assertEquals(expectedHearingContactDate, dwpResponseReceivedDate.get());
    }

    @Test
    public void givenAdjournedEvent_thenExtractDateForReferenceEvent() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T14:02:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(ADJOURNED);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate =
            hearingContactDateExtractor.extractForReferenceEvent(
                wrapper.getNewSscsCaseData(),
                ADJOURNED
            );

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void returnsEmptyOptionalWhenDwpResponseReceivedDateIsNotPresent() {

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(DWP_RESPONSE_RECEIVED);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.empty());

        Optional<ZonedDateTime> dwpResponseReceivedDate = hearingContactDateExtractor.extract(wrapper.getSscsCaseDataWrapper());

        assertFalse(dwpResponseReceivedDate.isPresent());
    }

    @Test
    public void returnsEmptyOptionalWhenNotificationEventTypeNotAcceptable() {

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(APPEAL_RECEIVED);

        Optional<ZonedDateTime> dwpResponseReceivedDate = hearingContactDateExtractor.extract(wrapper.getSscsCaseDataWrapper());

        assertFalse(dwpResponseReceivedDate.isPresent());

        verify(dwpResponseReceivedDateExtractor, never()).extract(wrapper.getNewSscsCaseData());
    }

}
