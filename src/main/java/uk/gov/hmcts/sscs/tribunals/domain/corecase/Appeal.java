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

    public Appeal(String caseCode, String originatingOffice, ZonedDateTime dateOfDecision, ZonedDateTime dateAppealMade) {
        this.caseCode = caseCode;
        this.originatingOffice = originatingOffice;
        this.dateOfDecision = dateOfDecision;
        this.dateAppealMade = dateAppealMade;
    }

    @XmlTransient
    public String getCaseCode() { return caseCode; }

    @XmlElement(name = "caseCode", required = true)
    public String getCaseCodeWithSuffix() { return caseCode + "DD"; }

    public void setCaseCode(String caseCode) { this.caseCode = caseCode; }

    public String getOriginatingOffice() { return originatingOffice; }

    public void setOriginatingOffice(String originatingOffice) { this.originatingOffice = originatingOffice; }

    @XmlJavaTypeAdapter(CustomDateTimeXmlAdapter.class)
    public ZonedDateTime getDateOfDecision() {
        return dateOfDecision;
    }

    public void setDateOfDecision(ZonedDateTime dateOfDecision) {
        this.dateOfDecision = dateOfDecision;
    }

    @XmlJavaTypeAdapter(CustomDateTimeXmlAdapter.class)
    public ZonedDateTime getDateAppealMade() { return dateAppealMade; }

    public void setDateAppealMade(ZonedDateTime dateAppealMade) { this.dateAppealMade = dateAppealMade; }

    @Override
    public String toString() {
        return "Appeal{"
                + " caseCode='" + caseCode + '\''
                + ", dateOfDecision=" + dateOfDecision
                + ", originatingOffice='" + originatingOffice + '\''
                + ", dateAppealMade=" + dateAppealMade
                + '}';
    }
}
