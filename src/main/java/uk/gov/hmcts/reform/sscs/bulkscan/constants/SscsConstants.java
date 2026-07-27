package uk.gov.hmcts.reform.sscs.bulkscan.constants;

public final class SscsConstants {

    private SscsConstants() {

    }

    public static final String KEEP_HOME_ADDRESS_CONFIDENTIAL = "keep_home_address_confidential";
    public static final String AGREE_LESS_HEARING_NOTICE_LITERAL = "agree_less_hearing_notice";
    public static final String APPEAL_GROUNDS = "appeal_grounds";
    public static final String APPEAL_GROUNDS_2 = "appeal_grounds_2";
    public static final String PERSON1_VALUE = "person1";
    public static final String PERSON_1_CHILD_MAINTENANCE_NUMBER = "person1_child_maintenance_number";
    public static final String PERSON2_VALUE = "person2";
    public static final String OTHER_PARTY_VALUE = "other_party";
    public static final String IS_OTHER_PARTY_ADDRESS_KNOWN = "is_other_party_address_known";
    public static final String REPRESENTATIVE_VALUE = "representative";
    public static final String HEARING_TYPE_ORAL = "oral";
    public static final String HEARING_TYPE_PAPER = "paper";
    public static final String IS_HEARING_TYPE_ORAL_LITERAL = "is_hearing_type_oral";
    public static final String IS_HEARING_TYPE_PAPER_LITERAL = "is_hearing_type_paper";
    public static final String HEARING_OPTIONS_SIGN_LANGUAGE_INTERPRETER_LITERAL = "hearing_options_sign_language_interpreter";
    public static final String HEARING_OPTIONS_SIGN_LANGUAGE_TYPE_LITERAL = "hearing_options_sign_language_type";
    public static final String HEARING_OPTIONS_LANGUAGE_TYPE_LITERAL = "hearing_options_language_type";
    public static final String HEARING_OPTIONS_DIALECT_LITERAL = "hearing_options_dialect";
    public static final String HEARING_OPTIONS_ACCESSIBLE_HEARING_ROOMS_LITERAL = "hearing_options_accessible_hearing_rooms";
    public static final String HEARING_OPTIONS_HEARING_LOOP_LITERAL = "hearing_options_hearing_loop";
    public static final String HEARING_OPTIONS_EXCLUDE_DATES_LITERAL = "hearing_options_exclude_dates";
    public static final String HEARING_SUPPORT_ARRANGEMENTS_LITERAL = "hearing_support_arrangements";
    public static final String DEFAULT_SIGN_LANGUAGE = "British Sign Language";
    public static final String BENEFIT_TYPE_DESCRIPTION = "benefit_type_description";
    public static final String BENEFIT_TYPE_OTHER = "benefit_type_other";
    public static final String IS_BENEFIT_TYPE_OTHER = "is_benefit_type_other";
    public static final String IS_BENEFIT_TYPE_TAX_CREDIT = "is_benefit_type_tax_credit";
    public static final String HEARING_TYPE_DESCRIPTION = "hearing_type";
    public static final String REPRESENTATIVE_NAME_OR_ORGANISATION_DESCRIPTION = "representative_name_or_organisation";
    public static final String TELL_TRIBUNAL_ABOUT_DATES = "tell_tribunal_about_dates";
    public static final String MRN_DATE = "mrn_date";
    public static final String ISSUING_OFFICE = "office";
    public static final String IBCA_ISSUING_OFFICE = "IBCA";
    public static final String YES_LITERAL = "Yes";
    public static final String NO_LITERAL = "No";
    public static final String TITLE = "_title";
    public static final String FIRST_NAME = "_first_name";
    public static final String LAST_NAME = "_last_name";
    public static final String ADDRESS_LINE1 = "_address_line1";
    public static final String ADDRESS_LINE2 = "_address_line2";
    public static final String ADDRESS_LINE3 = "_address_line3";
    public static final String ADDRESS_LINE4 = "_address_line4";
    public static final String ADDRESS_COUNTRY = "_Country";
    public static final String ADDRESS_PORT_OF_ENTRY = "_port_of_entry";
    public static final String ADDRESS_POSTCODE = "_postcode";
    public static final String PHONE = "_phone";
    public static final String MOBILE = "_mobile";
    public static final String EMAIL = "_email";
    public static final String NINO = "_nino";
    public static final String IBCA_REFERENCE = "_ibca_reference";
    public static final String DOB = "_dob";
    public static final String WANTS_SMS_NOTIFICATIONS = "_want_sms_notifications";
    public static final String IBC_ROLE_FOR_SELF = "person1_for_self";
    public static final String IBC_ROLE_FOR_U18 = "person1_for_person_under_18";
    public static final String IBC_ROLE_FOR_LACKING_CAPACITY = "person1_on_behalf_of_a_person_who_lacks_capacity";
    public static final String IBC_ROLE_FOR_POA = "person1_as_poa";
    public static final String IBC_ROLE_FOR_DECEASED = "person1_as_rep_of_deceased";
    public static final String IBC_ROLE = "ibcRole";
    public static final String IS_EMPTY = "is empty";
    public static final String IS_MISSING = "is missing";
    public static final String ARE_EMPTY = "are empty. At least one must be populated";
    public static final String IS_INVALID = "is invalid";
    public static final String FIELDS_EMPTY = "fields are empty";
    public static final String HAS_INVALID_ADDRESS  = "has invalid characters at the beginning";
    public static final String IS_IN_FUTURE = "is in future";
    public static final String IS_IN_PAST = "is in past";
    public static final String HEARING_TYPE_TELEPHONE_LITERAL = "hearing_type_telephone";
    public static final String HEARING_TELEPHONE_LITERAL = "hearing_telephone_number";
    public static final String HEARING_TELEPHONE_NUMBER_MULTIPLE_LITERAL = "hearing_telephone_number_multiple";
    public static final String HEARING_TYPE_VIDEO_LITERAL = "hearing_type_video";
    public static final String HEARING_VIDEO_EMAIL_LITERAL = "hearing_video_email";
    public static final String HEARING_TYPE_FACE_TO_FACE_LITERAL = "hearing_type_face_to_face";
    public static final String HEARING_SUB_TYPE_TELEPHONE_OR_VIDEO_FACE_TO_FACE_DESCRIPTION = "hearing_sub_type_telephone_or_video_face_to_face";
    public static final String PHONE_SELECTED_NOT_PROVIDED = "has not been provided but data indicates hearing telephone is required";
    public static final String EMAIL_SELECTED_NOT_PROVIDED = "has not been provided but data indicates hearing video is required";
    public static final String HEARING_EXCLUDE_DATES_MISSING = "Excluded dates have been provided which must be recorded on CCD";
    public static final String HAS_REPRESENTATIVE_FIELD_MISSING = "The \"Has representative\" field is not selected, please select an option to proceed";
    public static final String IS_PAYING_PARENT = "is_paying_parent";
    public static final String IS_RECEIVING_PARENT = "is_receiving_parent";
    public static final String IS_ANOTHER_PARTY = "is_another_party";
    public static final String OTHER_PARTY_DETAILS = "other_party_details";
    public static final String FORM_TYPE = "form_type";
    public static final String PORT_OF_ENTRY_INVALID_ERROR = "person1_port_of_entry is not a valid port of entry code. Please refer to guidance to update it to its associated port of entry code. Please refer to guidance.";

    public static final String INFECTED_BLOOD_COMPENSATION = "infectedBloodCompensation";
}
