package uk.gov.hmcts.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;


@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SyaEvidence {

    private String url;
    private String fileName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate uploadedDate;

    public SyaEvidence(@JsonProperty("url") String url,
                       @JsonProperty("fileName") String fileName,
                       @JsonProperty("uploadedDate") LocalDate uploadedDate) {
        this.url = url;
        this.fileName = fileName;
        this.uploadedDate = uploadedDate;
    }
}
