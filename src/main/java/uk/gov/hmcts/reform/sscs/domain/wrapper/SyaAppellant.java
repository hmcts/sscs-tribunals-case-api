package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import java.time.LocalDate;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
public class SyaAppellant {

    private String title;

    private String firstName;

    private String lastName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate dob;

    private String nino;

    @JsonProperty("contactDetails")
    private SyaContactDetails contactDetails;

    @JsonProperty("isAddressSameAsAppointee")
    private Boolean isAddressSameAsAppointee;

    public void setNino(String nino) {
        this.nino = nino.replaceAll("\\s", "");;
    }

    @JsonIgnore
    public String getFullName() {
        return title + " " + firstName + " " + lastName;
    }

}
