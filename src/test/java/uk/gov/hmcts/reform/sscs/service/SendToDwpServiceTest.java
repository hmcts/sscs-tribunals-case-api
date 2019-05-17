package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SEND_TO_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SENT_TO_DWP;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getSyaCaseWrapper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class SendToDwpServiceTest {

    @Mock
    private CcdService ccdService;

    SendToDwpService sendToDwpService;

    private SscsCaseData data = convertSyaToCcdCaseData(getSyaCaseWrapper());

    @Before
    public void setup() {
        initMocks(this);
        sendToDwpService = new SendToDwpService(ccdService);
        ReflectionTestUtils.setField(sendToDwpService, "sendToDwpFeature", true);
    }

    @Test
    public void givenAPaperCase_thenTriggerSendToDwpEvent() {
        data.getAppeal().setReceivedVia("Paper");
        IdamTokens tokens = IdamTokens.builder().build();
        sendToDwpService.sendToDwp(data, 1L, tokens);

        verify(ccdService).updateCase(eq(data), eq(1L), eq(SEND_TO_DWP.getCcdType()), eq("Send to DWP"), eq("Send to DWP event has been triggered from Tribunals service"), eq(tokens));
    }

    @Test
    public void givenAnOnlineCase_thenTriggerSentToDwpEvent() {
        data.getAppeal().setReceivedVia("Online");
        IdamTokens tokens = IdamTokens.builder().build();
        sendToDwpService.sendToDwp(data, 1L, tokens);

        verify(ccdService).updateCase(eq(data), eq(1L), eq(SENT_TO_DWP.getCcdType()), eq("Sent to DWP"), eq("Case has been sent to the DWP by Robotics"), eq(tokens));
    }

    @Test
    public void givenSendToDwpFeatureFlagIsOff_thenDoNotUpdateCase() {
        ReflectionTestUtils.setField(sendToDwpService, "sendToDwpFeature", false);

        data.getAppeal().setReceivedVia("Online");
        IdamTokens tokens = IdamTokens.builder().build();
        sendToDwpService.sendToDwp(data, 1L, tokens);

        verifyNoMoreInteractions(ccdService);
    }

}