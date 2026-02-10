package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.factory.NotificationWrapper;

public class NotificationsForAdminAppealWithdrawalTest extends NotificationProcessingServiceBaseTest {

    @Value("${notification.english.appealWithdrawn.appellant.docmosisId}")
    private String adminAppealWithdrawalDocmosisId;

    @BeforeEach
    public void setUp() {
        setNotificationProcessingService(initialiseNotificationService());
    }

    @Test
    public void adminAppealWithdrawalWhenNoSubscription_shouldSendMandatoryLetter() throws Exception {
        NotificationSscsCaseDataWrapper sscsCaseDataWrapper = initTestData();
        sscsCaseDataWrapper.getNewSscsCaseData().setCreatedInGapsFrom(READY_TO_LIST.getId());
        when(getPdfLetterService().generateLetter(any(), any(), any())).thenReturn("%PDF".getBytes());
        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(sscsCaseDataWrapper), false);

        verify(getNotificationGateway(), times(0)).sendEmail(any(), any(), any(), any(), any(), any());
        verify(getNotificationGateway(), times(0))
            .sendSms(any(), any(), any(), any(), any(), any(), any());
        verify(getNotificationExecutionManager(), times(1))
            .executeNotification(any(NotificationWrapper.class), eq(adminAppealWithdrawalDocmosisId), eq("Letter"),
                any());
    }

    private NotificationSscsCaseDataWrapper initTestData() {
        SscsCaseData newSscsCaseData = getSscsCaseData(null);
        newSscsCaseData.setSubscriptions(null);
        newSscsCaseData.getAppeal().setRep(null);
        return getSscsCaseDataWrapper(newSscsCaseData, null, ADMIN_APPEAL_WITHDRAWN);
    }

}
