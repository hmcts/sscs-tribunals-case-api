package uk.gov.hmcts.sscs.model;

public class AppConstants {

    public static final String ADDRESS_LINE_1 = "address_line1";
    public static final String ADDRESS_LINE_2 = "address_line2";
    public static final String ADDRESS_LINE_3 = "address_line3";
    public static final String ADJOURNED_LETTER_RECEIVED_BY_DATE = "adjournedLetterReceivedByDate";
    public static final String CONTENT_KEY = "contentKey";
    public static final String DATE = "date";
    public static final String DECISION_LETTER_RECEIVE_BY_DATE = "decisionLetterReceiveByDate";
    public static final String DWP_RESPONSE_DATE_LITERAL = "dwpResponseDate";
    public static final String EMAIL = "email";
    public static final String EVIDENCE_TYPE = "evidence_type";
    public static final String EVIDENCE_PROVIDED_BY = "evidence_provided_by";
    public static final String GOOGLE_MAP_URL = "google_map_url";
    public static final String HEARING_DATETIME = "hearing_date_time";
    public static final String HEARING_CONTACT_DATE_LITERAL = "hearing_contact_date";
    public static final String PHONE = "phone";
    public static final String POSTCODE = "postcode";
    public static final String TYPE = "type";
    public static final String VENUE_NAME = "venue_name";

    public static final int ADJOURNED_LETTER_RECEIVED_MAX_DAYS = 7;
    public static final int DAYS_FROM_DWP_RESPONSE_DATE_FOR_HEARING_CONTACT = 42;
    public static final int DORMANT_TO_CLOSED_DURATION_IN_MONTHS = 1;
    public static final int HEARING_DATE_CONTACT_WEEKS = 6;
    public static final int HEARING_DECISION_LETTER_RECEIVED_MAX_DAYS = 7;
    public static final int MAX_DWP_RESPONSE_DAYS = 35;
    public static final int PAST_HEARING_BOOKED_IN_WEEKS = 6;

    private AppConstants(){
        //
    }


}
