package uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.Destination;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.Notification;

@RunWith(JUnitParamsRunner.class)
public class NotificationTest {

    @Test
    @Parameters({"07398785050", "+447398785050"})
    public void allowsValidUkMobilePhoneNumbers(String validPhoneNumber) {
        Destination destination = Destination.builder().sms(validPhoneNumber).build();
        Notification notification = Notification.builder().destination(destination).build();
        assertTrue(notification.isSms());
    }

    @Test
    @Parameters({"02038375920", "+442038375920"})
    public void doesNotAllowUkLandLineNumbers(String landLineNumber) {
        Destination destination = Destination.builder().sms(landLineNumber).build();
        Notification notification = Notification.builder().destination(destination).build();
        assertFalse(notification.isSms());
    }

    @Test
    @Parameters({"0014168213640", "+41762191008"})
    public void allowsInternationalNumbers(String landLineNumber) {
        Destination destination = Destination.builder().sms(landLineNumber).build();
        Notification notification = Notification.builder().destination(destination).build();
        assertTrue(notification.isSms());
    }

    @Test
    @Parameters({"07398785050,+447398785050", "(07)3987 85-050,+447398785050"})
    public void getMobileWillFormatTheNumber(String mobileNumber, String expectedNumber) {
        Destination destination = Destination.builder().sms(mobileNumber).build();
        Notification notification = Notification.builder().destination(destination).build();
        assertEquals(expectedNumber, notification.getMobile());
    }

    @Test
    @Parameters({"07+398785050", "07£398785050"})
    public void getMobileWillNotFormatAnInvalidPhoneNumber(String incorrectPhoneNumber) {
        Destination destination = Destination.builder().sms(incorrectPhoneNumber).build();
        Notification notification = Notification.builder().destination(destination).build();
        assertEquals(incorrectPhoneNumber, notification.getMobile());
    }

}
