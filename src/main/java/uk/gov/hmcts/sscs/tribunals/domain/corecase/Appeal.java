package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import java.time.ZonedDateTime;

public class Appeal {

    private String type;

    private Mrn mrn;

    private ZonedDateTime dlReceivedDate;

    private String appealFormat;

    private String receiptFormat;

    private String originatingOffice;

    private ZonedDateTime dateAppealMade;

    private String requestingParty;

    private Boolean ftaReconsiderationEnclosed;

    private Boolean isAdmissable;

    private Boolean isFurtherEvidenceRequired;

    private ZonedDateTime notificationDate;

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public Mrn getMrn() { return mrn; }

    public void setMrn(Mrn mrn) { this.mrn = mrn; }

    public ZonedDateTime getDlReceivedDate() { return dlReceivedDate; }

    public void setDlReceivedDate(ZonedDateTime dlReceivedDate) { this.dlReceivedDate = dlReceivedDate; }

    public String getAppealFormat() { return appealFormat; }

    public void setAppealFormat(String appealFormat) { this.appealFormat = appealFormat; }

    public String getReceiptFormat() { return receiptFormat; }

    public void setReceiptFormat(String receiptFormat) { this.receiptFormat = receiptFormat; }

    public String getOriginatingOffice() { return originatingOffice; }

    public void setOriginatingOffice(String originatingOffice) { this.originatingOffice = originatingOffice; }

    public ZonedDateTime getDateAppealMade() { return dateAppealMade; }

    public void setDateAppealMade(ZonedDateTime dateAppealMade) { this.dateAppealMade = dateAppealMade; }

    public String getRequestingParty() { return requestingParty; }

    public void setRequestingParty(String requestingParty) { this.requestingParty = requestingParty; }

    public Boolean getFtaReconsiderationEnclosed() { return ftaReconsiderationEnclosed; }

    public void setFtaReconsiderationEnclosed(Boolean ftaReconsiderationEnclosed) { this.ftaReconsiderationEnclosed = ftaReconsiderationEnclosed; }

    public Boolean getAdmissable() { return isAdmissable; }

    public void setAdmissable(Boolean admissable) { isAdmissable = admissable; }

    public Boolean getFurtherEvidenceRequired() { return isFurtherEvidenceRequired; }

    public void setFurtherEvidenceRequired(Boolean furtherEvidenceRequired) { isFurtherEvidenceRequired = furtherEvidenceRequired; }

    public ZonedDateTime getNotificationDate() { return notificationDate; }

    public void setNotificationDate(ZonedDateTime notificationDate) { this.notificationDate = notificationDate; }

    @Override
    public String toString() {
        return "Appeal{" + " type='" + type + '\'' + ", mrn=" + mrn + ", dlReceivedDate=" + dlReceivedDate + ", appealFormat='" + appealFormat + '\'' + ", receiptFormat='" + receiptFormat + '\'' + ", originatingOffice='" + originatingOffice + '\'' + ", dateAppealMade=" + dateAppealMade + ", requestingParty='" + requestingParty + '\'' + ", ftaReconsiderationEnclosed=" + ftaReconsiderationEnclosed + ", isAdmissable=" + isAdmissable + ", isFurtherEvidenceRequired=" + isFurtherEvidenceRequired + ", notificationDate=" + notificationDate + '}';
    }
}
