package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;

public class NotificationServiceForAdminAppealWithdrawalTest extends NotificationServiceBase {

    @Value("${notification.english.appealWithdrawn.appellant.docmosisId}")
    private String adminAppealWithdrawalDocmosisId;

    @BeforeEach
    public void setUp() {
        setNotificationService(initialiseNotificationService());
    }

    @Test
    public void adminAppealWithdrawalWhenNoSubscription_shouldSendMandatoryLetter() throws Exception {
        NotificationSscsCaseDataWrapper sscsCaseDataWrapper = initTestData();
        sscsCaseDataWrapper.getNewSscsCaseData().setCreatedInGapsFrom(READY_TO_LIST.getId());
        when(getPdfLetterService().generateLetter(any(), any(), any())).thenReturn("%PDF".getBytes());
        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(sscsCaseDataWrapper), false);

        verify(getNotificationSender(), times(0)).sendEmail(any(), any(), any(), any(), any(), any());
        verify(getNotificationSender(), times(0))
            .sendSms(any(), any(), any(), any(), any(), any(), any());
        verify(getNotificationHandler(), times(1))
            .sendNotification(any(NotificationWrapper.class), eq(adminAppealWithdrawalDocmosisId), eq("Letter"),
                any());
    }

    private NotificationSscsCaseDataWrapper initTestData() {
        SscsCaseData newSscsCaseData = getSscsCaseData(null);
        newSscsCaseData.setSubscriptions(null);
        newSscsCaseData.getAppeal().setRep(null);
        return getSscsCaseDataWrapper(newSscsCaseData, null, ADMIN_APPEAL_WITHDRAWN);
    }

}
