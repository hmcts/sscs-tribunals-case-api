package uk.gov.hmcts.reform.sscs.tyanotifications.extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_RESPOND;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.APPEAL_RECEIVED;

import java.security.NoSuchAlgorithmException;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.tyanotifications.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;

@RunWith(JUnitParamsRunner.class)
public class DwpResponseReceivedDateExtractorTest {

    private DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor;

    @BeforeEach
    public void setUp() throws NoSuchAlgorithmException {
        dwpResponseReceivedDateExtractor = new DwpResponseReceivedDateExtractor();
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"DWP_RESPONSE_RECEIVED", "DWP_UPLOAD_RESPONSE"})
    public void extractsDwpResponseReceivedDate(NotificationEventType eventType) {

        String ccdEventDate = "2018-01-01T14:01:18";
        ZonedDateTime expectedDwpResponseReceivedDate = ZonedDateTime.parse("2018-01-01T14:01:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithEvent(
            eventType,
            DWP_RESPOND,
            ccdEventDate
        );

        Optional<ZonedDateTime> dwpResponseReceivedDate = dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData());

        assertTrue(dwpResponseReceivedDate.isPresent());
        assertEquals(expectedDwpResponseReceivedDate, dwpResponseReceivedDate.get());
    }

    @Test
    public void returnsEmptyOptionalWhenDateNotPresent() {

        CcdNotificationWrapper ccdResponse = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(
            APPEAL_RECEIVED
        );

        Optional<ZonedDateTime> dwpResponseReceivedDate = dwpResponseReceivedDateExtractor.extract(ccdResponse.getNewSscsCaseData());

        assertFalse(dwpResponseReceivedDate.isPresent());
    }

    @Test
    public void returnsDwpResponseDateWhenThereAreNoEvents() {
        ZonedDateTime expectedDwpResponseReceivedDate = ZonedDateTime.parse("2018-01-25T00:00:00Z[Europe/London]");

        CcdNotificationWrapper ccdResponse = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(
            APPEAL_RECEIVED
        );
        ccdResponse.getSscsCaseDataWrapper().getNewSscsCaseData().setDwpResponseDate("2018-01-25");

        Optional<ZonedDateTime> dwpResponseReceivedDate = dwpResponseReceivedDateExtractor.extract(ccdResponse.getNewSscsCaseData());

        assertThat(dwpResponseReceivedDate).isPresent();
        assertThat(dwpResponseReceivedDate.get().toLocalTime()).isBetween(LocalTime.MIN, LocalTime.MAX);
    }

}
