package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import uk.gov.hmcts.sscs.service.xml.CustomDateTimeXmlAdapter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.ZonedDateTime;

public class Appeal {

    private String caseCode;

    private String originatingOffice;

    private ZonedDateTime dateOfDecision;

    private ZonedDateTime dateAppealMade;

    private Boolean ftaReconsiderationEnclosed;

    private Boolean isAdmissable;

    private Boolean isFurtherEvidenceRequired;

    public Appeal(String caseCode, String originatingOffice, ZonedDateTime dateOfDecision, ZonedDateTime dateAppealMade, Boolean ftaReconsiderationEnclosed, Boolean isAdmissable, Boolean isFurtherEvidenceRequired) {
        this.caseCode = caseCode;
        this.dateOfDecision = dateOfDecision;
        this.originatingOffice = originatingOffice;
        this.dateAppealMade = dateAppealMade;
        this.ftaReconsiderationEnclosed = ftaReconsiderationEnclosed;
        this.isAdmissable = isAdmissable;
        this.isFurtherEvidenceRequired = isFurtherEvidenceRequired;
    }

    @XmlTransient
    public String getCaseCode() { return caseCode; }

    @XmlElement(name = "caseCode", required = true)
    public String getCaseCodeWithSuffix() { return caseCode + "DD"; }

    public void setCaseCode(String caseCode) { this.caseCode = caseCode; }

    @XmlElement
    public String getOriginatingOffice() { return originatingOffice; }

    public void setOriginatingOffice(String originatingOffice) { this.originatingOffice = originatingOffice; }

    @XmlElement(name = "dateOfDecision", required = true)
    @XmlJavaTypeAdapter(CustomDateTimeXmlAdapter.class)
    public ZonedDateTime getDateOfDecision() {
        return dateOfDecision;
    }

    public void setDateOfDecision(ZonedDateTime dateOfDecision) {
        this.dateOfDecision = dateOfDecision;
    }

    @XmlElement(name = "dateAppealMade", required = true)
    @XmlJavaTypeAdapter(CustomDateTimeXmlAdapter.class)
    public ZonedDateTime getDateAppealMade() { return dateAppealMade; }

    public void setDateAppealMade(ZonedDateTime dateAppealMade) { this.dateAppealMade = dateAppealMade; }

    @XmlElement
    public Boolean getFtaReconsiderationEnclosed() { return ftaReconsiderationEnclosed; }

    public void setFtaReconsiderationEnclosed(Boolean ftaReconsiderationEnclosed) { this.ftaReconsiderationEnclosed = ftaReconsiderationEnclosed; }

    @XmlElement
    public Boolean isAdmissable() { return isAdmissable; }

    public void setAdmissable(Boolean isAdmissable) { this.isAdmissable = isAdmissable; }

    @XmlElement
    public Boolean isFurtherEvidenceRequired() { return isFurtherEvidenceRequired; }

    public void setFurtherEvidenceRequired(Boolean isFurtherEvidenceRequired) { this.isFurtherEvidenceRequired = isFurtherEvidenceRequired; }

    @Override
    public String toString() {
        return "Appeal{"
                + " caseCode='" + caseCode + '\''
                + ", dateOfDecision=" + dateOfDecision
                + ", originatingOffice='" + originatingOffice + '\''
                + ", dateAppealMade=" + dateAppealMade
                + ", ftaReconsiderationEnclosed=" + ftaReconsiderationEnclosed
                + ", isAdmissable=" + isAdmissable
                + ", isFurtherEvidenceRequired=" + isFurtherEvidenceRequired
                + '}';
    }
}
