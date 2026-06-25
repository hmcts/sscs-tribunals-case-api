package uk.gov.hmcts.reform.sscs.bulkscan.constants;

import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.ADDRESS_COUNTRY;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.ADDRESS_LINE1;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.ADDRESS_LINE2;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.ADDRESS_LINE3;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.ADDRESS_LINE4;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.ADDRESS_PORT_OF_ENTRY;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.ADDRESS_POSTCODE;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.DOB;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.FIRST_NAME;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_OPTIONS_EXCLUDE_DATES_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_TELEPHONE_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_VIDEO_EMAIL_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IBCA_REFERENCE;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.ISSUING_OFFICE;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.LAST_NAME;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.MOBILE;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.NINO;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.TITLE;
import static uk.gov.hmcts.reform.sscs.bulkscan.domain.CallbackType.EXCEPTION_CALLBACK;

import uk.gov.hmcts.reform.sscs.bulkscan.domain.CallbackType;

public enum WarningMessage {

    APPELLANT_TITLE(TITLE, "Appellant title"),
    APPOINTEE_TITLE(TITLE, "Appointee title"),
    REPRESENTATIVE_TITLE(TITLE, "Representative title"),
    OTHER_PARTY_TITLE(TITLE, "Other party title"),
    APPELLANT_FIRST_NAME(FIRST_NAME, "Appellant first name"),
    APPOINTEE_FIRST_NAME(FIRST_NAME, "Appointee first name"),
    REPRESENTATIVE_FIRST_NAME(FIRST_NAME, "Representative first name"),
    OTHER_PARTY_FIRST_NAME(FIRST_NAME, "Other party first name"),
    APPELLANT_LAST_NAME(LAST_NAME, "Appellant last name"),
    APPOINTEE_LAST_NAME(LAST_NAME, "Appointee last name"),
    REPRESENTATIVE_LAST_NAME(LAST_NAME, "Representative last name"),
    OTHER_PARTY_LAST_NAME(LAST_NAME, "Other party last name"),
    APPELLANT_ADDRESS_LINE1(ADDRESS_LINE1, "Appellant address line 1"),
    APPOINTEE_ADDRESS_LINE1(ADDRESS_LINE1, "Appointee address line 1"),
    REPRESENTATIVE_ADDRESS_LINE1(ADDRESS_LINE1, "Representative address line 1"),
    OTHER_PARTY_ADDRESS_LINE1(ADDRESS_LINE1, "Other party address line 1"),
    APPELLANT_ADDRESS_LINE2(ADDRESS_LINE2, "Appellant address town"),
    APPOINTEE_ADDRESS_LINE2(ADDRESS_LINE2, "Appointee address town"),
    REPRESENTATIVE_ADDRESS_LINE2(ADDRESS_LINE2, "Representative address town"),
    OTHER_PARTY_ADDRESS_LINE2(ADDRESS_LINE2, "Other party address town"),
    APPELLANT_ADDRESS_LINE3(ADDRESS_LINE3, "Appellant address town"),
    APPOINTEE_ADDRESS_LINE3(ADDRESS_LINE3, "Appointee address town"),
    REPRESENTATIVE_ADDRESS_LINE3(ADDRESS_LINE3, "Representative address town"),
    OTHER_PARTY_ADDRESS_LINE3(ADDRESS_LINE3, "Other party address county"),
    APPELLANT_ADDRESS_LINE3_COUNTY(ADDRESS_LINE3, "Appellant address county"),
    APPOINTEE_ADDRESS_LINE3_COUNTY(ADDRESS_LINE3, "Appointee address county"),
    REPRESENTATIVE_ADDRESS_LINE3_COUNTY(ADDRESS_LINE3, "Representative address county"),
    APPELLANT_ADDRESS_LINE4(ADDRESS_LINE4, "Appellant address county"),
    APPOINTEE_ADDRESS_LINE4(ADDRESS_LINE4, "Appointee address county"),
    REPRESENTATIVE_ADDRESS_LINE4(ADDRESS_LINE4, "Representative address county"),
    APPELLANT_POSTCODE(ADDRESS_POSTCODE, "Appellant postcode"),
    APPOINTEE_POSTCODE(ADDRESS_POSTCODE, "Appointee postcode"),
    REPRESENTATIVE_POSTCODE(ADDRESS_POSTCODE, "Representative postcode"),
    OTHER_PARTY_POSTCODE(ADDRESS_POSTCODE, "Other party postcode"),
    APPELLANT_ADDRESS_COUNTRY(ADDRESS_COUNTRY, "Appellant address country"),
    APPELLANT_ADDRESS_PORT_OF_ENTRY(ADDRESS_PORT_OF_ENTRY, "Appellant address port of entry"),
    APPELLANT_IBCA_REFERENCE(IBCA_REFERENCE, "Appellant ibca reference"),
    BENEFIT_TYPE_DESCRIPTION(SscsConstants.BENEFIT_TYPE_DESCRIPTION, "Benefit type description"),
    BENEFIT_TYPE_OTHER(SscsConstants.BENEFIT_TYPE_OTHER, "Benefit type description"),
    MRN_DATE(SscsConstants.MRN_DATE, "Mrn date"),
    APPEAL_GROUNDS(SscsConstants.APPEAL_GROUNDS, "Grounds for appeal"),
    OFFICE(ISSUING_OFFICE, "DWP issuing office"),
    HEARING_OPTIONS_EXCLUDE_DATES(HEARING_OPTIONS_EXCLUDE_DATES_LITERAL, "Hearing options exclude dates"),
    APPELLANT_NINO(NINO, "Appellant nino"),
    APPELLANT_MOBILE(MOBILE, "Appellant mobile"),
    APPOINTEE_MOBILE(MOBILE, "Appointee mobile"),
    REPRESENTATIVE_MOBILE(MOBILE, "Representative mobile"),
    APPELLANT_DOB(DOB, "Appellant date of birth"),
    APPOINTEE_DOB(DOB, "Appointee date of birth"),
    HEARING_TYPE("is_hearing_type_oral and/or is_hearing_type_paper", "Hearing type"),
    REPRESENTATIVE_NAME_OR_ORGANISATION("representative_company, representative_first_name and representative_last_name", "Representative organisation, Representative first name and Representative last name"),
    HEARING_TYPE_VIDEO(HEARING_VIDEO_EMAIL_LITERAL, "Hearing video email address"),
    HEARING_TELEPHONE_NUMBER_MULTIPLE("Telephone hearing selected but the number used is invalid. Please check either the hearing_telephone_number or person1_phone fields", "Telephone hearing selected but the number used is invalid. Please check either the telephone or hearing telephone number fields"),
    HEARING_TYPE_TELEPHONE(HEARING_TELEPHONE_LITERAL, "Hearing telephone number"),
    HEARING_SUB_TYPE_TELEPHONE_OR_VIDEO_FACE_TO_FACE("hearing_type_telephone, hearing_type_video and hearing_type_face_to_face", "Hearing option telephone, video and face to face"),
    PERSON1_CHILD_MAINTENANCE_NUMBER("person1_child_maintenance_number", "Child maintenance number"),
    APPELLANT_PARTY_NAME("is_paying_parent, is_receiving_parent, is_another_party and other_party_details", "Appellant role and/or description"),
    APPELLANT_PARTY_DESCRIPTION("other_party_details", "Appellant role and/or description");
    private String exceptionRecordMessage;
    private String validationRecordMessage;

    WarningMessage(String exceptionRecordMessage, String validationRecordMessage) {
        this.exceptionRecordMessage = exceptionRecordMessage;
        this.validationRecordMessage = validationRecordMessage;
    }

    public static String getMessageByCallbackType(CallbackType callbackType, String personType, String name, String endMessage) {
        String startMessage =  callbackType == EXCEPTION_CALLBACK
            ? personType + valueOf(name.toUpperCase()).exceptionRecordMessage
            : valueOf(name.toUpperCase()).validationRecordMessage;

        return endMessage != null ? startMessage + " " + endMessage : startMessage;
    }

}
