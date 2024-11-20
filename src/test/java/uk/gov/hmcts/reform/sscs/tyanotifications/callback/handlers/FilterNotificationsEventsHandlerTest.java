package uk.gov.hmcts.reform.sscs.tyanotifications.callback.handlers;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;

import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.RetryNotificationService;

public class FilterNotificationsEventsHandlerTest {
    @Mock
    private NotificationService notificationService;

    @Mock
    private RetryNotificationService retryNotificationService;

    @InjectMocks
    private FilterNotificationsEventsHandler handler;

    private NotificationSscsCaseDataWrapper callback;
    private SscsCaseData newCaseData;
    private SscsCaseData oldCaseData;
    private AutoCloseable autoCloseable;

    @BeforeEach
    public void setUp() {
        autoCloseable = openMocks(this);

        oldCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(Appellant.builder().build())
                .build())
            .postponementRequest(PostponementRequest.builder().build())
            .build();

        newCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(Appellant.builder().build())
                .build())
            .build();

        callback = NotificationSscsCaseDataWrapper.builder()
            .notificationEventType(VALID_APPEAL_CREATED)
            .oldSscsCaseData(oldCaseData)
            .newSscsCaseData(newCaseData)
            .build();
    }

    @AfterEach
    public void after() throws Exception {
        autoCloseable.close();
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({
        "ACTION_HEARING_RECORDING_REQUEST",
        "ACTION_POSTPONEMENT_REQUEST_WELSH",
        "ADJOURNED",
        "ADMIN_APPEAL_WITHDRAWN",
        "APPEAL_DORMANT",
        "APPEAL_LAPSED",
        "APPEAL_RECEIVED",
        "APPEAL_WITHDRAWN",
        "DECISION_ISSUED",
        "DECISION_ISSUED_WELSH",
        "DIRECTION_ISSUED",
        "DIRECTION_ISSUED_WELSH",
        "DIRECTION_ISSUED_WELSH",
        "DRAFT_TO_NON_COMPLIANT",
        "DRAFT_TO_VALID_APPEAL_CREATED",
        "DWP_APPEAL_LAPSED",
        "DWP_RESPONSE_RECEIVED",
        "DWP_UPLOAD_RESPONSE",
        "EVIDENCE_RECEIVED",
        "HEARING_BOOKED",
        "ISSUE_ADJOURNMENT_NOTICE",
        "ISSUE_FINAL_DECISION",
        "ISSUE_FINAL_DECISION_WELSH",
        "JOINT_PARTY_ADDED",
        "NON_COMPLIANT",
        "POSTPONEMENT",
        "PROCESS_AUDIO_VIDEO",
        "PROCESS_AUDIO_VIDEO_WELSH",
        "REISSUE_DOCUMENT",
        "REQUEST_FOR_INFORMATION",
        "RESEND_APPEAL_CREATED",
        "REVIEW_CONFIDENTIALITY_REQUEST",
        "STRUCK_OUT",
        "SUBSCRIPTION_UPDATED",
        "UPDATE_OTHER_PARTY_DATA",
        "VALID_APPEAL_CREATED"
    })
    public void willHandleEvents(NotificationEventType notificationEventType) {
        callback.setNotificationEventType(notificationEventType);

        willHandle(callback);
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({
        "DO_NOT_SEND",
        "SYA_APPEAL_CREATED",
        "null"})
    public void willNotHandleEvents(@Nullable NotificationEventType notificationEventType) {
        callback.setNotificationEventType(notificationEventType);

        willNotHandle(callback);
    }

    // JunitParamsRunnerToParameterized conversion not supported
@Parameters({"DO_NOT_SEND", "SYA_APPEAL_CREATED"})
    public void willThrowExceptionIfTriesToHandleEvents(NotificationEventType notificationEventType) {
        callback.setNotificationEventType(notificationEventType);

        willNotHandle(callback);
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"grant", "refuse"})
    public void willHandleActionPostponementRequestEvents(String actionSelected) {
        callback.setNotificationEventType(ACTION_POSTPONEMENT_REQUEST);
        newCaseData.getPostponementRequest().setActionPostponementRequestSelected(actionSelected);

        willHandle(callback);
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"sendToJudge", "refuseOnTheDay"})
    public void willNotHandleActionPostponementRequestEvents(String actionSelected) {
        callback.setNotificationEventType(ACTION_POSTPONEMENT_REQUEST);
        newCaseData.getPostponementRequest().setActionPostponementRequestSelected(actionSelected);

        willNotHandle(callback);
    }

    @ParameterizedTest
    public void willHandleForNonGapsHearingRoutes() {
        callback.setNotificationEventType(HEARING_BOOKED);
        newCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);

        willHandle(callback);
    }

    @ParameterizedTest
    public void wontHandleForGapsHearingRoutes() {
        callback.setNotificationEventType(HEARING_BOOKED);
        newCaseData.getSchedulingAndListingFields().setHearingRoute(GAPS);

        willNotHandle(callback);
    }

    @ParameterizedTest
    @MethodSource("eventTypeAndNewAppointees")
    public void willHandleDeathOfAppellantEventsWithNewAppointee(NotificationEventType notificationEventType,
                                                                 Appointee existing, Appointee newlyAdded) {
        callback.setNotificationEventType(notificationEventType);

        String isAppointeeExisting = getIsAppointee(nonNull(existing));
        oldCaseData.getAppeal().getAppellant().setIsAppointee(isAppointeeExisting);
        oldCaseData.getAppeal().getAppellant().setAppointee(existing);

        String isAppointeeNew = getIsAppointee(nonNull(newlyAdded));
        newCaseData.getAppeal().getAppellant().setIsAppointee(isAppointeeNew);
        newCaseData.getAppeal().getAppellant().setAppointee(newlyAdded);

        willHandle(callback);
    }

    @ParameterizedTest
    @MethodSource("eventTypeAndNoNewAppointees")
    public void willNotHandleDeathOfAppellantEventsWithoutNewAppointee(NotificationEventType notificationEventType,
                                                                       Appointee existing, Appointee newlyAdded) {
        callback.setNotificationEventType(notificationEventType);

        String isAppointeeExisting = getIsAppointee(nonNull(existing));
        oldCaseData.getAppeal().getAppellant().setIsAppointee(isAppointeeExisting);
        oldCaseData.getAppeal().getAppellant().setAppointee(existing);

        String isAppointeeNew = getIsAppointee(nonNull(newlyAdded));
        newCaseData.getAppeal().getAppellant().setIsAppointee(isAppointeeNew);
        newCaseData.getAppeal().getAppellant().setAppointee(newlyAdded);

        willNotHandle(callback);
    }

    @ParameterizedTest
    public void shouldCallToRescheduleNotificationWhenErrorIsNotificationServiceExceptionError() {
        doThrow(new NotificationServiceException("error msg test", new RuntimeException("error")))
            .when(notificationService)
            .manageNotificationAndSubscription(new CcdNotificationWrapper(callback), false);

        assertThatExceptionOfType(NotificationServiceException.class)
            .isThrownBy(() -> handler.handle(callback))
            .withMessageContaining("error msg test");

        verify(retryNotificationService).rescheduleIfHandledGovNotifyErrorStatus(
            eq(1), eq(new CcdNotificationWrapper(callback)), any(NotificationServiceException.class));
    }

    @ParameterizedTest
    public void shouldRescheduleNotificationWhenErrorIsNotANotificationServiceException() {
        doThrow(new RuntimeException("error msg test"))
            .when(notificationService)
            .manageNotificationAndSubscription(new CcdNotificationWrapper(callback), false);

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> handler.handle(callback))
            .withMessageContaining("error msg test");

        verifyNoInteractions(retryNotificationService);
    }

    private void willHandle(NotificationSscsCaseDataWrapper callback) {
        assertThat(handler.canHandle(callback)).isTrue();
        handler.handle(callback);
        verify(notificationService).manageNotificationAndSubscription(new CcdNotificationWrapper(callback), false);
        verifyNoInteractions(retryNotificationService);
    }

    private void willNotHandle(NotificationSscsCaseDataWrapper callback) {
        assertThat(handler.canHandle(callback)).isFalse();
        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> handler.handle(callback))
            .withMessage("Cannot handle callback");
        verifyNoInteractions(notificationService);
        verifyNoInteractions(retryNotificationService);
    }

    private static String getIsAppointee(boolean appointee) {
        return appointee ? YES.getValue() : NO.getValue();
    }

    private static Object[] eventTypeAndNewAppointees() {
        Appointee appointeeBefore = Appointee.builder().name(Name.builder().firstName("John").build()).build();
        Appointee appointeeAfter = Appointee.builder().name(Name.builder().firstName("Harry").build()).build();
        return new Object[]{
            new Object[]{DEATH_OF_APPELLANT, null, Appointee.builder().build()},
            new Object[]{DEATH_OF_APPELLANT, appointeeBefore, appointeeAfter},
            new Object[]{PROVIDE_APPOINTEE_DETAILS, null, Appointee.builder().build()},
            new Object[]{PROVIDE_APPOINTEE_DETAILS, appointeeBefore, appointeeAfter},
        };
    }

    private static Object[] eventTypeAndNoNewAppointees() {
        Appointee appointee = Appointee.builder().name(Name.builder().firstName("John").build()).build();
        return new Object[]{
            new Object[]{DEATH_OF_APPELLANT, null, null},
            new Object[]{DEATH_OF_APPELLANT, appointee, appointee},
            new Object[]{PROVIDE_APPOINTEE_DETAILS, null, null},
            new Object[]{PROVIDE_APPOINTEE_DETAILS, appointee, appointee},
        };
    }
}
