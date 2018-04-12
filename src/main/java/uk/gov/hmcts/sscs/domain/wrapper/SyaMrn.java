package uk.gov.hmcts.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class SyaMrn {

    private String dwpIssuingOffice;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate date;

    private String reasonForBeingLate;

    @JsonProperty("reasonForNoMRN")
    private String reasonForNoMrn;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dateAppealSubmitted;


    public SyaMrn() {
        //For JSON
    }

    public String getDwpIssuingOffice() {
        return dwpIssuingOffice;
    }

    public void setDwpIssuingOffice(String dwpIssuingOffice) {
        this.dwpIssuingOffice = dwpIssuingOffice;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getReasonForBeingLate() {
        return reasonForBeingLate;
    }

    public void setReasonForBeingLate(String reasonForBeingLate) {
        this.reasonForBeingLate = reasonForBeingLate;
    }

    public String getReasonForNoMrn() {
        return reasonForNoMrn;
    }

    public void setReasonForNoMrn(String reasonForNoMrn) {
        this.reasonForNoMrn = reasonForNoMrn;
    }

    public LocalDate getDateAppealSubmitted() {
        return dateAppealSubmitted;
    }

    public void setDateAppealSubmitted(LocalDate dateAppealSubmitted) {
        this.dateAppealSubmitted = dateAppealSubmitted;
    }

    @Override
    public String toString() {
        return "SyaMrn{"
                + " dwpIssuingOffice='" + dwpIssuingOffice + '\''
                + ", date=" + date
                + ", reasonForBeingLate='" + reasonForBeingLate + '\''
                + ", reasonForNoMrn='" + reasonForNoMrn + '\''
                + ", dateAppealSubmitted='" + dateAppealSubmitted + '\''
                + '}';
    }
}
