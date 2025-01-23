package uk.gov.hmcts.reform.sscs.model;

import java.util.List;
import java.util.Objects;

public class AppConstants {

    public static final String ADDRESS_LINE_1 = "addressLine1";
    public static final String ADDRESS_LINE_2 = "addressLine2";
    public static final String ADDRESS_LINE_3 = "addressLine3";
    public static final String ADJOURNED_DATE = "adjournedDate";
    public static final String ADJOURNED_LETTER_RECEIVED_BY_DATE = "adjournedLetterReceivedByDate";
    public static final String CONTENT_KEY = "contentKey";
    public static final String DATE = "date";
    public static final String DECISION_LETTER_RECEIVE_BY_DATE = "decisionLetterReceiveByDate";
    public static final String DWP_RESPONSE_DATE_LITERAL = "dwpResponseDate";
    public static final String EVIDENCE_TYPE = "evidenceType";
    public static final String EVIDENCE_PROVIDED_BY = "evidenceProvidedBy";
    public static final String GOOGLE_MAP_URL = "googleMapUrl";
    public static final String HEARING_DATETIME = "hearingDateTime";
    public static final String HEARING_CONTACT_DATE_LITERAL = "hearingContactDate";
    public static final String PHONE = "phone";
    public static final String POSTCODE = "postcode";
    public static final String TYPE = "type";
    public static final String VENUE_NAME = "venueName";

    public static final int ADJOURNED_LETTER_RECEIVED_MAX_DAYS = 7;
    public static final int DWP_RESPONSE_HEARING_CONTACT_DATE_IN_WEEKS = 8;
    public static final int DORMANT_TO_CLOSED_DURATION_IN_MONTHS = 1;
    public static final int ADJOURNED_HEARING_DATE_CONTACT_WEEKS = 6;
    public static final int HEARING_DECISION_LETTER_RECEIVED_MAX_DAYS = 7;
    public static final int MAX_DWP_RESPONSE_DAYS = 28;
    public static final int MAX_DWP_RESPONSE_DAYS_CHILD_SUPPORT = 42;
    public static final int PAST_HEARING_BOOKED_IN_WEEKS = 8;

    public static final String DWP_DOCUMENT_AT38_FILENAME_PREFIX = "AT38 received";
    public static final String DWP_DOCUMENT_APPENDIX12_FILENAME_PREFIX = "Appendix 12 received";
    public static final String DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX = "FTA response received";
    public static final String DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX = "FTA evidence received";
    public static final String DWP_DOCUMENT_EDITED_RESPONSE_FILENAME_PREFIX = "FTA edited response received";
    public static final String DWP_DOCUMENT_EDITED_EVIDENCE_FILENAME_PREFIX = "FTA edited evidence received";
    public static final String DATE_FORMAT_YYYYMMDD = "yyyy-MM-dd";
    public static final List<String> BENEFIT_CODES_FOR_ISSUE_AND_SEND_TO_ADMIN = List.of("093", "016", "022");
    public static final String IBCA_BENEFIT_CODE = "093";
    public static final String INFECTED_BLOOD_COMPENSATION = "infectedBloodCompensation";
    public static final int FUNCTIONAL_FETCH_ATTEMPTS = 5;
    public static final int FUNCTIONAL_RETRY_LIMIT = 3;

    private AppConstants() {
        //
    }
}
