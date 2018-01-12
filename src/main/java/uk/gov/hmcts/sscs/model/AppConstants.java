package uk.gov.hmcts.sscs.model;

public class AppConstants {

    public static final String ADDRESS_LINE_1 = "address_line1";
    public static final String ADDRESS_LINE_2 = "address_line2";
    public static final String ADDRESS_LINE_3 = "address_line3";
    public static final String ADJOURNED_LETTER_RECEIVED_BY_DATE = "adjournedLetterReceivedByDate";
    public static final String APPEAL_REF = "appeal_ref";
    public static final String APPEAL_ID = "appeal_id";
    public static final String APPEAL_ID_LITERAL = "appeal_id";
    public static final String APPEAL_RESPOND_DATE = "appeal_respond_date";
    public static final String APPELLANT_NAME = "name";
    public static final String BENEFIT_NAME_ACRONYM = "ESA benefit";
    public static final String BENEFIT_FULL_NAME = "Employment Support Allowance";
    public static final String BENEFIT_NAME_ACRONYM_LITERAL = "benefit_name_acronym";
    public static final String BENEFIT_FULL_NAME_LITERAL = "benefit_full_name";
    public static final String CONTENT_KEY = "contentKey";
    public static final String DATE = "date";
    public static final String DECISION_LETTER_RECEIVE_BY_DATE = "decisionLetterReceiveByDate";
    public static final String DISPLAY_PARAGRAPH_LITERAL = "display_paragraph";
    public static final String DWP_RESPONSE_DATE_LITERAL = "dwpResponseDate";
    public static final String DWP_ACRONYM = "DWP";
    public static final String DWP_FUL_NAME = "Department for Work and Pensions";
    public static final String EMAIL = "email";
    public static final String EVIDENCE_TYPE = "evidence_type";
    public static final String EVIDENCE_PROVIDED_BY = "evidence_provided_by";
    public static final String FIRST_TIER_AGENCY_ACRONYM = "first_tier_agency_acronym";
    public static final String FIRST_TIER_AGENCY_FULL_NAME = "first_tier_agency_full_name";
    public static final String GOOGLE_MAP_URL = "google_map_url";
    public static final String HEARING_TIME_FORMAT = "hh:mm a";
    public static final String HEARING_DATETIME = "hearing_date_time";
    public static final String HEARING_DATE = "hearing_date";
    public static final String HEARING_TIME = "hearing_time";
    public static final String HEARING_CONTACT_DATE_LITERAL = "hearing_contact_date";
    public static final String MAC_ALGO = "HmacSHA256";
    public static final String MAC_LITERAL = "mac";
    public static final String MANAGE_EMAILS_LINK = "manage_emails_link";
    public static final String PHONE = "phone";
    public static final String PHONE_NUMBER = "phone_number";
    public static final String POSTCODE = "postcode";
    public static final String RESPONSE_DATE_FORMAT = "dd MMMM yyyy";
    public static final String SUBMIT_EVIDENCE_LINK_LITERAL = "submit_evidence_link";
    public static final String SUPPORTER_NAME = "supporter_name";
    public static final String TRACK_APPEAL_LINK_LITERAL = "track_appeal_link";
    public static final String TYPE = "type";
    public static final String VENUE_NAME = "venue_name";
    public static final String ZONE_ID = "Europe/London";

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
