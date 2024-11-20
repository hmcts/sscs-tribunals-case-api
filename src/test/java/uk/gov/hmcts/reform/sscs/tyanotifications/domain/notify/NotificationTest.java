package uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify;

import static org.junit.jupiter.api.Assertions.*;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class NotificationTest {

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"07398785050", "+447398785050"})
    public void allowsValidUkMobilePhoneNumbers(String validPhoneNumber) {
        Destination destination = Destination.builder().sms(validPhoneNumber).build();
        Notification notification = Notification.builder().destination(destination).build();
        assertTrue(notification.isSms());
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"02038375920", "+442038375920"})
    public void doesNotAllowUkLandLineNumbers(String landLineNumber) {
        Destination destination = Destination.builder().sms(landLineNumber).build();
        Notification notification = Notification.builder().destination(destination).build();
        assertFalse(notification.isSms());
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"0014168213640", "+41762191008"})
    public void allowsInternationalNumbers(String landLineNumber) {
        Destination destination = Destination.builder().sms(landLineNumber).build();
        Notification notification = Notification.builder().destination(destination).build();
        assertTrue(notification.isSms());
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"07398785050,+447398785050", "(07)3987 85-050,+447398785050"})
    public void getMobileWillFormatTheNumber(String mobileNumber, String expectedNumber) {
        Destination destination = Destination.builder().sms(mobileNumber).build();
        Notification notification = Notification.builder().destination(destination).build();
        assertEquals(expectedNumber, notification.getMobile());
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"07+398785050", "07£398785050"})
    public void getMobileWillNotFormatAnInvalidPhoneNumber(String incorrectPhoneNumber) {
        Destination destination = Destination.builder().sms(incorrectPhoneNumber).build();
        Notification notification = Notification.builder().destination(destination).build();
        assertEquals(incorrectPhoneNumber, notification.getMobile());
    }

}
