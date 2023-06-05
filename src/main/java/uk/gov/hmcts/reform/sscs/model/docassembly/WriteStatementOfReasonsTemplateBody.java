package uk.gov.hmcts.reform.sscs.model.docassembly;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

import java.time.LocalDate;

@Builder(toBuilder = true)
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WriteStatementOfReasonsTemplateBody implements FormPayload {
    String name;

    @JsonProperty("sscs_url")
    String sscsUrl;

    String hmcts2;

    @JsonProperty("benefit_name_acronym")
    String benefitNameAcronym;

    @JsonProperty("benefit_name_acronym_welsh")
    String benefitNameAcronymWelsh;

    @JsonProperty("appeal_ref")
    String appealRef;

    @JsonProperty("phone_number")
    String phoneNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonProperty("hearing_date")
    LocalDate hearingDate;

    String entityType;

    @JsonProperty("address_name")
    String addressName;

    @JsonProperty("letter_address_line_1")
    String addressLine1;

    @JsonProperty("letter_address_line_2")
    String addressLine2;

    @JsonProperty("letter_address_line_3")
    String town;

    @JsonProperty("letter_address_line_4")
    String county;

    @JsonProperty("letter_address_postcode")
    String postcode;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonProperty("generated_date")
    LocalDate generatedDate;

}
