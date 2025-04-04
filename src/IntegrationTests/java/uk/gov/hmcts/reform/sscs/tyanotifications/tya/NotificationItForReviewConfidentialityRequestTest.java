package uk.gov.hmcts.reform.sscs.tyanotifications.tya;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.tyanotifications.helper.IntegrationTestHelper.getRequestWithAuthHeader;
import static uk.gov.hmcts.reform.sscs.tyanotifications.helper.IntegrationTestHelper.updateEmbeddedJson;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.DatedRequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;

public class NotificationItForReviewConfidentialityRequestTest extends NotificationsItBase {

    @NamedParameters("grantedOrRefused")
    @SuppressWarnings("unused")
    private Object[] grantedOrRefused() {
        return new Object[]{
            new DatedRequestOutcome[]{DatedRequestOutcome.builder()
                .requestOutcome(RequestOutcome.GRANTED).date(LocalDate.now()).build()},
            new DatedRequestOutcome[]{DatedRequestOutcome.builder()
                .requestOutcome(RequestOutcome.REFUSED).date(LocalDate.now()).build()},
        };
    }

    @Test
    @Parameters(named = "grantedOrRefused")
    public void givenAppellantConfidentialityRequest_shouldSendConfidentialityLetter(DatedRequestOutcome requestOutcome) throws Exception {
        String path = getClass().getClassLoader().getResource("json/ccdResponseWithJointParty.json").getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = updateEmbeddedJson(json, "reviewConfidentialityRequest", "event_id");
        json = updateEmbeddedJson(json, requestOutcome, "case_details", "case_data", "confidentialityRequestOutcomeAppellant");

        getResponse(getRequestWithAuthHeader(json));

        verify(notificationClient, times(0)).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(0)).sendSms(any(), any(), any(), any(), any());
        verify(notificationClient, times(1)).sendPrecompiledLetterWithInputStream(any(), any());
    }

    @Test
    @Parameters(named = "grantedOrRefused")
    public void givenJointPartyConfidentialityRequest_shouldSendConfidentialityLetter(DatedRequestOutcome requestOutcome) throws Exception {
        String path = getClass().getClassLoader().getResource("json/ccdResponseWithJointParty.json").getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = updateEmbeddedJson(json, "reviewConfidentialityRequest", "event_id");
        json = updateEmbeddedJson(json, requestOutcome, "case_details", "case_data", "confidentialityRequestOutcomeJointParty");

        getResponse(getRequestWithAuthHeader(json));

        verify(notificationClient, times(0)).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(0)).sendSms(any(), any(), any(), any(), any());
        verify(notificationClient, times(1)).sendPrecompiledLetterWithInputStream(any(), any());
    }

    @Test
    @Parameters(named = "grantedOrRefused")
    public void givenJointPartyAndAppellantConfidentialityRequest_shouldSendBothConfidentialityLetters(DatedRequestOutcome requestOutcome) throws Exception {
        String path = getClass().getClassLoader().getResource("json/ccdResponseWithJointParty.json").getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = updateEmbeddedJson(json, "reviewConfidentialityRequest", "event_id");
        json = updateEmbeddedJson(json, requestOutcome, "case_details", "case_data", "confidentialityRequestOutcomeAppellant");
        json = updateEmbeddedJson(json, requestOutcome, "case_details", "case_data", "confidentialityRequestOutcomeJointParty");

        getResponse(getRequestWithAuthHeader(json));

        verify(notificationClient, times(0)).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(0)).sendSms(any(), any(), any(), any(), any());
        verify(notificationClient, times(2)).sendPrecompiledLetterWithInputStream(any(), any());
    }
}
