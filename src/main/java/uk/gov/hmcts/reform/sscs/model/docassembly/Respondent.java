package uk.gov.hmcts.reform.sscs.model.docassembly;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Respondent {
    @JsonProperty("name")
    private String name;

    @JsonIgnore
    public static String[] labelPrefixes = {"Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth", "Ninth", "Tenth", "Eleventh"};

    @JsonIgnore
    public static String HMRC = "Respondent: HM Revenue & Customs";

    @JsonIgnore
    public static String DWP = "Respondent: Secretary of State for Work and Pensions";

    @JsonIgnore
    public static String IBCA = "Respondent: Infected Blood Compensation Authority";
    // TODO translate
    @JsonIgnore
    public static String HMRC_WELSH = "Atebydd: Cyllid a Thollau EF";
    // TODO translate
    @JsonIgnore
    public static String DWP_WELSH = "Atebydd: Ysgrifennydd Gwladol dros Waith a Phensiynau";
    // TODO translate
    @JsonIgnore
    public static String IBCA_WELSH = "Atebydd: Awdurdod Iawndal Gwaed Heintiedig";
}
