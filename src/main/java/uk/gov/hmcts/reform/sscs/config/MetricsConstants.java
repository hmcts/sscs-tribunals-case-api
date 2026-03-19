package uk.gov.hmcts.reform.sscs.config;

public final class MetricsConstants {

    private MetricsConstants() {
    }

    // CCD Callbacks
    public static final String CCD_CALLBACK_DURATION = "sscs.ccd.callback.duration";
    public static final String CCD_CALLBACK_ERRORS = "sscs.ccd.callback.errors";

    // Notifications
    public static final String NOTIFICATION_SENT = "sscs.notification.sent";
    public static final String NOTIFICATION_FAILED = "sscs.notification.failed";

    // Evidence Share
    public static final String EVIDENCE_SHARE_SENT = "sscs.evidenceshare.sent";
    public static final String EVIDENCE_SHARE_FAILED = "sscs.evidenceshare.failed";

    // Bulk Scan
    public static final String BULK_SCAN_PROCESSED = "sscs.bulkscan.processed";
    public static final String BULK_SCAN_VALIDATION_FAILED = "sscs.bulkscan.validation.failed";

    // Hearings
    public static final String HEARINGS_REQUEST_DURATION = "sscs.hearings.request.duration";
    public static final String HEARINGS_REQUEST_ERRORS = "sscs.hearings.request.errors";
    public static final String HEARINGS_EVENTS_PROCESSED = "sscs.hearings.events.processed";
    public static final String HEARINGS_EVENTS_FAILED = "sscs.hearings.events.failed";

    // Service Bus
    public static final String SERVICEBUS_RECEIVED = "sscs.servicebus.messages.received";

    // Retries
    public static final String RETRY_ATTEMPTS = "sscs.retry.attempts";

    // Tags
    public static final String TAG_EVENT_TYPE = "eventType";
    public static final String TAG_CHANNEL = "channel";
    public static final String TAG_OPERATION = "operation";
    public static final String TAG_OUTCOME = "outcome";
}
