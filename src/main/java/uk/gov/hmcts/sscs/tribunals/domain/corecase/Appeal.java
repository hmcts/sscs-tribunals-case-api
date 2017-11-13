package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import java.time.LocalDate;
import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import uk.gov.hmcts.sscs.service.xml.CustomDateTimeXmlAdapter;

public class Appeal {

    private Benefit benefit;

    private String originatingOffice;

    private LocalDate dateOfDecision;

    private LocalDate dateAppealMade;

    public Appeal(Benefit benefit, String originatingOffice, LocalDate dateOfDecision,
                  LocalDate dateAppealMade) {
        this.benefit = benefit;
        this.originatingOffice = originatingOffice;
        this.dateOfDecision = dateOfDecision;
        this.dateAppealMade = dateAppealMade;
    }

    public Appeal() {

    }

    @XmlTransient
    public Benefit getBenefit() {
        return benefit;
    }

    @XmlElement(name = "caseCode", required = true)
    public String getBenefitAsCaseCode() {
        return benefit.getCode() + "DD";
    }

    public void setBenefit(Benefit benefit) {
        this.benefit = benefit;
    }

    public String getOriginatingOffice() {
        return originatingOffice;
    }

    public void setOriginatingOffice(String originatingOffice) {
        this.originatingOffice = originatingOffice;
    }

    @XmlJavaTypeAdapter(CustomDateTimeXmlAdapter.class)
    public LocalDate getDateOfDecision() {
        return dateOfDecision;
    }

    public void setDateOfDecision(LocalDate dateOfDecision) {
        this.dateOfDecision = dateOfDecision;
    }

    @XmlJavaTypeAdapter(CustomDateTimeXmlAdapter.class)
    public LocalDate getDateAppealMade() {
        return dateAppealMade;
    }

    public void setDateAppealMade(LocalDate dateAppealMade) {
        this.dateAppealMade = dateAppealMade;
    }

    @Override
    public String toString() {
        return "Appeal{"
                + " benefit='" + benefit + '\''
                + ", dateOfDecision=" + dateOfDecision
                + ", originatingOffice='" + originatingOffice + '\''
                + ", dateAppealMade=" + dateAppealMade
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Appeal)) {
            return false;
        }
        Appeal appeal = (Appeal) o;
        return Objects.equals(benefit, appeal.benefit)
                && Objects.equals(originatingOffice, appeal.originatingOffice)
                && Objects.equals(dateOfDecision, appeal.dateOfDecision)
                && Objects.equals(dateAppealMade, appeal.dateAppealMade);
    }

    @Override
    public int hashCode() {
        return Objects.hash(benefit, originatingOffice, dateOfDecision, dateAppealMade);
    }
}
