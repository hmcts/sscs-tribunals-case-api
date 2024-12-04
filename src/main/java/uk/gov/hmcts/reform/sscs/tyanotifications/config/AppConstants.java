package uk.gov.hmcts.reform.sscs.tyanotifications.config;

import java.time.format.DateTimeFormatter;

public final class AppConstants {

    public static final DateTimeFormatter CC_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String DAYS_STRING = " days";
    public static final String DWP_ACRONYM = "DWP";
    public static final String DWP_ACRONYM_WELSH = "AGP";
    public static final String DWP_FIRST_TIER_AGENCY_GROUP = "the Child Maintenance Group";
    public static final String DWP_FIRST_TIER_AGENCY_GROUP_TITLE = "Child Maintenance Group";
    public static final String DWP_FIRST_TIER_AGENCY_GROUP_WELSH = "Grŵp Cynhaliaeth Plant";
    public static final String DWP_FULL_NAME = "Department for Work and Pensions";
    public static final String DWP_FULL_NAME_WELSH = "Adran Gwaith a Phensiynau";
    public static final String FINAL_DECISION_DATE_FORMAT = "dd/MM/yyyy";
    public static final String HEARING_TIME_FORMAT = "hh:mm a";
    public static final String HMRC_ACRONYM = "HMRC";
    public static final String HMRC_ACRONYM_WELSH = "CThEM";
    public static final String HMRC_FULL_NAME = "Her Majesty's Revenue and Customs";
    public static final String HMRC_FULL_NAME_WELSH = "Cyllid a Thollau Ei Mawrhydi";
    public static final String IBCA_ACRONYM = "IBCA";
    public static final String JOINT_TEXT_WITH_A_SPACE = "joint ";
    public static final String JOINT_TEXT_WITH_A_SPACE_WELSH = "ar y cyd ";
    public static final String MAC_ALGO = "HmacSHA256";
    public static final String MAC_LITERAL = "mac";
    public static final String REP_SALUTATION = "Sir / Madam";
    public static final String RESPONSE_DATE_FORMAT = "d MMMM yyyy";
    public static final String THE_STRING = "the ";
    public static final String THE_STRING_WELSH = "yr ";
    public static final String TOMORROW_STRING = "tomorrow";
    public static final String ZONE_ID = "Europe/London";
    public static final int MAX_DWP_RESPONSE_DAYS = 28;
    public static final int MAX_DWP_RESPONSE_DAYS_CHILD_SUPPORT = 42;

    private AppConstants() {
        //
    }
}
